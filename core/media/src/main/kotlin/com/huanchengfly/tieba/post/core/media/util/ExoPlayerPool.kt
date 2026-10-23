package com.huanchengfly.tieba.post.core.media.util

import android.content.Context
import android.os.Looper
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.Player.COMMAND_CHANGE_MEDIA_ITEMS
import androidx.media3.common.Player.COMMAND_PLAY_PAUSE
import androidx.media3.common.Player.COMMAND_SET_VIDEO_SURFACE
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.huanchengfly.tieba.post.core.media.util.MediaUtil.idHash
import com.huanchengfly.tieba.post.core.media.util.MediaUtil.tryRelease
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.cancellation.CancellationException

@androidx.annotation.OptIn(UnstableApi::class)
class ExoPlayerPool(
    private val maxSize: Int = DEFAULT_PLAYER_POOL_SIZE,
    builder: () -> ExoPlayer
) {
    private val playerFactory = DefaultPlayerFactory(builder)
    private val available = ArrayDeque<ExoPlayer>()
    private val inUse = LinkedHashSet<ExoPlayer>()

    private val availableSize: Int
        get() = available.size

    var disposed: Boolean = false
        private set

    private val signal = Channel<Unit>(capacity = Channel.CONFLATED)

    suspend fun acquirePlayer(): ExoPlayer {
        requireMainThread()
        var player: ExoPlayer? = acquirePlayerInternal()
        while (player == null && signal.receiveCatching().isSuccess && !disposed) {
            currentCoroutineContext().ensureActive()

            player = acquirePlayerInternal()?.also {
                Log.i(TAG, "onAcquirePlayer: Woke up, player ${it.idHash} acquired")
            }
        }
        return player ?: throw CancellationException("ExoPlayerPool disposed")
    }

    private fun acquirePlayerInternal(): ExoPlayer? {
        if (disposed) throw CancellationException("ExoPlayerPool disposed")

        var player = available.removeFirstOrNull()
        if (player != null) {
            inUse += player
            Log.d(TAG, "onAcquire: reused: true, player: ${player.idHash}, available: $availableSize, inUse: ${inUse.size}")
        } else if (inUse.size < maxSize) {
            player = playerFactory.createPlayer()
            inUse += player
            Log.i(TAG, "onAcquire: reused:false, player: ${player.idHash}, available: $availableSize, inUse: ${inUse.size}")
        } else {
            Log.w(TAG, "onAcquire: pool exhausted, maxSize: $maxSize, inUse: ${inUse.size}")
        }
        return player
    }

    fun releasePlayer(player: ExoPlayer) {
        requireMainThread()

        val isInUse = inUse.remove(player)
        // Stop the player and release into the pool for reusing
        if (player.isCommandAvailable(COMMAND_PLAY_PAUSE)) player.playWhenReady = false
        if (player.isCommandAvailable(COMMAND_CHANGE_MEDIA_ITEMS)) player.clearMediaItems()
        if (player.isCommandAvailable(COMMAND_SET_VIDEO_SURFACE)) player.clearVideoSurface()
        // Check is our player, not borrowed from elsewhere
        if (!isInUse && !available.contains(player)) {
            Log.e(TAG, "onReleasePlayer: Unknown player: ${player.idHash}, released directly")
            player.tryRelease()
            return
        }
        if (disposed) {
            Log.w(TAG, "onReleasePlayer: Pool disposed, releasing player: ${player.idHash}, remaining: ${inUse.size}")
            player.tryRelease()
            return
        }

        available.addLast(player)
        Log.i(TAG, "onReleasePlayer: Player: ${player.idHash}, available: ${available.size}, inUse: ${inUse.size}")
        signal.trySend(Unit)
    }

    fun dispose() {
        requireMainThread()
        if (disposed) return
        disposed = true

        if (inUse.isEmpty()) {
            Log.w(TAG, "onDispose: available: $availableSize")
        } else {
            Log.e(TAG, "onDispose: available: $availableSize, ${inUse.size} players still in use")
        }
        signal.close()

        // In-use players will be released when returned via releasePlayer because disposed == true.
        available.forEach { it.tryRelease() }
        available.clear()
    }

    companion object {
        private const val TAG = "ExoPlayerPool"

        const val DEFAULT_PLAYER_POOL_SIZE = 3

        private const val LOAD_CONTROL_MIN_BUFFER_MS = 1_500
        private const val LOAD_CONTROL_MAX_BUFFER_MS = 4_500
        private const val LOAD_CONTROL_BUFFER_FOR_PLAYBACK_MS = 500

        private class DefaultPlayerFactory(private val builder: () -> ExoPlayer) {
            private var playerCounter = 0

            fun createPlayer(): ExoPlayer {
                val player = builder()
                playerCounter++
                player.repeatMode = ExoPlayer.REPEAT_MODE_ONE
                return player
            }
        }

        private fun requireMainThread() {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                val message = androidx.media3.common.util.Util.formatInvariant(
                    """
                      Accessed on the wrong thread.
                      Current thread: '%s'
                      Expected thread: '%s'
                      See https://developer.android.com/guide/topics/media/issues/player-accessed-on-wrong-thread
                    """.trimIndent(),
                    Thread.currentThread().name,
                    Looper.getMainLooper().thread.name
                )
                throw IllegalStateException(message)
            }
        }

        fun defaultExoPlayerPool(
            context: Context,
            maxSize: Int = DEFAULT_PLAYER_POOL_SIZE
        ): ExoPlayerPool {
            val context = context.applicationContext
            val factory = DefaultMediaSourceFactory(MediaCache.Factory(context))
            return ExoPlayerPool(maxSize) {
                val trackSelector = DefaultTrackSelector(context).apply {
                    setParameters(
                        buildUponParameters().setRendererDisabled(C.TRACK_TYPE_AUDIO, true)
                    )
                }
                val loadControl = DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        LOAD_CONTROL_MIN_BUFFER_MS,
                        LOAD_CONTROL_MAX_BUFFER_MS,
                        LOAD_CONTROL_BUFFER_FOR_PLAYBACK_MS,
                        DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS / 2,
                    )
                    .setPrioritizeTimeOverSizeThresholds(true)
                    .build()

                ExoPlayer.Builder(context)
                    .setMediaSourceFactory(factory)
                    .setTrackSelector(trackSelector)
                    .setLoadControl(loadControl)
                    .build()
            }
        }
    }
}
