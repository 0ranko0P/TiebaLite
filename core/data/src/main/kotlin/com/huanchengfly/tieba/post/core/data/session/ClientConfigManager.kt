package com.huanchengfly.tieba.post.core.data.session

import android.util.Log
import com.huanchengfly.tieba.post.core.common.di.ApplicationScope
import com.huanchengfly.tieba.post.core.data.model.settings.ClientConfig
import com.huanchengfly.tieba.post.core.data.model.settings.Settings
import com.huanchengfly.tieba.post.core.data.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.core.network.exception.getErrorMessage
import com.huanchengfly.tieba.post.core.network.session.ClientConfigProvider
import com.huanchengfly.tieba.post.core.network.source.AuthNetworkDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.IOException
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

private const val TAG = "ClientConfigManager"

/**
 * 原位置: com.huanchengfly.tieba.post.utils.ClientUtils
 *
 * @author 0Ranko0P
 * @author HuanChengFly
 *
 * @since 4.0.0 alpha 14
 * */
@Singleton
class ClientConfigManager @Inject constructor(
    @param:ApplicationScope private val coroutineScope: CoroutineScope,
    private val authDataSourceProvider: Provider<AuthNetworkDataSource>,
    settingsRepo: SettingsRepository,
): ClientConfigProvider {

    private val clientConfigSettings: Settings<ClientConfig> = settingsRepo.clientConfig

    val config: SharedFlow<ClientConfig> = clientConfigSettings
        .shareIn(coroutineScope, started = SharingStarted.Eagerly, replay = 1)

    override var activeTimestamp: Long = System.currentTimeMillis()
        private set

    override fun getClientId(): String? = currentConfig().clientId

    override fun getSampleId(): String? = currentConfig().sampleId

    override fun getBaiduId(): String? = currentConfig().baiduId

    fun init(config: ClientConfig) {
        sync(config.clientId)
    }

    fun currentConfig(): ClientConfig = runBlocking { config.first() }

    override fun saveBaiduId(id: String?) {
        if (id.isNullOrEmpty() || id.isBlank() || id == getBaiduId()) return

        clientConfigSettings.save { it.copy(baiduId = id) }
    }

    fun refreshActiveTimestamp() {
        val activeTimestamp = System.currentTimeMillis()
        if (activeTimestamp - this.activeTimestamp >= 60 * 1000) {
            this.activeTimestamp = activeTimestamp
        }
    }

    private fun sync(clientId: String?) = coroutineScope.launch {
        val start = System.currentTimeMillis()
        try {
            val rec = authDataSourceProvider.get().syncClient(clientId)
            val client = rec.client
            val wlConfig = rec.wlConfig
            if (clientId != client.clientId || getSampleId() != wlConfig.sampleId) {
                clientConfigSettings.save {
                    it.copy(clientId = client.clientId, sampleId = wlConfig.sampleId)
                }
            }
            Log.w(TAG, "onSync: Cost ${System.currentTimeMillis() - start}ms")
        } catch (e: IOException) {
            Log.w(TAG, "onSync: Failed: ${e.getErrorMessage()}")
        } catch (e: Throwable) {
            Log.e(TAG, "onSync: Failed:", e)
        }
    }
}