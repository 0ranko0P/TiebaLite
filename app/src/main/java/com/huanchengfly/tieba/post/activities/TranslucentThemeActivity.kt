package com.huanchengfly.tieba.post.activities

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Save
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.UCropActivity.Companion.registerUCropResult
import com.huanchengfly.tieba.post.theme.DarkBlueColors
import com.huanchengfly.tieba.post.theme.DefaultColors
import com.huanchengfly.tieba.post.theme.Grey800
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.DisplayUtil
import com.huanchengfly.tieba.post.utils.PermissionUtils
import com.huanchengfly.tieba.post.utils.PickMediasRequest
import com.huanchengfly.tieba.post.utils.PickMediasResult
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.registerPickMediasLauncher
import com.huanchengfly.tieba.post.utils.requestPermission
import com.huanchengfly.tieba.post.utils.shouldUsePhotoPicker
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class TranslucentThemeActivity : AppCompatActivity(), RequestListener<Drawable> {

    private val vm: TranslucentThemeViewModel by viewModels()

    private var placeHolder: Drawable? = null

    private val mediaPickerActivityLauncher = registerPickMediasLauncher { result: PickMediasResult ->
        val sourceUri = result.uris.firstOrNull()?: return@registerPickMediasLauncher
        lifecycleScope.launch {
            delay(240L) // Wait exit animation of MediaPicker Activity

            // Launch UCropActivity now
            val uCrop = buildUCropOptions(sourceUri, vm.primaryColor.toArgb())
            ucropActivityResultLauncher.launch(uCrop)
        }
    }

    private val ucropActivityResultLauncher = registerUCropResult {
        val result: Result<Uri> = it ?: return@registerUCropResult // Canceled
        if (result.isSuccess) {
            vm.onNewWallpaperSelected(uri = result.getOrThrow())
        } else {
            val error = result.exceptionOrNull()?.message ?: getString(R.string.error_unknown)
            toastShort(error)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isDarkTheme = ThemeUtil.isNightMode()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).run {
            isAppearanceLightStatusBars = !isDarkTheme
            isAppearanceLightNavigationBars = !isDarkTheme
        }

        setContent {
             ProvideTheme(dark = isDarkTheme) {
                MyScaffold(
                    backgroundColor = MaterialTheme.colors.background,
                    topBar = actionBar
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        SideBySideWallpaper(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                            vm = vm,
                            placeHolder = { placeHolder },
                            listener = this@TranslucentThemeActivity
                        )

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = ExtendedTheme.colors.bottomBar,
                            contentColor = ExtendedTheme.colors.text,
                            elevation = 8.dp
                        ) {
                            TranslucentThemeContent(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .windowInsetsPadding(WindowInsets.navigationBars),
                                viewModel = vm,
                                onSelectWallpaper = this@TranslucentThemeActivity::launchImagePicker
                            )
                        }
                    }
                } // End of MyScaffold

                AnimatedVisibility(vm.savingWallpaper, enter = fadeIn(), exit = fadeOut()) {
                    LoadingOverlay(modifier = Modifier.clickable { /*** Click Blocker ***/ })
                }
            }
        }
    }

    // Do not use App Theme from DataStore
    @Composable
    private fun ProvideTheme(dark: Boolean, content: @Composable () -> Unit) {
        val colors = if (dark) {
            DarkBlueColors.copy(secondary = Color(0xFF303134))
        } else {
            val dividerColor = Color(0xFFF7FBFE)
            DefaultColors.copy(
                background = dividerColor,
                secondary = dividerColor,
                onSecondary = Grey800
            )
        }
        TiebaLiteTheme(colors, content = content)
    }

    private fun onSaveWallpaperClicked() {
        lifecycleScope.launch {
            val result: Result<Unit> = vm.saveWallpaper().await()
            if (result.isSuccess) {
                finish()
            } else {
                toastShort(result.exceptionOrNull()?.message ?: getString(R.string.error_unknown))
            }
        }
    }

    private val actionBar: @Composable () -> Unit = {
        TitleCentredToolbar(
            title = {
                Text(text = stringResource(id = R.string.title_theme_translucent))
            },
            navigationIcon = {
                BackNavigationIcon(onBackPressed = this@TranslucentThemeActivity::finish)
            },
            actions = {
                AnimatedVisibility(vm.configChanged) {
                    ActionItem(
                        icon = Icons.Rounded.Save,
                        contentDescription = stringResource(R.string.button_finish),
                        onClick = this@TranslucentThemeActivity::onSaveWallpaperClicked
                    )
                }
            }
        )
    }

    // Overlay to block user click while saving
    @Composable
    private fun LoadingOverlay(modifier: Modifier = Modifier) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background.copy(0.4f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.width(64.dp),
                color = vm.primaryColor,
                backgroundColor = vm.primaryColor.copy(0.4f),
            )
        }
    }

    private fun launchImagePicker() = askPermission {
        mediaPickerActivityLauncher.launch(
            PickMediasRequest(mediaType = PickMediasRequest.ImageOnly)
        )
    }

    override fun onResourceReady(resource: Drawable, model: Any, target: Target<Drawable>?, dataSource: DataSource, isFirstResource: Boolean): Boolean {
        if (resource is BitmapDrawable) {
            if (isFirstResource) {
                vm.onWallpaperDecoded(resource.bitmap)
                placeHolder = resource
                return true
            }
        } else {
            throw RuntimeException("Unrecognized resource type: ${resource::class.simpleName}")
        }
        return false
    }

    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean): Boolean {
        return false
    }

    /**
     * @return UCrop to launch [UCropActivity]
     * */
    private fun buildUCropOptions(sourceUri: Uri, @ColorInt accent: Int): UCrop {
        // Save cropped image to cache dir temporary
        val destUri = Uri.fromFile(File(cacheDir, "cropped_${sourceUri.hashCode()}.webp"))

        // Restrict image to screen aspect ratio
        val screen = DisplayUtil.getScreenPixels(this)
        val aspectRatio = screen.width / screen.height.toFloat()

        return UCrop.of(sourceUri, destUri)
            .withAspectRatio(aspectRatio, 1f)
            .withMaxResultSize(screen.width, screen.height)
            .withOptions(UCrop.Options().apply {
                setStatusBarColor(accent)
                setToolbarColor(accent)
                setToolbarWidgetColor(Color.White.toArgb())
                setActiveControlsWidgetColor(accent)
                setLogoColor(accent)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    setCompressionFormat(Bitmap.CompressFormat.WEBP_LOSSY)
                } else {
                    setCompressionFormat(Bitmap.CompressFormat.WEBP)
                }
                setCompressionQuality(99)
            })
    }

    private fun askPermission(granted: () -> Unit) {
        if (shouldUsePhotoPicker()) {
            granted()
        } else {
            requestPermission {
                unchecked = true
                permissions = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    listOf(
                        PermissionUtils.READ_EXTERNAL_STORAGE,
                        PermissionUtils.WRITE_EXTERNAL_STORAGE
                    )
                } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    listOf(PermissionUtils.READ_EXTERNAL_STORAGE)
                } else {
                    listOf(PermissionUtils.READ_MEDIA_IMAGES)
                }
                description = getString(R.string.tip_permission_storage)
                onGranted = granted
                onDenied = { toastShort(R.string.toast_no_permission_insert_photo) }
            }
        }
    }

    override fun onDestroy() {
        Glide.get(App.INSTANCE).clearMemory()
        super.onDestroy()
    }
}