package com.huanchengfly.tieba.post.ui.models.search

import androidx.compose.runtime.Immutable

/**
 * UI Model of [com.huanchengfly.tieba.post.core.network.model.SearchUserBean.UserBean]
 * */
@Immutable
class SearchUser(
    val id: Long,
    val avatar: String,
    val nickname: String,
    val username: String?,
    val intro: String?
)