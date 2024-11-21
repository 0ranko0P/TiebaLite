package com.huanchengfly.tieba.post.activities

import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.App.Companion.INSTANCE
import com.huanchengfly.tieba.post.components.NetworkObserver
import com.huanchengfly.tieba.post.ui.widgets.VoicePlayerView
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

abstract class BaseActivity : AppCompatActivity(), CoroutineScope {
    override val coroutineContext: CoroutineContext = lifecycleScope.coroutineContext

    private var isActivityRunning = true

    val appPreferences: AppPreferencesUtils by lazy { AppPreferencesUtils.getInstance(INSTANCE) }

    override fun onPause() {
        super.onPause()
        isActivityRunning = false
    }

    //禁止app字体大小跟随系统字体大小调节
    override fun getResources(): Resources {
        val fontScale = appPreferences.fontScale
        val resources = super.getResources()
        if (resources.configuration.fontScale != fontScale) {
            val configuration = resources.configuration
            configuration.fontScale = fontScale
            resources.updateConfiguration(configuration, resources.displayMetrics)
        }
        return resources
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

    fun exitApplication() {
        INSTANCE.removeAllActivity()
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
}