package com.huanchengfly.tieba.post.activities

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.util.fastDistinctBy
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.components.glide.BlurTransformation
import com.huanchengfly.tieba.post.components.imageProcessor.RenderEffectImageProcessor
import com.huanchengfly.tieba.post.components.imageProcessor.RenderScriptImageProcessor
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.getColor
import com.huanchengfly.tieba.post.putColor
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils.Companion.KEY_TRANSLUCENT_BACKGROUND_FILE
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils.Companion.KEY_TRANSLUCENT_PRIMARY_COLOR
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils.Companion.KEY_TRANSLUCENT_THEME
import com.huanchengfly.tieba.post.utils.FileUtil.deleteQuietly
import com.huanchengfly.tieba.post.utils.ImageUtil.toFile
import com.huanchengfly.tieba.post.utils.ThemeUtil
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.math.roundToInt

class TranslucentThemeViewModel : ViewModel() {
    private val context = App.INSTANCE
    private val dataStore = context.dataStore

    private val KEY_TRANSLUCENT_BLUR by lazy { floatPreferencesKey("trans_blur") }
    private val KEY_TRANSLUCENT_ALPHA by lazy { floatPreferencesKey("trans_alpha") }

    private val _colorPalette: SnapshotStateList<Color> = mutableStateListOf()
    val colorPalette: List<Color> get() = _colorPalette

    /**
     * Accent/Primary color of Translucent Theme
     *
     * @see KEY_TRANSLUCENT_PRIMARY_COLOR
     * */
    var primaryColor: Color by mutableStateOf(DefaultColors[0]) // TiebaBlue by default
        private set

    /**
     * Light/Dark mode of translucent theme, default is Light
     *
     * @see [ThemeUtil.TRANSLUCENT_THEME_LIGHT]
     * @see [ThemeUtil.TRANSLUCENT_THEME_DARK]
     * */
    var themeMode: Int by mutableIntStateOf(ThemeUtil.TRANSLUCENT_THEME_LIGHT)
        private set

    var alpha: Float by mutableFloatStateOf(1f)
        private set

    var blurRadius: Float by mutableFloatStateOf(0f)
        private set

    var wallpaper: Uri? by mutableStateOf(null)
        private set

    var wallpaperTransformation: BitmapTransformation? by mutableStateOf(null)
        private set

    var savingWallpaper: Boolean by mutableStateOf(false)
        private set

