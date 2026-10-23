/*
 * Copyright (C) 2026 Anilbeesetti.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.huanchengfly.tieba.post.core.media.state

import android.app.AppOpsManager
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Process
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.DisposableEffectResult
import androidx.compose.runtime.DisposableEffectScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.net.toUri
import androidx.core.util.Consumer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.ui.compose.state.PlayerStateObserver
import androidx.media3.ui.compose.state.PresentationState
import androidx.media3.ui.compose.state.observeState
import androidx.media3.ui.compose.material3.R as media3UiR

/**
 * https://github.com/anilbeesetti/nextplayer
 *
 * feature/player/src/main/java/dev/anilbeesetti/nextplayer/feature/player/state/PictureInPictureState.kt
 *
 * commit e4e3ae9 'Avoid redundant Picture-in-Picture parameter updates' on branch main.
 *
 * 0Ranko0p changes:
 *   1. Add seek back and forward action
 *   2. Migrate to Media3 [PlayerStateObserver]
 *   3. Use [androidx.media3.common.util.Util] to handle play, replay and pause action
 *   4. Use string and icon resource from Media3 UI Compose Material3
 *   5. Use [PresentationState.videoSizeDp] to calculate aspect ratio
 */

@Composable
fun rememberPictureInPictureState(
    player: Player,
    autoEnter: Boolean = true,
): PictureInPictureState {
    val activity = LocalActivity.current
    val pictureInPictureState = remember {
        PictureInPictureState(
            player = player,
            activity = activity as ComponentActivity,
            autoEnter = autoEnter,
        )
    }
    DisposableEffect(activity) { pictureInPictureState.handleListeners(this) }
    LaunchedEffect(player) { pictureInPictureState.observe() }
    return pictureInPictureState
}

