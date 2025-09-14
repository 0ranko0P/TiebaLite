package com.huanchengfly.tieba.post.activities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.util.fastDistinctBy
import androidx.core.graphics.createBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import com.huanchengfly.tieba.post.arch.shareInBackground
import com.huanchengfly.tieba.post.components.glide.BlurTransformation
import com.huanchengfly.tieba.post.components.imageProcessor.ImageProcessor
import com.huanchengfly.tieba.post.components.imageProcessor.RenderEffectImageProcessor
import com.huanchengfly.tieba.post.components.imageProcessor.RenderScriptImageProcessor
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.theme.BlueViolet
import com.huanchengfly.tieba.post.theme.MerlotPink
import com.huanchengfly.tieba.post.theme.SunsetOrange
import com.huanchengfly.tieba.post.theme.TiebaBlue
import com.huanchengfly.tieba.post.ui.models.settings.Theme
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.set
import com.huanchengfly.tieba.post.utils.FileUtil.deleteQuietly
import com.huanchengfly.tieba.post.utils.ImageUtil.toFile
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Ui State for Translucent Activity
 *
 * @param primaryColor current Accent/Primary color
 * @param colorPalette palette generated from picked [wallpaper]
 * @param isDarkTheme is Light/Dark translucent theme, default is false
 * @param alpha alpha filter on wallpaper
 * @param wallpaper current cropped wallpaper file
 * @param wallpaperTransformation blur filter transformation
 * */
@Immutable
/* data */class UiState(
    val primaryColor: Color = TiebaBlue,
    val colorPalette: List<Color> = emptyList(),
    val isDarkTheme: Boolean = false,
    val alpha: Float = DefaultAlpha,
    val wallpaper: Uri? = null,
    val wallpaperTransformation: BlurTransformation? = null,
) {
    val blur: Float
        get() = wallpaperTransformation?.radius ?: 0f

    fun copy(
        primaryColor: Color = this.primaryColor,
        colorPalette: List<Color> = this.colorPalette,
        isDarkTheme: Boolean = this.isDarkTheme,
        alpha: Float = this.alpha,
        wallpaper: Uri? = this.wallpaper,
        wallpaperTransformation: BlurTransformation? = this.wallpaperTransformation,
    ) = UiState(
        primaryColor, colorPalette, isDarkTheme, alpha, wallpaper, wallpaperTransformation
    )
}

