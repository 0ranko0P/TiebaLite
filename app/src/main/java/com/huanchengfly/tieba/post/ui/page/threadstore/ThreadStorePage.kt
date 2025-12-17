package com.huanchengfly.tieba.post.ui.page.threadstore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.LocalHabitSettings
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.ui.models.ThreadStore
import com.huanchengfly.tieba.post.ui.page.Destination.Thread
import com.huanchengfly.tieba.post.ui.page.Destination.UserProfile
import com.huanchengfly.tieba.post.ui.page.thread.ThreadFrom
import com.huanchengfly.tieba.post.ui.page.thread.ThreadSortType
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen

@Composable
fun ThreadStorePage(
    navigator: NavController,
    viewModel: ThreadStoreViewModel = hiltViewModel()
) {
    MyScaffold(
        topBar = {
            TitleCentredToolbar(
                title = stringResource(id = R.string.title_my_collect),
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = navigator::navigateUp)
                },
            )
        },
    ) { contentPadding ->
        val context = LocalContext.current
        val snackbarHostState = LocalSnackbarHostState.current

        val isRefreshing by viewModel.uiState.collectPartialAsState(
            prop1 = ThreadStoreUiState::isRefreshing,
            initial = false
        )
        val isEmpty by viewModel.uiState.collectPartialAsState(
            prop1 = ThreadStoreUiState::isEmpty,
            initial = true
        )

        val error by viewModel.uiState.collectPartialAsState(
            prop1 = ThreadStoreUiState::error,
            initial = null
        )

        LaunchedEffect(Unit) {
            viewModel.uiEvent.collect { event ->
                val message = when(event) {
                    is ThreadStoreUiEvent.Delete.Failure -> context.getString(
                        R.string.delete_store_failure,
                        event.error.getErrorMessage()
                    )

                    is ThreadStoreUiEvent.Delete.Success -> context.getString(R.string.delete_store_success)

                    else -> null
                }
                message?.let { snackbarHostState.showSnackbar(message) }
            }
        }

        StateScreen(
            isEmpty = isEmpty,
            isLoading = isRefreshing,
            error = error,
            onReload = viewModel::onRefresh,
            screenPadding = contentPadding,
        ) {
            val isLoadingMore by viewModel.uiState.collectPartialAsState(
                prop1 = ThreadStoreUiState::isLoadingMore,
                initial = false
            )
            val hasMore by viewModel.uiState.collectPartialAsState(
                prop1 = ThreadStoreUiState::hasMore,
                initial = true
            )
            val data by viewModel.uiState.collectPartialAsState(
                prop1 = ThreadStoreUiState::data,
                initial = emptyList()
            )

            val habit = LocalHabitSettings.current

            // Initialize click listeners now
            val onUserClicked: (Long) -> Unit = { navigator.navigate(UserProfile(uid = it)) }

            val onThreadClicked: (ThreadStore) -> Unit = { thread ->
                navigator.navigate(
                    route = Thread(
                        threadId = thread.id,
                        postId = thread.markPid,
                        seeLz = habit.favoriteSeeLz,
                        sortType = if (habit.favoriteDesc) ThreadSortType.BY_DESC else ThreadSortType.DEFAULT,
                        from = ThreadFrom.Store(maxPid = thread.maxPid, maxFloor = thread.postNo)
                    )
                )
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = viewModel::onRefresh,
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
            ) {
                SwipeUpLazyLoadColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                    isLoading = isLoadingMore,
                    onLazyLoad = viewModel::onLoadMore,
                    onLoad = null,
                    bottomIndicator = {
                        LoadMoreIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            isLoading = isLoadingMore,
                            noMore = !hasMore,
                            onThreshold = it
                        )
                    }
                ) {
                    items(items = data, key = { it.id }) { info ->
                        StoreItem(
                            info = info,
                            onUserClick = onUserClicked,
                            onClick = onThreadClicked,
                            onDelete = viewModel::onDelete
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreItem(
    info: ThreadStore,
    onUserClick: (uid: Long) -> Unit,
    onDelete: (ThreadStore) -> Unit,
    onClick: (ThreadStore) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasUpdate = info.count != 0 && info.postNo != 0

    LongClickMenu(
        menuContent = {
            TextMenuItem(text = R.string.title_collect_on, onClick = { onDelete(info) })
        },
        onClick = { onClick(info) }
    ) {
        val colorScheme = MaterialTheme.colorScheme
        Column(
            modifier = modifier
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            UserHeader(
                name = info.author.name,
                avatar = info.author.avatarUrl,
                onClick = {
                    onUserClick(info.author.id)
                },
                desc = if (hasUpdate) {
                    stringResource(id = R.string.tip_thread_store_update, info.postNo)
                } else {
                    null
                }
            ) {
                Spacer(Modifier.weight(1.0f))

                Surface (
                    shape = MaterialTheme.shapes.extraSmall,
                    color = colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = stringResource(id = R.string.title_forum_name, info.forumName),
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            Text(
                text = info.title,
                color = if (info.isDeleted) colorScheme.outlineVariant else colorScheme.onSurface,
                fontSize = 15.sp,
                textDecoration = if (info.isDeleted) TextDecoration.LineThrough else null
            )

            if (info.isDeleted) {
                Text(
                    text = stringResource(id = R.string.tip_thread_store_deleted),
                    fontSize = 12.sp,
                    color = colorScheme.outlineVariant
                )
            }
        }
    }
}