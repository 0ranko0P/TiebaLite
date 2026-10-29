package com.huanchengfly.tieba.post.ui.page.main.explore.personalized

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.MacrobenchmarkConstant
import com.huanchengfly.tieba.post.MacrobenchmarkConstant.testColumn
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.collectUiEventWithLifecycle
import com.huanchengfly.tieba.post.core.designsystem.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.core.network.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.core.network.exception.getErrorMessage
import com.huanchengfly.tieba.post.core.ui.component.FeedGridItemSpacing
import com.huanchengfly.tieba.post.core.ui.component.FeedLazyVerticalStaggeredGrid
import com.huanchengfly.tieba.post.core.ui.component.defaultGridCells
import com.huanchengfly.tieba.post.core.ui.util.toastShort
import com.huanchengfly.tieba.post.ui.models.explore.Dislike
import com.huanchengfly.tieba.post.ui.page.main.explore.LaunchedFabStateEffect
import com.huanchengfly.tieba.post.ui.page.main.explore.ThreadClickListeners
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockTip
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.widgets.compose.CardHorizontalSpacing
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.StrongBox
import com.huanchengfly.tieba.post.ui.widgets.compose.ThreadContentType
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.trace
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val ThreadBlockedTip: @Composable BoxScope.() -> Unit = {
    BlockTip(modifier = Modifier.padding(horizontal = CardHorizontalSpacing, vertical = 4.dp)) {
        Text(text = stringResource(id = R.string.tip_blocked_thread))
    }
}

@Composable
fun PersonalizedPage(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    threadClickListeners: ThreadClickListeners,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    onHideFab: (Boolean) -> Unit,
    viewModel: PersonalizedViewModel = hiltViewModel(),
) {
    val coroutineScope = rememberCoroutineScope()

    var refreshCount by remember { mutableIntStateOf(0) }

    viewModel.uiEvent.collectUiEventWithLifecycle {
        when (it) {
            is PersonalizedUiEvent.RefreshSuccess -> coroutineScope.launch {
                gridState.scrollToItem(0)
                refreshCount = it.count // Show refresh tip
                delay(2000)
                refreshCount = 0        // Hide refresh tip
            }

            is PersonalizedUiEvent.BlockRuleUpdated -> toastShort(R.string.toast_block_rule_updated)

            is PersonalizedUiEvent.DislikeFailed -> if (it.e !is TiebaNotLoggedInException) {
                toastShort(R.string.toast_exception, it.e.getErrorMessage())
            }

            is CommonUiEvent.ToastError -> toastShort(R.string.toast_exception, it.message)
        }
    }

    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = PersonalizedUiState::isRefreshing,
        initial = true
    )
    val isEmpty by viewModel.uiState.collectPartialAsState(
        prop1 = PersonalizedUiState::isEmpty,
        initial = true
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = PersonalizedUiState::error,
        initial = null
    )
    val isError = error != null

    LaunchedFabStateEffect(gridState, onHideFab, isRefreshing, isError)

    StateScreen(
        isEmpty = isEmpty,
        isLoading = isRefreshing && isEmpty, // Only initial load, allow browse existing content on refresh
        error = error,
        onReload = viewModel::onRefresh,
        screenPadding = contentPadding,
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::onRefresh,
            contentPadding = contentPadding
        ) {
            val hideBlockedContent by viewModel.hideBlockedContent.collectAsStateWithLifecycle()

            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val data = uiState.data
            val dislikeReasons = remember { mutableStateSetOf<Dislike>() }
            val itemPlacementSpec = MaterialTheme.motionScheme.slowSpatialSpec<IntOffset>()
            val itemModifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))

            FeedLazyVerticalStaggeredGrid(
                columns = defaultGridCells(),
                state = gridState,
                modifier = modifier.fillMaxSize().testColumn(),
                contentPadding = contentPadding + PaddingValues(horizontal = FeedGridItemSpacing),
                isLoading = { uiState.isLoadingMore },
                onLazyLoad = viewModel::onLoadMore.takeUnless { isRefreshing },
            ) {
                itemsIndexed(data, key = { _, it -> it.id }, ThreadContentType) { _, thread ->
                    trace(MacrobenchmarkConstant.TRACE_FEED_CARD) {
                        BlockableContent(
                            blocked = thread.blocked,
                            blockedTip = ThreadBlockedTip,
                            modifier = Modifier
                                .animateItem(fadeInSpec = null, placementSpec = itemPlacementSpec, fadeOutSpec = null),
                            hideBlockedContent = hideBlockedContent
                        ) {
                            FeedCard(
                                modifier = itemModifier,
                                thread = thread,
                                onClick = threadClickListeners.onClicked,
                                onLike = viewModel::onThreadLikeClicked,
                                onClickReply = threadClickListeners.onReplyClicked,
                                onClickUser = threadClickListeners.onAuthorClicked,
                                onClickForum = threadClickListeners.onForumClicked,
                                onClickOriginThread = threadClickListeners.onOriginThreadClicked,
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
                                        viewModel.onThreadDislike(thread, dislikeReasons.toList())
                                    }
                                },
                            )
                        }
                    }
                }
            }

            StrongBox {
                AnimatedVisibility(
                    visible = refreshCount > 0,
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

@Preview("RefreshTip", backgroundColor = 0xFFFFFFFF)
@Composable
private fun RefreshTipPreview() = TiebaLiteTheme {
    Surface {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RefreshTip(refreshCount = Int.MAX_VALUE)
        }
    }
}
