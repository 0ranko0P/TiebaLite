package com.huanchengfly.tieba.post.ui.models

import android.content.Context
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.bawuType
import com.huanchengfly.tieba.post.utils.StringUtil

/**
 * Represents [User] in UI
 * */
data class UserData(
    val id: Long,
    val name: String,
    val nameShow: String,
    val avatarUrl: String,
    val portrait: String,
    val ip: String,
    val levelId: Int,
    val bawuType: String?,
    val isLz: Boolean
) {
    constructor(user: User, isLz: Boolean) : this(
        user.id,
        user.name,
        user.nameShow,
        StringUtil.getAvatarUrl(user.portrait),
        user.portrait,
        user.ip_address,
        user.level_id,
        user.bawuType,
        isLz
    )

    fun getDisplayName(context: Context): String = StringUtil.getUserNameString(context, name, nameShow)

    companion object {

        val Empty = UserData(-1, "?", "bug", "", "", "Void", 2, null, false)
    }
}