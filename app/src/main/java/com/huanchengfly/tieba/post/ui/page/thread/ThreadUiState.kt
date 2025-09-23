package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.runtime.Immutable
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.repository.PageData
import com.huanchengfly.tieba.post.ui.models.PostData
import com.huanchengfly.tieba.post.ui.models.SimpleForum
import com.huanchengfly.tieba.post.ui.models.ThreadInfoData
import com.huanchengfly.tieba.post.ui.models.UserData

@Immutable
data class ThreadUiState(
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isLoadingLatestReply: Boolean = false,
    val error: Throwable? = null,
    val seeLz: Boolean = false,
    val sortType: Int = ThreadSortType.DEFAULT,
    val user: UserData? = null,
    val firstPost: PostData? = null,
    val thread: ThreadInfoData? = null,
    val tbs: String? = null,
    val data: List<PostData> = emptyList(),
    val latestPosts: List<PostData>? = null,
    val page: PageData = PageData()
) : UiState {

    val lz: UserData?
        get() = firstPost?.author

    val forum: SimpleForum?
        get() = thread?.simpleForum
}
