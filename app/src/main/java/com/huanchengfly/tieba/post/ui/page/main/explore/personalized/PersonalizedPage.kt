package com.huanchengfly.tieba.post.ui.page.main.explore.personalized

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.page.Destination.Forum
import com.huanchengfly.tieba.post.ui.page.Destination.Thread
import com.huanchengfly.tieba.post.ui.page.Destination.UserProfile
import com.huanchengfly.tieba.post.ui.page.main.explore.LaunchedFabStateEffect
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockTip
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedType
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.appPreferences
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalizedPage(
    navigator: NavController,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onHideFab: (Boolean) -> Unit,
    viewModel: PersonalizedViewModel = pageViewModel(),
) {
    val context = LocalContext.current
    val listState: LazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(PersonalizedUiIntent.Refresh)
        viewModel.initialized = true
    }
    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = PersonalizedUiState::isRefreshing,
        initial = false
    )
    val isLoadingMore by viewModel.uiState.collectPartialAsState(
        prop1 = PersonalizedUiState::isLoadingMore,
        initial = false
    )
    val currentPage by viewModel.uiState.collectPartialAsState(
        prop1 = PersonalizedUiState::currentPage,
        initial = 1
    )
    val data by viewModel.uiState.collectPartialAsState(
        prop1 = PersonalizedUiState::data,
        initial = persistentListOf()
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = PersonalizedUiState::error,
        initial = null
    )
    val refreshPosition by viewModel.uiState.collectPartialAsState(
        prop1 = PersonalizedUiState::refreshPosition,
        initial = 0
    )

    val hideBlockedContent = context.appPreferences.hideBlockedContent

    val isEmpty by remember {
        derivedStateOf {
            data.isEmpty()
        }
    }

    val isError by remember {
        derivedStateOf {
            error != null
        }
    }
    var refreshCount by remember {
        mutableIntStateOf(0)
    }
    var showRefreshTip by remember {
        mutableStateOf(false)
    }

    viewModel.onEvent<PersonalizedUiEvent.RefreshSuccess> {
        refreshCount = it.count
        showRefreshTip = true
    }

    if (showRefreshTip) {
        LaunchedEffect(Unit) {
            coroutineScope.launch {
                delay(20)
                listState.scrollToItem(0, 0)
            }
            delay(2000)
            showRefreshTip = false
        }
    }
    val onRefresh: () -> Unit = { viewModel.send(PersonalizedUiIntent.Refresh) }

    LaunchedFabStateEffect(listState, onHideFab, isRefreshing, isError)

    StateScreen(
        modifier = Modifier.fillMaxSize(),
        isEmpty = isEmpty,
        isError = isError,
        isLoading = isRefreshing,
        onReload = onRefresh,
        errorScreen = {
            ErrorScreen(error = error?.item, modifier = Modifier.padding(contentPadding))
        }
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            contentPadding = contentPadding
        ) {
            SwipeUpLazyLoadColumn(
                modifier = modifier.fillMaxSize(),
                state = listState,
                contentPadding = contentPadding,
                horizontalAlignment = Alignment.CenterHorizontally,
                isLoading = isLoadingMore,
                onLazyLoad = {
                    if (data.isNotEmpty()) {
                        viewModel.send(PersonalizedUiIntent.LoadMore(currentPage + 1))
                    }
                },
                onLoad = null, // Disable manual load
                bottomIndicator = {
                    LoadMoreIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        isLoading = isLoadingMore,
                        noMore = false, // Infinite
                        onThreshold = false
                    )
                }
            ) {
                itemsIndexed(
                    items = data,
                    key = { _, it -> it.thread.info.id.toString() },
                    contentType = { _, it ->
                        when {
                            it.thread.info.videoInfo != null -> FeedType.Video
                            it.thread.info.media.size == 1 -> FeedType.SingleMedia
                            it.thread.info.media.size > 1 -> FeedType.MultiMedia
                            else -> FeedType.PlainText
                        }
                    }
                ) { index, item ->
                    val personalized = item.personalized

                    val isHidden = item.hidden
                    // TODO: Do filtering in ViewModel
                    /* remember(hiddenThreadIds, item, hidden) {
                        hiddenThreadIds.contains(item.threadId) || hidden
                    }*/

                    val isRefreshPosition by remember { derivedStateOf { index + 1 == refreshPosition } }

                    AnimatedVisibility(
                        visible = !isHidden,
                        enter = EnterTransition.None,
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            BlockableContent(
                                blocked = item.blocked,
                                blockedTip = { BlockTip(text = { Text(text = stringResource(id = R.string.tip_blocked_thread)) }) },
                                modifier = Modifier.fillMaxWidth(),
                                hideBlockedContent = hideBlockedContent
                            ) {
                                Column {
                                    FeedCard(
                                        item = item.thread,
                                        onClick = {
                                            navigator.navigate(Thread(it.id, it.forumId))
                                        },
                                        onClickReply = {
                                            navigator.navigate(Thread(it.id, it.forumId, scrollToReply = true))
                                        },
                                        onAgree = viewModel::onAgreeClicked,
                                        onClickForum = {
                                            val extraKey = item.threadId.toString()
                                            navigator.navigate(Forum(it.name, it.avatar, extraKey))
                                        },
                                        onClickUser = {
                                            navigator.navigate(UserProfile(it.id))
                                        },
                                        dislikeAction = {
                                            if (personalized == null) return@FeedCard
                                            Dislike(personalized = personalized) { clickTime, reasons ->
                                                viewModel.send(
                                                    PersonalizedUiIntent.Dislike(
                                                        forumId = item.thread.info.forumInfo?.id ?: 0,
                                                        threadId = item.threadId,
                                                        reasons = reasons,
                                                        clickTime = clickTime
                                                    )
                                                )
                                            }
                                        }
                                    )

                                    val showDivider by remember {
                                        derivedStateOf {
                                            !isHidden && !isRefreshPosition && index < data.lastIndex
                                        }
                                    }
                                    if (showDivider) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            thickness = 2.dp
                                        )
                                    }
                                }
                            }
                            if (isRefreshPosition) {
                                RefreshTip {
                                    viewModel.send(PersonalizedUiIntent.Refresh)
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showRefreshTip,
                enter = fadeIn() + slideInVertically(),
                exit = slideOutVertically() + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                RefreshTip(
                    modifier = Modifier
                        .padding(contentPadding)
                        .padding(top = 12.dp),
                    refreshCount = refreshCount
                )
            }
        }
    }
}

@Composable
private fun RefreshTip(modifier: Modifier = Modifier, refreshCount: Int) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 4.dp
    ) {
        Text(
            text = stringResource(id = R.string.toast_feed_refresh, refreshCount),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun RefreshTip(onRefresh: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRefresh)
            .padding(8.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = stringResource(id = R.string.tip_refresh),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Preview("RefreshTip", backgroundColor = 0xFFFFFFFF)
@Composable
private fun RefreshTipPreview() = TiebaLiteTheme {
    Surface {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RefreshTip(refreshCount = Int.MAX_VALUE)
            RefreshTip(onRefresh = { })
        }
    }
}
