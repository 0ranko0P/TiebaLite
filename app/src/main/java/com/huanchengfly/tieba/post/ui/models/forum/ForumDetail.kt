package com.huanchengfly.tieba.post.ui.models.forum

import androidx.compose.runtime.Immutable
import com.huanchengfly.tieba.post.ui.models.Author

typealias ForumManager = Author

@Immutable
class ForumDetail(
    val avatar: String,
    val name: String,
    val id: Long,
    val intro: String = "",
    val slogan: String = "",
    val memberCount: Int = 0,
    val threadCount: Int = 0,
    val postCount: Int = 0,
    val managers: List<ForumManager>? = null
)