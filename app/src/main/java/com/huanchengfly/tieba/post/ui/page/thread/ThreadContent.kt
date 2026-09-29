package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlignVerticalTop
import androidx.compose.material.icons.rounded.Poll
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.sharp.AccessTime
import androidx.compose.material.icons.sharp.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.MacrobenchmarkConstant.testColumn
import com.huanchengfly.tieba.post.PaddingNone
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.core.network.model.protos.PollOption
import com.huanchengfly.tieba.post.core.navigation.LocalNavigator
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.common.theme.compose.onNotNull
import com.huanchengfly.tieba.post.ui.models.PostData
import com.huanchengfly.tieba.post.ui.models.SubPostItemData
import com.huanchengfly.tieba.post.ui.models.ThreadPollInfo
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.Destination.CopyText
import com.huanchengfly.tieba.post.ui.page.Destination.Thread
import com.huanchengfly.tieba.post.ui.page.Destination.UserProfile
import com.huanchengfly.tieba.post.ui.page.subposts.PostLikeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockTip
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.widgets.compose.Card
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.OriginThreadCard
import com.huanchengfly.tieba.post.ui.widgets.compose.OutlinedIconTextButton
import com.huanchengfly.tieba.post.ui.widgets.compose.PbContentText
import com.huanchengfly.tieba.post.ui.widgets.compose.ProvideContentColor
import com.huanchengfly.tieba.post.ui.widgets.compose.SharedTransitionUserHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.defaultBottomIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.DefaultEmptyScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreenScope
import com.huanchengfly.tieba.post.ui.widgets.compose.stickyHeaderBackground
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import java.text.NumberFormat
import kotlin.time.Instant

sealed class Type(val key: String) {
    object FirstPost: Type("FirstPost")
    object Header: Type("ThreadHeader")
    object LoadPrevious: Type("LoadPreviousBtn")
    object Post: Type("") // Use PostData.id as item key
}

/**
 * Get [LazyListItemInfo.offset] of first visible post.
 *
 * @see ThreadViewModel.requestLoadPrevious
 * */
private fun LazyListState.firstVisiblePostOffset(): Int {
    val postItem = layoutInfo.visibleItemsInfo.firstOrNull { it.contentType === Type.Post }
    return postItem?.offset ?: 0
}

