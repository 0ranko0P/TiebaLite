package com.huanchengfly.tieba.post.components

import android.util.Log
import com.huanchengfly.tieba.post.core.common.di.ApplicationScope
import com.huanchengfly.tieba.post.core.network.exception.getErrorMessage
import com.huanchengfly.tieba.post.core.network.session.ClientConfigProvider
import com.huanchengfly.tieba.post.core.network.source.AuthNetworkDataSource
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.models.settings.ClientConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

private const val TAG = "ClientConfigManager"

/**
 * From com.huanchengfly.tieba.post.utils.ClientUtils
 * */
@Singleton
class ClientConfigManager @Inject constructor(
    @ApplicationScope private val coroutineScope: CoroutineScope,
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
        this.activeTimestamp = activeTimestamp
        clientConfigSettings.save { it.copy(activeTimestamp = activeTimestamp) }
    }

    private fun sync(clientId: String?) = coroutineScope.launch {
        val start = System.currentTimeMillis()
        runCatching { authDataSourceProvider.get().syncClient(clientId) }
            .onFailure {
                Log.w(TAG, "onSync: Failed: ${it.getErrorMessage()}")
            }
            .onSuccess { rec ->
                val client = rec.client
                val wlConfig = rec.wlConfig
                if (clientId == client.clientId && getSampleId() == wlConfig.sampleId) {
                    return@onSuccess
                }
                withContext(Dispatchers.Main) {
                    clientConfigSettings.save {
                        it.copy(clientId = client.clientId, sampleId = wlConfig.sampleId)
                    }
                }
                val cost = System.currentTimeMillis() - start
                Log.w(TAG, "onSync: Done, cost ${cost}ms")
            }
    }
}