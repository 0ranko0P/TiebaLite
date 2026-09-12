package com.huanchengfly.tieba.post.ui.page.photoview

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MimeTypes
import androidx.recyclerview.widget.RecyclerView
import coil3.imageLoader
import com.github.iielse.imageviewer.ImageViewerDialogFragment
import com.github.iielse.imageviewer.core.Components
import com.github.iielse.imageviewer.core.OverlayCustomizer
import com.github.iielse.imageviewer.core.Transformer
import com.github.iielse.imageviewer.core.ViewerCallback
import com.github.iielse.imageviewer.utils.Config
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectIn
import com.huanchengfly.tieba.post.components.viewer.SimpleImageLoader
import com.huanchengfly.tieba.post.core.network.exception.getErrorMessage
import com.huanchengfly.tieba.post.goToActivityDebounced
import com.huanchengfly.tieba.post.models.PhotoViewData
import com.huanchengfly.tieba.post.models.PicItem
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.utils.DisplayUtil.doOnApplyWindowInsets
import com.huanchengfly.tieba.post.utils.ImageUtil
import com.huanchengfly.tieba.post.utils.extension.getParcelableExtraCompat
import com.huanchengfly.tieba.post.utils.extension.toShareIntent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PhotoViewActivity : AppCompatActivity(), OverlayCustomizer, ViewerCallback {

    private val viewModel: PhotoViewViewModel by viewModels()

    private val fragmentManager: FragmentManager by lazy { supportFragmentManager }

    private val windowInsetsController: WindowInsetsControllerCompat by lazy {
        WindowCompat.getInsetsController(window, window.decorView)
    }

    private var currentPage: Int = 0

    private lateinit var appbar: LinearLayout
    private var toolbar: Toolbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_view)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val data: PhotoViewData = intent.getParcelableExtraCompat(EXTRA_PHOTO_VIEW_DATA)!!

        viewModel.initData(data)
        viewModel.state.collectIn(this) { uiState ->
            when {
                uiState.error != null -> {
                    Toast.makeText(application, uiState.error.getErrorMessage(), Toast.LENGTH_LONG).show()
                    finish()
                    return@collectIn
                }

                uiState.data.isEmpty() -> return@collectIn

                fragmentManager.findFragmentById(android.R.id.content) != null -> return@collectIn
            }

            // Use window background
            Config.VIEWER_BACKGROUND_COLOR = Color.TRANSPARENT
            Config.SWIPE_DISMISS = false

            if (Components.working) finish()
            Components.initialize(
                imageLoader = SimpleImageLoader(
                    lifecycle = lifecycle,
                    loader = imageLoader,
                    onClick = this::onImageClicked,
                ),
                dataProvider = viewModel,
                transformer = object : Transformer { /*** NO-OP ***/ }
            )
            Components.setViewerCallback(this)
            Components.setOverlayCustomizer(overlayCustomizer)

            supportFragmentManager.beginTransaction()
                .add(android.R.id.content, ImageViewerFragment())
                .commit()

            val indicator = findViewById<View>(android.R.id.progress) ?: return@collectIn
            (indicator.parent as ViewGroup).removeView(indicator)
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
                val sysBar = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                if (sysBar.top != 0) {
                    updatePadding(left = sysBar.left, top = sysBar.top, right = sysBar.right)
                }
                return@doOnApplyWindowInsets true
            }

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
            ImageUtil.downloadForShare(applicationContext, currentImg.originUrl)
                .onSuccess {
                    val intent = it.toShareIntent(this@PhotoViewActivity, MimeTypes.IMAGE_JPEG, getString(R.string.title_share_pic))
                    runCatching { startActivity(intent) }
                }
                .onFailure {
                    toastShort(it.getErrorMessage())
                }
        }
    }

    override fun onPageSelected(position: Int, viewHolder: RecyclerView.ViewHolder) {
        currentPage = position
        toolbar?.let {
            val state = viewModel.state.value
            val currentItem = state.data[position]
            it.title = "${currentItem.overallIndex} / ${state.totalAmount}"
        }
    }

    private fun getCurrentItem(): PhotoViewItem? = viewModel.state.value.data.getOrNull(currentPage)

    companion object {

        /**
         * Intent Extra: [PhotoViewData] data.
         *
         * @since 4.0.0 Dev 12
         * */
        const val EXTRA_PHOTO_VIEW_DATA = "photo_view_data"

        fun launch(context: Context, data: PhotoViewData) {
            context.goToActivityDebounced<PhotoViewActivity> {
                putExtra(EXTRA_PHOTO_VIEW_DATA, data)
            }
        }

        fun launchSinglePhoto(context: Context, url: String) {
            if (url.isNotEmpty() && url.isNotBlank()) {
                val picItem = PicItem(picId = ImageUtil.getPicId(url), picIndex = 1, url)
                launch(context, data = PhotoViewData(data = null, picItems = listOf(picItem)))
            } else {
                context.toastShort(R.string.desc_image_empty_url)
            }
        }

        class ImageViewerFragment : ImageViewerDialogFragment() {

            /**
             * Suppress exit animation in super
             * */
            override fun onBackPressed() {
                requireActivity().finish()
                Components.release() // Do not wait onDestroyView, release it now
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