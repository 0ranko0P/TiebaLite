package com.huanchengfly.tieba.post.ui.page.search.thread

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.pullRefreshIndicator
import com.huanchengfly.tieba.post.ui.page.Destination.Forum
import com.huanchengfly.tieba.post.ui.page.Destination.Thread
import com.huanchengfly.tieba.post.ui.page.Destination.UserProfile
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.search.SearchUiEvent
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalShouldLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.SearchThreadItem
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.VerticalDivider
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SearchThreadPage(
    keyword: String,
    initialSortType: Int = SearchThreadSortType.SORT_TYPE_NEWEST,
    contentPadding: PaddingValues,
    listState: LazyListState = rememberLazyListState(),
    viewModel: SearchThreadViewModel = pageViewModel(),
) {
    val navigator = LocalNavController.current
    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(SearchThreadUiIntent.Refresh(keyword, initialSortType))
        viewModel.initialized = true
    }
    val currentKeyword by viewModel.uiState.collectPartialAsState(
        prop1 = SearchThreadUiState::keyword,
        initial = ""
    )
    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = SearchThreadUiState::isRefreshing,
        initial = true
    )
    val isLoadingMore by viewModel.uiState.collectPartialAsState(
        prop1 = SearchThreadUiState::isLoadingMore,
        initial = false
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = SearchThreadUiState::error,
        initial = null
    )
    val data by viewModel.uiState.collectPartialAsState(
        prop1 = SearchThreadUiState::data,
        initial = persistentListOf()
    )
    val currentPage by viewModel.uiState.collectPartialAsState(
        prop1 = SearchThreadUiState::currentPage,
        initial = 1
    )
    val hasMore by viewModel.uiState.collectPartialAsState(
        prop1 = SearchThreadUiState::hasMore,
        initial = true
    )
    val sortType by viewModel.uiState.collectPartialAsState(
        prop1 = SearchThreadUiState::sortType,
        initial = initialSortType
    )

    onGlobalEvent<SearchThreadUiEvent.SwitchSortType> {
        viewModel.send(SearchThreadUiIntent.Refresh(keyword, it.sortType))
    }
    val shouldLoad = LocalShouldLoad.current
    LaunchedEffect(currentKeyword) {
        if (currentKeyword.isNotEmpty() && keyword != currentKeyword) {
            if (shouldLoad) {
                viewModel.send(SearchThreadUiIntent.Refresh(keyword, sortType))
            } else {
                viewModel.initialized = false
            }
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.send(SearchThreadUiIntent.Refresh(keyword, sortType)) }
    )

    val isEmpty by remember {
        derivedStateOf { data.isEmpty() }
    }

    onGlobalEvent<SearchUiEvent.KeywordChanged> {
        viewModel.send(SearchThreadUiIntent.Refresh(it.keyword, sortType))
    }

    StateScreen(
        modifier = Modifier.fillMaxSize(),
        isEmpty = isEmpty,
        isError = error != null,
        isLoading = isRefreshing,
        onReload = { viewModel.send(SearchThreadUiIntent.Refresh(keyword, sortType)) },
        errorScreen = { error?.let { ErrorScreen(error = it.item) } }
    ) {
        Box(
            modifier = Modifier.pullRefresh(pullRefreshState)
        ) {
            SwipeUpLazyLoadColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = contentPadding,
                isLoading = isLoadingMore,
                onLazyLoad = {
                    if (hasMore) {
                        viewModel.send(SearchThreadUiIntent.LoadMore(keyword, currentPage, sortType))
                    }
                },
                onLoad = null, // Refuse manual load more
                bottomIndicator = {
                    LoadMoreIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        isLoading = isLoadingMore,
                        noMore = !hasMore,
                        onThreshold = false
                    )
                }
            ) {
                itemsIndexed(data) { index, item ->
                    if (index > 0) {
                        VerticalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    SearchThreadItem(
                        item = item,
                        onClick = {
                            navigator.navigate(Thread(threadId = it.tid.toLong()))
                        },
                        onUserClick = {
                            navigator.navigate(UserProfile(it.userId.toLong()))
                        },
                        onForumClick = { forum, transitionKey ->
                            navigator.navigate(Forum(forum.forumName, forum.avatar, transitionKey))
                        },
                        searchKeyword = keyword
                    )
                }
            }

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier
                    .padding(contentPadding)
                    .align(Alignment.TopCenter),
                backgroundColor = ExtendedTheme.colors.pullRefreshIndicator,
                contentColor = ExtendedTheme.colors.primary,
            )
        }
    }
}
