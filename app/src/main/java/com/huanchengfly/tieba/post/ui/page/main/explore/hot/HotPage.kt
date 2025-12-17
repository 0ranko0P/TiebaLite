package com.huanchengfly.tieba.post.ui.page.main.explore.hot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.tracing.trace
import com.huanchengfly.tieba.post.MacrobenchmarkConstant
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.theme.OrangeA700
import com.huanchengfly.tieba.post.theme.RedA700
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.theme.YellowA700
import com.huanchengfly.tieba.post.ui.common.theme.compose.BebasFamily
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.models.explore.HotTab
import com.huanchengfly.tieba.post.ui.page.main.explore.ConsumeThreadPageResult
import com.huanchengfly.tieba.post.ui.page.main.explore.ExplorePageItem
import com.huanchengfly.tieba.post.ui.page.main.explore.LaunchedFabStateEffect
import com.huanchengfly.tieba.post.ui.page.main.explore.createThreadClickListeners
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCardPlaceholder
import com.huanchengfly.tieba.post.ui.widgets.compose.ProvideContentColor
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.VerticalGrid
import com.huanchengfly.tieba.post.ui.widgets.compose.itemsIndexed
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString

private enum class HotType {
    TopicHeader, TopicList, ThreadTabs, ThreadListTip, Thread, PlaceHolder
}

@Composable
fun HotPage(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    listState: LazyListState = rememberLazyListState(),
    navigator: NavController,
    onHideFab: (Boolean) -> Unit,
    viewModel: HotViewModel = hiltViewModel()
) {
    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = HotUiState::isRefreshing,
        initial = true
    )

    val error by viewModel.uiState.collectPartialAsState(
        prop1 = HotUiState::error,
        initial = null
    )
    val isError = error != null

    LaunchedFabStateEffect(ExplorePageItem.Hot, listState, onHideFab, isRefreshing, isError)

    val threadClickListeners = remember(navigator) {
        createThreadClickListeners(onNavigate = navigator::navigate)
    }

    // result from ThreadPage
    ConsumeThreadPageResult(navigator, viewModel::onThreadResult)

    StateScreen(
        isLoading = isRefreshing,
        error = error,
        onReload = viewModel::onRefresh,
        screenPadding = contentPadding
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::onRefresh,
            contentPadding = contentPadding
        ) {
            val colorScheme = MaterialTheme.colorScheme
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val topicList = uiState.topics
            val threadList = uiState.threads

            LazyColumn(
                modifier = modifier.fillMaxSize(),
                state = listState,
                contentPadding = contentPadding,
            ) {
                if (topicList.isNotEmpty()) {
                    item(key = HotType.TopicHeader, contentType = HotType.TopicHeader) {
                        Chip(
                            text = stringResource(id = R.string.hot_topic_rank),
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp)
                        )
                    }

                    item(key = HotType.TopicList, contentType = HotType.TopicList) {
                        VerticalGrid(
                            column = 2,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            itemsIndexed(items = topicList) { index, (topicName, topicTag) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = (index + 1).toString(),
                                        fontWeight = FontWeight.Bold,
                                        color = when (index) {
                                            0 -> RedA700
                                            1 -> OrangeA700
                                            2 -> YellowA700
                                            else -> colorScheme.onSurfaceVariant
                                        },
                                        fontFamily = BebasFamily,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                    Text(
                                        text = topicName,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    when (topicTag) {
                                        2 -> TopicTag(isHot = true)

                                        1 -> TopicTag(isHot = false)

                                        // else ->
                                    }
                                }
                            }
                            item {
                                ProvideContentColor(color = colorScheme.secondary) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickableNoIndication(onClick = threadClickListeners.onNavigateHotTopicList)
                                            .padding(bottom = 8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.tip_more_topic),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item(key = "TopicDivider") {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 8.dp),
                            thickness = 2.dp
                        )
                    }
                }

                if (uiState.tabs.isNotEmpty()) {
                    item(key = HotType.ThreadTabs, contentType = HotType.ThreadTabs) {
                        ThreadTabs(uiState.tabs, uiState.selectedTab, viewModel::onTabSelected)
                    }
                }

                if (threadList.isNullOrEmpty()) {
                    items(4, contentType = { HotType.PlaceHolder }) {
                        FeedCardPlaceholder()
                    }
                    return@LazyColumn
                }

                item(key = HotType.ThreadListTip, contentType = HotType.ThreadListTip) {
                    Box(
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.hot_thread_rank_rule),
                            color = colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                itemsIndexed(
                    items = threadList,
                    key = { _, thread -> thread.id },
                    contentType = { _,_ -> HotType.Thread }
                ) { index, thread ->
                    // Start of FeedCard trace
                    trace(MacrobenchmarkConstant.TRACE_FEED_CARD) {
                    Column {
                        FeedCard(
                            thread = thread,
                            onClick = threadClickListeners.onClicked,
                            onLike = viewModel::onThreadLikeClicked,
                            onClickReply = threadClickListeners.onReplyClicked,
                            onClickUser = threadClickListeners.onAuthorClicked,
                            onClickForum = threadClickListeners.onForumClicked,
                        ) {
                            HotRankText(rank = index + 1, hotNum = thread.hotNum)
                        }

                        if (index != threadList.lastIndex) {
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 2.dp)
                        }
                    }
                } } // End of FeedCard trace
            }
        }
    }
}

@Composable
fun TopicTag(modifier: Modifier = Modifier, isHot: Boolean) {
    Text(
        text = stringResource(id = if (isHot) R.string.topic_tag_hot else R.string.topic_tag_new),
        fontSize = 10.sp,
        color = Color.White,
        modifier = modifier
            .background(
                color = if (isHot) RedA700 else OrangeA700,
                shape = MaterialTheme.shapes.extraSmall
            )
            .padding(vertical = 2.dp, horizontal = 4.dp)
    )
}

@Composable
private fun ThreadTabs(
    tabs: List<HotTab>,
    selected: HotTab,
    onTabSelected: (HotTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier
            .padding(8.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items(items = tabs, key = { it.tabCode }) {
            Chip(
                // Default TAB has empty name, get name from resource
                text = it.name.ifEmpty { stringResource(id = R.string.tab_all_hot_thread) },
                invertColor = selected.tabCode == it.tabCode,
                onClick = { onTabSelected(it) }
            )
        }
    }
}

@Composable
private fun HotRankText(
    modifier: Modifier = Modifier,
    rank: Int,
    hotNum: Int
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val color = when (rank) {
            1 -> RedA700
            2 -> OrangeA700
            3 -> YellowA700
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        Text(
            text = rank.toString(),
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            text = stringResource(id = R.string.hot_num, hotNum.getShortNumString()),
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}

@Preview("HotRankText")
@Composable
private fun HotRankTextPreview() = TiebaLiteTheme {
    Surface {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            HotRankText(rank = 1, hotNum = 21000)
            HotRankText(rank = 2, hotNum = 19000)
            HotRankText(rank = 3, hotNum = 15000)
            HotRankText(rank = 4, hotNum = 12000)
        }
    }
}