@Composable
private fun PollOption(
    title: String,
    percentage: String,
    num: Long,
    total: Long,
    polled: Boolean,
    onClick: (() -> Unit)? = null,
) {
    val colorScheme = MaterialTheme.colorScheme
    val optionColor = colorScheme.background.copy(0.3f)
    val progressColor = if (polled) colorScheme.primaryContainer else colorScheme.background

    Row(
        modifier = Modifier
            .border(width = 1.5.dp, color = progressColor, shape = CircleShape)
            .clip(shape = CircleShape)
            .onNotNull(onClick) { clickable(onClick = it) }
            .drawBehind {
                drawRect(color = optionColor)
                if (num > 0) {
                    drawRect(
                        color = progressColor,
                        size = size.copy(width = size.width * num / total.toFloat()),
                    )
                }
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(polled) {
            Icon(
                imageVector = Icons.Sharp.Check,
                contentDescription = null,
                modifier = Modifier.padding(end = ButtonDefaults.IconSpacing).size(14.dp),
                tint = colorScheme.onPrimaryContainer
            )
        }
        Text(
            text = title,
            modifier = Modifier.weight(1.0f),
            color = if (polled) colorScheme.onPrimaryContainer else LocalContentColor.current,
            maxLines = 1,
            style = MaterialTheme.typography.labelLarge,
        )

        Text(text = percentage, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ThreadPoll(modifier: Modifier = Modifier, info: ThreadPollInfo, onPull: ((List<Int>) -> Unit)? = null) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val percentages = remember(info.options) {
        val numberFormat = NumberFormat.getNumberInstance()
        numberFormat.maximumFractionDigits = 0
        info.options.fastMap {
            "${numberFormat.format(it.num / info.totalPoll.toDouble() * 100)}%"
        }
    }
    // ID of polled option
    val polledIds = remember(info.polledValue) {
        mutableStateSetOf<Int>().apply {
            if (!info.polledValue.isNullOrEmpty()) addAll(info.polledValue)
        }
    }
    val pollEnabled = !info.isPolled && onPull != null && !info.isTimeExpired

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            Text(
                text = info.title ?: stringResource(R.string.text_poll_title),
                style = MaterialTheme.typography.titleMedium
            )
            Row {
                if (info.totalPoll > 0) {
                    Text(
                        text = stringResource(R.string.text_poll_votes, info.totalPoll),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Text(
                    text = stringResource(if (info.isMulti) R.string.text_poll_multi else R.string.text_poll_single),
                    modifier = Modifier.padding(horizontal = 6.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                info.options.fastForEachIndexed { i, it ->
                    PollOption(
                        title = it.text,
                        percentage = percentages[i],
                        num = it.num,
                        total = info.totalPoll,
                        polled = polledIds.contains(it.id),
                        onClick = {
                            if (!pollEnabled || info.isLoading) return@PollOption
                            if (polledIds.contains(it.id)) {
                                polledIds -= it.id
                            } else {
                                if (!info.isMulti) polledIds.clear()
                                polledIds += it.id
                            }
                        },
                    )
                }
            }

            if (info.endTime > 0) {
                val endTime = remember {
                    DateTimeUtils.getRelativeTimeString(context, info.endTime)
                }
                ProvideContentColor(color = colorScheme.onSurfaceVariant) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Sharp.AccessTime, null, modifier = Modifier.size(14.dp))

                        Text(
                            text = stringResource(R.string.text_poll_end_time, endTime),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }

            if (pollEnabled) {
                AnimatedVisibility(
                    visible = remember { derivedStateOf { polledIds.isNotEmpty() } }.value,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    OutlinedIconTextButton(
                        onClick = { onPull(polledIds.toList()) },
                        modifier = Modifier.padding(top = 8.dp),
                        enabled = !info.isLoading,
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                        icon = {
                            if (info.isLoading) {
                                CircularProgressIndicator()
                            } else {
                                Icon(Icons.Rounded.Poll, contentDescription = null)
                            }
                        },
                    ) {
                        Text(text = stringResource(id = R.string.text_poll_title))
                    }
                }
            }
        }
    }
}

@Composable
fun StateScreenScope.ThreadContent(
    modifier: Modifier = Modifier,
    viewModel: ThreadViewModel,
    lazyListState: LazyListState,
    contentPadding: PaddingValues = PaddingNone,
    topAppBarScrollBehavior: TopAppBarScrollBehavior,
    useStickyHeader: Boolean // Bug: StickyHeader doesn't respect content padding
) {
    val navigator = LocalNavigator.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val collectPid = state.thread?.collectMarkPid ?: -1
    val latestPosts = state.latestPosts
    val isLoadingMore = state.isLoadingMore
    val hasMore = state.pageData.hasMore
    val localUid = state.user?.id

    val onSwipeUpRefresh: (() -> Unit)? = viewModel::requestLoadLatestPosts.takeIf {
        state.data.isNotEmpty() && state.sortType == ThreadSortType.BY_ASC
    }

    // Container {
        SwipeUpLazyLoadColumn(
            modifier = modifier
                .fillMaxSize()
                .testColumn(),
            state = lazyListState,
            contentPadding = contentPadding,
            isLoading = isLoadingMore,
            onLoad = onSwipeUpRefresh,
            onLazyLoad = viewModel::requestLoadMore.takeIf { hasMore && state.data.isNotEmpty() },
            bottomIndicator = {
                if (onSwipeUpRefresh == null) {
                    defaultBottomIndicator(this, it)
                } else {
                    LoadMoreIndicator(noMore = !hasMore, onThreshold = it)
                }
            }
        ) {
            item(key = Type.FirstPost.key, contentType = Type.FirstPost) {
                val firstPost = state.firstPost ?: return@item
                Column {
                    PostCardItem(viewModel, firstPost, localUid, collectPid)

                    state.thread?.originThreadInfo?.let { info ->
                        OriginThreadCard(
                            originThreadInfo = info,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        ) {
                            val threadId = info.item.tid.toLong()
                            navigator.navigate(Thread(threadId, forumId = info.get { fid }))
                        }
                    }

                    state.thread?.pollInfo?.let { pollInfo ->
                        ThreadPoll(
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            info = pollInfo,
                            onPull = viewModel::requestPollPost.takeIf { localUid != null },
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                        thickness = 2.dp
                    )
                }
            }

            if (state.thread != null) {
                if (useStickyHeader) {
                    stickyHeader(key = Type.Header.key, contentType = Type.Header) {
                        val appbarState = topAppBarScrollBehavior.state
                        val colors = TiebaLiteTheme.topAppBarColors
                        ThreadHeader(
                            modifier = Modifier.stickyHeaderBackground(appbarState, colors, lazyListState),
                            uiState = state,
                            viewModel = viewModel
                        )
                    }
                } else {
                    item(key = Type.Header.key, contentType = Type.Header) {
                        ThreadHeader(uiState = state, viewModel = viewModel)
                    }
                }
            }

            if (state.sortType == ThreadSortType.BY_DESC && !latestPosts.isNullOrEmpty()) {
                items(items = latestPosts, key = { post -> "LatestPost_${post.id}" }) { post ->
                    PostCardItem(viewModel, post, localUid, collectPid)
                }
                postTipItem(isDesc = true)    // DESC tip on bottom
            }

            if (state.pageData.hasPrevious) {
                item(key = Type.LoadPrevious.key, contentType = Type.LoadPrevious) {
                    LoadPreviousButton(isLoading = state.isLoadingMore) {
                        viewModel.requestLoadPrevious(offset = lazyListState.firstVisiblePostOffset())
                    }
                }
            }

            if (state.data.isEmpty()) {
                item(key = "EmptyTip") {
                    DefaultEmptyScreen(
                        modifier = Modifier.fillParentMaxHeight(fraction = 0.9f),
                        titleRes = if (state.seeLz) R.string.title_lz_empty else R.string.title_empty,
                        messageRes = R.string.message_turn_off_see_lz.takeIf { state.seeLz },
                    )
                }
            } else {
                items(items = state.data, key = { it.id }, contentType = { Type.Post }) { item ->
                    PostCardItem(viewModel, item, localUid, collectPid)
                }
            }

            if (state.sortType != ThreadSortType.BY_DESC && !latestPosts.isNullOrEmpty()) {
                postTipItem(isDesc = false)  // ASC Tip on top
                items(items = latestPosts, key = { post -> "LatestPost_${post.id}" }) { post ->
                    PostCardItem(viewModel, post, localUid, collectPid)
                }
            }
        }
    // }
}

@Composable
private fun LoadPreviousButton(isLoading: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        enabled = !isLoading,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.5.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.AlignVerticalTop,
                    contentDescription = stringResource(id = R.string.btn_load_previous),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = stringResource(id = R.string.btn_load_previous))
        }
    }
}

private fun LazyListScope.postTipItem(isDesc: Boolean) = this.item("LatestPostsTip") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(if (isDesc) R.string.above_is_latest_post else R.string.below_is_latest_post),
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.bodySmall,
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PostCardItem(viewModel: ThreadViewModel, post: PostData, localUid: Long?, collectPid: Long) {
    val navigator = LocalNavigator.current
    val loggedIn = localUid != null
    val onUserClickedListener: () -> Unit = {
        navigator.navigate(UserProfile(user = post.author, transitionKey = post.id.toString()))
    }

    if (loggedIn) {
        PostCard(
            post = post,
            immersiveMode = viewModel.isImmersiveMode,
            isCollected = post.id == collectPid,
            onUserClick = onUserClickedListener,
            onLikeClick = viewModel::onPostLikeClicked,
            onReplyClick = viewModel::onReplyClicked.takeUnless { viewModel.hideReply },
            onSubPostReplyClick = viewModel::onReplySubPost.takeUnless { viewModel.hideReply },
            onOpenSubPosts = { subPostId ->
                viewModel.onOpenSubPost(post, subPostId)
            },
            onMenuCopyClick = {
                navigator.navigate(CopyText(it))
            },
            onMenuFavoriteClick = {
                val isPostCollected = post.id == collectPid
                if (isPostCollected) {
                    viewModel.removeFromCollections()
                } else {
                    viewModel.updateCollections(markedPost = post)
                }
            },
            onMenuDeleteClick = { viewModel.onDeletePost(post) }.takeIf { post.author.id == localUid }
        )
    } else {
        PostCard(
            post = post,
            immersiveMode = viewModel.isImmersiveMode,
            onUserClick = onUserClickedListener,
            onLikeClick = viewModel::onPostLikeClicked,
            onOpenSubPosts = { subPostId -> viewModel.onOpenSubPost(post, subPostId) },
            onMenuCopyClick = {
                navigator.navigate(CopyText(it))
            }
        )
    }
}

@NonRestartableComposable
@Composable
private fun SelectableText(modifier: Modifier = Modifier, text: String, selected: Boolean) {
    Text(
        text = text,
        modifier = modifier,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        color = MaterialTheme.colorScheme.run { if (selected) onSurface else onSurfaceVariant },
    )
}

@Composable
fun ThreadHeader(
    modifier: Modifier = Modifier,
    replyNum: Int,
    @ThreadSortType sortType: Int = ThreadSortType.BY_ASC,
    onSortTypeChanged: (Int) -> Unit = {},
    isSeeLz: Boolean,
    onSeeLzChanged: () -> Unit = {},
) = Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.title_thread_header, replyNum.toString()),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )

        VerticalDivider(modifier = Modifier.padding(horizontal = 8.dp))

        SelectableText(
            text = stringResource(R.string.title_see_lz),
            modifier = Modifier.clickableNoIndication(onClick = onSeeLzChanged),
            selected = isSeeLz,
        )

        Spacer(modifier = Modifier.weight(1f))

        SelectableText(
            text = stringResource(R.string.title_asc),
            modifier = Modifier.clickableNoIndication(
                enabled = sortType == ThreadSortType.BY_DESC,
                onClick = { onSortTypeChanged(ThreadSortType.BY_ASC) }
            ),
            selected = sortType == ThreadSortType.BY_ASC ,
        )

        VerticalDivider(modifier = Modifier.padding(horizontal = 8.dp))

        SelectableText(
            text = stringResource(R.string.title_desc),
            modifier = Modifier.clickableNoIndication(
                enabled = sortType == ThreadSortType.BY_ASC,
                onClick = { onSortTypeChanged(ThreadSortType.BY_DESC) }
            ),
            selected = sortType == ThreadSortType.BY_DESC,
        )
    }

