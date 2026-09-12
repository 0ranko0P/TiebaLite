package com.huanchengfly.tieba.post.core.network.session

interface ClientConfigProvider {

    val activeTimestamp: Long

    fun getClientId(): String?

    fun getSampleId(): String?

    fun getBaiduId(): String?

    fun saveBaiduId(id: String?)
}