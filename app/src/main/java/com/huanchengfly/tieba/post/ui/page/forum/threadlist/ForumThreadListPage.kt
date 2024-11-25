package com.huanchengfly.tieba.post.ui.page.forum.threadlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.PaddingNone
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.frsPage.Classify
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.page.Destination.ForumRuleDetail
import com.huanchengfly.tieba.post.ui.page.Destination.Thread
import com.huanchengfly.tieba.post.ui.page.Destination.UserProfile
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockTip
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.VerticalDivider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private enum class ItemType {
    Top, PlainText, SingleMedia, MultiMedia, Video
}

@Composable
private fun GoodClassifyTabs(
    goodClassifyHolders: ImmutableList<ImmutableHolder<Classify>>,
    selectedItem: Int?,
    onSelected: (Int) -> Unit,
) {
    LazyRow(
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = goodClassifyHolders,
            key = { it.get { "${class_id}_$class_name" } }
        ) { holder ->
            Chip(
                text = holder.get { class_name },
                invertColor = selectedItem == holder.get { class_id },
                onClick = { onSelected(holder.get { class_id }) }
            )
        }
    }
}

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
        Chip(
            text = type,
            shape = RoundedCornerShape(3.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.subtitle2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
            fontSize = 15.sp
        )
    }
}

@Composable
fun GoodThreadListPage(
    modifier: Modifier = Modifier,
    forumId: Long,
    forumName: String,
    sortType: () -> Int,
    listState: LazyListState,
    contentPadding: PaddingValues,
    viewModel: GoodThreadListViewModel = pageViewModel<GoodThreadListViewModel>()
) {
    LazyLoad(loaded = viewModel.initialized) {
        viewModel.onFirstLoad(forumName, forumId)
        viewModel.initialized = true
    }
    // Request from ForumPage
    onGlobalEvent<ForumThreadListUiEvent.Refresh>(filter = { it.isGood }) {
        viewModel.requestRefresh(goodClassifyId = 0)
    }

    val goodClassifyId by viewModel.uiState.collectPartialAsState(
        prop1 = ForumThreadListUiState::goodClassifyId,
        initial = null
    )
    val goodClassifies by viewModel.uiState.collectPartialAsState(
        prop1 = ForumThreadListUiState::goodClassifies,
        initial = persistentListOf()
    )

    if (goodClassifies.size <= 1) { // Unclassified
        ForumThreadListPage(modifier, forumId, true, sortType, listState, contentPadding, viewModel)
    } else {
        Column(modifier = modifier.padding(contentPadding)) {
            GoodClassifyTabs(
                goodClassifyHolders = goodClassifies,
                selectedItem = goodClassifyId,
                onSelected = viewModel::requestRefresh
            )

            ForumThreadListPage(modifier, forumId, true, sortType, listState, PaddingNone, viewModel)
        }
    }
}

@Composable
fun NormalThreadListPage(
    modifier: Modifier = Modifier,
    forumId: Long,
    forumName: String,
    sortType: () -> Int,
    listState: LazyListState,
    contentPadding: PaddingValues,
    viewModel: LatestThreadListViewModel = pageViewModel<LatestThreadListViewModel>()
) {
    LazyLoad(loaded = viewModel.initialized) {
        viewModel.onFirstLoad(forumName, forumId, sortType())
        viewModel.initialized = true
    }
    // Request from ForumPage
    onGlobalEvent<ForumThreadListUiEvent.Refresh>(filter = { !it.isGood }) {
        viewModel.requestRefresh(it.sortType)
    }

    ForumThreadListPage(modifier, forumId, false, sortType, listState, contentPadding, viewModel)
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ForumThreadListPage(
    modifier: Modifier = Modifier,
    forumId: Long,
    isGood: Boolean = false,
    sortType: () -> Int,
    listState: LazyListState,
    contentPadding: PaddingValues,
    viewModel: ForumThreadListViewModel
) {
    val context = LocalContext.current
    val navigator = LocalNavController.current
    val snackbarHostState = LocalSnackbarHostState.current

    onGlobalEvent<ForumThreadListUiEvent.BackToTop>(
        filter = { it.isGood == isGood },
    ) {
        listState.animateScrollToItem(0)
    }
    viewModel.onEvent<ForumThreadListUiEvent.AgreeFail> {
        val snackbarResult = snackbarHostState.showSnackbar(
            message = context.getString(
                R.string.snackbar_agree_fail,
                it.errorCode,
                it.errorMsg
            ),
            actionLabel = context.getString(R.string.button_retry)
        )

        if (snackbarResult == SnackbarResult.ActionPerformed) {
            viewModel.send(
                ForumThreadListUiIntent.Agree(
                    it.threadId,
                    it.postId,
                    it.hasAgree
                )
            )
        }
    }

    val isLoadingMore by viewModel.uiState.collectPartialAsState(
        prop1 = ForumThreadListUiState::isLoadingMore,
        initial = false
    )
    val hasMore by viewModel.uiState.collectPartialAsState(
        prop1 = ForumThreadListUiState::hasMore,
        initial = true
    )
    val currentPage by viewModel.uiState.collectPartialAsState(
        prop1 = ForumThreadListUiState::currentPage,
        initial = 1
    )
    val forumRuleTitle by viewModel.uiState.collectPartialAsState(
        prop1 = ForumThreadListUiState::forumRuleTitle,
        initial = null
    )
    val threadList by viewModel.uiState.collectPartialAsState(
        prop1 = ForumThreadListUiState::threadList,
        initial = persistentListOf()
    )
    val threadListIds by viewModel.uiState.collectPartialAsState(
        prop1 = ForumThreadListUiState::threadListIds,
        initial = persistentListOf()
    )

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Container {
            SwipeUpLazyLoadColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = contentPadding,
                isLoading = isLoadingMore,
                onLazyLoad = {
                    if (hasMore) {
                        viewModel.onLoadMore(currentPage, threadListIds, sortType())
                    }
                },
                onLoad = {
                    if (threadList.isNotEmpty()) {
                        viewModel.onLoadMore(currentPage, threadListIds, sortType())
                    }
                },
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
                    key = { index, holder -> "${index}_${holder.thread.item.id}" },
                    contentType = { _, holder ->
                        val item = holder.thread.item
                        when {
                            item.isTop == 1 -> ItemType.Top
                            item.media.isNotEmpty() && item.media.size == 1 -> ItemType.SingleMedia
                            item.media.isNotEmpty() && item.media.size != 1 -> ItemType.MultiMedia
                            item.videoInfo != null -> ItemType.Video
                            else -> ItemType.PlainText
                        }
                    }
                ) { index, (holder, blocked) ->
                    BlockableContent(
                        blocked = blocked,
                        blockedTip = { BlockTip(text = { Text(text = stringResource(id = R.string.tip_blocked_thread)) }) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                    ) {
                        val (item) = holder
                        if (item.isTop == 1) {
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
                                    if (threadList[index - 1].thread.get { isTop } == 1) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    VerticalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                }
                                FeedCard(
                                    item = holder,
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