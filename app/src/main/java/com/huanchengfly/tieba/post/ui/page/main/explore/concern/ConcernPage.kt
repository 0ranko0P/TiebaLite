package com.huanchengfly.tieba.post.ui.page.main.explore.concern

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.ui.page.main.explore.ConsumeThreadPageResult
import com.huanchengfly.tieba.post.ui.page.main.explore.ExplorePageItem
import com.huanchengfly.tieba.post.ui.page.main.explore.LaunchedFabStateEffect
import com.huanchengfly.tieba.post.ui.page.main.explore.createThreadClickListeners
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.ThreadContentType
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConcernPage(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    navigator: NavController,
    onHideFab: (Boolean) -> Unit,
    viewModel: ConcernViewModel = hiltViewModel(),
) {
    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = ConcernUiState::isRefreshing,
        initial = false
    )
    val isEmpty by viewModel.uiState.collectPartialAsState(
        prop1 = ConcernUiState::isEmpty,
        initial = false
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = ConcernUiState::error,
        initial = null
    )
    val isError = error != null

    val listState: LazyListState = rememberLazyListState()
    LaunchedFabStateEffect(ExplorePageItem.Concern, listState, onHideFab, isRefreshing, isError)

    val threadClickListeners = remember(navigator) {
        createThreadClickListeners(onNavigate = navigator::navigate)
    }

    // result from ThreadPage
    ConsumeThreadPageResult(navigator, viewModel::onThreadResult)

    StateScreen(
        modifier = Modifier.fillMaxSize(),
        isEmpty = isEmpty,
        isError = isError,
        isLoading = isRefreshing,
        onReload = viewModel::onRefresh,
        errorScreen = {
            ErrorScreen(error = error, modifier = Modifier.padding(contentPadding))
        }
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::onRefresh,
            contentPadding = contentPadding
        ) {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val isLoadingMore = uiState.isLoadingMore
            val hasMore = uiState.hasMore
            val data = uiState.data

            SwipeUpLazyLoadColumn(
                modifier = modifier.fillMaxSize(),
                state = listState,
                contentPadding = contentPadding,
                horizontalAlignment = Alignment.CenterHorizontally,
                isLoading = isLoadingMore,
                onLazyLoad = {
                    if (hasMore) viewModel.onLoadMore()
                },
                onLoad = null, // Disable manual load more
                bottomIndicator = { onThreshold ->
                    LoadMoreIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        isLoading = isLoadingMore,
                        noMore = !hasMore,
                        onThreshold = onThreshold
                    )
                }
            ) {
                itemsIndexed(data, key = { _, it -> it.id }, ThreadContentType) { i, thread ->
                    Column {
                        FeedCard(
                            thread = thread,
                            onClick = threadClickListeners.onClicked,
                            onLike = viewModel::onThreadLikeClicked,
                            onClickReply = threadClickListeners.onReplyClicked,
                            onClickUser = threadClickListeners.onAuthorClicked,
                            onClickForum = threadClickListeners.onForumClicked,
                        )
                        if (i < data.lastIndex) {
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 2.dp)
                        }
                    }
                }
            }
        }
    }
}
