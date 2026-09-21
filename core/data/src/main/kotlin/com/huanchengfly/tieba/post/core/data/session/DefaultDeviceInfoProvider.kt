package com.huanchengfly.tieba.post.core.data.session

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.content.getSystemService
import com.huanchengfly.tieba.post.core.data.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.core.data.util.DeviceUtils
import com.huanchengfly.tieba.post.core.data.util.MobileInfoUtil
import com.huanchengfly.tieba.post.core.network.session.DeviceInfoProvider
import kotlinx.coroutines.runBlocking
import java.util.UUID

/**
 * [DeviceInfoProvider] 的默认实现.
 *
 * Note: 从4.0 beta 6 以后屏幕信息不再保持更新
 * */
class DefaultDeviceInfoProvider private constructor(
    private val context: Context,
    settingsRepo: SettingsRepository,
    metrics: DisplayMetrics,
): DeviceInfoProvider {

    override val imei: String by lazy { MobileInfoUtil.getIMEI(context) }

    override val deviceScore by lazy { DeviceUtils.getDeviceScore().toString() }

    override val density: Float = metrics.density

    override val screenHeight: Int = metrics.heightPixels

    override val screenWidth: Int = metrics.widthPixels

    override val uuid: String = runBlocking {
        val uuidSettings = settingsRepo.UUIDSettings
        var id = uuidSettings.snapshot()
        if (id.isEmpty()) {
            id = UUID.randomUUID().toString()
            uuidSettings.set(new = id)
        }
        return@runBlocking id
    }

    companion object {
        fun instance(application: Context, settingsRepo: SettingsRepository): DefaultDeviceInfoProvider {
            val metrics = DisplayMetrics()
            application.getSystemService<WindowManager>()!!.defaultDisplay.getMetrics(metrics)
            return DefaultDeviceInfoProvider(application, settingsRepo, metrics)
        }
    }
}
