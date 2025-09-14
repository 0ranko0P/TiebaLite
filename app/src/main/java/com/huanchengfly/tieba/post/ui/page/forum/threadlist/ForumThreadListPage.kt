package com.huanchengfly.tieba.post.ui.page.forum.threadlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.ui.page.Destination.ForumRuleDetail
import com.huanchengfly.tieba.post.ui.page.Destination.Thread
import com.huanchengfly.tieba.post.ui.page.Destination.UserProfile
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.forum.threadlist.ForumThreadListViewModel.Companion.ForumVMFactory
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockTip
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedType
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import java.util.Objects

@Composable
private fun TopThreadItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: String = stringResource(id = R.string.content_top),
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            color = MaterialTheme.colorScheme.onSurface,
            text = type,
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.extraSmall
                )
                .padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

private val ThreadBlockedTip: @Composable BoxScope.() -> Unit = {
    BlockTip(
        text = {
            Text(text = stringResource(id = R.string.tip_blocked_thread))
        }
    )
}

@Composable
fun ForumThreadList(
    modifier: Modifier = Modifier,
    forumId: Long,
    forumName: String,
    type: ForumType,
    forumRuleTitle: String?,
    contentPadding: PaddingValues,
    viewModel: ForumThreadListViewModel = hiltViewModel<ForumThreadListViewModel, ForumVMFactory>(
        key = Objects.hash(forumId, forumName, type).toString()
    ) {
        it.create(forumName, forumId, type = type)
    }
) {
    val isGood = type == ForumType.Good
    val navigator = LocalNavController.current
    val snackbarHostState = LocalSnackbarHostState.current
    val listState = rememberLazyListState()

    onGlobalEvent<ForumThreadListUiEvent.BackToTop>(
        filter = { it.isGood == isGood },
    ) {
        listState.scrollToItem(0)
    }

    onGlobalEvent<ForumThreadListUiEvent.Refresh> {
        viewModel.onRefresh()
    }

    if (isGood) {
        onGlobalEvent<ForumThreadListUiEvent.ClassifyChanged> {
            viewModel.onClassifyIdChanged(classifyId = it.goodClassifyId)
        }
    } else {
        onGlobalEvent<ForumThreadListUiEvent.SortTypeChanged> {
            viewModel.onSortTypeChanged(sortType = it.sortType)
        }
    }

    val threadList by viewModel.uiState.collectPartialAsState(
        prop1 = ForumThreadListUiState::threads,
        initial = emptyList()
    )
    val isLoading by viewModel.uiState.collectPartialAsState(
        prop1 = ForumThreadListUiState::isRefreshing,
        initial = true
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = ForumThreadListUiState::error,
        initial = null
    )

    StateScreen(
        modifier = Modifier.fillMaxSize(),
        isEmpty = threadList.isEmpty(),
        isError = error != null,
        isLoading = isLoading,
        errorScreen = {
            error?.let { ErrorScreen(it, modifier = Modifier.padding(contentPadding)) }
        }
    ) {
        val hideBlocked by viewModel.hideBlocked.collectAsStateWithLifecycle()

        val isLoadingMore by viewModel.uiState.collectPartialAsState(
            prop1 = ForumThreadListUiState::isLoadingMore,
            initial = false
        )
        val hasMore by viewModel.uiState.collectPartialAsState(
            prop1 = ForumThreadListUiState::hasMore,
            initial = true
        )

        Container {
            SwipeUpLazyLoadColumn(
                modifier = modifier.fillMaxSize(),
                state = listState,
                contentPadding = contentPadding,
                isLoading = isLoadingMore,
                onLazyLoad = {
                    if (hasMore) {
                        viewModel.loadMore()
                    }
                },
                onLoad = viewModel::loadMore.takeUnless{ threadList.isEmpty() },
                bottomIndicator = { onThreshold ->
                    LoadMoreIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        isLoading = isLoadingMore,
                        noMore = !hasMore,
                        onThreshold = onThreshold
                    )
                }
            ) {
                forumRuleTitle?.let { rule ->
                    if (rule.isEmpty()) return@let
                    item(key = "ForumRule") {
                        TopThreadItem(
                            title = rule,
                            onClick = { navigator.navigate(ForumRuleDetail(forumId)) },
                            modifier = Modifier.fillMaxWidth(),
                            type = stringResource(id = R.string.desc_forum_rule)
                        )
                    }
                }

                itemsIndexed(
                    items = threadList,
                    key = { _, item -> item.threadId },
                    contentType = { _, holder ->
                        val item: ThreadInfo = holder.thread.info
                        when {
                            item.isTop == 1 -> FeedType.Top
                            item.media.size == 1 -> FeedType.SingleMedia
                            item.media.size > 1 -> FeedType.MultiMedia
                            item.videoInfo != null -> FeedType.Video
                            else -> FeedType.PlainText
                        }
                    }
                ) { index, item ->
                    BlockableContent(
                        blocked = item.blocked,
                        blockedTip = ThreadBlockedTip,
                        hideBlockedContent = hideBlocked
                    ) {
                        if (item.isTop) {
                            TopThreadItem(
                                title = item.title,
                                onClick = {
                                    navigator.navigate(
                                        Thread(item.threadId, forumId = item.forumId)
                                    )
                                }
                            )
                        } else {
                            Column {
                                if (index > 0) {
                                    if (threadList[index - 1].isTop) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                }
                                FeedCard(
                                    item = item.thread,
                                    onClick = {
                                        navigator.navigate(
                                            Thread(item.threadId, forumId = item.forumId)
                                        )
                                    },
                                    onClickReply = {
                                        navigator.navigate(
                                            Thread(it.threadId, forumId = it.forumId, scrollToReply = true)
                                        )
                                    },
                                    onAgree = viewModel::onAgree,
                                    onClickOriginThread = {
                                        navigator.navigate(
                                            Thread(threadId = it.tid.toLong(), forumId = it.fid)
                                        )
                                    },
                                    onClickUser = {
                                        navigator.navigate(UserProfile(it.id))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}