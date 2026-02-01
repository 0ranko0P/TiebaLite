package com.huanchengfly.tieba.post.ui.models.user

import com.huanchengfly.tieba.post.api.models.PermissionListBean

/**
 * UI Model of [PermissionListBean]
 * */
data class PermissionList(
    val follow: Boolean = false,
    val interact: Boolean = false,
    val chat: Boolean = false,
) {

    constructor(bean: PermissionListBean): this(
        follow = bean.follow == 1,
        interact = bean.interact == 1,
        chat = bean.chat == 1
    )
}