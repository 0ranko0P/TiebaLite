package com.huanchengfly.tieba.post.ui.widgets.compose.video

import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.view.Window
import androidx.annotation.MainThread
import kotlin.math.roundToInt

/** 音量写入系统媒体流；亮度只写入当前 Window，释放时恢复手势调节前的亮度 */
@MainThread
internal class BrightnessVolumeController(
    context: Context,
) {
    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var window: Window? = null
    private var savedBrightness: Float? = null
    private var cachedBrightness = -1f

    /** 绑定当前 Activity 的 Window，用于控制屏幕亮度；切换 Window 时重置原始亮度记录 */
    fun bindWindow(window: Window?) {
        if (this.window == window) return
        restoreBrightness()
        this.window = window
        savedBrightness = null
    }

    /** 返回当前媒体音量比例 */
    fun getVolume(): Float {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (max <= 0) return 0f
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max.toFloat()
    }

    /** 设置媒体音量比例 */
    fun setVolume(level: Float) {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = (level * max).roundToInt().coerceIn(0, max)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
    }

    /** 返回屏幕亮度；Window 为 -1 时读取并缓存系统亮度 */
    fun getBrightness(): Float {
        val w = window ?: return cachedBrightness.coerceAtLeast(0f)
        val brightness = w.attributes.screenBrightness
        if (brightness >= 0f) {
            cachedBrightness = brightness
            return brightness
        }
        val resolver = w.context.contentResolver
        return try {
            val brightnessMode =
                Settings.System.getInt(
                    resolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
                )
            val level = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
            if (brightnessMode != Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) cachedBrightness = level
            level
        } catch (_: Settings.SettingNotFoundException) {
            cachedBrightness.coerceAtLeast(0f)
        }
    }

    /** 设置屏幕亮度；首次调用时保存原始亮度以便恢复 */
    fun setBrightness(level: Float) {
        val w = window ?: return
        val params = w.attributes
        if (savedBrightness == null) savedBrightness = params.screenBrightness
        params.screenBrightness = level.coerceIn(0f, 1f)
        w.attributes = params
    }

    /** 恢复到手势调节前的屏幕亮度 */
    fun restoreBrightness() {
        val original = savedBrightness ?: return
        savedBrightness = null
        window?.attributes?.let { params ->
            params.screenBrightness = original
            window?.attributes = params
        }
    }
}
