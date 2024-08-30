package com.huanchengfly.tieba.post.ui.page.photoview

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ShareCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MimeTypes
import com.github.iielse.imageviewer.ImageViewerDialogFragment
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.collectIn
import com.huanchengfly.tieba.post.components.glide.ProgressListener
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.utils.ImageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FullScreenImageViewerDialogFragment : ImageViewerDialogFragment(), ProgressListener {

    private val viewModel: PhotoViewViewModel by activityViewModels()

    private lateinit var tvIndex: TextView
    private lateinit var ivDownload: ImageView
    private lateinit var ivShare: ImageView
    private lateinit var indicator: LinearProgressIndicator

    @SuppressLint("DefaultLocale")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tvIndex = view.findViewById(R.id.tv_photo_index)
        ivShare = view.findViewById(R.id.iv_photo_share)
        ivDownload = view.findViewById(R.id.iv_photo_download)
        indicator = view.findViewById(R.id.progress_indicator)

        ivShare.setOnClickListener(this::onShareImageClicked)

        ivDownload.setOnClickListener {
            val currentImg = getCurrentItem() ?: return@setOnClickListener
            ImageUtil.download(requireActivity(), currentImg.originUrl, this@FullScreenImageViewerDialogFragment)
        }

        viewModel.currentPos.collectIn(this, Lifecycle.State.RESUMED) { position ->
            val totalAmount = viewModel.state.value.totalAmount
            tvIndex.text = String.format("%d / %d", position + 1, totalAmount)
        }
    }

    override fun setWindow(win: Window) {
        super.setWindow(win)
        val windowInsetsController = WindowCompat.getInsetsController(win, win.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun onShareImageClicked(v: View) {
        val context = v.context.applicationContext
        val currentImg = getCurrentItem() ?: return
        context.toastShort(R.string.toast_preparing_share_pic)
        lifecycleScope.launch {
            val rec = ImageUtil.downloadForShare(context, currentImg.originUrl, this@FullScreenImageViewerDialogFragment)
            if (rec.isFailure) {
                rec.exceptionOrNull()?.let { context.toastShort(it.getErrorMessage()) }
            } else {
                val uri = rec.getOrNull() ?: return@launch
                ShareCompat.IntentBuilder(requireActivity())
                    .setType(MimeTypes.IMAGE_JPEG)
                    .setStream(uri)
                    .setChooserTitle(getString(R.string.title_share_pic))
                    .startChooser()
            }
        }
    }

    override fun onProgress(progress: Int) {
        lifecycleScope.launch(Dispatchers.Main) {
            if (!indicator.isVisible) indicator.visibility = View.VISIBLE

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                indicator.setProgress(progress, false)
            } else {
                indicator.setProgressCompat(progress, false)
            }
            if (progress == 100) {
                indicator.visibility = View.GONE
                indicator.setProgress(0)
            }
        }
    }

    override fun onBackPressed() {
        //super.onBackPressed()
        requireActivity().finish()
    }

    private fun getCurrentItem(): PhotoViewItem? {
        val items = viewModel.state.value.data
        val currentPosition = viewModel.currentPos.value
        return if (currentPosition in items.indices) items[currentPosition] else null
    }
}
