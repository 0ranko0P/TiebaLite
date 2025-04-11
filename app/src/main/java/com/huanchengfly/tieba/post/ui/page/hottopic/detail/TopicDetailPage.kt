package com.huanchengfly.tieba.post.ui.page.hottopic.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.TopicInfoBean
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadingIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.Toolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min


@Composable
fun TopicDetailPage(
    topicId: Long,
    topicName: String,
    navigator: NavController,
    viewModel: TopicDetailViewModel = pageViewModel()
) {
    val pageSize = 10
    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(TopicDetailUiIntent.Refresh(topicId, topicName, pageSize))
        viewModel.initialized = true
    }
    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = TopicDetailUiState::isRefreshing,
        initial = false
    )
    val isError by viewModel.uiState.collectPartialAsState(
        prop1 = TopicDetailUiState::isError,
        initial = false
    )
    val isLoadingMore by viewModel.uiState.collectPartialAsState(
        prop1 = TopicDetailUiState::isLoadingMore,
        initial = false
    )
    val currentPage by viewModel.uiState.collectPartialAsState(
        prop1 = TopicDetailUiState::currentPage,
        initial = 1
    )
    val hasMore by viewModel.uiState.collectPartialAsState(
        prop1 = TopicDetailUiState::hasMore,
        initial = true
    )
    val topicInfo by viewModel.uiState.collectPartialAsState(
        prop1 = TopicDetailUiState::topicInfo,
        initial = null
    )
    val relateForum by viewModel.uiState.collectPartialAsState(
        prop1 = TopicDetailUiState::relateForum,
        initial = persistentListOf()
    )
    val relateThread by viewModel.uiState.collectPartialAsState(
        prop1 = TopicDetailUiState::relateThread,
        initial = persistentListOf()
    )
    val lazyListState = rememberLazyListState()
    val density = LocalDensity.current
    var heightOffset by rememberSaveable { mutableFloatStateOf(0f) }
    var headerHeight by rememberSaveable {
        mutableFloatStateOf(
            with(density) {
                (Sizes.Large + 16.dp * 2).toPx()
            }
        )
    }
    val isShowTopBarArea by remember {
        derivedStateOf {
            heightOffset.absoluteValue < headerHeight
        }
    }

    ProvideNavigator(navigator = navigator) {
        StateScreen(
            modifier = Modifier.fillMaxSize(),
            isEmpty = topicInfo == null,
            isLoading = isRefreshing,
            isError = isError,
            onReload = {
                viewModel.send(
                    TopicDetailUiIntent.Refresh(
                        topicId,
                        topicName,
                        pageSize
                    )
                )
            }
        ) {
            MyScaffold(
                backgroundColor = Color.Transparent,
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopicToolbar(
                        topicName = topicName,
                        showTitle = !isShowTopBarArea,
                        topicId = topicId
                    )
                }
            ) { contentPadding ->
                var isFakeLoading by remember { mutableStateOf(false) }
                LaunchedEffect(isFakeLoading) {
                    if (isFakeLoading) {
                        delay(1000)
                        isFakeLoading = false
                    }
                }

                PullToRefreshBox(
                    isRefreshing = isFakeLoading,
                    onRefresh = {
                        viewModel.send(
                            TopicDetailUiIntent.Refresh(
                                topicId,
                                topicName,
                                pageSize
                            )
                        )
                        isFakeLoading = true
                    },
                    contentPadding = contentPadding
                ) {
                    val headerNestedScrollConnection = remember {
                        object : NestedScrollConnection {
                            override fun onPreScroll(
                                available: Offset,
                                source: NestedScrollSource,
                            ): Offset {
                                if (available.y < 0) {
                                    val prevHeightOffset = heightOffset
                                    heightOffset = max(heightOffset + available.y, -headerHeight)
                                    if (prevHeightOffset != heightOffset) {
                                        return available.copy(x = 0f)
                                    }
                                }

                                return Offset.Zero
                            }

                            override fun onPostScroll(
                                consumed: Offset,
                                available: Offset,
                                source: NestedScrollSource,
                            ): Offset {
                                if (available.y > 0f) {
                                    // Adjust the height offset in case the consumed delta Y is less than what was
                                    // recorded as available delta Y in the pre-scroll.
                                    val prevHeightOffset = heightOffset
                                    heightOffset = min(heightOffset + available.y, 0f)
                                    if (prevHeightOffset != heightOffset) {
                                        return available.copy(x = 0f)
                                    }
                                }

                                return Offset.Zero
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .padding(contentPadding)
                            .nestedScroll(headerNestedScrollConnection)
                    ) {
                        Column {
                            val containerHeight by remember {
                                derivedStateOf {
                                    with(density) {
                                        (headerHeight + heightOffset).toDp()
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .height(containerHeight)
                                    .clipToBounds()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .wrapContentHeight(
                                            align = Alignment.Bottom,
                                            unbounded = true
                                        )
                                        .onSizeChanged {
                                            headerHeight = it.height.toFloat()
                                        }
                                ) {
                                    topicInfo?.let {
                                        TopicHeader(
                                            it,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp)
                                        )
                                    }
                                }
                            }
                            SwipeUpLazyLoadColumn(
                                modifier = Modifier.fillMaxWidth(),
                                state = lazyListState,
                                isLoading = isLoadingMore,
                                contentPadding = contentPadding,
                                onLazyLoad = {
                                    if (hasMore) viewModel.send(
                                        TopicDetailUiIntent.LoadMore(
                                            topicId,
                                            topicName,
                                            currentPage + 1,
                                            pageSize,
                                            relateThread.last().feedId
                                        )
                                    )
                                },
                                onLoad = null, // Disable manual load
                                bottomIndicator = {
                                    LoadingIndicator(isLoading = isLoadingMore)
                                }
                            ) {
                                itemsIndexed(
                                    items = relateThread,
                                    key = { _, item -> "${item.feedId}" },
                                ) { index, item ->
                                    Container {
                                        Column {
                                            FeedCard(
                                                thread = item,
                                                onClick = {
                                                    navigator.navigate(
                                                        Destination.Thread(
                                                            item.id,
                                                            item.simpleForum.first
                                                        )
                                                    )
                                                },
                                                onClickReply = {
                                                    navigator.navigate(
                                                        Destination.Thread(
                                                            item.id,
                                                            item.simpleForum.first,
                                                            scrollToReply = true
                                                        )
                                                    )
                                                },
                                                onLike = {
                                                    viewModel.send(
                                                        TopicDetailUiIntent.Agree(
                                                            item.id,
                                                            item.simpleForum.first,
                                                            if (item.liked) 1 else 0
                                                        )
                                                    )
                                                },
                                                onClickForum = {
                                                    navigator.navigate(
                                                        Destination.Forum(item.simpleForum.second)
                                                    )
                                                },
                                                onClickUser = {
                                                    navigator.navigate(
                                                        Destination.UserProfile(item.author.id)
                                                    )
                                                },
                                            )
                                            if (index < relateThread.size - 1) {
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
}

@Composable
private fun TopicToolbar(
    topicName: String,
    showTitle: Boolean,
    topicId: Long? = null,
) {
    val navigator = LocalNavController.current
    Toolbar(
        title = {
            if (showTitle) Text(
                text = stringResource(
                    id = R.string.title_topic,
                    topicName
                )
            )
        },
        navigationIcon = {
            BackNavigationIcon(onBackPressed = {
                val navigateUp =
                    navigator.navigateUp()
            })
        }
    )
}

@Composable
private fun TopicHeader(
    topicInfo: TopicInfoBean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(
                data = topicInfo.topicImage,
                size = Sizes.Large,
                contentDescription = topicInfo.topicDesc,
                shape = MaterialTheme.shapes.extraSmall
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.title_topic, topicInfo.topicName),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(id = R.string.topic_index, topicInfo.idxNum),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        id = R.string.hot_num, topicInfo.discussNum.getShortNumString()
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

        }
    }
}