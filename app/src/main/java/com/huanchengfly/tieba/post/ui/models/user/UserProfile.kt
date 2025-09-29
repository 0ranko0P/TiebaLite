package com.huanchengfly.tieba.post.ui.models.user

import androidx.compose.runtime.Immutable
import com.huanchengfly.tieba.post.api.models.protos.User

/**
 * UI Model of [User]
 *
 * @param following [User.has_concerned] mapped Boolean
 * @param followNum formatted [User.concern_num]
 * @param agreeNum formatted [User.total_agree_num]
 * @param privateForum 隐藏关注的吧
 * */
@Immutable
data class UserProfile(
    val uid: Long = -1,
    val portrait: String  = "",
    val name: String = "",
    val userName: String? = null,
    val tiebaUid: String = "",
    val intro: String? = null,
    val sex: String = "?",
    val tbAge: Float = 0f,
    val address: String? = null,
    val following: Boolean = false,
    val threadNum: Int = 0,
    val postNum: Int = 0,
    val forumNum: Int = 0,
    val followNum: String = 0.toString(),
    val fans: Int = 0,
    val agreeNum: String = 0.toString(),
    val bazuDesc: String? = null,
    val newGod: String? = null,
    val privateForum: Boolean = true,
    val isOfficial: Boolean = true
)