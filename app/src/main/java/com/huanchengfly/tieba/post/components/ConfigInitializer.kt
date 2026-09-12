package com.huanchengfly.tieba.post.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.webkit.WebSettings
import com.github.gzuliyujiang.oaid.DeviceID
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.core.network.session.OAIDProvider
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.utils.packageInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

// Note: Config.init moved here for dependency injection
@Singleton
class ConfigInitializer @Inject constructor(
    @ApplicationContext val context: Context,
    private val clientConfigManagerProvider: Provider<ClientConfigManager>,
    settingsRepository: SettingsRepository
): OAIDProvider {

    private val clientSettings = settingsRepository.clientConfig

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
            val clientConfigManager = clientConfigManagerProvider.get()
            var config = clientConfigManager.currentConfig()
            appFirstInstallTime = config.firstInstallTime ?: context.packageInfo.firstInstallTime
            appLastUpdateTime = config.lastUpdateTime ?: context.packageInfo.lastUpdateTime

            // Make app install time constant, save to settings
            if (config.firstInstallTime == null) {
                config = config.copy(firstInstallTime = appFirstInstallTime, lastUpdateTime = appLastUpdateTime)
                clientSettings.set(config)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
                context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PERMISSION_GRANTED
            ) {
                clientConfigManager.init(config)
            }
            inited = true
        }
    }

    override fun isOAIDSupported(): Boolean = App.Config.isOAIDSupported

    override fun isTrackLimited(): Boolean = App.Config.isTrackLimited

    override fun getEncodedOAID(): String = App.Config.encodedOAID

    override fun getStatusCode(): Int = App.Config.statusCode

    override val appFirstInstallTime: Long
        get() = App.Config.appFirstInstallTime

    override val appLastUpdateTime: Long
        get() = App.Config.appLastUpdateTime

    override val userAgent: String?
        get() = App.Config.userAgent
}
