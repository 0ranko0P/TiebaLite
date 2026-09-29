package com.huanchengfly.tieba.post.ui.page.main.explore.concern

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.arch.collectCommonUiEventWithLifecycle
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.ui.page.main.explore.LaunchedFabStateEffect
import com.huanchengfly.tieba.post.ui.page.main.explore.ThreadClickListeners
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.ThreadContentType
import com.huanchengfly.tieba.post.ui.widgets.compose.defaultBottomIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen

@Composable
fun ConcernPage(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    threadClickListeners: ThreadClickListeners,
    listState: LazyListState = rememberLazyListState(),
    onHideFab: (Boolean) -> Unit,
    viewModel: ConcernViewModel = hiltViewModel(),
) {
    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = ConcernUiState::isRefreshing,
        initial = true
    )
    val isEmpty by viewModel.uiState.collectPartialAsState(
        prop1 = ConcernUiState::isEmpty,
        initial = true
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = ConcernUiState::error,
        initial = null
    )

    viewModel.uiEvent.collectCommonUiEventWithLifecycle()

    LaunchedFabStateEffect(listState, onHideFab, isRefreshing, isError = error != null)

    StateScreen(
        isEmpty = isEmpty,
        isLoading = isRefreshing && isEmpty,
        error = error,
        onReload = viewModel::onRefresh,
        screenPadding = contentPadding,
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::onRefresh,
            contentPadding = contentPadding
        ) {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val isLoadingMore = uiState.isLoadingMore
            val data = uiState.data

            SwipeUpLazyLoadColumn(
                modifier = modifier.fillMaxSize(),
                state = listState,
                contentPadding = contentPadding,
                isLoading = isLoadingMore,
                onLazyLoad = viewModel::onLoadMore.takeIf { uiState.hasMore },
                bottomIndicator = defaultBottomIndicator,
            ) {
                itemsIndexed(data, key = { _, it -> it.id }, ThreadContentType) { i, thread ->
                    FeedCard(
                        thread = thread,
                        onClick = threadClickListeners.onClicked,
                        onLike = viewModel::onThreadLikeClicked,
                        onClickReply = threadClickListeners.onReplyClicked,
                        onClickUser = threadClickListeners.onAuthorClicked,
                        onClickForum = threadClickListeners.onForumClicked,
                        onClickOriginThread = threadClickListeners.onOriginThreadClicked,
                        cardDivider = i < data.lastIndex
                    )
                }
            }
        }
    }
}
