package com.huanchengfly.tieba.post.components

import android.content.Context
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.core.data.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.core.network.session.DeviceInfoProvider
import com.huanchengfly.tieba.post.utils.DeviceUtils
import com.huanchengfly.tieba.post.utils.MobileInfoUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultDeviceInfoProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    settingsRepo: SettingsRepository,
): DeviceInfoProvider {

    private val uuidSettings = settingsRepo.UUIDSettings

    override val imei: String by lazy { MobileInfoUtil.getIMEI(context) }

    override val deviceScore by lazy { DeviceUtils.getDeviceScore().toString() }

    override val density: Float
        get() = App.ScreenInfo.DENSITY

    override val screenHeight: Int
        get() = App.ScreenInfo.SCREEN_HEIGHT

    override val screenWidth: Int
        get() = App.ScreenInfo.SCREEN_WIDTH

    /**
     * From com.huanchengfly.tieba.post.utils.UIDUtil.uUID
     * */
    override val uuid: String = runBlocking {
        var id = uuidSettings.snapshot()
        if (id.isEmpty()) {
            id = UUID.randomUUID().toString()
            uuidSettings.set(new = id)
        }
        return@runBlocking id
    }
}