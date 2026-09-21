package com.huanchengfly.tieba.post

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.webkit.WebView
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composer
import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import androidx.compose.runtime.tooling.ComposeStackTraceMode
import androidx.compose.ui.ComposeUiFlags
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.SingletonImageLoader
import com.huanchengfly.tieba.post.activities.CrashActivity
import com.huanchengfly.tieba.post.components.coil.TbImageLoaderFactory
import com.huanchengfly.tieba.post.core.data.di.SettingsEntryPoint
import com.huanchengfly.tieba.post.core.data.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.utils.EmoticonManager
import com.huanchengfly.tieba.post.utils.ImageCacheUtil
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Configuration.Provider {

    private val mActivityList: MutableList<Activity> = mutableListOf()

    val powerManager: PowerManager by lazy {
        getSystemService(POWER_SERVICE) as PowerManager
    }

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    // For components that can't work with Hilt inject
    val settingRepository: SettingsRepository
        get() = EntryPointAccessors
            .fromApplication<SettingsEntryPoint>(this)
            .settingsRepository()

    private fun getProcessName(context: Context): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) return getProcessName()

        val manager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (processInfo in manager.runningAppProcesses) {
            if (processInfo.pid == Process.myPid()) {
                return processInfo.processName
            }
        }
        return null
    }

    @OptIn(ExperimentalComposeRuntimeApi::class)
    override fun onCreate() {
        INSTANCE = this
        super.onCreate()
        val processName = getProcessName(this)
        if (processName?.endsWith("error_handler") == true) return

        setupUncaughtExceptionHandler(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (processName != null && packageName != processName) { //判断不等于默认进程名称
                WebView.setDataDirectorySuffix(processName)
            }
        }
        SingletonImageLoader.setSafe(TbImageLoaderFactory())
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        EmoticonManager.init(this)
        if (BuildConfig.DEBUG) {
            // Enable verbose Compose stack traces for local debugging
            Composer.setDiagnosticStackTraceMode(ComposeStackTraceMode.SourceInformation)
        } else {
            Composer.setDiagnosticStackTraceMode(ComposeStackTraceMode.Auto)
        }
        ComposeUiFlags.isMediaQueryIntegrationEnabled = true

        AppBackgroundScope.launch {
            delay(3000)
            ImageCacheUtil.clearGlideDiskCache(this@App)
        }
    }

    //解决魅族 Flyme 系统夜间模式强制反色
    @Keep
    fun mzNightModeUseOf(): Int = 2

    /**
     * 添加Activity
     */
    fun addActivity(activity: Activity) {
        // 判断当前集合中不存在该Activity
        if (!mActivityList.contains(activity)) {
            mActivityList.add(activity) //把当前Activity添加到集合中
        }
    }

    /**
     * 销毁单个Activity
     */
    @JvmOverloads
    fun removeActivity(activity: Activity, finish: Boolean = false) {
        //判断当前集合中存在该Activity
        if (mActivityList.contains(activity)) {
            mActivityList.remove(activity) //从集合中移除
            if (finish) activity.finish() //销毁当前Activity
        }
    }

    /**
     * 销毁所有的Activity
     */
    fun removeAllActivity() {
        //通过循环，把集合中的所有Activity销毁
        for (activity in mActivityList) {
            activity.finish()
        }
    }

    object ScreenInfo {
        @JvmField
        var EXACT_SCREEN_HEIGHT = 0

        @JvmField
        var EXACT_SCREEN_WIDTH = 0

        @JvmField
        var SCREEN_HEIGHT = 0

        @JvmField
        var SCREEN_WIDTH = 0

        @JvmField
        var DENSITY = 0f
    }

    companion object {

        @JvmStatic
        lateinit var INSTANCE: App
            private set

        private fun setupUncaughtExceptionHandler(context: Context) {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { t, e ->
                context.goToActivity<CrashActivity> {
                    // Note: Do not serialize Throwable
                    putExtra(CrashActivity.KEY_THROWABLE, e.stackTraceToString())
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                defaultHandler?.uncaughtException(t, e)
            }
        }

        val AppBackgroundScope = CoroutineScope(Dispatchers.IO + CoroutineName("AppBackground") + SupervisorJob())
    }
}
