package com.huanchengfly.tieba.post.activities

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.App.Companion.INSTANCE
import com.huanchengfly.tieba.post.components.NetworkObserver
import com.huanchengfly.tieba.post.ui.widgets.VoicePlayerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs

abstract class BaseActivity : AppCompatActivity(), CoroutineScope {
    override val coroutineContext: CoroutineContext = lifecycleScope.coroutineContext

    private var isActivityRunning = true

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        if (newBase != null) {
            overrideFontScaleOneShot(newBase)
        }
    }

    override fun onPause() {
        super.onPause()
        isActivityRunning = false
    }

    override fun onStop() {
        super.onStop()
        VoicePlayerView.Manager.release()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NetworkObserver.observeOnLifecycle(this)
        getDeviceDensity()
        INSTANCE.addActivity(this)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        getDeviceDensity()
    }

    override fun onResume() {
        super.onResume()
        isActivityRunning = true
    }

    override fun onDestroy() {
        super.onDestroy()
        INSTANCE.removeActivity(this)
    }

    private fun getDeviceDensity() {
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        App.ScreenInfo.EXACT_SCREEN_HEIGHT = height
        App.ScreenInfo.EXACT_SCREEN_WIDTH = width
        val density = metrics.density
        App.ScreenInfo.DENSITY = metrics.density
        App.ScreenInfo.SCREEN_HEIGHT = (height / density).toInt()
        App.ScreenInfo.SCREEN_WIDTH = (width / density).toInt()
    }

    private fun overrideFontScaleOneShot(baseContext: Context) {
        val settingsRepo = (baseContext.applicationContext as App).settingRepository
        runCatching {
            // Block the Main thread to avoid IllegalStateException in ContextThemeWrapper
            val fontScale = runBlocking { settingsRepo.fontScale.snapshot() }
            val currentFontScale = baseContext.resources.configuration.fontScale
            if (abs(currentFontScale - fontScale) > 0.01f) {
                val fontConfig = Configuration()
                fontConfig.fontScale = fontScale
                applyOverrideConfiguration(fontConfig)
            }
        }
        .onFailure { it.printStackTrace() }
    }
}