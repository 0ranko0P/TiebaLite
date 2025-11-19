package com.huanchengfly.tieba.post.ui.page.main.explore.personalized

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.models.explore.Dislike
import com.huanchengfly.tieba.post.ui.page.main.explore.ConsumeThreadPageResult
import com.huanchengfly.tieba.post.ui.page.main.explore.ExplorePageItem
import com.huanchengfly.tieba.post.ui.page.main.explore.LaunchedFabStateEffect
import com.huanchengfly.tieba.post.ui.page.main.explore.createThreadClickListeners
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockTip
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.ThreadContentType
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ThreadBlockedTip: @Composable BoxScope.() -> Unit = {
    BlockTip(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = stringResource(id = R.string.tip_blocked_thread),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun PersonalizedPage(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    listState: LazyListState = rememberLazyListState(),
    navigator: NavController,
    onHideFab: (Boolean) -> Unit,
    viewModel: PersonalizedViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var refreshCount by remember { mutableIntStateOf(0) }
    val showRefreshTip = refreshCount > 0

    LaunchedEffect(viewModel.uiEvent) {
        viewModel.uiEvent.collect {
            when(it) {
                is PersonalizedUiEvent.RefreshSuccess -> {
                    coroutineScope.launch {
                        listState.scrollToItem(0, 0)
                        delay(300)
                        refreshCount = it.count
                        delay(2000)
                        refreshCount = 0
                    }
                }

                is PersonalizedUiEvent.DislikeFailed -> {
                    context.toastShort(R.string.toast_exception, it.e.getErrorMessage())
                }

                is CommonUiEvent.Toast -> context.toastShort(it.message.toString())

                else -> {}
            }
        }
    }

    val threadClickListeners = remember(navigator) {
        createThreadClickListeners(onNavigate = navigator::navigate)
    }

    // result from ThreadPage
    ConsumeThreadPageResult(navigator, viewModel::onThreadResult)

    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = PersonalizedUiState::isRefreshing,
        initial = true
    )
    val isEmpty by viewModel.uiState.collectPartialAsState(
        prop1 = PersonalizedUiState::isEmpty,
        initial = false
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = PersonalizedUiState::error,
        initial = null
    )
    val isError = error != null

    LaunchedFabStateEffect(ExplorePageItem.Personalized, listState, onHideFab, isRefreshing, isError)

    StateScreen(
        modifier = Modifier.fillMaxSize(),
        isEmpty = isEmpty,
        isError = isError,
        isLoading = isRefreshing,
        onReload = viewModel::onRefresh,
        errorScreen = {
            ErrorScreen(error = error, modifier = Modifier.padding(contentPadding))
        }
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::onRefresh,
            contentPadding = contentPadding
        ) {
            val hideBlockedContent by viewModel.hideBlockedContent.collectAsStateWithLifecycle()

            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val data = uiState.data
            val isLoadingMore = uiState.isLoadingMore
            val dislikeReasons = remember { mutableStateSetOf<Dislike>() }

            SwipeUpLazyLoadColumn(
                modifier = modifier.fillMaxSize(),
                state = listState,
                contentPadding = contentPadding,
                horizontalAlignment = Alignment.CenterHorizontally,
                isLoading = isLoadingMore,
                onLazyLoad = {
                    if (data.isNotEmpty()) viewModel.onLoadMore()
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
                itemsIndexed(data, key = { _, it -> it.id }, ThreadContentType) { index, thread ->
                    val isHidden = thread.blocked && hideBlockedContent

                    Column(
                        modifier = Modifier.animateItem()
                    ) {
                        BlockableContent(
                            blocked = thread.blocked,
                            blockedTip = ThreadBlockedTip,
                            modifier = Modifier.fillMaxWidth(),
                            hideBlockedContent = hideBlockedContent
                        ) {
                            Column {
                                FeedCard(
                                    thread = thread,
                                    onClick = threadClickListeners.onClicked,
                                    onLike = viewModel::onThreadLikeClicked,
                                    onClickReply = threadClickListeners.onReplyClicked,
                                    onClickUser = threadClickListeners.onAuthorClicked,
                                    onClickForum = threadClickListeners.onForumClicked,
                                    dislikeAction = {
                                        if (thread.dislikeResource.isNullOrEmpty()) return@FeedCard
                                        Dislike(
                                            dislikeResource = thread.dislikeResource,
                                            selectedReasons = dislikeReasons,
                                            onDismiss = dislikeReasons::clear,
                                            onDislikeSelected = {
                                                if (it in dislikeReasons) {
                                                    dislikeReasons.remove(it)
                                                } else {
                                                    dislikeReasons.add(it)
                                                }
                                            },
                                        ) {
                                            viewModel.onThreadDislike(
                                                thread,
                                                dislikeReasons.toList()
                                            )
                                        }
                                    }
                                )

                                if (!isHidden && index < data.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        thickness = 2.dp
                                    )
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
