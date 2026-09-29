package com.huanchengfly.tieba.post.ui.page.search.thread

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.arch.collectCommonUiEventWithLifecycle
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.SearchThreadItem
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen

@Composable
fun SearchThreadPage(
    modifier: Modifier = Modifier,
    keyword: String,
    @SearchThreadSortType threadSortType: Int = SearchThreadSortType.NEWEST,
    contentPadding: PaddingValues,
    onNavigateForum: (Destination.Forum) -> Unit = {},
    onNavigateThread: (Destination.Thread) -> Unit = {},
    onNavigateUser: (Destination.UserProfile) -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    viewModel: SearchThreadViewModel = hiltViewModel(),
) {

    LaunchedEffect(keyword) {
        viewModel.onKeywordChanged(keyword)
    }

    LaunchedEffect(threadSortType) {
        viewModel.onSortTypeChanged(sortType = threadSortType)
    }

    viewModel.uiEvent.collectCommonUiEventWithLifecycle()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    StateScreen(
        isEmpty = uiState.isEmpty,
        isLoading = uiState.isRefreshing,
        error = uiState.error,
        onReload = viewModel::onRefresh,
        screenPadding = contentPadding,
    ) {
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = viewModel::onRefresh,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val isLoadingMore = uiState.isLoadingMore
            val onLazyLoad: () -> Unit = {
                if (uiState.hasMore && !uiState.isLoadingMore) viewModel.onLoadMore()
            }

            SwipeUpLazyLoadColumn(
                modifier = modifier.fillMaxSize(),
                state = listState,
                contentPadding = contentPadding,
                isLoading = isLoadingMore,
                onLoad = onLazyLoad,
                onLazyLoad = onLazyLoad.takeIf { uiState.hasMore },
                bottomIndicator = {
                    LoadMoreIndicator(noMore = !uiState.hasMore, onThreshold = it)
                }
            ) {
                itemsIndexed(uiState.data, key = { _, it -> it.lazyListKey }) { index, item ->
                    if (index > 0) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }

                    SearchThreadItem(
                        item = item,
                        onClick = {
                            onNavigateThread(Destination.Thread(threadId = it.tid))
                        },
                        onValidUserClick = {
                            val transitionKey = item.lazyListKey.toString()
                            onNavigateUser(Destination.UserProfile(user = item.author, transitionKey))
                        },
                        onForumClick = { (forumName, forumAvatar), transitionKey ->
                            onNavigateForum(Destination.Forum(forumName, forumAvatar, transitionKey))
                        }
                    )
                }
            }
        }
    }
}
