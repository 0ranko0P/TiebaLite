package com.huanchengfly.tieba.post.ui.page.main.notifications.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.pullRefreshIndicator
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockTip
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.EmoticonText
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeader
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NotificationsListPage(
    type: NotificationsType,
    viewModel: NotificationsListViewModel = when (type) {
        NotificationsType.ReplyMe -> pageViewModel<NotificationsListUiIntent, ReplyMeListViewModel>()
        NotificationsType.AtMe -> pageViewModel<NotificationsListUiIntent, AtMeListViewModel>()
    }
) {
    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(NotificationsListUiIntent.Refresh)
        viewModel.initialized = true
    }
    val navigator = LocalNavController.current
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
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.send(NotificationsListUiIntent.Refresh) }
    )
    val lazyListState = rememberLazyListState()
    Container(
        modifier = Modifier.pullRefresh(pullRefreshState)
    ) {
        SwipeUpLazyLoadColumn(
            modifier = Modifier.fillMaxSize(),
            state = lazyListState,
            contentPadding = PaddingValues(vertical = 4.dp),
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
            items(
                items = data,
                key = { "${it.info.postId}_${it.info.replyer?.id}_${it.info.time}" },
            ) { (info, blocked) ->
                BlockableContent(
                    blocked = blocked,
                    blockedTip = {
                        BlockTip {
                            Text(text = stringResource(id = R.string.tip_blocked_message))
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .clickable {
                                val threadId: Long = info.threadId!!.toLong()
                                val postId: Long = info.postId!!.toLong()
                                if (info.isFloor == "1") {
                                    navigator.navigate(
                                        Destination.SubPosts(threadId, subPostId = postId)
                                    )
                                } else {
                                    navigator.navigate(
                                        Destination.Thread(threadId, postId = postId)
                                    )
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (info.replyer != null) {
                            UserHeader(
                                name = info.replyer.name ?: "",
                                nameShow = info.replyer.nameShow,
                                portrait = info.replyer.portrait,
                                onClick = {
                                    navigator.navigate(Destination.UserProfile(info.replyer.id!!.toLong()))
                                },
                                desc = DateTimeUtils.getRelativeTimeString(
                                    LocalContext.current,
                                    info.time!!
                                )
                            )
                        }
                        EmoticonText(text = info.content ?: "")
                        val quoteText = if (type == NotificationsType.ReplyMe) {
                            if ("1" == info.isFloor) {
                                info.quoteContent
                            } else {
                                stringResource(
                                    id = R.string.text_message_list_item_reply_my_thread,
                                    info.title ?: ""
                                )
                            }
                        } else {
                            info.title
                        }
                        if (quoteText.isNullOrEmpty()) return@Column

                        EmoticonText(
                            text = quoteText,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    val threadId = info.threadId!!.toLong()
                                    if ("1" == info.isFloor && info.quotePid != null) {
                                        navigator.navigate(
                                            Destination.SubPosts(
                                                threadId = threadId,
                                                postId = info.quotePid.toLong()
                                            )
                                        )
                                    } else {
                                        navigator.navigate(Destination.Thread(threadId = threadId))
                                    }
                                }
                                .background(ExtendedTheme.colors.chip, RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            color = ExtendedTheme.colors.onChip,
                            fontSize = 12.sp,
                        )
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