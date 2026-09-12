package com.huanchengfly.tieba.post.ui.models.user

/**
 * UI Model of [com.huanchengfly.tieba.post.core.network.model.PermissionListBean]
 * */
data class PermissionList(
    val follow: Boolean = false,
    val interact: Boolean = false,
    val chat: Boolean = false,
)