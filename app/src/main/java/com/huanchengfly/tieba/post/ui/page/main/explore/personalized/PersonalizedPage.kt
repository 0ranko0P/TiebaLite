package com.huanchengfly.tieba.post.ui.page.main.explore.personalized

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.CommonUiEvent.ScrollToTop.bindScrollToTopEvent
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.pullRefreshIndicator
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.ForumPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockTip
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.VerticalDivider
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PersonalizedPage(
    viewModel: PersonalizedViewModel = pageViewModel()
) {
    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(PersonalizedUiIntent.Refresh)
        viewModel.initialized = true
    }
    val navigator = LocalNavigator.current
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
    val hiddenThreadIds by viewModel.uiState.collectPartialAsState(
        prop1 = PersonalizedUiState::hiddenThreadIds,
        initial = persistentListOf()
    )
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.send(PersonalizedUiIntent.Refresh) }
    )
    val lazyListState = rememberLazyListState()
    viewModel.bindScrollToTopEvent(lazyListState = lazyListState)
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

    onGlobalEvent<GlobalEvent.Refresh>(
        filter = { it.key == "personalized" }
    ) {
        viewModel.send(PersonalizedUiIntent.Refresh)
    }
    viewModel.onEvent<PersonalizedUiEvent.RefreshSuccess> {
        refreshCount = it.count
        showRefreshTip = true
    }

    if (showRefreshTip) {
        LaunchedEffect(Unit) {
            launch {
                delay(20)
                lazyListState.scrollToItem(0, 0)
            }
            delay(2000)
            showRefreshTip = false
        }
    }
//    if (lazyListState.isScrollInProgress) {
//        DisposableEffect(Unit) {
//            PauseLoadWhenScrollingDrawableDecodeInterceptor.scrolling = true
//            onDispose {
//                PauseLoadWhenScrollingDrawableDecodeInterceptor.scrolling = false
//            }
//        }
//    }
    StateScreen(
        modifier = Modifier.fillMaxSize(),
        isEmpty = isEmpty,
        isError = isError,
        isLoading = isRefreshing,
        onReload = { viewModel.send(PersonalizedUiIntent.Refresh) },
        errorScreen = {
            error?.let {
                ErrorScreen(error = it.get())
            }
        }
    ) {
        Container(modifier = Modifier.pullRefresh(pullRefreshState)) {
            SwipeUpLazyLoadColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
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
                    key = { _, it -> it.thread.item.id.toString() },
                    contentType = { _, it ->
                        when {
                            it.thread.item.videoInfo != null -> "Video"
                            it.thread.item.media.size == 1 -> "SingleMedia"
                            it.thread.item.media.size > 1 -> "MultiMedia"
                            else -> "PlainText"
                        }
                    }
                ) { index, (item, blocked, personalized, hidden) ->
                    val isHidden = remember(hiddenThreadIds, item, hidden) {
                        hiddenThreadIds.contains(item.get().threadId) || hidden
                    }

                    val isRefreshPosition = remember(index, refreshPosition) {
                        index + 1 == refreshPosition
                    }

                    val isNotLast = remember(index, data.size) { index < data.size - 1 }
                    val showDivider = remember(isHidden, isRefreshPosition, isNotLast) {
                        !isHidden && !isRefreshPosition && isNotLast
                    }

                    AnimatedVisibility(
                        visible = !isHidden,
                        enter = EnterTransition.None,
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            BlockableContent(
                                blocked = blocked,
                                blockedTip = { BlockTip(text = { Text(text = stringResource(id = R.string.tip_blocked_thread)) }) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 16.dp)
                            ) {
                                Column {
                                    FeedCard(
                                        item = item,
                                        onClick = {
                                            navigator.navigate(
                                                ThreadPageDestination(it.id, it.forumId, threadInfo = it)
                                            )
                                        },
                                        onClickReply = {
                                            navigator.navigate(
                                                ThreadPageDestination(it.id, it.forumId, scrollToReply = true)
                                            )
                                        },
                                        onAgree = {
                                            viewModel.send(
                                                PersonalizedUiIntent.Agree(
                                                    it.threadId,
                                                    it.firstPostId,
                                                    it.agree?.hasAgree ?: 0
                                                )
                                            )
                                        },
                                        onClickForum = {
                                            navigator.navigate(ForumPageDestination(it.name))
                                        },
                                        onClickUser = {
                                            navigator.navigate(UserProfilePageDestination(it.id))
                                        },
                                        dislikeAction = {
                                            if (personalized == null) return@FeedCard
                                            Dislike(personalized = personalized) { clickTime, reasons ->
                                                viewModel.send(
                                                    PersonalizedUiIntent.Dislike(
                                                        forumId = item.get().forumInfo?.id ?: 0,
                                                        threadId = item.get().threadId,
                                                        reasons = reasons,
                                                        clickTime = clickTime
                                                    )
                                                )
                                            }
                                        }
                                    )

                                    if (showDivider) {
                                        VerticalDivider(
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

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = ExtendedTheme.colors.pullRefreshIndicator,
                contentColor = ExtendedTheme.colors.primary,
            )

            AnimatedVisibility(
                visible = showRefreshTip,
                enter = fadeIn() + slideInVertically(),
                exit = slideOutVertically() + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                RefreshTip(refreshCount = refreshCount)
            }
        }
    }
}

@Composable
private fun BoxScope.RefreshTip(refreshCount: Int) {
    Box(
        modifier = Modifier
            .padding(top = 72.dp)
            .background(color = ExtendedTheme.colors.primary, shape = CircleShape)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .align(Alignment.TopCenter)
    ) {
        Text(
            text = stringResource(id = R.string.toast_feed_refresh, refreshCount),
            color = ExtendedTheme.colors.onPrimary
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
            style = MaterialTheme.typography.subtitle1
        )
    }
}