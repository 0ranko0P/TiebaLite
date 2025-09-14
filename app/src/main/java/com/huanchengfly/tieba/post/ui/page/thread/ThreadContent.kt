package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlignVerticalTop
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.huanchengfly.tieba.post.PaddingNone
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.PbContentText
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.models.PostData
import com.huanchengfly.tieba.post.ui.models.SubPostItemData
import com.huanchengfly.tieba.post.ui.page.Destination.CopyText
import com.huanchengfly.tieba.post.ui.page.Destination.Thread
import com.huanchengfly.tieba.post.ui.page.Destination.UserProfile
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.subposts.PostLikeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockTip
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.widgets.compose.Card
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.OriginThreadCard
import com.huanchengfly.tieba.post.ui.widgets.compose.PositiveButton
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.TipScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.UserDataHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreenScope
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import kotlinx.coroutines.launch

const val ITEM_POST_KEY_PREFIX = "Post_"

private sealed class Type(val key: String) {
    object FirstPost: Type("FirstPost")
    object Header: Type("ThreadHeader")
    object LoadPrevious: Type("LoadPreviousBtn")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StateScreenScope.ThreadContent(
    modifier: Modifier = Modifier,
    viewModel: ThreadViewModel,
    lazyListState: LazyListState,
    contentPadding: PaddingValues = PaddingNone,
    useStickyHeader: Boolean // Bug: StickyHeader doesn't respect content padding
) {
    val navigator = LocalNavController.current
    val state by viewModel.threadUiState.collectAsStateWithLifecycle()
    val latestPosts = state.latestPosts
    val isLoadingMore = state.isLoadingMore
    val localUid = state.user?.id

    val onSwipeUpRefresh: () -> Unit = {
        if (!state.isLoadingMore) viewModel.requestLoadLatestPosts()
    }

    Container {
        SwipeUpLazyLoadColumn(
            modifier = modifier.fillMaxSize(),
            state = lazyListState,
            contentPadding = contentPadding,
            isLoading = isLoadingMore,
            onLoad = onSwipeUpRefresh.takeIf {  // Enable it conditionally
                state.data.isNotEmpty() && state.sortType != ThreadSortType.BY_DESC
            },
            onLazyLoad = { if (state.hasMore) viewModel.requestLoadMore() },
            bottomIndicator = { onThreshold ->
                LoadMoreIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = isLoadingMore,
                    noMore = state.hasMore.not(),
                    onThreshold = onThreshold
                )
            }
        ) {
            item(key = Type.FirstPost.key, contentType = Type.FirstPost) {
                val firstPost = state.firstPost ?: return@item
                Column {
                    PostCardItem(viewModel, firstPost, localUid)

                    val info = viewModel.info?.originThreadInfo
                    if (info != null && viewModel.info?.isShareThread == true) {
                        OriginThreadCard(
                            originThreadInfo = info.wrapImmutable(),
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        ) {
                            navigator.navigate(
                                route = Thread(threadId = info.tid.toLong(), forumId = info.fid)
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                        thickness = 2.dp
                    )
                }
            }

            if (useStickyHeader) {
                stickyHeader(key = Type.Header.key, contentType = Type.Header) {
                    ThreadHeader(Modifier.background(MaterialTheme.colorScheme.surfaceContainer), viewModel)
                }
            } else {
                item(key = Type.Header.key, contentType = Type.Header) {
                    ThreadHeader(viewModel = viewModel)
                }
            }

            if (state.sortType == ThreadSortType.BY_DESC && latestPosts.isNotEmpty()) {
                items(items = latestPosts, key = { post -> "LatestPost_${post.id}" }) { post ->
                    PostCardItem(viewModel, post, localUid)
                }
                postTipItem(isDesc = true)    // DESC tip on bottom
            }

            if (state.hasPrevious) {
                item(key = Type.LoadPrevious.key, contentType = Type.LoadPrevious) {
                    LoadPreviousButton(onClick = viewModel::requestLoadPrevious)
                }
            }

            val data = state.data
            if (data.isEmpty()) {
                item(key = "EmptyTip") {
                    EmptyScreen(canReload, onReload = this@ThreadContent::reload)
                }
            } else {
                items(data, key = { "$ITEM_POST_KEY_PREFIX${it.id}" }) { item ->
                    PostCardItem(viewModel, item, localUid)
                }
            }

            if (state.sortType != ThreadSortType.BY_DESC && latestPosts.isNotEmpty()) {
                postTipItem(isDesc = false)  // ASC Tip on top
                items(items = latestPosts, key = { post -> "LatestPost_${post.id}" }) { post ->
                    PostCardItem(viewModel, post, localUid)
                }
            }
        }
    }
}

@Composable
private fun LoadPreviousButton(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.AlignVerticalTop,
                contentDescription = stringResource(id = R.string.btn_load_previous),
                modifier = Modifier.size(16.dp)
            )
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
private fun PostCardItem(viewModel: ThreadViewModel, post: PostData, localUid: Long?) {
    val navigator = LocalNavController.current
    val loggedIn = localUid != null

    if (loggedIn) {
        PostCard(
            post = post,
            immersiveMode = viewModel.isImmersiveMode,
            isCollected = post.id == viewModel.info?.collectMarkPid,
            onUserClick = {
                navigator.navigate(UserProfile(post.author.id))
            },
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
                val isPostCollected = post.id == viewModel.info?.collectMarkPid
                if (isPostCollected) {
                    viewModel.requestRemoveFavorite()
                } else {
                    viewModel.requestAddFavorite(post)
                }
            },
            onMenuDeleteClick = { viewModel.onDeletePost(post) }.takeIf { post.author.id == localUid }
        )
    } else {
        PostCard(
            post = post,
            immersiveMode = viewModel.isImmersiveMode,
            onUserClick = {
                navigator.navigate(UserProfile(post.author.id))
            },
            onLikeClick = viewModel::onPostLikeClicked,
            onOpenSubPosts = { subPostId -> viewModel.onOpenSubPost(post, subPostId) },
            onMenuCopyClick = {
                navigator.navigate(CopyText(it))
            }
        )
    }
}

@Composable
fun ThreadHeader(modifier: Modifier = Modifier, viewModel: ThreadViewModel) {
    StickyHeader(
        modifier = modifier,
        replyNum = viewModel.info!!.replyNum - 1,
        isSeeLz = viewModel.seeLz,
        onSeeLzChanged = { seeLz -> viewModel.requestLoadFirstPage(seeLz) }
    )
}

@Composable
private fun StickyHeader(
    modifier: Modifier = Modifier,
    replyNum: Int,
    isSeeLz: Boolean,
    onSeeLzChanged: (Boolean) -> Unit
) = Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val colors = MaterialTheme.colorScheme
        Text(
            text = stringResource(R.string.title_thread_header, replyNum.toString()),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = stringResource(R.string.text_all),
            modifier = Modifier
                .clickableNoIndication(
                    enabled = isSeeLz,
                    onClick = { onSeeLzChanged(false) }
                ),
            fontSize = 13.sp,
            fontWeight = if (!isSeeLz) FontWeight.SemiBold else FontWeight.Normal,
            color = if (!isSeeLz) colors.onSurface else colors.onSurfaceVariant,
        )