@HiltViewModel
class TranslucentThemeViewModel @Inject constructor(
    @ApplicationContext val application: Context,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val imageProcessor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        RenderEffectImageProcessor()
    } else {
        RenderScriptImageProcessor(application)
    }

    val configChanged: SharedFlow<Boolean> = combine(
        flow = settingsRepository.themeSettings.flow,
        flow2 = _uiState
    ) { settings, state ->
        when {
            state.wallpaper == null -> false

            settings.theme != Theme.TRANSLUCENT -> true

            else -> state.isDarkTheme != settings.transDarkColorMode
                    || state.primaryColor != settings.transColor
                    || state.alpha != settings.transAlpha
                    || state.blur != settings.transBlur
        }
    }
    .shareInBackground()

    /**
     * Backup File without any filter
     * */
    private val croppedWallpaperFile: File = application.translucentBackground(CROPPED_WALLPAPER_FILE)

    init {
        viewModelScope.launch {
            val wallpaper: Uri? = withContext(Dispatchers.IO) {
                Uri.fromFile(croppedWallpaperFile).takeIf { croppedWallpaperFile.exists() }
            }
            val initState = settingsRepository.themeSettings.flow
                .map {
                    val blur: Float? = it.transBlur.takeUnless { f -> f == 0f }
                    UiState(
                        primaryColor = it.transColor,
                        isDarkTheme = it.transDarkColorMode,
                        alpha = it.transAlpha,
                        wallpaper = wallpaper,
                        wallpaperTransformation = blur?.let { BlurTransformation(imageProcessor, blur) }
                    )
                }
                .first()
            _uiState.set { initState }
        }
    }

    fun onWallpaperDecoded(bitmap: Bitmap) {
        val colorPalette = _uiState.value.colorPalette
        // Do not generate for blurring bitmap
        if (colorPalette.isEmpty()) {
            viewModelScope.launch {
                val newPalette = genPalette(bitmap)
                _uiState.update { it.copy(colorPalette = newPalette) }
            }
        }
    }

    fun onNewWallpaperSelected(uri: Uri) {
        val oldWallpaper = _uiState.value.wallpaper
        if (uri.path != oldWallpaper?.path) {
            _uiState.set { // Clear all filters
                copy(
                    colorPalette = emptyList(),
                    alpha = 1.0f,
                    wallpaper = uri,
                    wallpaperTransformation = null,
                )
            }
        }
    }

    fun onColorPicked(color: Color) {
        if (color != _uiState.value.primaryColor) {
            _uiState.set { copy(primaryColor = color) }
        }
    }

    fun onColorModeChanged() = _uiState.set { copy(isDarkTheme = !isDarkTheme) }

    fun onAlphaChanged(alpha: Float) = _uiState.set { copy(alpha = alpha) }

    fun onBlurChanged(radius: Float) {
        val transformation = if (radius > 0f) BlurTransformation(imageProcessor, radius) else null
        _uiState.set { copy(wallpaperTransformation = transformation) }
    }

    fun saveWallpaper(): Deferred<Result<Unit>> {
        val start = System.currentTimeMillis()

        return viewModelScope.async (Dispatchers.IO) {
            val state = _uiState.first()
            val source = File(state.wallpaper?.path ?: throw IOException("Invalid URI: ${state.wallpaper}"))
            val target = File(application.filesDir, "background_${System.currentTimeMillis()}.webp")
            val isWallpaperChanged = source != croppedWallpaperFile

            val alpha = state.alpha
            val blurRadius = state.blur
            val hasFilter = alpha < 1.0f || blurRadius > 0

            try {
                if (hasFilter) {
                    processWallpaper(imageProcessor, source, target, alpha, blurRadius)
                }

                // Save unmodified original copy
                if (isWallpaperChanged) {
                    source.copyTo(croppedWallpaperFile, overwrite = true)
                    delay(100)
                    source.deleteQuietly() // Clean source manually
                }

                val themeSettings = settingsRepository.themeSettings
                val currentTheme = themeSettings.flow.first()
                val previousWallpaper: String? = currentTheme.transBackground
                themeSettings.save {
                    it.copy(
                        theme = Theme.TRANSLUCENT,
                        transColor = state.primaryColor,
                        transAlpha = state.alpha,
                        transBlur = state.blur,
                        transDarkColorMode = state.isDarkTheme,
                        transBackground = if (hasFilter) target.name else CROPPED_WALLPAPER_FILE
                    )
                }

                // Delete previous wallpaper now
                if (previousWallpaper != null && previousWallpaper != CROPPED_WALLPAPER_FILE) {
                    application.translucentBackground(previousWallpaper).deleteQuietly()
                }
            } catch (e: Exception) {
                Log.w(TAG, "onSaveWallpaper", e)
                if (e is IOException) {
                    target.deleteQuietly()
                }
                return@async Result.failure(e)
            }

            val cost = System.currentTimeMillis() - start
            Log.i(TAG, "onSaveWallpaper: cost ${cost}ms, filter: $hasFilter, reused: ${!isWallpaperChanged}")
            return@async Result.success(Unit)
        }
    }

    override fun onCleared() {
        super.onCleared()
        imageProcessor.cleanup()
    }

    companion object {
        private const val TAG = "ThemeViewModel"

        private const val CROPPED_WALLPAPER_FILE = "cropped_background"

        val DefaultColors by lazy {
            arrayOf(
                Color(TiebaBlue.toArgb()),
                Color(MerlotPink.toArgb()),
                Color(SunsetOrange.toArgb()),
                Color(BlueViolet.toArgb()),
            )
        }

        private suspend fun genPalette(bitmap: Bitmap): List<Color> = withContext(Dispatchers.IO) {
            val target = if (!bitmap.isMutable) bitmap.copy(Bitmap.Config.RGB_565, true) else bitmap
            val palette = Palette.from(target).generate()
            if (target !== bitmap) {
                target.recycle()
            }
            ensureActive()
            val colors: List<Color> = listOfNotNull(
                palette.vibrantSwatch?.rgb,
                palette.mutedSwatch?.rgb,
                palette.dominantSwatch?.rgb,
                palette.darkVibrantSwatch?.rgb,
                palette.darkMutedSwatch?.rgb,
                palette.lightVibrantSwatch?.rgb,
                palette.lightMutedSwatch?.rgb,
            ).map { Color(it) }

            // Append our default colors and distinct
            return@withContext (colors + DefaultColors).fastDistinctBy { it.value }
        }

        fun Context.translucentBackground(name: String): File = File(filesDir, name)

        @WorkerThread
        private fun processWallpaper(
            imageProcessor: ImageProcessor,
            source: File,
            target: File,
            alpha: Float,
            blurRadius: Float
        ) {
            val canvasAlpha = (alpha * 255).roundToInt()
            var bitmap = source.inputStream().use { ins ->
                BitmapFactory.decodeStream(ins, null, BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                    inMutable = true
                })
            } ?: throw IOException("Decode $source failed!")

            // Apply alpha filter
            if (canvasAlpha < 255) {
                val alphaBitmap = createBitmap(bitmap.width, bitmap.height, bitmap.config!!)
                Canvas(alphaBitmap).apply {
                    drawColor(Color.Black.toArgb())
                    drawBitmap(bitmap, 0f, 0f, Paint().also {it.alpha = canvasAlpha })
                }
                bitmap.recycle()
                bitmap = alphaBitmap
            }

            // Apply blur filter
            if (blurRadius > 0) {
                imageProcessor.configureInputAndOutput(bitmap)
                bitmap = imageProcessor.blur(blurRadius)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                bitmap.toFile(target, 99, CompressFormat.WEBP_LOSSY)
            } else {
                bitmap.toFile(target, 99, CompressFormat.WEBP)
            }
        }
    }
}
