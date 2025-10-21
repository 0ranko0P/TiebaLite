package com.huanchengfly.tieba.post.ui.models

import androidx.compose.runtime.Immutable

@Immutable
class LikedForum(
    val avatar: String = "",
    val id: Long,
    val name: String = "",
    val signed: Boolean = false,
    val level: String = ""
)