    private val imageProcessor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        RenderEffectImageProcessor()
    } else {
        RenderScriptImageProcessor(context)
    }

    var configChanged: Boolean by mutableStateOf(false)
        private set

    /**
     * Backup File without any filter
     * */
    private val croppedWallpaperFile: File = File(context.filesDir, CROPPED_WALLPAPER_FILE)

    init {
        viewModelScope.launch {
            if (croppedWallpaperFile.exists()) {
                wallpaper = Uri.fromFile(croppedWallpaperFile)
                // Wallpaper cropped before, user can apply theme immediately
                configChanged = !ThemeUtil.isTranslucentTheme()
            }
            // Restore saved configs
            val data = dataStore.data.first()
            themeMode = data[KEY_TRANSLUCENT_THEME] ?: ThemeUtil.TRANSLUCENT_THEME_LIGHT
            primaryColor = data.getColor(KEY_TRANSLUCENT_PRIMARY_COLOR) ?: DefaultColors[0]
            alpha = data[KEY_TRANSLUCENT_ALPHA] ?: 1f
            blurRadius = data[KEY_TRANSLUCENT_BLUR]?: 0f
        }
    }

    fun onWallpaperDecoded(bitmap: Bitmap) {
        // Do not generate for blurring bitmap
        if (_colorPalette.isEmpty()) {
            viewModelScope.launch(Dispatchers.Main) {
                val result = genPalette(bitmap)
                _colorPalette.clear()
                _colorPalette.addAll(result)
            }
        }
    }

    fun onNewWallpaperSelected(uri: Uri) {
        if (uri.path != wallpaper?.path) {
            // Clear all filters
            blurRadius = 0f
            wallpaperTransformation = null
            alpha = 1.0f
            _colorPalette.clear()

            // Update new wallpaper now
            wallpaper = uri
            configChanged = true
        }
    }

    fun onColorPicked(color: Color) {
        if (primaryColor != color) {
            primaryColor = color
            checkConfigChanges()
        }
    }

    fun onColorModeChanged() {
        themeMode = if (themeMode == ThemeUtil.TRANSLUCENT_THEME_LIGHT) {
            ThemeUtil.TRANSLUCENT_THEME_DARK
        } else {
            ThemeUtil.TRANSLUCENT_THEME_LIGHT
        }
        checkConfigChanges()
    }

    fun onAlphaChanged(alpha: Float) {
        this.alpha = alpha
    }

    fun onBlurChanged(radius: Float) {
        this.blurRadius = radius
    }

    fun updateImage() {
        val radius = blurRadius
        wallpaperTransformation = if (radius == 0f) null else BlurTransformation(imageProcessor, radius)
        checkConfigChanges()
    }

    private fun checkConfigChanges() {
        if (wallpaper == null) return
        if (!ThemeUtil.isTranslucentTheme(ThemeUtil.themeState.value)) {
            configChanged = true
        } else {
            viewModelScope.launch {
                configChanged = dataStore.data.map {
                    themeMode != it[KEY_TRANSLUCENT_THEME]
                            || primaryColor != it.getColor(KEY_TRANSLUCENT_PRIMARY_COLOR)
                            || alpha != it[KEY_TRANSLUCENT_ALPHA]
                            || blurRadius != it[KEY_TRANSLUCENT_BLUR]
                }.first()
            }
        }
    }

    fun saveWallpaper(): Deferred<Result<Unit>> {
        savingWallpaper = true
        val start = System.currentTimeMillis()
        val context = App.INSTANCE
        val source = File(wallpaper?.path ?: throw IOException("Invalid URI: $wallpaper"))
        val target = File(context.filesDir, "background_${System.currentTimeMillis()}.webp")
        val isWallpaperChanged = source != croppedWallpaperFile
        val hasFilter = alpha < 1.0f || blurRadius > 0

        return viewModelScope.async (Dispatchers.IO) {
            try {
                if (hasFilter) {
                    val canvasAlpha = (alpha * 255).roundToInt()
                    // Decode full background wallpaper
                    var bitmap = source.inputStream().use { ins ->
                        BitmapFactory.decodeStream(ins, null, BitmapFactory.Options().apply {
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                            inMutable = true
                        })
                    } ?: throw IOException("Decode $source failed!")

                    // Apply alpha filter
                    if (canvasAlpha < 255) {
                        val alphaBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
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

                // Save unmodified original copy
                if (isWallpaperChanged) {
                    source.copyTo(croppedWallpaperFile, overwrite = true)
                    delay(100)
                    source.deleteQuietly() // Clean source manually
                }

                var previousWallpaperFile: String? = null
                dataStore.edit {
                    previousWallpaperFile = it[KEY_TRANSLUCENT_BACKGROUND_FILE]
                    it.putColor(KEY_TRANSLUCENT_PRIMARY_COLOR, primaryColor)
                    it[KEY_TRANSLUCENT_THEME] = themeMode
                    it[KEY_TRANSLUCENT_ALPHA] = alpha
                    it[KEY_TRANSLUCENT_BLUR] = blurRadius
                    // Save the image file name
                    it[KEY_TRANSLUCENT_BACKGROUND_FILE] = if (hasFilter) target.name else CROPPED_WALLPAPER_FILE
                }

                // Delete previous wallpaper now
                previousWallpaperFile?.let {
                    if (it != CROPPED_WALLPAPER_FILE) File(context.filesDir, it).deleteQuietly()
                }
            } catch (e: Exception) {
                Log.w(TAG, "onSaveWallpaper: ", e)
                if (e is IOException) {
                    target.deleteQuietly()
                }
                savingWallpaper = false
                return@async Result.failure(e)
            }

            val cost = System.currentTimeMillis() - start
            withContext(Dispatchers.Main) {
                ThemeUtil.switchTheme(ThemeUtil.THEME_TRANSLUCENT, false)
                Log.i(TAG, "onSaveWallpaper: cost ${cost}ms, filter: $hasFilter, reused: ${!isWallpaperChanged}")
            }
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
                Color(0xFF4477E0),
                Color(0xFFFF9A9E),
                Color(0xFFC51100),
                Color(0xFF000000),
                Color(0xFF512DA8)
            )
        }

        private suspend fun genPalette(bitmap: Bitmap): List<Color> = withContext(Dispatchers.IO) {
            val target = if (!bitmap.isMutable) bitmap.copy(Bitmap.Config.RGB_565, true) else bitmap
            val palette = Palette.from(target).generate()
            if (target != bitmap) {
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
    }
}
