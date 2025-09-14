package com.huanchengfly.tieba.post.ui.page.main.explore.concern

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.api.models.protos.hasAgree
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.models.ThreadInfoItem
import com.huanchengfly.tieba.post.ui.page.Destination.Forum
import com.huanchengfly.tieba.post.ui.page.Destination.Thread
import com.huanchengfly.tieba.post.ui.page.Destination.UserProfile
import com.huanchengfly.tieba.post.ui.page.main.explore.LaunchedFabStateEffect
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConcernPage(
    navigator: NavController,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onHideFab: (Boolean) -> Unit,
    viewModel: ConcernViewModel = pageViewModel(),
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

    val listState: LazyListState = rememberLazyListState()

    LaunchedFabStateEffect(listState, onHideFab, isRefreshing, false)

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.send(ConcernUiIntent.Refresh) },
        contentPadding = contentPadding
    ) {
        SwipeUpLazyLoadColumn(
            modifier = modifier.fillMaxSize(),
            state = listState,
            contentPadding = contentPadding,
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
                Box {
                    if (item.recommendType == 1 && item.threadList != null) {
                        Column {
                            FeedCard(
                                item = ThreadInfoItem(item.threadList),
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
                                onClickForum = {
                                    val extraKey = item.threadList.threadId.toString()
                                    navigator.navigate(route = Forum(it.name, it.avatar, extraKey))
                                },
                                onClickUser = { navigator.navigate(UserProfile(it.id)) },
                            )
                            if (index < data.lastIndex) {
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 2.dp)
                            }
                        }
                    } else {
                        Box {}
                    }
                }
            }
        }
    }
}
