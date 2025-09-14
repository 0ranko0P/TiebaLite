package com.huanchengfly.tieba.post.ui.models

import androidx.compose.runtime.Immutable

@Immutable
class ThreadItemList(
    val threads: List<ThreadItemData>,
    val threadIds: List<Long>,
    val hasMore: Boolean
)