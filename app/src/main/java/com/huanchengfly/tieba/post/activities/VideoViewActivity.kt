package com.huanchengfly.tieba.post.activities

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.bumptech.glide.Glide
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.VideoInfo
import com.huanchengfly.tieba.post.arch.collectIn
import com.huanchengfly.tieba.post.components.BD_VIDEO_HOST
import com.huanchengfly.tieba.post.goToActivity
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.widgets.compose.video.OnFullScreenModeChangedListener
import com.huanchengfly.tieba.post.ui.widgets.compose.video.VideoPlayer
import com.huanchengfly.tieba.post.ui.widgets.compose.video.VideoPlayerController
import com.huanchengfly.tieba.post.ui.widgets.compose.video.VideoPlayerSource
import com.huanchengfly.tieba.post.ui.widgets.compose.video.retainVideoPlayerController
import com.huanchengfly.tieba.post.utils.ThemeUtil
import kotlinx.coroutines.flow.distinctUntilChangedBy
import java.util.Objects

class VideoViewActivity: ComponentActivity(), OnFullScreenModeChangedListener {

    private lateinit var mInsetsController: WindowInsetsControllerCompat

    private var videoPlayerController: VideoPlayerController? = null

    private var playOnRecreate = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(scrim = android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(scrim = android.graphics.Color.TRANSPARENT)
        )

        super.onCreate(savedInstanceState)
        playOnRecreate = savedInstanceState?.getBoolean(KEY_PLAY_ON_RECREATE, false) == true
        mInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        mInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        val data = intent.data ?: throw NullPointerException("No video provided!")
        val thumbnailUrl = intent.getStringExtra(EXTRA_THUMBNAIL)

        setContent {
            videoPlayerController = retainVideoPlayerController(
                source = VideoPlayerSource.Network(data.toString()),
                thumbnailUrl = thumbnailUrl,
                fullScreenModeChangedListener = this,
                playWhenReady = true,
            )

            TiebaLiteTheme(colorSchemeExt = ThemeUtil.colorState.value) {
                VideoPlayer(videoPlayerController = videoPlayerController!!)
            }

            LaunchedEffect(Unit) {
                if (playOnRecreate) {
                    videoPlayerController!!.play()
                }
                videoPlayerController!!.state
                    .distinctUntilChangedBy { Objects.hash(it.isPlaying, it.controlsVisible) }
                    .collectIn(this@VideoViewActivity) {
                        if (it.isPlaying) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            // Update SystemBars visibility
                            if (it.controlsVisible) {
                                mInsetsController.show(WindowInsetsCompat.Type.systemBars())
                            } else {
                                mInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                            }
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            // SystemBars always visible if video paused
                            mInsetsController.show(WindowInsetsCompat.Type.systemBars())
                        }
                    }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        playOnRecreate = videoPlayerController?.state?.value?.isPlaying == true
        videoPlayerController?.pause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_PLAY_ON_RECREATE, playOnRecreate)
        super.onSaveInstanceState(outState)
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
        private const val KEY_PLAY_ON_RECREATE = "com.huanchengfly.tieba.post.VideoViewActivity.PLAY_ON_RECREATE"

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