@androidx.annotation.OptIn(UnstableApi::class)
@Stable
class PictureInPictureState(
    private val player: Player,
    private val activity: ComponentActivity,
    private val autoEnter: Boolean = true,
) {
    companion object {
        private const val PIP_INTENT_ACTION = "pip_action"
        private const val PIP_INTENT_ACTION_CODE = "pip_action_code"
        private const val PIP_ACTION_PLAY = 1
        private const val PIP_ACTION_PAUSE = 2
        private const val PIP_ACTION_NEXT = 3
        private const val PIP_ACTION_PREVIOUS = 4
        private const val PIP_ACTION_SEEK_BACK = 12
        private const val PIP_ACTION_SEEK_FROWARD = 13
    }

    val isPipSupported: Boolean = activity.isPipFeatureSupported

    val hasPipPermission: Boolean
        get() = if (isPipSupported) {
            val appOps = getSystemService(activity, AppOpsManager::class.java) ?: return true
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_PICTURE_IN_PICTURE, Process.myUid(), activity.packageName) == AppOpsManager.MODE_ALLOWED
        } else {
            true
        }

    var isInPictureInPictureMode: Boolean by mutableStateOf(false)
        private set

    private var lastAppliedSourceRectHint: Rect? = null
    private var lastAppliedAspectRatio: Rational? = null
    private var lastAppliedAutoEnterEnabled: Boolean? = null
    private var lastAppliedActionsPlaybackState: Boolean? = null

    private val pictureInPictureParamsBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        PictureInPictureParams.Builder().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setSeamlessResizeEnabled(true)
            }
        }
    } else {
        null
    }

    private val playerStateObserver: PlayerStateObserver =
        player.observeState(
            Player.EVENT_IS_PLAYING_CHANGED,
        ) {
            updateAutoEnterEnabled()
            updatePictureInPictureActions()
        }

    fun setVideoViewRect(rect: Rect, videoSize: IntSize) {
        if (pictureInPictureParamsBuilder == null) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (rect.width() <= 0 || rect.height() <= 0) return

        val sourceRectHint = Rect(rect)
        val aspectRatio = Rational(videoSize.width, videoSize.height)
            .takeIf { it.toFloat() in 0.5f..2.39f }

        var pictureInPictureParamsChanged = false

        if (aspectRatio != null && aspectRatio != lastAppliedAspectRatio) {
            pictureInPictureParamsBuilder.setAspectRatio(aspectRatio)
            lastAppliedAspectRatio = aspectRatio
            pictureInPictureParamsChanged = true
        }
        if (sourceRectHint != lastAppliedSourceRectHint) {
            pictureInPictureParamsBuilder.setSourceRectHint(sourceRectHint)
            lastAppliedSourceRectHint = sourceRectHint
            pictureInPictureParamsChanged = true
        }

        if (pictureInPictureParamsChanged) {
            applyPictureInPictureParams()
        }
    }

    fun enterPictureInPictureMode(): Boolean {
        if (pictureInPictureParamsBuilder == null) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        if (isInPictureInPictureMode) return false

        return activity.enterPictureInPictureMode(pictureInPictureParamsBuilder.build())
    }

    fun openPictureInPictureSettings() {
        val intent = Intent("android.settings.PICTURE_IN_PICTURE_SETTINGS").apply {
            data = "package:${activity.packageName}".toUri()
        }
        activity.startActivity(intent)
    }

    fun handleListeners(disposableEffectScope: DisposableEffectScope): DisposableEffectResult = with(disposableEffectScope) {
        val pipBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null || intent.action != PIP_INTENT_ACTION) return
                when (intent.getIntExtra(PIP_INTENT_ACTION_CODE, 0)) {
                    PIP_ACTION_PLAY -> Util.handlePlayButtonAction(player)
                    PIP_ACTION_PAUSE -> Util.handlePauseButtonAction(player)
                    PIP_ACTION_NEXT -> player.seekToNext()
                    PIP_ACTION_PREVIOUS -> player.seekToPrevious()
                    PIP_ACTION_SEEK_BACK -> player.let {
                        if (it.isCommandAvailable(Player.COMMAND_SEEK_BACK)) it.seekBack()
                    }
                    PIP_ACTION_SEEK_FROWARD -> player.let {
                        if (it.isCommandAvailable(Player.COMMAND_SEEK_FORWARD)) it.seekForward()
                    }
                }
            }
        }

        val pictureInPictureModeChangedListener: Consumer<PictureInPictureModeChangedInfo> = Consumer {
            updateIsInPictureInPictureMode(pipBroadcastReceiver)
        }

        updateIsInPictureInPictureMode(pipBroadcastReceiver)
        activity.addOnPictureInPictureModeChangedListener(pictureInPictureModeChangedListener)

        return onDispose {
            runCatching { activity.unregisterReceiver(pipBroadcastReceiver) }
            activity.removeOnPictureInPictureModeChangedListener(pictureInPictureModeChangedListener)
        }
    }

    suspend fun observe() {
        updateAutoEnterEnabled()
        updatePictureInPictureActions()
        playerStateObserver.observe()
    }

    private fun updateIsInPictureInPictureMode(pipBroadcastReceiver: BroadcastReceiver) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        isInPictureInPictureMode = activity.isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            ContextCompat.registerReceiver(
                activity,
                pipBroadcastReceiver,
                IntentFilter(PIP_INTENT_ACTION),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        } else {
            runCatching { activity.unregisterReceiver(pipBroadcastReceiver) }
        }
    }

    private fun updateAutoEnterEnabled() {
        if (pictureInPictureParamsBuilder == null) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

        val autoEnterEnabled = autoEnter && player.isPlaying
        if (autoEnterEnabled == lastAppliedAutoEnterEnabled) return

        pictureInPictureParamsBuilder.setAutoEnterEnabled(autoEnterEnabled)
        lastAppliedAutoEnterEnabled = autoEnterEnabled
        applyPictureInPictureParams()
    }

    private fun updatePictureInPictureActions() {
        if (pictureInPictureParamsBuilder == null) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val isPlaying = player.isPlaying
        if (isPlaying == lastAppliedActionsPlaybackState) return

        val actions = listOf(
            createPipAction(
                context = activity,
                title = activity.getString(media3UiR.string.seek_back_button),
                icon = player.defaultSeekBackIcon,
                actionCode = PIP_ACTION_SEEK_BACK,
            ),
            if (isPlaying) {
                createPipAction(
                    context = activity,
                    title = activity.getString(media3UiR.string.playpause_button_pause),
                    icon = media3UiR.drawable.media3_icon_pause,
                    actionCode = PIP_ACTION_PAUSE,
                )
            } else {
                createPipAction(
                    context = activity,
                    title = activity.getString(media3UiR.string.playpause_button_play),
                    icon = media3UiR.drawable.media3_icon_play,
                    actionCode = PIP_ACTION_PLAY,
                )
            },
            createPipAction(
                context = activity,
                title = activity.getString(media3UiR.string.seek_forward_button),
                icon = player.defaultSeekForwardIcon,
                actionCode = PIP_ACTION_SEEK_FROWARD,
            ),
        )

        pictureInPictureParamsBuilder.setActions(actions)
        lastAppliedActionsPlaybackState = isPlaying
        applyPictureInPictureParams()
    }

    private fun applyPictureInPictureParams() {
        if (pictureInPictureParamsBuilder == null) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        try {
            activity.setPictureInPictureParams(pictureInPictureParamsBuilder.build())
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createPipAction(
        context: Context,
        title: String,
        @DrawableRes icon: Int,
        actionCode: Int,
    ): RemoteAction {
        return RemoteAction(
            Icon.createWithResource(context, icon),
            title,
            title,
            PendingIntent.getBroadcast(
                context,
                actionCode,
                Intent(PIP_INTENT_ACTION).apply {
                    putExtra(PIP_INTENT_ACTION_CODE, actionCode)
                    setPackage(context.packageName)
                },
                PendingIntent.FLAG_IMMUTABLE,
            ),
        )
    }
}

private val Context.isPipFeatureSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

/** Note: Keep sync with [androidx.media3.ui.compose.material3.buttons.defaultSeekBackPainterIcon] */
private val Player.defaultSeekBackIcon: Int
    @DrawableRes get() = when (seekBackIncrement) {
        in 2500..7500 -> media3UiR.drawable.media3_icon_skip_back_5
        in 7500..12500 -> media3UiR.drawable.media3_icon_skip_back_10
        in 12500..20000 -> media3UiR.drawable.media3_icon_skip_back_15
        in 20000..40000 -> media3UiR.drawable.media3_icon_skip_back_30
        else -> media3UiR.drawable.media3_icon_skip_back
    }

/** Note: Keep sync with [androidx.media3.ui.compose.material3.buttons.defaultSeekForwardPainterIcon] */
private val Player.defaultSeekForwardIcon: Int
    @DrawableRes get() = when (seekForwardIncrement) {
        in 2500..7500 -> media3UiR.drawable.media3_icon_skip_forward_5
        in 7500..12500 -> media3UiR.drawable.media3_icon_skip_forward_10
        in 12500..20000 -> media3UiR.drawable.media3_icon_skip_forward_15
        in 20000..40000 -> media3UiR.drawable.media3_icon_skip_forward_30
        else -> media3UiR.drawable.media3_icon_skip_forward
    }
