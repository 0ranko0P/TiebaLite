package com.huanchengfly.tieba.post.ui.page.photoview

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ShareCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.media3.common.MimeTypes
import com.github.iielse.imageviewer.ImageViewerDialogFragment
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectIn
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.utils.ImageUtil
import com.huanchengfly.tieba.post.utils.download

class FullScreenImageViewerDialogFragment : ImageViewerDialogFragment() {

    private val viewModel: PhotoViewViewModel by activityViewModels()

    private lateinit var tvIndex: TextView
    private lateinit var ivDownload: ImageView
    private lateinit var ivShare: ImageView

    @SuppressLint("DefaultLocale")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tvIndex = view.findViewById(R.id.tv_photo_index)
        ivShare = view.findViewById(R.id.iv_photo_share)
        ivDownload = view.findViewById(R.id.iv_photo_download)

        ivShare.setOnClickListener {
            val context = requireActivity()
            context.toastShort(R.string.toast_preparing_share_pic)
            val currentImg = getCurrentItem() ?: return@setOnClickListener
            ImageUtil.download(context, currentImg.originUrl, true) { uri: Uri ->
                runCatching {
                    ShareCompat.IntentBuilder(context)
                        .setType(MimeTypes.IMAGE_JPEG)
                        .setStream(uri)
                        .setChooserTitle(getString(R.string.title_share_pic))
                        .startChooser()
                }
            }
        }

        ivDownload.setOnClickListener {
            val currentImg = getCurrentItem() ?: return@setOnClickListener
            ImageUtil.download(requireActivity(), currentImg.originUrl)
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
