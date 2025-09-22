package com.huanchengfly.tieba.post.ui.models

import androidx.compose.runtime.Immutable

@Immutable
class ThreadStore(
    val id: Long,
    val title: String,
    val forumName: String,
    val isDeleted: Boolean,
    val maxPid: Long,
    val markPid: Long,
    val postNo: Int,
    val count: Int,
    val author: Author,
)