package com.huanchengfly.tieba.post.core.network.model.web

data class SignDataBean(
    val forumName: String,
    val forumId: String,
    val userName: String,
    val tbs: String,
    val canUseMSign: Boolean = false
)