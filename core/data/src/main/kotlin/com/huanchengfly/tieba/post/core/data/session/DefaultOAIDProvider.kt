package com.huanchengfly.tieba.post.core.data.session

import android.Manifest
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.util.Log
import android.webkit.WebSettings
import com.github.gzuliyujiang.oaid.DeviceID
import com.github.gzuliyujiang.oaid.IGetter
import com.huanchengfly.tieba.post.core.data.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.core.network.exception.getErrorMessage
import com.huanchengfly.tieba.post.core.network.session.OAIDProvider
import com.huanchengfly.tieba.post.core.network.util.helios.Base32
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * [OAIDProvider] 的默认实现
 *
 * 原位置: com.huanchengfly.tieba.post.App.Config
 *
 * @author HuanChengFly
 * @author 0Ranko0P
 *
 * @since 4.0.0 alpha 14
 * */
@Singleton
class DefaultOAIDProvider @Inject constructor(
    @param:ApplicationContext val context: Context,
    private val clientConfigManagerProvider: Provider<ClientConfigManager>,
    settingsRepository: SettingsRepository
): OAIDProvider {

    init {
        initOAID()
    }

    private val clientSettings = settingsRepository.clientConfig

    override var appFirstInstallTime: Long = 0

    override var appLastUpdateTime: Long = 0

    override var encodedOAID: String = ""

    var oaid: String = ""
        private set

    override var statusCode: Int = -200

    override var isOAIDSupported: Boolean = false

    override var isTrackLimited: Boolean = false

    override var userAgent: String? = null

    fun initOAID() {
        isOAIDSupported = DeviceID.supportedOAID(context)
        if (isOAIDSupported) {
            DeviceID.getOAID(context, OAIDGetter())
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
    }

    private inner class OAIDGetter : IGetter {
        override fun onOAIDGetComplete(result: String) {
            oaid = result
            encodedOAID = Base32.encode(result.encodeToByteArray())
            statusCode = 0
            isTrackLimited = false
        }

        override fun onOAIDGetError(error: Exception?) {
            statusCode = -100
            isTrackLimited = true
            Log.w(TAG, "onOAIDGetError: ${error?.getErrorMessage()}")
        }
    }

    companion object {
        private const val TAG = "OAIDProvider"

        val Context.packageInfo: PackageInfo
            get() = packageManager.getPackageInfo(packageName, 0)
    }
}
