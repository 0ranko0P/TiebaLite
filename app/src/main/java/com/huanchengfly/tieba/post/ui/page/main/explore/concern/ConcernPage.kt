package com.huanchengfly.tieba.post.ui.page.main.explore.concern

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.arch.collectCommonUiEventWithLifecycle
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.core.ui.component.FeedGridItemSpacing
import com.huanchengfly.tieba.post.core.ui.component.FeedLazyVerticalStaggeredGrid
import com.huanchengfly.tieba.post.core.ui.component.defaultGridCells
import com.huanchengfly.tieba.post.ui.page.main.explore.LaunchedFabStateEffect
import com.huanchengfly.tieba.post.ui.page.main.explore.ThreadClickListeners
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.ThreadContentType
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen

@Composable
fun ConcernPage(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    threadClickListeners: ThreadClickListeners,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
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

    LaunchedFabStateEffect(gridState, onHideFab, isRefreshing, isError = error != null)

    StateScreen(
        isEmpty = isEmpty,
        isLoading = isRefreshing && isEmpty,
        error = error,
        onReload = viewModel::onRefresh,
        screenPadding = contentPadding,
    ) {
        PullToRefreshBox(
            modifier = Modifier.padding(horizontal = FeedGridItemSpacing),
            isRefreshing = isRefreshing,
            onRefresh = viewModel::onRefresh,
            contentPadding = contentPadding
        ) {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val itemModifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))

            FeedLazyVerticalStaggeredGrid(
                columns = defaultGridCells(),
                state = gridState,
                modifier = modifier.fillMaxSize(),
                contentPadding = contentPadding,
                isLoading = { uiState.isLoadingMore },
                onLazyLoad = viewModel::onLoadMore.takeIf { uiState.hasMore },
            ) {
                itemsIndexed(uiState.data, key = { _, it -> it.id }, ThreadContentType) { _, thread ->
                    FeedCard(
                        modifier = itemModifier,
                        thread = thread,
                        onClick = threadClickListeners.onClicked,
                        onLike = viewModel::onThreadLikeClicked,
                        onClickReply = threadClickListeners.onReplyClicked,
                        onClickUser = threadClickListeners.onAuthorClicked,
                        onClickForum = threadClickListeners.onForumClicked,
                        onClickOriginThread = threadClickListeners.onOriginThreadClicked,
                    )
                }
            }
        }
    }
}
