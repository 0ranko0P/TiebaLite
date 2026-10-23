package com.huanchengfly.tieba.post.core.media.state

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

@Composable
internal fun rememberVolumeAndBrightnessState(): VolumeAndBrightnessState {
    val activity = LocalActivity.current!!
    val state = remember(activity) {
        VolumeAndBrightnessState(activity.applicationContext, activity.window)
    }

    DisposableEffect(state) {
        state.observe()
        onDispose {
            state.unregisterObserver()
        }
    }

    return state
}

/**
 * State that holds all interactions to correctly deal with a UI component representing a music
 * volume and screen brightness controller.
 *
 * In most cases, this will be created via [rememberVolumeAndBrightnessState].
 *
 * @param[context] Activity context.
 */
@Stable
internal class VolumeAndBrightnessState(
    context: Context,
    private val window: Window,
) {

    private val audioManager = ContextCompat.getSystemService(context, AudioManager::class.java)!!

    private val contentResolver = context.contentResolver

    private var initialScreenBrightness: Float = window.attributes.screenBrightness

    val maxBrightness: Float = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL

    val maxVolume: Int = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    var currentBrightness: Float by mutableFloatStateOf(0f)
        private set

    var currentVolume: Float by mutableFloatStateOf(audioManager.currentStreamVolume / maxVolume.toFloat())
        private set

    private val observer = object: ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            try {
                currentBrightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
                if (initialScreenBrightness < 0) {
                    initialScreenBrightness = currentBrightness
                }
            } catch (e: Throwable) {
                Log.w(TAG, "onChange: ", e)
            }
        }
    }

    fun setBrightness(brightness: Float) {
        currentBrightness = brightness.coerceIn(0f, maxBrightness)
        window.attributes = window.attributes.apply {
            screenBrightness = currentBrightness
        }
    }

    fun setVolume(volume: Float) {
        currentVolume = volume.coerceIn(0f, 1.0f)
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            (currentVolume * maxVolume).roundToInt(),
            AudioManager.FLAG_VIBRATE // or 0,
        )
    }

    fun observe() {
        contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
            false,
            observer
        )
        observer.onChange(false)
    }

    fun unregisterObserver() {
        contentResolver.unregisterContentObserver(observer)
        setBrightness(initialScreenBrightness)
    }

    companion object {
        private const val TAG = "BrightnessState"

        private val AudioManager.currentStreamVolume: Int
            get() = getStreamVolume(AudioManager.STREAM_MUSIC)
    }
}
