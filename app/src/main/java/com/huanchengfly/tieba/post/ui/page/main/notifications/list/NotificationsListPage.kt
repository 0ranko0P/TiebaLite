package com.huanchengfly.tieba.post.ui.page.main.notifications.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.PaddingNone
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.Error
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorCode
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.main.notifications.list.NotificationsListViewModel.Companion.NotificationsListVmFactory
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockTip
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.widgets.compose.EmoticonText
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.LocalAccount
import java.util.Objects

@Composable
fun NotificationsListPage(
    modifier: Modifier = Modifier,
    type: NotificationsType,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingNone,
    viewModel: NotificationsListViewModel = hiltViewModel<NotificationsListViewModel, NotificationsListVmFactory>(
        key = Objects.hash(type.name, LocalAccount.current?.uid).toString()
    ) {
        it.create(type)
    }
) {
    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(NotificationsListUiIntent.Refresh)
        viewModel.initialized = true
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val error = uiState.error

    val onRefresh: () -> Unit = {
        viewModel.send(NotificationsListUiIntent.Refresh)
    }

    StateScreen(
        isLoading = uiState.isRefreshing,
        error = error,
        onReload = onRefresh.takeIf { error?.getErrorCode() != Error.ERROR_NOT_LOGGED_IN },
        screenPadding = contentPadding,
    ) {
        val hideBlocked by viewModel.hideBlocked.collectAsStateWithLifecycle()

        NotificationsListContent(
            modifier = modifier,
            type = type,
            listState = listState,
            hideBlocked = hideBlocked,
            contentPadding = contentPadding,
            uiState = uiState,
            onRefresh = onRefresh,
            onLoadMore = {
                if (uiState.hasMore && uiState.data.isNotEmpty()) {
                    viewModel.send(NotificationsListUiIntent.LoadMore(uiState.currentPage + 1))
                }
            }
        )
    }
}

@Composable
private fun NotificationsListContent(
    modifier: Modifier = Modifier,
    type: NotificationsType,
    listState: LazyListState = rememberLazyListState(),
    hideBlocked: Boolean = false,
    contentPadding: PaddingValues = PaddingNone,
    uiState: NotificationsListUiState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
) {
    val navigator = LocalNavController.current
    val context = LocalContext.current
    val isLoadingMore = uiState.isLoadingMore

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        val blockedTip: @Composable BoxScope.() -> Unit = {
            BlockTip { Text(text = stringResource(id = R.string.tip_blocked_message)) }
        }

        SwipeUpLazyLoadColumn(
            modifier = modifier.fillMaxSize(),
            state = listState,
            contentPadding = contentPadding,
            isLoading = isLoadingMore,
            onLazyLoad = onLoadMore,
            onLoad = null, // Disable manual load!
            bottomIndicator = {
                LoadMoreIndicator(isLoading = isLoadingMore, noMore = !uiState.hasMore, onThreshold = false)
            }
        ) {
            items(items = uiState.data, key = { it.lazyListItemKey }) { info ->
                BlockableContent(
                    blocked = info.isBlocked,
                    blockedTip = blockedTip,
                    hideBlockedContent = hideBlocked
                ) {
                    Column(
                        modifier = Modifier
                            .clickable {
                                val route = if (info.isFloor) {
                                    Destination.SubPosts(
                                        threadId = info.threadId,
                                        //quotePid引用不确定，可能为postId，也可能未subPostId,导致子楼加载失败或者子回复异常
                                        //先传0，在子楼页面获取正确的postId
                                        postId = 0,
                                        subPostId = info.postId,
                                        isSheet = false,
                                    )
                                } else {
                                    Destination.Thread(threadId = info.threadId, postId = info.postId)
                                }
                                navigator.navigate(route)
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UserHeader(
                            name = info.replyUser.nameShow,
                            avatar = info.replyUser.avatarUrl,
                            onClick = {
                                navigator.navigate(Destination.UserProfile(info.replyUser.id))
                            },
                            desc = remember { DateTimeUtils.getRelativeTimeString(context, info.time) }
                        )

                        if (info.content != null) {
                            EmoticonText(text = info.content)
                        }

                        val quoteText = if (type == NotificationsType.ReplyMe && info.isFloor) {
                            info.quoteContent
                        } else {
                            info.title
                        }
                        if (quoteText.isNullOrEmpty()) return@Column

                        EmoticonText(
                            text = quoteText,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable {
                                    val route = if (info.isFloor) {
                                        Destination.SubPosts(
                                            threadId = info.threadId,
                                            postId = info.quotePid ?: 0,
                                            subPostId = info.postId,
                                            isSheet = false,
                                        )
                                    } else {
                                        Destination.Thread(info.threadId)
                                    }
                                    navigator.navigate(route)
                                }
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(8.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}