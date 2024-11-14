package com.huanchengfly.tieba.post.ui.page.photoview

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ShareCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.collectIn
import com.huanchengfly.tieba.post.components.glide.ProgressListener
import com.huanchengfly.tieba.post.components.viewer.SimpleImageLoader
import com.huanchengfly.tieba.post.models.PhotoViewData
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.utils.ImageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PhotoViewActivity : AppCompatActivity(), ProgressListener, OverlayCustomizer, ViewerCallback {

    private val viewModel: PhotoViewViewModel by viewModels()

    private val fragmentManager: FragmentManager by lazy { supportFragmentManager }

    private val windowInsetsController: WindowInsetsControllerCompat by lazy {
        WindowCompat.getInsetsController(window, window.decorView)
    }
    private var isStatusBarVisible = true

    private var currentPage: Int = 0

    private lateinit var appbar: AppBarLayout
    private lateinit var indicator: LinearProgressIndicator
    private var toolbar: Toolbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_view)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Note: buggy ViewCompat#setOnApplyWindowInsetsListener on old Android devices
            window.decorView.setOnApplyWindowInsetsListener { _, insets ->
                isStatusBarVisible = insets.isVisible(WindowInsets.Type.statusBars())
                return@setOnApplyWindowInsetsListener insets
            }
        } else {
            window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
                isStatusBarVisible = visibility.and(View.SYSTEM_UI_FLAG_FULLSCREEN) == 0
            }
        }

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
                imageLoader = SimpleImageLoader(this::onImageClicked),
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
    @Suppress("DEPRECATION")
    private val overlayCustomizer = object : OverlayCustomizer {
        override fun provideView(parent: ViewGroup): View? {
            val view = layoutInflater.inflate(R.layout.overlay_photo_view, parent, false)
            appbar = view.findViewById(R.id.appbar)
            indicator = view.findViewById(R.id.progress_indicator)
            toolbar = appbar.findViewById<Toolbar>(R.id.toolbar).apply {
                inflateMenu(R.menu.menu_photo_view)
                setNavigationIcon(R.drawable.ic_round_arrow_back)
                setNavigationOnClickListener { this@PhotoViewActivity.finish() }
                navigationIcon?.setTint(resources.getColor(android.R.color.white))
                setOnMenuItemClickListener(this@PhotoViewActivity::onOptionsItemSelected)
            }
            return view
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_share -> onShareImage()

            R.id.menu_download -> ImageUtil.download(this, getCurrentItem()?.originUrl, this)

            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    /**
     * Hide or show system bar on image clicked
     * */
    @Suppress("unused")
    private fun onImageClicked(v: View) {
        if (isStatusBarVisible) {
            appbar.animate().alpha(0f).withEndAction {
                appbar.visibility = View.GONE
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
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
            val rec = ImageUtil.downloadForShare(applicationContext, currentImg.originUrl, this@PhotoViewActivity)
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

    override fun onProgress(progress: Int) {
        lifecycleScope.launch(Dispatchers.Main) {
            // Hide when progress is 100
            val visibility = if (progress == 100) View.GONE else View.VISIBLE
            val finalProgress = if (progress == 100) 0 else progress

            indicator.setProgress(finalProgress, false)
            if (indicator.visibility != visibility) {
                indicator.visibility = visibility
            }
        }
    }

    @SuppressLint("DefaultLocale")
    override fun onPageSelected(position: Int, viewHolder: RecyclerView.ViewHolder) {
        currentPage = position
        val totalAmount = viewModel.state.value.totalAmount
        toolbar?.title = String.format("%d / %d", position + 1, totalAmount)
    }

    private fun getCurrentItem(): PhotoViewItem? {
        val items = viewModel.state.value.data
        return if (currentPage in items.indices) items[currentPage] else null
    }

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
    }
}