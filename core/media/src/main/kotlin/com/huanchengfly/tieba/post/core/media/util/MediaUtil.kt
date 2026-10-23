@file:Suppress("NOTHING_TO_INLINE")

package com.huanchengfly.tieba.post.core.media.util

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource

/** Extended [androidx.media3.common.util.Util] */
@androidx.annotation.OptIn(UnstableApi::class)
object MediaUtil {

    /** Similar to [androidx.media3.common.util.Util.handlePauseButtonAction] */
    fun handleSeekToAction(player: Player?, positionMs: Long): Boolean {
        if (player?.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM) == true) {
            player.seekTo(positionMs)
            return true
        }
        return false
    }

    fun getCurrentPositionMs(player: Player?): Long {
        return if (player?.isCommandAvailable(Player.COMMAND_GET_CURRENT_MEDIA_ITEM) == true) {
            player.currentPosition
        } else {
            C.TIME_UNSET
        }
    }

    fun ExoPlayer.initialize(context: Context, mediaItem: MediaItem) {
        val factory = ProgressiveMediaSource.Factory(MediaCache.Factory(context))
        setMediaSource(factory.createMediaSource(mediaItem))
        prepare()
    }

    inline fun ExoPlayer.tryRelease() {
        if (!isReleased) release()
    }

    inline val Player.idHash: Int
        get() = System.identityHashCode(this)
}