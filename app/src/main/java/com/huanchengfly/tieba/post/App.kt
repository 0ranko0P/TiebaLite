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
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.huanchengfly.tieba.post.activities.CrashActivity
import com.huanchengfly.tieba.post.components.ConfigInitializer
import com.huanchengfly.tieba.post.utils.EmoticonManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Configuration.Provider {

    private val mActivityList: MutableList<Activity> = mutableListOf()

    val powerManager: PowerManager by lazy {
        getSystemService(POWER_SERVICE) as PowerManager
    }

    /**
     * OAID config initializer
     *
     * @see [App.Config]
     * */
    @Inject lateinit var configInit : ConfigInitializer

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

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

    override fun onCreate() {
        INSTANCE = this
        super.onCreate()
        val processName = getProcessName(this)
        if (processName?.endsWith("error_handler") == true) return

        setupUncaughtExceptionHandler(this)
        configInit.init()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (processName != null && packageName != processName) { //判断不等于默认进程名称
                WebView.setDataDirectorySuffix(processName)
            }
        }
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        EmoticonManager.init(this)
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

    object Config {
        var inited: Boolean = false

        var isOAIDSupported: Boolean = false
        var statusCode: Int = -200
        var oaid: String = ""
        var encodedOAID: String = ""
        var isTrackLimited: Boolean = false
        var userAgent: String? = null
        var appFirstInstallTime: Long = 0L
        var appLastUpdateTime: Long = 0L
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
