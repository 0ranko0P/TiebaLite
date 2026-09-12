package com.huanchengfly.tieba.post.ui.page.forum.generaltablist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.core.network.model.protos.FrsTabInfo
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.forum.generaltablist.GeneralTabListViewModel.Companion.GeneralTabListVMFactory
import com.huanchengfly.tieba.post.ui.page.forum.threadlist.forumThreadList
import com.huanchengfly.tieba.post.ui.page.main.explore.ConsumeThreadPageResult
import com.huanchengfly.tieba.post.ui.page.main.explore.ThreadClickListeners
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import kotlinx.collections.immutable.persistentListOf
import java.util.Objects

@Composable
fun GeneralTabListPage(
    forumId: Long,
    forumName: String,
    initialSortType: Int,
    navTabInfo: FrsTabInfo,
    threadClickListeners: ThreadClickListeners,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues.Zero,
    listState: LazyListState = rememberLazyListState(),
    viewModel: GeneralTabListViewModel = hiltViewModel<GeneralTabListViewModel, GeneralTabListVMFactory>(
        key = Objects.hash(forumId, forumName, navTabInfo.tabId).toString()
    ) {
        it.create(forumName, forumId, navTabInfo, initialSortType)
    },
) {
    val navigator = LocalNavController.current

    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = GeneralTabListUiState::isRefreshing,
        initial = false
    )
    val threadList by viewModel.uiState.collectPartialAsState(
        prop1 = GeneralTabListUiState::threads,
        initial = persistentListOf()
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = GeneralTabListUiState::error,
        initial = null
    )

    onGlobalEvent<GeneralTabListUiEvent.SortTypeChanged>(filter = { it.tabId == navTabInfo.tabId }) {
        viewModel.onSortTypeChanged(sortType = it.sortType)
    }

    onGlobalEvent<GeneralTabListUiEvent.Refresh>(filter = { it.tabId == navTabInfo.tabId }) {
        viewModel.onRefresh()
    }

    ConsumeThreadPageResult<Destination.Forum>(navigator, viewModel::onThreadResult)

    StateScreen(
        isEmpty = threadList.isEmpty(),
        isLoading = isRefreshing,
        error = error,
        screenPadding = contentPadding,
    ) {
        val hideBlocked by viewModel.hideBlocked.collectAsStateWithLifecycle()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        Container {
            SwipeUpLazyLoadColumn(
                modifier = modifier.fillMaxSize(),
                state = listState,
                contentPadding = contentPadding,
                isLoading = uiState.isLoadingMore,
                onLoad = viewModel::loadMore,
                onLazyLoad = viewModel::loadMore.takeIf { uiState.hasMore },
                bottomIndicator = {
                    LoadMoreIndicator(noMore = !uiState.hasMore, onThreshold = it)
                }
            ) {
                if (navTabInfo.sub_tab_list.isNotEmpty()) {
                    item {
                        LazyRow(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(items = navTabInfo.sub_tab_list, key = { it.class_id }) { menu ->
                                Chip(text = menu.class_name)
                            }
                        }
                    }
                }

                forumThreadList(
                    threads = threadList,
                    threadClickListeners = threadClickListeners,
                    onLikeClicked = viewModel::onThreadLikeClicked,
                    onOriginThreadClicked = {
                        val route = Destination.Thread(threadId = it.tid.toLong(), forumId = it.fid)
                        navigator.navigateDebounced(route)
                    },
                    hideBlocked = hideBlocked,
                )
            }
        }
    }
}
