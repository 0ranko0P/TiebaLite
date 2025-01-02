package com.huanchengfly.tieba.post.activities

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.VideoInfo
import com.huanchengfly.tieba.post.arch.collectIn
import com.huanchengfly.tieba.post.components.BD_VIDEO_HOST
import com.huanchengfly.tieba.post.goToActivity
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.widgets.compose.video.OnFullScreenModeChangedListener
import com.huanchengfly.tieba.post.ui.widgets.compose.video.VideoPlayer
import com.huanchengfly.tieba.post.ui.widgets.compose.video.VideoPlayerController
import com.huanchengfly.tieba.post.ui.widgets.compose.video.VideoPlayerSource
import com.huanchengfly.tieba.post.ui.widgets.compose.video.rememberVideoPlayerController
import kotlinx.coroutines.flow.distinctUntilChangedBy

class VideoViewActivity: ComponentActivity(), OnFullScreenModeChangedListener {

    private val mInsetsController by lazy { WindowCompat.getInsetsController(window, window.decorView) }

    private var videoPlayerController: VideoPlayerController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val data = intent.data ?: throw NullPointerException("No video provided!")
        val thumbnailUrl = intent.getStringExtra(EXTRA_THUMBNAIL)

        setContent {
            videoPlayerController = rememberVideoPlayerController(
                source = VideoPlayerSource.Network(data.toString()),
                thumbnailUrl = thumbnailUrl,
                fullScreenModeChangedListener = this
            )
            VideoPlayer(
                videoPlayerController = videoPlayerController!!,
                modifier = Modifier.fillMaxSize(),
                backgroundColor = Color.Black
            )

            LaunchedEffect(Unit) {
                videoPlayerController!!.play()
                videoPlayerController!!.state
                    .distinctUntilChangedBy { it.isPlaying }
                    .collectIn(this@VideoViewActivity) {
                        if (it.isPlaying) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        videoPlayerController?.pause()
    }

    override fun onFullScreenModeChanged() {
        if (resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE) {
            mInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            mInsetsController.show(WindowInsetsCompat.Type.systemBars())
            this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    companion object {
        const val EXTRA_THUMBNAIL = "video_thumbnail"

        fun launch(context: Context, videoUrl: String, thumbnailUrl: String?) {
            val data = Uri.parse(videoUrl)

            // Check tb-video is unauthorized
            if (data.host == BD_VIDEO_HOST && videoUrl.endsWith(".mp4")) {
                context.toastShort(R.string.title_not_logged_in)
                return
            }

            // Free more memory now
            Glide.get(context).clearMemory()

            context.goToActivity<VideoViewActivity> {
                this.data = data
                thumbnailUrl?.let { putExtra(EXTRA_THUMBNAIL, it) }
            }
        }

        fun launch(context: Context, videoInfo: VideoInfo) {
            launch(context, videoInfo.videoUrl, videoInfo.thumbnailUrl)
        }
    }
}