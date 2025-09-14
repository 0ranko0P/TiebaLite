package com.huanchengfly.tieba.post.ui.page.threadstore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.ThreadStoreBean
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.collectPreferenceAsState
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.ui.page.Destination.Thread
import com.huanchengfly.tieba.post.ui.page.Destination.UserProfile
import com.huanchengfly.tieba.post.ui.page.thread.ThreadFrom
import com.huanchengfly.tieba.post.ui.page.thread.ThreadSortType
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils.Companion.KEY_COLLECTED_DESC
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils.Companion.KEY_COLLECTED_SEE_LZ

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadStorePage(
    navigator: NavController,
    viewModel: ThreadStoreViewModel = pageViewModel()
) {
    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(ThreadStoreUiIntent.Refresh)
        viewModel.initialized = true
    }

    MyScaffold(
        topBar = {
            TitleCentredToolbar(
                title = stringResource(id = R.string.title_my_collect),
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = navigator::navigateUp)
                },
            )
        },
    ) { contentPaddings ->
        val context = LocalContext.current
        val snackbarHostState = LocalSnackbarHostState.current

        val isRefreshing by viewModel.uiState.collectPartialAsState(
            prop1 = ThreadStoreUiState::isRefreshing,
            initial = false
        )
        val isLoadingMore by viewModel.uiState.collectPartialAsState(
            prop1 = ThreadStoreUiState::isLoadingMore,
            initial = false
        )
        val hasMore by viewModel.uiState.collectPartialAsState(
            prop1 = ThreadStoreUiState::hasMore,
            initial = true
        )
        val currentPage by viewModel.uiState.collectPartialAsState(
            prop1 = ThreadStoreUiState::currentPage,
            initial = 0
        )
        val data by viewModel.uiState.collectPartialAsState(
            prop1 = ThreadStoreUiState::data,
            initial = emptyList()
        )
        val error by viewModel.uiState.collectPartialAsState(
            prop1 = ThreadStoreUiState::error,
            initial = null
        )
        val isError by remember { derivedStateOf { error != null } }

        viewModel.onEvent<ThreadStoreUiEvent.Delete.Failure> {
            snackbarHostState.showSnackbar(
                context.getString(R.string.delete_store_failure, it.errorMsg)
            )
        }
        viewModel.onEvent<ThreadStoreUiEvent.Delete.Success> {
            snackbarHostState.showSnackbar(context.getString(R.string.delete_store_success))
        }

        StateScreen(
            isEmpty = data.isEmpty(),
            isError = isError,
            isLoading = isRefreshing,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPaddings),
            onReload = {
                viewModel.send(ThreadStoreUiIntent.Refresh)
            },
            errorScreen = {
                error?.item?.let { ErrorScreen(error = it) }
            }
        ) {
            val dataStore = context.dataStore
            val seeLz by dataStore.collectPreferenceAsState(booleanPreferencesKey(KEY_COLLECTED_SEE_LZ), false)
            val descSort by dataStore.collectPreferenceAsState(booleanPreferencesKey(KEY_COLLECTED_DESC), false)

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.send(ThreadStoreUiIntent.Refresh) }
            ) {
                SwipeUpLazyLoadColumn(
                    modifier = Modifier.fillMaxSize(),
                    isLoading = isLoadingMore,
                    onLazyLoad = {
                        if (hasMore) viewModel.send(ThreadStoreUiIntent.LoadMore(currentPage + 1))
                    },
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
                    items(items = data, key = { it.threadId }) { info ->
                        StoreItem(
                            info = info,
                            onUserClick = {
                                info.author.lzUid?.let {
                                    navigator.navigate(UserProfile(it.toLong()))
                                }
                            },
                            onClick = {
                                navigator.navigate(
                                    Thread(
                                        threadId = info.threadId.toLong(),
                                        postId = info.markPid.toLong(),
                                        seeLz = seeLz,
                                        sortType = if(descSort) ThreadSortType.BY_DESC else ThreadSortType.DEFAULT,
                                        from = ThreadFrom.Store(
                                            maxPid = info.maxPid.toLong(),
                                            maxFloor = info.postNo
                                        )
                                    )
                                )
                            },
                            onDelete = {
                                viewModel.send(ThreadStoreUiIntent.Delete(info.threadId))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreItem(
    info: ThreadStoreBean.ThreadStoreInfo,
    onUserClick: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasUpdate = info.count != 0 && info.postNo != 0
    val isDeleted = info.isDeleted == 1

    LongClickMenu(
        menuContent = {
            TextMenuItem(text = R.string.title_collect_on, onClick = onDelete)
        },
        onClick = onClick
    ) {
        val colorScheme = MaterialTheme.colorScheme
        Column(
            modifier = modifier
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            UserHeader(
                portrait = info.author.userPortrait,
                name = info.author.name ?: "",
                nameShow = info.author.nameShow,
                onClick = onUserClick,
                desc = if (hasUpdate) {
                    stringResource(id = R.string.tip_thread_store_update, info.postNo)
                } else {
                    null
                }
            ) {
                Spacer(Modifier.weight(1.0f))

                Box(
                    modifier = Modifier
                        .background(colorScheme.secondaryContainer, MaterialTheme.shapes.extraSmall)
                        .padding(vertical = 4.dp, horizontal = 12.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.title_forum_name, info.forumName),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            Text(
                text = info.title,
                fontSize = 15.sp,
                color = if (isDeleted) colorScheme.outlineVariant else colorScheme.onSurface,
            )
            if (isDeleted) {
                Text(
                    text = stringResource(id = R.string.tip_thread_store_deleted),
                    fontSize = 12.sp,
                    color = colorScheme.outlineVariant
                )
            }
        }
    }
}