        VerticalDivider(modifier = Modifier.padding(horizontal = 8.dp))

        Text(
            text = stringResource(R.string.title_see_lz),
            modifier = Modifier
                .clickableNoIndication(
                    enabled = !isSeeLz,
                    onClick = { onSeeLzChanged(true) }
                ),
            fontSize = 13.sp,
            fontWeight = if (isSeeLz) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSeeLz) colors.onSurface else colors.onSurfaceVariant,
        )
    }

val SubPostBlockedTip: @Composable BoxScope.() -> Unit = {
    Text(
        text = stringResource(id = R.string.tip_blocked_sub_post),
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
fun PostCard(
    post: PostData,
    immersiveMode: Boolean = false,
    isCollected: Boolean = false,
    onUserClick: () -> Unit = {},
    onLikeClick: (PostData) -> Unit = {},
    onReplyClick: ((PostData) -> Unit)? = null,
    onSubPostReplyClick: ((PostData, SubPostItemData) -> Unit)? = null,
    onOpenSubPosts: (subPostId: Long) -> Unit = {},
    onMenuCopyClick: (String) -> Unit,
    onMenuFavoriteClick: (() -> Unit)? = null,
    onMenuDeleteClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val preferences = context.appPreferences
    val navigator = LocalNavController.current
    val coroutineScope = rememberCoroutineScope()

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
        hideBlockedContent = preferences.hideBlockedContent || immersiveMode,
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
                    onMenuCopyClick(post.plainText)
                }
                TextMenuItem(text = R.string.title_report) {
                    coroutineScope.launch {
                        TiebaUtil.reportPost(context, navigator, post.id.toString())
                    }
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
                    UserDataHeader(
                        author = author,
                        desc = remember { post.getDescText(context) },
                        onClick = onUserClick
                    ) {
                        if (post.floor > 1) {
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
                            val hideBlockedContent = preferences.hideBlockedContent
                            post.subPosts.fastForEach { item ->
                                BlockableContent(
                                    blocked = item.blocked,
                                    blockedTip = SubPostBlockedTip,
                                    hideBlockedContent = hideBlockedContent
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
    val context = LocalContext.current
    val navigator = LocalNavController.current
    val coroutineScope = rememberCoroutineScope()
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
                coroutineScope.launch {
                    TiebaUtil.reportPost(context, navigator, subPost.id.toString())
                }
            }
        },
        shape = MaterialTheme.shapes.extraSmall,
        onClick = { onOpenSubPosts(subPost.id) }
    ) {
        PbContentText(
            text = subPost.content!!,
            modifier = modifier,
            overflow = TextOverflow.Ellipsis,
            maxLines = 4,
            lineSpacing = 0.4.sp,
            inlineContent = if (subPost.isLz) ThreadViewModel.cachedLzInlineContent else null,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun EmptyScreen(canReload: Boolean, onReload: () -> Unit) =
    TipScreen(
        modifier = Modifier.fillMaxSize(),
        title = { Text(stringResource(id = R.string.title_empty)) },
        image = {
            val composition by rememberLottieComposition(
                LottieCompositionSpec.RawRes(R.raw.lottie_empty_box)
            )
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.fillMaxWidth()
            )
        },
        actions = {
            PositiveButton(textRes = R.string.btn_refresh, enabled = canReload, onClick = onReload)
        },
    )

@Preview("LoadPreviousButton")
@Composable
private fun LoadPreviousButtonPreview() {
    TiebaLiteTheme {
        Column {
            LoadPreviousButton(onClick = {})
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

@Preview("StickyHeader")
@Composable
private fun StickyHeaderPreview() {
    TiebaLiteTheme {
        StickyHeader(replyNum = 999, isSeeLz = true, onSeeLzChanged = {})
    }
}
