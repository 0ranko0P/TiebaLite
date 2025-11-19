package com.huanchengfly.tieba.post.activities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.viewModels
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.huanchengfly.tieba.post.LocalWindowAdaptiveInfo
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.TranslucentThemeViewModel.Companion.CROP_FILE_PREFIX
import com.huanchengfly.tieba.post.activities.UCropActivity.Companion.registerUCropResult
import com.huanchengfly.tieba.post.arch.unsafeLazy
import com.huanchengfly.tieba.post.theme.DefaultColors
import com.huanchengfly.tieba.post.theme.DefaultDarkColors
import com.huanchengfly.tieba.post.theme.ExtendedColorScheme
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.theme.isTranslucent
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.isLooseWindowWidth
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultFabEnterTransition
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultFabExitTransition
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.AnyPopDialogProperties
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.DirectionState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.utils.DisplayUtil
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class TranslucentThemeActivity : AppCompatActivity() {

    private val windowInsetsController by unsafeLazy {
        WindowCompat.getInsetsController(window, window.decorView)
    }

    private val vm: TranslucentThemeViewModel by viewModels()

    private var placeHolder: Drawable? = null

    private val mediaPickerActivityLauncher = registerForActivityResult(PickVisualMedia()) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            delay(240L) // Wait exit animation of MediaPicker Activity

            val primaryColor = vm.uiState.first().primaryColor
            val uCrop = buildUCropOptions(uri, primaryColor.toArgb())
            ucropActivityLauncher.launch(uCrop)
        }
    }

    // Launch UCropActivity for result (cropped theme wallpaper)
    private val ucropActivityLauncher = registerUCropResult { result ->
        result?.run { // null when RESULT_CANCELED, do nothing
            onSuccess { uri ->
                vm.onNewWallpaperSelected(uri)
            }
            onFailure { e ->
                toastShort(e.message ?: getString(R.string.error_unknown))
            }
        }
    }

    private val glideWallpaperListener = object : RequestListener<Drawable> {
        override fun onResourceReady(resource: Drawable, model: Any, target: Target<Drawable>?, dataSource: DataSource, isFirstResource: Boolean): Boolean {
            if (isFirstResource) {
                val res = resource as? BitmapDrawable ?: throw RuntimeException("Unrecognized res ${resource::class.simpleName}")
                vm.onWallpaperDecoded(bitmap = res.bitmap)
                placeHolder = res
            }
            return false
        }

        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean) = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val colorSchemeExt = currentThemeNoTrans(isSystemInDarkTheme())
            LaunchedEffect(colorSchemeExt) {
                windowInsetsController.run {
                    isAppearanceLightStatusBars = ThemeUtil.isStatusBarFontDark(colorSchemeExt.colorScheme)
                    isAppearanceLightNavigationBars = ThemeUtil.isNavigationBarFontDark(colorSchemeExt.colorScheme)
                }
            }

            TiebaLiteTheme(colorSchemeExt = colorSchemeExt) {
                val savingDialogState = rememberDialogState()

                Scaffold(
                    topBar = {
                        TitleCentredToolbar(
                            title = stringResource(id = R.string.activity_translucent),
                            navigationIcon = { BackNavigationIcon(onBackPressed = ::finish) },
                            actions = {
                                val configChanged by vm.configChanged.collectAsStateWithLifecycle()
                                AnimatedVisibility(
                                    visible = configChanged,
                                    enter = DefaultFabEnterTransition,
                                    exit = DefaultFabExitTransition
                                ) {
                                    ActionItem(
                                        icon = Icons.Rounded.Save,
                                        contentDescription = stringResource(R.string.button_save_profile)
                                    ) {
                                        savingDialogState.show()
                                        onSavingTheme()
                                    }
                                }
                            }
                        )
                    },
                ) { paddingValues ->
                    val windowSize = LocalWindowAdaptiveInfo.current.windowSizeClass

                    val contents = remember(windowSize) {
                        movableContentOf<Modifier> {
                            val state by vm.uiState.collectAsStateWithLifecycle()
                            SideBySideWallpaper(
                                modifier = it.padding(16.dp),
                                wallpaper = state.wallpaper,
                                alpha = state.alpha,
                                primary = state.primaryColor,
                                isDarkTheme = state.isDarkTheme,
                                transformation = state.wallpaperTransformation,
                                placeHolder = { placeHolder },
                                listener = glideWallpaperListener
                            )

                            Surface(
                                modifier = it.verticalScroll(rememberScrollState()),
                                tonalElevation = 6.dp
                            ) {
                                TranslucentThemeContent(
                                    modifier = Modifier.padding(16.dp),
                                    viewModel = vm,
                                    state = state,
                                    onSelectWallpaper = {
                                        mediaPickerActivityLauncher.launch(
                                            PickVisualMediaRequest(PickVisualMedia.ImageOnly)
                                        )
                                    }
                                )
                            }
                        }
                    }

                    if (windowSize.isLooseWindowWidth()) {
                        Row(modifier = Modifier.padding(paddingValues)) {
                            contents(Modifier
                                .weight(1.0f)
                                .fillMaxHeight())
                        }
                    } else {
                        Column(modifier = Modifier.padding(paddingValues)) {
                            contents(Modifier
                                .weight(1.0f)
                                .fillMaxWidth())
                        }
                    }
                }

                SavingDialog(dialogState = savingDialogState)
            }
        }
    }

    private fun onSavingTheme() = lifecycleScope.launch {
        vm.saveWallpaper()
            .await()
            .onSuccess {
                setResult(RESULT_OK)
                finish()
            }
            .onFailure { toastShort(it.message ?: getString(R.string.error_unknown)) }
    }

    override fun onStop() {
        super.onStop()
        Glide.get(applicationContext).trimMemory(TRIM_MEMORY_UI_HIDDEN)
    }

    companion object {

        // Do not use App Theme if it's translucent
        @NonRestartableComposable
        @Composable
        private fun currentThemeNoTrans(dark: Boolean) : ExtendedColorScheme {
            val current = TiebaLiteTheme.extendedColorScheme
            return if (current.colorScheme.isTranslucent) {
                if (dark) DefaultDarkColors else DefaultColors
            } else {
                current
            }
        }

        @Composable
        private fun SavingDialog(modifier: Modifier = Modifier, dialogState: DialogState) {
            if (dialogState.show) {
                Dialog(
                    modifier = modifier,
                    dialogState = dialogState,
                    dialogProperties = AnyPopDialogProperties(
                        direction = DirectionState.CENTER,
                        dismissOnBackPress = false,
                        dismissOnClickOutside = false
                    ),
                    title = { Text(text = stringResource(R.string.theme_dialog_saving)) },
                ) {
                    Text(text = stringResource(id = R.string.dialog_content_wait))
                }
            }
        }

        /**
         * @return UCrop to launch [UCropActivity]
         * */
        private fun Context.buildUCropOptions(sourceUri: Uri, @ColorInt accent: Int): UCrop {
            // Save cropped image to cache dir temporary
            val destUri = Uri.fromFile(File(cacheDir, "$CROP_FILE_PREFIX${System.currentTimeMillis()}.webp"))

            // Restrict image to screen aspect ratio
            val screen = DisplayUtil.getScreenPixels(this)
            val aspectRatio = screen.width / screen.height.toFloat()

            return UCrop.of(sourceUri, destUri)
                .withAspectRatio(aspectRatio, 1f)
                .withMaxResultSize(screen.width, screen.height)
                .withOptions(UCrop.Options().apply {
                    setToolbarColor(accent)
                    setLogoColor(accent)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        setCompressionFormat(Bitmap.CompressFormat.WEBP_LOSSY)
                    } else {
                        setCompressionFormat(Bitmap.CompressFormat.WEBP)
                    }
                    setCompressionQuality(99)
                })
        }
    }
}