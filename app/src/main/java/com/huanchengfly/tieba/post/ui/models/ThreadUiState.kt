package com.huanchengfly.tieba.post.ui.models

import androidx.compose.runtime.Stable
import com.huanchengfly.tieba.post.api.models.protos.Anti
import com.huanchengfly.tieba.post.api.models.protos.SimpleForum
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.ui.page.thread.ThreadSortType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Stable
data class ThreadUiState(
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isLoadingLatestReply: Boolean = false,
    val error: ImmutableHolder<Throwable>? = null,

    val hasMore: Boolean = true,
    val nextPagePostId: Long = 0,
    val hasPrevious: Boolean = false,
    val currentPageMin: Int = 0,
    val currentPageMax: Int = 0,
    val totalPage: Int = 0,
    val sortType: Int = ThreadSortType.DEFAULT,
    val lz: UserData? = null,
    val user: UserData? = null,
    val firstPost: PostData? = null,
    val forum: ImmutableHolder<SimpleForum>? = null,
    val anti: ImmutableHolder<Anti>? = null,
    val data: ImmutableList<PostData> = persistentListOf(),
    val latestPosts: ImmutableList<PostData> = persistentListOf(),
) : UiState