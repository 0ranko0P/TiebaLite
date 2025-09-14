package com.huanchengfly.tieba.post.utils

import com.huanchengfly.tieba.post.App.Companion.AppBackgroundScope
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.models.settings.ClientConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object ClientUtils {

    private var clientConfigSettings: Settings<ClientConfig>? = null

    var clientId: String? = null
        private set

    var sampleId: String? = null
        private set

    var baiduId: String? = null
        private set

    var activeTimestamp: Long = System.currentTimeMillis()
        private set

    fun init(settingsRepository: SettingsRepository) {
        clientConfigSettings = settingsRepository.clientConfig
        AppBackgroundScope.launch {
            val config = settingsRepository.clientConfig.flow.first()
            withContext(Dispatchers.Main.immediate) {
                clientId = config.clientId
                sampleId = config.sampleId
                baiduId = config.baiduId
                activeTimestamp = config.activeTimestamp
            }
            sync()
        }
    }

    fun saveBaiduId(id: String?) {
        if (id.isNullOrEmpty() || id.isBlank() || id == baiduId) return
        baiduId = id
        clientConfigSettings!!.save {
            it.copy(baiduId = id)
        }
    }

    fun refreshActiveTimestamp() {
        activeTimestamp = System.currentTimeMillis()
        clientConfigSettings?.save {
            it.copy(activeTimestamp = activeTimestamp)
        }
    }

    private suspend fun sync() {
        val rec = TiebaApi.getInstance()
            .syncFlow(clientId)
            .catch { it.printStackTrace() }
            .firstOrNull() ?: return

        val client = rec.client
        val wlConfig = rec.wlConfig
        if (clientId == client.clientId && sampleId == wlConfig.sampleId) {
            return
        }
        clientId = client.clientId
        sampleId = wlConfig.sampleId

        clientConfigSettings!!.save {
            it.copy(clientId = client.clientId, sampleId = wlConfig.sampleId)
        }
    }
}