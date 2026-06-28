package com.huanchengfly.tieba.post.ui.widgets.compose.video

import android.text.format.Formatter
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.components.MediaCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 视频信息展示项 */
data class VideoInfo(
    val resolution: String,
    val duration: String,
    val fileSize: String,
    val videoUrl: String,
)

/** 播放器信息快照：缓存视频尺寸和时长 */
private data class VideoSnapshot(
    val width: Int = 0,
    val height: Int = 0,
    val durationMs: Long = 0L,
)

/** 从 ExoPlayer 读取当前视频尺寸和总时长 */
@OptIn(UnstableApi::class)
private fun captureVideoSnapshot(player: ExoPlayer): VideoSnapshot {
    val format = player.videoFormat
    return VideoSnapshot(
        width = format?.width ?: 0,
        height = format?.height ?: 0,
        durationMs = player.duration.takeIf { it > 0 } ?: 0L,
    )
}

/** 监听播放器状态并更新视频信息，同时异步读取文件大小 */
@Composable
@OptIn(UnstableApi::class)
internal fun rememberVideoInfo(
    player: PlayerHandle,
    exoPlayer: ExoPlayer,
): VideoInfo {
    val infoSnapshot by
        produceState(initialValue = VideoSnapshot(), exoPlayer) {
            val listener =
                object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) value = captureVideoSnapshot(exoPlayer)
                    }

                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                        value =
                            value.copy(
                                width = videoSize.width,
                                height = videoSize.height,
                            )
                    }
                }

            exoPlayer.addListener(listener)
            value = captureVideoSnapshot(exoPlayer)
            awaitDispose { exoPlayer.removeListener(listener) }
        }
    val context = LocalContext.current
    val currentUrl = player.sourceUrl()
    val upstreamContentLengthBytes by player.contentLengthBytes.collectAsStateWithLifecycle()
    val appContext = remember(context) { context.applicationContext }
    val metadataContentLengthBytes by produceState(initialValue = -1L, currentUrl) {
        value =
            if (currentUrl != null) {
                withContext(Dispatchers.IO) {
                    MediaCache.contentLength(appContext, currentUrl)
                }
            } else {
                -1L
            }
    }
    val contentLengthBytes = metadataContentLengthBytes.takeIf { it > 0 } ?: upstreamContentLengthBytes
    val unknownText = stringResource(R.string.unknown)
    val resolution =
        if (infoSnapshot.width > 0 && infoSnapshot.height > 0) {
            "${infoSnapshot.width} × ${infoSnapshot.height}"
        } else {
            unknownText
        }
    val duration =
        infoSnapshot.durationMs.let {
            if (it > 0) {
                getDurationString(it)
            } else {
                unknownText
            }
        }
    val fileSize =
        if (contentLengthBytes > 0) {
            Formatter.formatFileSize(context, contentLengthBytes)
        } else {
            unknownText
        }
    val videoUrl = currentUrl ?: unknownText

    return VideoInfo(resolution, duration, fileSize, videoUrl)
}
