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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.huanchengfly.tieba.post.arch.collectPartialAsState
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
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsListPage(
    type: NotificationsType,
    contentPadding: PaddingValues = PaddingNone,
    viewModel: NotificationsListViewModel = hiltViewModel<NotificationsListViewModel, NotificationsListVmFactory>(
        key = type.name
    ) {
        it.create(type)
    }
) {
    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(NotificationsListUiIntent.Refresh)
        viewModel.initialized = true
    }
    val navigator = LocalNavController.current
    val context = LocalContext.current

    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = NotificationsListUiState::isRefreshing,
        initial = false
    )
    val isLoadingMore by viewModel.uiState.collectPartialAsState(
        prop1 = NotificationsListUiState::isLoadingMore,
        initial = false
    )
    val hasMore by viewModel.uiState.collectPartialAsState(
        prop1 = NotificationsListUiState::hasMore,
        initial = true
    )
    val data by viewModel.uiState.collectPartialAsState(
        prop1 = NotificationsListUiState::data,
        initial = persistentListOf()
    )
    val currentPage by viewModel.uiState.collectPartialAsState(
        prop1 = NotificationsListUiState::currentPage,
        initial = 1
    )

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.send(NotificationsListUiIntent.Refresh) },
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        val hideBlocked by viewModel.hideBlocked.collectAsStateWithLifecycle()
        val blockedTip: @Composable BoxScope.() -> Unit = {
            BlockTip { Text(text = stringResource(id = R.string.tip_blocked_message)) }
        }

        SwipeUpLazyLoadColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            isLoading = isLoadingMore,
            onLazyLoad = {
                if (hasMore && data.isNotEmpty()) {
                    viewModel.send(NotificationsListUiIntent.LoadMore(currentPage + 1))
                }
            },
            onLoad = null, // Disable manual load!
            bottomIndicator = {
                LoadMoreIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = isLoadingMore,
                    noMore = !hasMore,
                    onThreshold = false
                )
            }
        ) {
            items(items = data, key = { it.lazyListItemKey }) { info ->
                BlockableContent(
                    blocked = info.isBlocked,
                    blockedTip = blockedTip,
                    hideBlockedContent = hideBlocked
                ) {
                    Column(
                        modifier = Modifier
                            .clickable {
                                val postId: Long = info.postId
                                val route = if (info.isFloor) {
                                    Destination.SubPosts(info.threadId, subPostId = postId)
                                } else {
                                    Destination.Thread(info.threadId, postId = postId)
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
                                    val route = if (info.isFloor && info.quotePid != null) {
                                        Destination.SubPosts(info.threadId, postId = info.quotePid)
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