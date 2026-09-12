package com.huanchengfly.tieba.post.core.network.model

data class PermissionListBean(
    var follow: Int = 0,
    var interact: Int = 0,
    var chat: Int = 0
)