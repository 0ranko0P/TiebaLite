package com.huanchengfly.tieba.post.ui.models

import androidx.compose.runtime.Immutable
import com.huanchengfly.tieba.post.api.models.protos.User

/**
 * UI Model of [User].
 *
 * @param id 用户 UID
 * @param name 用户名
 * @param nameShow 昵称
 * @param avatarUrl 头像 URL
 * @param portrait 用户肖像
 * @param ip IP 属地
 * @param levelId 吧等级
 * @param bawuType 吧务
 * @param isLz 是楼主
 * */
@Immutable
class UserData(
    val id: Long,
    val name: String,
    val nameShow: String,
    val avatarUrl: String,
    val portrait: String,
    val ip: String,
    val levelId: Int,
    val bawuType: String?,
    val isLz: Boolean
)