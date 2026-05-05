package com.huanchengfly.tieba.post.ui.page.hottopic.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.GlideImage
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.TopicInfoBean
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.collectUiEventWithLifecycle
import com.huanchengfly.tieba.post.arch.isOverlapping
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.theme.isTranslucent
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.hottopic.detail.TopicDetailViewModel.Companion.feedId
import com.huanchengfly.tieba.post.ui.page.main.explore.ConsumeThreadPageResult
import com.huanchengfly.tieba.post.ui.page.main.explore.createThreadClickListeners
import com.huanchengfly.tieba.post.ui.page.main.explore.personalized.ThreadBlockedTip
import com.huanchengfly.tieba.post.ui.page.thread.ThreadLikeUiEvent
import com.huanchengfly.tieba.post.ui.utils.rememberScrollOrientationConnection
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.widgets.compose.BlurScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.CollapsingTopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultBackToTopFAB
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.defaultBottomIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import kotlinx.coroutines.launch

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

    ConsumeThreadPageResult<Destination.HotTopicDetail>(navigator, viewModel::onThreadResult)

    StateScreen(
        modifier = Modifier.fillMaxSize(),
        isEmpty = uiState.isEmpty,
        isLoading = uiState.isRefreshing && uiState.isEmpty,
        error = uiState.error,
        onReload = viewModel::onRefresh
    ) {
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        val scrollOrientationConnection = rememberScrollOrientationConnection()

        val threadClickListeners = remember(navigator) {
            createThreadClickListeners(onNavigate = navigator::navigateDebounced)
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
            },
            floatingActionButton = {
                val fabVisible by remember {
                    derivedStateOf { lazyListState.canScrollBackward && scrollOrientationConnection.isScrollingForward }
                }
                DefaultBackToTopFAB(visible = fabVisible) {
                    coroutineScope.launch {
                        lazyListState.animateScrollToItem(0)
                        scrollBehavior.state.contentOffset = 0f
                    }
                }
            },
        ) { contentPadding ->
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::onRefresh,
                contentPadding = contentPadding,
            ) {
                Container (
                    modifier = Modifier
                        .nestedScroll(connection = scrollOrientationConnection)
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                ) {
                    ProvideNavigator(navigator) {
                        SwipeUpLazyLoadColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = lazyListState,
                            contentPadding = contentPadding,
                            isLoading = uiState.isLoadingMore,
                            onLazyLoad = viewModel::onLoadMore.takeIf { uiState.hasMore },
                            bottomIndicator = defaultBottomIndicator,
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

private val TopicToolbarExpandedHeight = 180.dp

@Composable
private fun TopicToolbar(
    modifier: Modifier = Modifier,
    topicInfo: TopicInfoBean,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onBack: () -> Unit = {},
) {
    val topAppBarColors = TiebaLiteTheme.topAppBarColors

    Box(
        modifier = modifier,
    ) {
        if (!TiebaLiteTheme.colorScheme.isTranslucent) {
            val gradientColors = listOf(
                Color.Transparent,
                topAppBarColors.containerColor.copy(alpha = 0.5f),
                topAppBarColors.containerColor.copy(alpha = 0.9f),
                topAppBarColors.containerColor,
            )

            GlideImage(
                model = topicInfo.topicImage,
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .drawWithContent {
                        if (scrollBehavior == null || scrollBehavior.state.collapsedFraction < 1) {
                            drawContent()
                            drawRect(brush = Brush.verticalGradient(colors = gradientColors))
                        }
                    }
                ,
                contentScale = ContentScale.Crop
            )
        }

        CollapsingTopAppBar(
            title = {
                Text(
                    text = stringResource(id = R.string.title_topic, topicInfo.topicName),
                    autoSize = TextAutoSize.StepBased(8.sp, LocalTextStyle.current.fontSize),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            },
            subtitle = {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.topic_index, topicInfo.idxNum),
                    )

                    Text(
                        text = stringResource(id = R.string.hot_num, topicInfo.discussNum.getShortNumString()),
                    )
                }
            },
            navigationIcon = {
                BackNavigationIcon(onBackPressed = onBack)
            },
            expandedHeight = TopicToolbarExpandedHeight,
            colors = topAppBarColors.copy(
                containerColor = topAppBarColors.containerColor.copy(0.01f) // 99% Transparent
            ),
            scrollBehavior = scrollBehavior,
            collapsibleExtraContent = true,
        ) {
            if (topicInfo.topicDesc.isNotEmpty()) {
                Text(
                    text = topicInfo.topicDesc,
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}