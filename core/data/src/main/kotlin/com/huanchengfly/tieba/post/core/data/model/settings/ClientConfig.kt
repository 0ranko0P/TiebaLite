package com.huanchengfly.tieba.post.core.data.model.settings

data class ClientConfig(
    val clientId: String?,
    val sampleId: String?,
    val baiduId: String?,
    val activeTimestamp: Long,
    val firstInstallTime: Long?,
    val lastUpdateTime: Long?
)