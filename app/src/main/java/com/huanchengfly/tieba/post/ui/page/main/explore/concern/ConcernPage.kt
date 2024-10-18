package com.huanchengfly.tieba.post.ui.page.main.explore.concern

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.api.models.protos.hasAgree
import com.huanchengfly.tieba.post.arch.CommonUiEvent.ScrollToTop.bindScrollToTopEvent
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.pullRefreshIndicator
import com.huanchengfly.tieba.post.ui.page.Destination.Forum
import com.huanchengfly.tieba.post.ui.page.Destination.Thread
import com.huanchengfly.tieba.post.ui.page.Destination.UserProfile
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.VerticalDivider
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ConcernPage(
    navigator: NavController,
    viewModel: ConcernViewModel = pageViewModel()
) {
    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(ConcernUiIntent.Refresh)
        viewModel.initialized = true
    }
    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = ConcernUiState::isRefreshing,
        initial = false
    )
    val isLoadingMore by viewModel.uiState.collectPartialAsState(
        prop1 = ConcernUiState::isLoadingMore,
        initial = false
    )
    val hasMore by viewModel.uiState.collectPartialAsState(
        prop1 = ConcernUiState::hasMore,
        initial = true
    )
    val nextPageTag by viewModel.uiState.collectPartialAsState(
        prop1 = ConcernUiState::nextPageTag,
        initial = ""
    )
    val data by viewModel.uiState.collectPartialAsState(
        prop1 = ConcernUiState::data,
        initial = persistentListOf()
    )
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.send(ConcernUiIntent.Refresh) })

    onGlobalEvent<GlobalEvent.Refresh>(
        filter = { it.key == "concern" }
    ) {
        viewModel.send(ConcernUiIntent.Refresh)
    }

    val lazyListState = rememberLazyListState()
    viewModel.bindScrollToTopEvent(lazyListState = lazyListState)

    Box(modifier = Modifier.pullRefresh(pullRefreshState)) {
        SwipeUpLazyLoadColumn(
            modifier = Modifier.fillMaxSize(),
            state = lazyListState,
            horizontalAlignment = Alignment.CenterHorizontally,
            isLoading = isLoadingMore,
            onLazyLoad = {
                if (hasMore) viewModel.send(ConcernUiIntent.LoadMore(nextPageTag))
            },
            onLoad = { viewModel.send(ConcernUiIntent.LoadMore(nextPageTag)) },
            bottomIndicator = { onThreshold ->
                LoadMoreIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = isLoadingMore,
                    noMore = !hasMore,
                    onThreshold = onThreshold
                )
            }
        ) {
            itemsIndexed(
                items = data,
                key = { _, item -> "${item.recommendType}_${item.threadList?.id}" },
                contentType = { _, item -> item.recommendType }
            ) { index, item ->
                Container {
                    if (item.recommendType == 1) {
                        Column {
                            FeedCard(
                                item = wrapImmutable(item.threadList!!),
                                onClick = {
                                    navigator.navigate(Thread(it.threadId, it.forumId))
                                },
                                onClickReply = {
                                    navigator.navigate(Thread(it.threadId, it.forumId, scrollToReply = true))
                                },
                                onAgree = {
                                    viewModel.send(
                                        ConcernUiIntent.Agree(it.threadId, it.firstPostId, it.hasAgree)
                                    )
                                },
                                onClickForum = { navigator.navigate(Forum(it.name)) },
                                onClickUser = { navigator.navigate(UserProfile(it.id)) },
                            )
                            if (index < data.size - 1) {
                                VerticalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    thickness = 2.dp
                                )
                            }
                        }
                    } else {
                        Box {}
                    }
                }
            }
        }

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = ExtendedTheme.colors.pullRefreshIndicator,
            contentColor = ExtendedTheme.colors.primary,
        )
    }
}