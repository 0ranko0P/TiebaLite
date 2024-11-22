package com.huanchengfly.tieba.post.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.arch.unsafeLazy
import com.huanchengfly.tieba.post.asyncEdit
import com.huanchengfly.tieba.post.dataStore
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object ClientUtils {

    private val clientIdKey by unsafeLazy { stringPreferencesKey("client_id") }
    private val sampleIdKey by unsafeLazy { stringPreferencesKey("sample_id") }
    private val baiduIdKey by unsafeLazy { stringPreferencesKey("baidu_id") }
    private val activeTimestampKey by unsafeLazy { longPreferencesKey("active_timestamp") }

    var clientId: String? = null
    var sampleId: String? = null
    var baiduId: String? = null
    var activeTimestamp: Long = System.currentTimeMillis()

    fun init(context: Context) = MainScope().launch {
        context.dataStore.data.first().let {
            clientId = it[clientIdKey]
            sampleId = it[sampleIdKey]
            baiduId = it[baiduIdKey]
            activeTimestamp = it[activeTimestampKey] ?: System.currentTimeMillis()
        }
        sync(context)
    }

    fun saveBaiduId(context: Context, id: String?) {
        if (id.isNullOrEmpty() || id.isBlank() || id == baiduId) return
        baiduId = id
        context.dataStore.asyncEdit(baiduIdKey, id)
    }

    suspend fun setActiveTimestamp(context: Context) {
        activeTimestamp = System.currentTimeMillis()
        context.dataStore.edit {
            it[activeTimestampKey] = activeTimestamp
        }
    }

    private suspend fun save(context: Context, clientId: String, sampleId: String) {
        context.dataStore.edit {
            it[clientIdKey] = clientId
            it[sampleIdKey] = sampleId
        }
    }

    private suspend fun sync(context: Context) {
        TiebaApi.getInstance()
            .syncFlow(clientId)
            .catch { it.printStackTrace() }
            .collect {
                if (clientId == it.client.clientId && sampleId == it.wlConfig.sampleId) {
                    return@collect
                }
                clientId = it.client.clientId
                sampleId = it.wlConfig.sampleId
                save(context, it.client.clientId, it.wlConfig.sampleId)
            }
    }
}