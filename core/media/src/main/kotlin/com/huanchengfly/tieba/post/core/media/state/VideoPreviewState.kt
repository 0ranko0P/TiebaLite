package com.huanchengfly.tieba.post.core.media.state

import android.util.Log
import androidx.collection.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import com.huanchengfly.tieba.post.core.media.util.ExoPlayerPool
import com.huanchengfly.tieba.post.core.media.util.MediaUtil
import com.huanchengfly.tieba.post.core.media.util.MediaUtil.idHash
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val LocalVideoPreviewState = staticCompositionLocalOf<VideoPreviewState?> { null }

@Composable
fun rememberVideoPreviewState(playerPool: ExoPlayerPool): VideoPreviewState {
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val state = remember(playerPool, coroutineScope) {
        VideoPreviewState(playerPool, coroutineScope)
    }

    DisposableEffect(lifecycleOwner, state) {
        lifecycleOwner.lifecycle.addObserver(state)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(state)
            state.onDispose()
        }
    }
    return state
}

class VideoPreviewState(
    private val playerPool: ExoPlayerPool,
    private val coroutineScope: CoroutineScope,
): DefaultLifecycleObserver {

    /** Holds current visible MediaID and Player */
    private val playerHolderMap = mutableMapOf<String, ExoPlayer>()

    /** Holds MediaID and playback progress */
    private val progressRecord = LruCache<String, Long>(maxSize = 30)

    var videoViewMediaId by mutableStateOf<String?>(null)
        private set

    val isInVideoViewMode: Boolean
        get() = videoViewMediaId != null

    /** Is video view Activity in the PictureInPicture mode */
    var isInPipMode by mutableStateOf(false)
        private set

    init {
        coroutineScope.launch {
            _videoViewMediaState.collect { (mediaId: String?, positionMs: Long) ->
                if (mediaId != null && positionMs != C.TIME_UNSET) {
                    progressRecord.put(mediaId, positionMs)
                    videoViewMediaId = null // Back from video view, clear now
                } else {
                    videoViewMediaId = mediaId
                }
            }
        }
        coroutineScope.launch {
            _pipModeState.collect { isInPipMode = it }
        }
    }

    suspend fun preparePreview(uri: String, mediaId: String): ExoPlayer {
        require(mediaId.isNotEmpty() && mediaId.isNotBlank())

        var player = playerHolderMap[mediaId]
        if (player != null) {
            Log.e(TAG, "onPreparePreview: Acquire before release! mediaId: $mediaId, player: ${player.idHash}")
            return player
        }

        player = playerPool.acquirePlayer()
        val mediaItem = MediaItem.Builder().setUri(uri).setMediaId(mediaId).build()
        try {
            player.setMediaItem(mediaItem)
            player.prepare()
            delay(100) // currentCoroutineContext().ensureActive()
        } catch (e: Throwable) {
            playerPool.releasePlayer(player)
            throw e
        }
        playerHolderMap[mediaId] = player
        return player
    }

    fun disposePreview(mediaId: String, player: ExoPlayer?) {
        Log.i(TAG, "onDisposePreview: Disposing player: ${player?.idHash}, mediaId $mediaId")
        playerHolderMap.remove(mediaId)?.takeIf { it !== player }?.let {
            playerPool.releasePlayer(it)
        }
        player?.let {
            pause(mediaId, player)
            playerPool.releasePlayer(player)
        }
    }

    /** Calls play for the given player and restore previous progress if possible */
    fun play(mediaId: String, player: ExoPlayer?) {
        if (player == null || isInVideoViewMode) return

        val positionMs = progressRecord[mediaId] ?: C.TIME_UNSET
        if (positionMs >= 0) {
            player.seekTo(positionMs)
        }
        player.playWhenReady = true
    }

    /** Pauses the given player and record playback position */
    fun pause(mediaId: String, player: ExoPlayer?) {
        player?.playWhenReady = false
        val positionMs = MediaUtil.getCurrentPositionMs(player)
        if (positionMs > 0) {
            progressRecord.put(mediaId, positionMs)
        }
    }

    private fun releaseAllPlayers() {
        playerHolderMap.values.forEach { player ->
            playerPool.releasePlayer(player)
        }
        playerHolderMap.clear()
    }

    fun onEnterVideoView(mediaId: String) {
        pause(mediaId, playerHolderMap[mediaId])
    }

    override fun onResume(owner: LifecycleOwner) {
        if (playerHolderMap.isEmpty()) return

        playerHolderMap.values.forEach { player ->
            Util.handlePlayButtonAction(player) // Do not restore progress, play directly
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        playerHolderMap.forEach { (mediaId, player) ->
            pause(mediaId, player)
        }
    }

    fun onDispose() {
        releaseAllPlayers()
        coroutineScope.cancel()
        progressRecord.evictAll()
    }

    companion object {
        private const val TAG = "VideoPreviewState"

        private val _pipModeState = MutableStateFlow(false)

        fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
            _pipModeState.update { isInPictureInPictureMode }
        }

        // Media ID, Playback position
        private val _videoViewMediaState = MutableStateFlow<Pair<String?, Long>>(null to C.TIME_UNSET)

        fun onVideoViewMediaChanged(mediaId: String?, positionMs: Long = C.TIME_UNSET) {
            _videoViewMediaState.update { mediaId to positionMs }
        }
    }
}