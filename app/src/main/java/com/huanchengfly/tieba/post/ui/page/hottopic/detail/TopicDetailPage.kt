package com.huanchengfly.tieba.post.ui.page.hottopic.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.TopicInfoBean
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.collectUiEventWithLifecycle
import com.huanchengfly.tieba.post.arch.isOverlapping
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.hottopic.detail.TopicDetailViewModel.Companion.feedId
import com.huanchengfly.tieba.post.ui.page.main.explore.createThreadClickListeners
import com.huanchengfly.tieba.post.ui.page.main.explore.personalized.ThreadBlockedTip
import com.huanchengfly.tieba.post.ui.page.thread.ThreadLikeUiEvent
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.widgets.compose.BlurScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadingIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.TwoRowsTopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicDetailPage(
    navigator: NavController,
    viewModel: TopicDetailViewModel = hiltViewModel<TopicDetailViewModel>()
) {
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    viewModel.uiEvent.collectUiEventWithLifecycle {
        when (it) {
            is TopicDetailUiEvent.RefreshSuccess -> coroutineScope.launch {
                lazyListState.scrollToItem(0, 0)
            }

            is ThreadLikeUiEvent -> toastShort(it.toMessage(context = this))

            is CommonUiEvent.ToastError -> toastShort(R.string.toast_exception, it.message)
        }
    }

    StateScreen(
        modifier = Modifier.fillMaxSize(),
        isEmpty = uiState.isEmpty,
        isLoading = uiState.isRefreshing,
        error = uiState.error,
        onReload = viewModel::onRefresh
    ) {
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        val threadClickListeners = remember(navigator) {
            createThreadClickListeners(onNavigate = navigator::navigate)
        }

        val hideBlockedContent by viewModel.hideBlockedContent.collectAsStateWithLifecycle()

        BlurScaffold(
            topHazeBlock = {
                blurEnabled = scrollBehavior.isOverlapping
            },
            topBar = {
                TopicToolbar(
                    topicInfo = uiState.topicInfo ?: return@BlurScaffold,
                    onBack = navigator::navigateUp,
                    scrollBehavior = scrollBehavior
                )
            }
        ) { contentPadding ->
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::onRefresh,
                contentPadding = contentPadding,
            ) {
                Container (
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                ) {
                    ProvideNavigator(navigator) {
                        SwipeUpLazyLoadColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = lazyListState,
                            isLoading = uiState.isLoadingMore,
                            contentPadding = contentPadding,
                            onLazyLoad = {
                                if (uiState.hasMore) viewModel.onLoadMore()
                            },
                            onLoad = null,
                            bottomIndicator = {
                                LoadingIndicator(isLoading = uiState.isLoadingMore)
                            }
                        ) {
                            itemsIndexed(
                                items = uiState.threads,
                                key = { _, item -> item.feedId },
                            ) { index, item ->
                                BlockableContent(
                                    blocked = item.blocked,
                                    blockedTip = ThreadBlockedTip,
                                    modifier = Modifier.fillMaxWidth(),
                                    hideBlockedContent = hideBlockedContent
                                ) {
                                    Column {
                                        FeedCard(
                                            thread = item,
                                            onClick = threadClickListeners.onClicked,
                                            onLike = viewModel::onThreadLikeClicked,
                                            onClickReply = threadClickListeners.onReplyClicked,
                                            onClickUser = threadClickListeners.onAuthorClicked,
                                            onClickForum = threadClickListeners.onForumClicked,
                                        )
                                        if (index < uiState.threads.lastIndex) {
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
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopicToolbar(
    modifier: Modifier = Modifier,
    topicInfo: TopicInfoBean,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onBack: () -> Unit = {},
) {
    TwoRowsTopAppBar(
        modifier = modifier,
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(
                    data = topicInfo.topicImage,
                    size =  Sizes.Large,
                    shape = MaterialTheme.shapes.extraSmall
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TopicTitle(text = stringResource(id = R.string.title_topic, topicInfo.topicName))

                    TopicTitle(
                        text = stringResource(id = R.string.topic_index, topicInfo.idxNum),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    TopicTitle(
                        text = stringResource(id = R.string.hot_num, topicInfo.discussNum.getShortNumString()),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        smallTitle = {
            TopicTitle(text = stringResource(id = R.string.title_topic, topicInfo.topicName))
        },
        navigationIcon = {
            BackNavigationIcon(onBackPressed = onBack)
        },
        scrollBehavior = scrollBehavior,
    )
}

@NonRestartableComposable
@Composable
private fun TopicTitle(
    modifier: Modifier = Modifier,
    text: String,
    style: TextStyle = LocalTextStyle.current
) {
    Text(text, modifier, maxLines = 1, overflow = TextOverflow.Ellipsis, style = style)
}