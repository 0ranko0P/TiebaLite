package com.huanchengfly.tieba.post.ui.page.photoview

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.github.iielse.imageviewer.ImageViewerBuilder
import com.github.iielse.imageviewer.ImageViewerDialogFragment
import com.github.iielse.imageviewer.core.OverlayCustomizer
import com.github.iielse.imageviewer.core.Transformer
import com.github.iielse.imageviewer.utils.Config
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectIn
import com.huanchengfly.tieba.post.components.viewer.SimpleImageLoader
import com.huanchengfly.tieba.post.models.PhotoViewData
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PhotoViewActivity: AppCompatActivity() {
    private val viewModel: PhotoViewViewModel by viewModels()
    private var showed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_view)

        val data: PhotoViewData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_PHOTO_VIEW_DATA, PhotoViewData::class.java)!!
        } else {
            intent.getParcelableExtra(EXTRA_PHOTO_VIEW_DATA)!!
        }

        viewModel.initData(data)
        viewModel.state.collectIn(this) { uiState ->
            val items = uiState.data
            if (items.isEmpty() || showed) return@collectIn

            // Use window background
            Config.VIEWER_BACKGROUND_COLOR = Color.TRANSPARENT
            Config.SWIPE_DISMISS = false

            val builder = ImageViewerBuilder(
                context = this,
                imageLoader = SimpleImageLoader(),
                dataProvider = viewModel,
                transformer = object : Transformer { /*** NO-OP ***/ }
            )
            builder.setViewerFactory(object : ImageViewerDialogFragment.Factory() {
                override fun build() = FullScreenImageViewerDialogFragment()
            })
            builder.setOverlayCustomizer(object : OverlayCustomizer {
                override fun provideView(parent: ViewGroup): View? =
                    LayoutInflater.from(parent.context).inflate(R.layout.overlay_photo_view, parent, false)
            })
            builder.setViewerCallback(viewModel)
            builder.show()
            showed = true

            MainScope().launch { // Wait Glide animation
                delay(300L)
                val indicator: CircularProgressIndicator = findViewById(android.R.id.progress)
                (indicator.parent as ViewGroup).removeView(indicator)
            }
        }
    }

    companion object {
        const val EXTRA_PHOTO_VIEW_DATA = "photo_view_data"
    }
}