package com.huanchengfly.tieba.post.ui.page.photoview

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ShareCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MimeTypes
import androidx.recyclerview.widget.RecyclerView
import com.github.iielse.imageviewer.ImageViewerDialogFragment
import com.github.iielse.imageviewer.core.Components
import com.github.iielse.imageviewer.core.OverlayCustomizer
import com.github.iielse.imageviewer.core.Transformer
import com.github.iielse.imageviewer.core.ViewerCallback
import com.github.iielse.imageviewer.utils.Config
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.collectIn
import com.huanchengfly.tieba.post.components.viewer.SimpleImageLoader
import com.huanchengfly.tieba.post.models.PhotoViewData
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.utils.DisplayUtil.doOnApplyWindowInsets
import com.huanchengfly.tieba.post.utils.ImageUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PhotoViewActivity : AppCompatActivity(), OverlayCustomizer, ViewerCallback {

    private val viewModel: PhotoViewViewModel by viewModels()

    private val fragmentManager: FragmentManager by lazy { supportFragmentManager }

    private val windowInsetsController: WindowInsetsControllerCompat by lazy {
        WindowCompat.getInsetsController(window, window.decorView)
    }

    private var currentPage: Int = 0

    private lateinit var appbar: LinearLayout
    private lateinit var indicator: ProgressBar
    private var toolbar: Toolbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_view)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Load photos now!
        @Suppress("DEPRECATION")
        val data: PhotoViewData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_PHOTO_VIEW_DATA, PhotoViewData::class.java)!!
        } else {
            intent.getParcelableExtra(EXTRA_PHOTO_VIEW_DATA)!!
        }

        viewModel.initData(data)
        viewModel.state.collectIn(this) { uiState ->
            if (uiState.data.isEmpty()) return@collectIn
            if (fragmentManager.findFragmentById(android.R.id.content) != null) return@collectIn

            // Use window background
            Config.VIEWER_BACKGROUND_COLOR = Color.TRANSPARENT
            Config.SWIPE_DISMISS = false

            Components.initialize(
                imageLoader = SimpleImageLoader(this::onImageClicked, this::onProgress),
                dataProvider = viewModel,
                transformer = object : Transformer { /*** NO-OP ***/ }
            )
            Components.setViewerCallback(this)
            Components.setOverlayCustomizer(overlayCustomizer)

            supportFragmentManager.beginTransaction()
                .add(android.R.id.content, ImageViewerFragment())
                .commit()

            lifecycleScope.launch { // Wait Glide animation
                delay(300L)
                val indicator = findViewById<View>(android.R.id.progress) ?: return@launch
                (indicator.parent as ViewGroup).removeView(indicator)
            }
        }
    }

    /**
     * Setup appbar in overlay, it's a workaround
     * */
    private val overlayCustomizer: OverlayCustomizer = object : OverlayCustomizer {
        override fun provideView(parent: ViewGroup): View? {
            val view = layoutInflater.inflate(R.layout.overlay_photo_view, parent, false)
            appbar = view.findViewById(R.id.appbar)
            appbar.doOnApplyWindowInsets { insets ->
                updatePadding(top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top)
                return@doOnApplyWindowInsets true
            }

            indicator = view.findViewById(R.id.progress_indicator)
            toolbar = appbar.findViewById<Toolbar>(R.id.toolbar).apply {
                inflateMenu(R.menu.menu_photo_view)
                setNavigationIcon(R.drawable.ic_round_arrow_back)
                setNavigationOnClickListener { this@PhotoViewActivity.finish() }
                navigationIcon?.setTint(Color.WHITE)
                setOnMenuItemClickListener(this@PhotoViewActivity::onOptionsItemSelected)
            }
            return view
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_share -> onShareImage()

            R.id.menu_download -> ImageUtil.download(this, url = getCurrentItem()?.originUrl)

            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    /**
     * Hide or show system bar on image clicked
     * */
    @Suppress("unused")
    private fun onImageClicked(v: View) {
        if (appbar.isVisible) {
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            appbar.animate().alpha(0f).withEndAction {
                appbar.visibility = View.GONE
            }
        } else {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            appbar.visibility = View.VISIBLE
            appbar.animate().alphaBy(1f)
        }
    }

    private fun onShareImage() {
        val currentImg = getCurrentItem() ?: return
        toastShort(R.string.toast_preparing_share_pic)
        lifecycleScope.launch {
            val rec = ImageUtil.downloadForShare(applicationContext, currentImg.originUrl, null)
            if (rec.isFailure) {
                rec.exceptionOrNull()?.let { toastShort(it.getErrorMessage()) }
            } else {
                val uri = rec.getOrNull() ?: return@launch
                ShareCompat.IntentBuilder(this@PhotoViewActivity)
                    .setType(MimeTypes.IMAGE_JPEG)
                    .setStream(uri)
                    .setChooserTitle(getString(R.string.title_share_pic))
                    .startChooser()
            }
        }
    }

    private fun onProgress(data: PhotoViewItem) {
        if (data.overallIndex != getCurrentItem()?.overallIndex) return // Preloading item, ignore

        // Hide when progress is 100
        val progress = if (data.progress == 100) 0 else data.progress
        indicator.setProgress(progress, false)
        indicator.isVisible = progress > 0
    }

    override fun onPageSelected(position: Int, viewHolder: RecyclerView.ViewHolder) {
        currentPage = position
        toolbar?.let {
            val state = viewModel.state.value
            val currentItem = state.data[position]
            it.title = "${currentItem.overallIndex} / ${state.totalAmount}"
            onProgress(currentItem)
        }
    }

    private fun getCurrentItem(): PhotoViewItem? = viewModel.state.value.data.getOrNull(currentPage)

    companion object {
        const val EXTRA_PHOTO_VIEW_DATA = "photo_view_data"

        class ImageViewerFragment : ImageViewerDialogFragment() {

            /**
             * Suppress exit animation in super
             * */
            override fun onBackPressed() {
                requireActivity().finish()
            }
        }

        inline var View.isVisible: Boolean
            get() = visibility == View.VISIBLE
            set(value) {
                if (isVisible xor value) {
                    visibility = if (value) View.VISIBLE else View.GONE
                }
            }
    }
}