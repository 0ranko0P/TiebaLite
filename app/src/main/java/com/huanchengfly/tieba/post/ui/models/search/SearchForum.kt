package com.huanchengfly.tieba.post.ui.models.search

import androidx.compose.runtime.Immutable
import com.huanchengfly.tieba.post.api.models.SearchForumBean.ForumInfoBean

/**
 * UI Model of [ForumInfoBean]
 *
 * @param id forum ID
 * @param name forum name
 * @param avatar avatar URL
 * @param postNum formatted post count of this forum
 * @param concernNum formatted follower count of this forum
 * @param slogan forum slogan
 * */
@Immutable
class SearchForum(
    val id: Long = -1,
    val name: String,
    val avatar: String = "",
    val postNum: String? = null,
    val concernNum: String? = null,
    val slogan: String? = null,
)