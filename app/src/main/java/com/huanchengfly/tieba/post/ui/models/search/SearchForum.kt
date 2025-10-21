package com.huanchengfly.tieba.post.ui.models.search

import androidx.compose.runtime.Immutable
import com.huanchengfly.tieba.post.api.models.SearchForumBean.ForumInfoBean

/**
 * UI Model of [ForumInfoBean]
 * */
@Immutable
class SearchForum(
    val id: Long = -1,
    val name: String,
    val avatar: String = "",
    val slogan: String? = null,
)