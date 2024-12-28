package com.huanchengfly.tieba.post

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatDelegate
import com.github.gzuliyujiang.oaid.DeviceID
import com.huanchengfly.tieba.post.activities.CrashActivity
import com.huanchengfly.tieba.post.components.OAIDGetter
import com.huanchengfly.tieba.post.utils.BlockManager
import com.huanchengfly.tieba.post.utils.ClientUtils
import com.huanchengfly.tieba.post.utils.EmoticonManager
import com.huanchengfly.tieba.post.utils.appPreferences
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.litepal.LitePal

@HiltAndroidApp
class App : Application() {
    private val mActivityList: MutableList<Activity> = mutableListOf()

    val powerManager by lazy {
        getSystemService(POWER_SERVICE) as PowerManager
    }

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
        ClientUtils.init(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (processName != null && packageName != processName) { //判断不等于默认进程名称
                WebView.setDataDirectorySuffix(processName)
            }
        }
        LitePal.initialize(this)
        Config.init(this)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        MainScope().launch {
            BlockManager.init()
            EmoticonManager.init(this@App)
        }
    }

    //解决魅族 Flyme 系统夜间模式强制反色
    @Keep
    fun mzNightModeUseOf(): Int = 2

    //禁止app字体大小跟随系统字体大小调节
    override fun getResources(): Resources {
        //INSTANCE = this
        val fontScale = appPreferences.fontScale
        val resources = super.getResources()
        if (resources.configuration.fontScale != fontScale) {
            val configuration = resources.configuration
            configuration.fontScale = fontScale
            resources.updateConfiguration(configuration, resources.displayMetrics)
        }
        return resources
    }

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

        fun init(context: Context) {
            if (!inited) {
                isOAIDSupported = DeviceID.supportedOAID(context)
                if (isOAIDSupported) {
                    DeviceID.getOAID(context, OAIDGetter)
                } else {
                    statusCode = -200
                    isTrackLimited = false
                }
                userAgent = WebSettings.getDefaultUserAgent(context)
                context.appPreferences.run {
                    appFirstInstallTime = installTime
                    appLastUpdateTime = updateTime
                }
                inited = true
            }
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
        const val TAG = "App"

        @JvmStatic
        lateinit var INSTANCE: App
            private set

        val isSystemNight: Boolean
            get() = nightMode == Configuration.UI_MODE_NIGHT_YES

        private val nightMode: Int
            get() = INSTANCE.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

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
    }
}