@NonRestartableComposable
@Composable
fun ThreadHeader(
    modifier: Modifier = Modifier,
    uiState: ThreadUiState,
    viewModel: ThreadViewModel,
) {
    ThreadHeader(
        modifier = modifier,
        replyNum = uiState.thread?.replyNum ?: 0,
        sortType = uiState.sortType,
        onSortTypeChanged = viewModel::onSortChanged,
        isSeeLz = uiState.seeLz,
        onSeeLzChanged = viewModel::onSeeLzChanged
    )
}

@NonRestartableComposable
@Composable
private fun SubPostBlockedTip(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(id = R.string.tip_blocked_sub_post),
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.labelLarge,
    )
}

@Composable
fun PostCard(
    post: PostData,
    immersiveMode: Boolean = false,
    isCollected: Boolean = false,
    onUserClick: () -> Unit = {},
    onLikeClick: ((PostData) -> Unit)? = null,
    onReplyClick: ((PostData) -> Unit)? = null,
    onSubPostReplyClick: ((PostData, SubPostItemData) -> Unit)? = null,
    onOpenSubPosts: (subPostId: Long) -> Unit = {},
    onMenuCopyClick: (String) -> Unit,
    onMenuFavoriteClick: (() -> Unit)? = null,
    onMenuDeleteClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current

    val hasPadding = post.floor > 1 && !immersiveMode
    val paddingModifier = if (hasPadding) Modifier.padding(start = Sizes.Small + 8.dp) else Modifier
    val author = post.author
    val showTitle = post.title != null && post.floor <= 1

    BlockableContent(
        blocked = post.blocked,
        modifier = Modifier.fillMaxWidth(),
        blockedTip = {
            BlockTip {
                Text(stringResource(id = R.string.tip_blocked_post, post.floor))
            }
        },
        hideBlockedContent = immersiveMode,
    ) {
        LongClickMenu(
            shape = MaterialTheme.shapes.medium,
            menuContent = {
                if (onReplyClick != null) {
                    TextMenuItem(text = R.string.btn_reply) {
                        onReplyClick(post)
                    }
                }
                TextMenuItem(text = R.string.menu_copy) {
                    onMenuCopyClick(if (post.floor == 1) post.title + "\n" + post.plainText else post.plainText)
                }
                TextMenuItem(text = R.string.title_report) {
                    navigator.navigate(Destination.Report(postId = post.id))
                }
                if (onMenuFavoriteClick != null) {
                    TextMenuItem(
                        text = if (isCollected) R.string.title_collect_on else R.string.title_collect_floor,
                        onClick = onMenuFavoriteClick
                    )
                }
                if (onMenuDeleteClick != null) {
                    TextMenuItem(text = R.string.title_delete, onClick = onMenuDeleteClick)
                }
            }
        ) {
            Card(
                header = {
                    if (immersiveMode) return@Card
                    SharedTransitionUserHeader(
                        author = author,
                        desc = remember { post.getDescText(context) },
                        extraKey = post.id,
                        onClick = onUserClick
                    ) {
                        if (post.floor > 1 && onLikeClick != null) {
                            PostLikeButton(like = post.like, onClick = { onLikeClick(post) })
                        }
                    }
                },
                content = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = paddingModifier.fillMaxWidth()
                    ) {
                        if (showTitle) {
                            Text(
                                text = post.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 15.sp
                            )
                        }

                        if (isCollected) {
                            Chip(
                                text = stringResource(id = R.string.title_collected_floor),
                                invertColor = true,
                                prefixIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }

                        post.contentRenders.fastForEach { it.Render() }
                    }

                    if (post.subPosts == null || post.subPostNumber <= 0 || immersiveMode) return@Card

                    Surface(
                        modifier = paddingModifier,
                        shape = MaterialTheme.shapes.small,
                        tonalElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            post.subPosts.fastForEach { item ->
                                BlockableContent(
                                    blocked = item.blocked,
                                    blockedTip = {
                                        SubPostBlockedTip(modifier = Modifier.padding(horizontal = 12.dp))
                                    },
                                    hideBlockedContent = false // filtered in repository
                                ) {
                                    SubPostItem(
                                        subPost = item,
                                        modifier = Modifier
                                            .padding(horizontal = 12.dp)
                                            .fillMaxWidth(),
                                        onReplyClick = onSubPostReplyClick?.let {
                                            { onSubPostReplyClick(post, item) }
                                        },
                                        onOpenSubPosts = onOpenSubPosts,
                                        onMenuCopyClick = onMenuCopyClick
                                    )
                                }
                            }

                            if (post.subPostNumber <= post.subPosts.size) return@Column
                            Text(
                                text = stringResource(R.string.open_all_sub_posts, post.subPostNumber),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenSubPosts(0) }
                                    .padding(vertical = 2.dp, horizontal = 12.dp),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun SubPostItem(
    subPost: SubPostItemData,
    modifier: Modifier = Modifier,
    onReplyClick: (() -> Unit)?,
    onOpenSubPosts: (Long) -> Unit,
    onMenuCopyClick: (String) -> Unit,
) {
    val navigator = LocalNavigator.current
    val menuState = rememberMenuState()

    LongClickMenu(
        menuState = menuState,
        menuContent = {
            if (onReplyClick != null) {
                TextMenuItem(text = R.string.title_reply, onClick = onReplyClick)
            }
            TextMenuItem(text = R.string.menu_copy) {
                onMenuCopyClick(subPost.plainText)
            }
            TextMenuItem(text = R.string.title_report) {
                navigator.navigate(Destination.Report(postId = subPost.id))
            }
        },
        indication = null,
        onClick = { onOpenSubPosts(subPost.id) }
    ) {
        PbContentText(
            text = subPost.abstractContent!!,
            modifier = modifier,
            overflow = TextOverflow.Ellipsis,
            maxLines = 4,
            lineSpacing = 0.4.sp,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Preview("LoadPreviousButton")
@Composable
private fun LoadPreviousButtonPreview() {
    TiebaLiteTheme {
        Column {
            LoadPreviousButton(isLoading = false, onClick = {})
            LoadPreviousButton(isLoading = true, onClick = {})
        }
    }
}

@Preview("PostTipItem")
@Composable
private fun PostTipItemPreview() {
    TiebaLiteTheme {
        LazyColumn {
            postTipItem(true)
        }
    }
}

@Preview("ThreadHeader")
@Composable
private fun ThreadHeaderPreview() {
    TiebaLiteTheme {
        ThreadHeader(replyNum = 999, isSeeLz = true, onSeeLzChanged = {})
    }
}

@Preview("ThreadPoll")
@Composable
private fun ThreadPollPreview() = TiebaLiteTheme {
    ThreadPoll(
        info = ThreadPollInfo(
            title = "来投票",
            totalPoll = 173,
            options = listOf(
                PollOption(id = 0, num=143, text="Test 1"),
                PollOption(id = 1, num=25, text="Test 2"),
                PollOption(id = 2, num=5, text="Test 3"),
            ),
            endTime = Instant.parse("2020-08-30T18:00:00Z").toEpochMilliseconds(),
            polledValue = listOf(0, 2),
        ),
    )
}
