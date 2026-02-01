package com.huanchengfly.tieba.post.api.models

data class PermissionListBean(
    var follow: Int = 0,
    var interact: Int = 0,
    var chat: Int = 0
)