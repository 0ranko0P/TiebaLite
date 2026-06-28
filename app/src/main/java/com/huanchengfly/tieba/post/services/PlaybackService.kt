package com.huanchengfly.tieba.post.services

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.widgets.compose.video.PlayerSessionRegistry
import com.huanchengfly.tieba.post.ui.widgets.compose.video.assertMainThread
import com.huanchengfly.tieba.post.utils.NotificationUtils

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private var foreground: Boolean = false
    private var providerSet: Boolean = false

    companion object {
        private const val CHANNEL = "media_playback"
        private const val NOTICE_ID = 1001
        private const val NO_GROUP = -1L
        const val EXTRA_GROUP_ID = "sync_group_id"

        @Volatile
        private var active: PlaybackBind? = null

        @Volatile
        private var channelReady: Boolean = false

        fun isActive(): Boolean = active != null

        fun isActiveGroup(syncGroupId: Long): Boolean = active?.syncGroupId == syncGroupId

        fun isActivePlayer(player: Player): Boolean = active?.player === player

        fun start(
            context: Context,
            syncGroupId: Long,
        ) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, PlaybackService::class.java).apply {
                    putExtra(EXTRA_GROUP_ID, syncGroupId)
                },
            )
        }

        fun stopFor(
            context: Context,
            player: Player,
        ) {
            val active = isActivePlayer(player)
            if (active) context.stopService(Intent(context, PlaybackService::class.java))
        }
    }

    private data class PlaybackBind(
        val syncGroupId: Long,
        val player: Player,
    )

    /** 播放器进入 IDLE 且无其他播放时自动停止服务 */
    private val playerEvents =
        object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_IDLE) {
                    mainHandler.post {
                        if (!hasActivePlayback()) pauseActivePlayerAndStopSelf()
                    }
                }
            }
        }

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        startPlaceholder()
        handleStart(intent)
        return super.onStartCommand(intent, flags, startId)
    }

    private fun handleStart(intent: Intent?) {
        assertMainThread()
        if (intent?.hasExtra(EXTRA_GROUP_ID) != true) {
            stopActiveAndStopSelf()
            return
        }

        val syncGroupId = intent.getLongExtra(EXTRA_GROUP_ID, NO_GROUP)
        if (syncGroupId == NO_GROUP) {
            stopActiveAndStopSelf()
            return
        }

        val player =
            PlayerSessionRegistry.player(syncGroupId)
                ?: run {
                    handleMissingPlayer(syncGroupId)
                    return
                }

        bind(syncGroupId, player)
    }

    private fun handleMissingPlayer(syncGroupId: Long) {
        val current = active
        when {
            current == null -> stopNow()
            current.syncGroupId == syncGroupId -> stopActiveAndStopSelf()
            isActiveBindValid(current) -> stopPlaceholder()
            else -> stopActiveAndStopSelf()
        }
    }

    private fun isActiveBindValid(bind: PlaybackBind): Boolean = PlayerSessionRegistry.player(bind.syncGroupId) === bind.player

    /** 绑定播放器后替换占位前台通知；切换同步组时释放旧孤立会话 */
    private fun bind(
        syncGroupId: Long,
        player: Player,
    ) {
        assertMainThread()
        val current = active
        if (current?.syncGroupId == syncGroupId && current.player === player && session != null) {
            stopPlaceholder()
            return
        }

        val oldGroup = current?.syncGroupId
        ensureProvider()

        active?.player?.removeListener(playerEvents)
        session?.release()

        active = PlaybackBind(syncGroupId, player)
        player.addListener(playerEvents)
        session =
            MediaSession
                .Builder(this, notificationPlayer(player))
                .setCallback(MediaCallback())
                .build()
        stopPlaceholder()

        if (oldGroup != null && oldGroup != syncGroupId) releaseIdleGroup(oldGroup)
    }

    private fun stopNow() {
        stopPlaceholder()
        stopSelf()
    }

    private fun stopActiveAndStopSelf() {
        assertMainThread()
        val syncGroupId = clearActive()
        session?.release()
        session = null
        if (syncGroupId != NO_GROUP) releaseIdleGroup(syncGroupId)
        stopNow()
    }

    private fun hasActivePlayback(): Boolean {
        val player = active?.player ?: return false
        return player.isPlaying || player.playbackState == Player.STATE_BUFFERING
    }

    private fun pauseActivePlayerAndStopSelf() {
        assertMainThread()
        active?.player?.pause()
        stopActiveAndStopSelf()
    }

    private fun ensureProvider() {
        if (providerSet) return
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider
                .Builder(this)
                .setChannelId(CHANNEL)
                .setChannelName(R.string.notification_channel_playback)
                .build(),
        )
        providerSet = true
    }

    private fun stopPlaceholder() {
        if (!foreground) return
        stopForeground(STOP_FOREGROUND_REMOVE)
        foreground = false
    }

    private fun releaseIdleGroup(syncGroupId: Long) {
        assertMainThread()
        PlayerSessionRegistry.releaseIdle(syncGroupId)
    }

    private fun clearActive(): Long {
        assertMainThread()
        val syncGroupId = active?.syncGroupId ?: NO_GROUP
        active?.player?.removeListener(playerEvents)
        active = null
        return syncGroupId
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (hasActivePlayback()) pauseActivePlayerAndStopSelf()
    }

    override fun onDestroy() {
        val syncGroupId = clearActive()
        foreground = false
        session?.release()
        session = null
        if (syncGroupId != NO_GROUP) releaseIdleGroup(syncGroupId)
        super.onDestroy()
    }

    /** 占位通知保证 startForegroundService 后及时进入前台，避免系统杀死服务 */
    private fun startPlaceholder() {
        if (foreground) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !channelReady) {
            NotificationUtils.createChannel(
                channelId = CHANNEL,
                name = getString(R.string.notification_channel_playback),
                importance = NotificationManagerCompat.IMPORTANCE_LOW,
            )
            channelReady = true
        }
        val notification =
            NotificationCompat
                .Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_round_drafts)
                .setContentTitle(getString(R.string.notification_channel_playback))
                .setContentText(getString(R.string.app_name))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build()
        val serviceType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            }
        ServiceCompat.startForeground(this, NOTICE_ID, notification, serviceType)
        foreground = true
    }

    private fun notificationPlayer(player: Player): Player =
        object : ForwardingPlayer(player) {
            override fun getAvailableCommands(): Player.Commands =
                super
                    .getAvailableCommands()
                    .buildUpon()
                    .removeTimelineCommands()
                    .build()

            override fun isCommandAvailable(command: Int): Boolean = !isTimelineCommand(command) && super.isCommandAvailable(command)
        }

    private val timelineCommands =
        intArrayOf(
            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
        )

    private fun Player.Commands.Builder.removeTimelineCommands(): Player.Commands.Builder =
        apply {
            timelineCommands.forEach { remove(it) }
        }

    private fun isTimelineCommand(command: Int): Boolean = command in timelineCommands

    private inner class MediaCallback : MediaSession.Callback
}
