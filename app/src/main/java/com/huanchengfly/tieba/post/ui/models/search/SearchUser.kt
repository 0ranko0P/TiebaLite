package com.huanchengfly.tieba.post.ui.models.search

import androidx.compose.runtime.Immutable
import com.huanchengfly.tieba.post.api.models.SearchUserBean.UserBean

/**
 * UI Model of [UserBean]
 * */
@Immutable
class SearchUser(
    val id: Long,
    val avatar: String,
    val nickname: String,
    val username: String?,
    val intro: String?
)