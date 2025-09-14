package com.huanchengfly.tieba.post.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.webkit.WebSettings
import com.github.gzuliyujiang.oaid.DeviceID
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.utils.ClientUtils
import com.huanchengfly.tieba.post.utils.packageInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// Note: Config.init moved here for dependency injection
@Singleton
class ConfigInitializer @Inject constructor(
    @ApplicationContext val context: Context,
    private val settingsRepository: SettingsRepository
) {

    fun init(reload: Boolean = false) = with(App.Config) {
        if (reload || !inited) {
            isOAIDSupported = DeviceID.supportedOAID(context)
            if (isOAIDSupported) {
                DeviceID.getOAID(context, OAIDGetter)
            } else {
                statusCode = -200
                isTrackLimited = false
            }
            userAgent = WebSettings.getDefaultUserAgent(context)
            appFirstInstallTime = context.packageInfo.firstInstallTime
            // Keep app update time private
            appLastUpdateTime = appFirstInstallTime
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
                context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PERMISSION_GRANTED
            ) {
                ClientUtils.init(settingsRepository)
            }
            inited = true
        }
    }
}
