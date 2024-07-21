package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlignVerticalTop
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.common.PbContentRender
import com.huanchengfly.tieba.post.ui.common.PbContentText
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.loadMoreIndicator
import com.huanchengfly.tieba.post.ui.common.theme.compose.pullRefreshIndicator
import com.huanchengfly.tieba.post.ui.models.PostData
import com.huanchengfly.tieba.post.ui.models.SubPostItemData
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.CopyTextDialogPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ReplyPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.huanchengfly.tieba.post.ui.page.subposts.PostAgreeBtn
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockTip
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.widgets.compose.Button
import com.huanchengfly.tieba.post.ui.widgets.compose.Card
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.HorizontalDivider
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.OriginThreadCard
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TipScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.VerticalDivider
import com.huanchengfly.tieba.post.ui.widgets.compose.buildChipInlineContent
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreenScope
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.Util.getIconColorByLevel
import com.huanchengfly.tieba.post.utils.appPreferences
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

const val ITEM_POST_KEY_PREFIX = "Post_"

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun StateScreenScope.ThreadContent(viewModel: ThreadViewModel, lazyListState: LazyListState) {

    val navigator = LocalNavigator.current

    val enablePullRefresh by remember {
        derivedStateOf {
            viewModel.threadUiState.run { hasPrevious || sortType == ThreadSortType.BY_DESC }
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = viewModel.isRefreshing,
        onRefresh = viewModel::requestLoadFirstPage
    )

    Container(
        modifier = Modifier.pullRefresh(state = pullRefreshState, enabled = enablePullRefresh)
    ) {

        val state = viewModel.threadUiState
        val firstPost = state.firstPost
        val latestPosts = state.latestPosts
        val forum = state.forum

        val loadMorePreloadCount = if (state.hasMore) 1 else 0
        LoadMoreLayout(
            isLoading = viewModel.isLoadingMore,
            onLoadMore = {
                if (state.hasMore) {
                    viewModel.requestLoadMore()
                } else if (viewModel.data.isNotEmpty() && state.sortType != ThreadSortType.BY_DESC) {
                    viewModel.requestLoadLatestPosts()
                }
            },
            loadEnd = !state.hasMore && state.sortType == ThreadSortType.BY_DESC,
            indicator = { isLoading, loadMoreEnd, willLoad ->
                ThreadLoadMoreIndicator(isLoading, loadMoreEnd, willLoad, state.hasMore)
            },
            lazyListState = lazyListState,
            isEmpty = viewModel.data.isEmpty(),
            preloadCount = loadMorePreloadCount,
        ) {
            MyLazyColumn(state = lazyListState, modifier = Modifier.fillMaxWidth()) {
                item(key = "FirstPost") {
                    if (firstPost == null) return@item
                    Column {
                        PostCard(
                            post = firstPost,
                            contentRenders = firstPost.contentRenders,
                            canDelete = firstPost.author.id == state.user?.id,
                            immersiveMode = viewModel.isImmersiveMode,
                            isCollected = firstPost.id == viewModel.info?.collectMarkPid,
                            onUserClick = {
                                navigator.navigate(UserProfilePageDestination(firstPost.author.id))
                            },
                            onReplyClick = {
                                navigator.navigate(
                                    ReplyPageDestination(
                                        forumId = viewModel.curForumId ?: 0,
                                        forumName = forum?.get { name }.orEmpty(),
                                        threadId = viewModel.threadId,
                                    )
                                )
                            },
                            onMenuCopyClick = {
                                navigator.navigate(CopyTextDialogPageDestination(it))
                            },
                            onMenuFavoriteClick = {
                                viewModel.requestAddFavorite(firstPost)
                            },
                            onMenuDeleteClick = viewModel::onDeleteThread
                        )

                        val info = viewModel.info?.originThreadInfo
                        if (info != null && viewModel.info?.isShareThread == true) {
                            OriginThreadCard(
                                originThreadInfo = info.wrapImmutable(),
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 16.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ExtendedTheme.colors.floorCard)
                                    .clickable {
                                        navigator.navigate(
                                            ThreadPageDestination(
                                                threadId = info.tid.toLong(),
                                                forumId = info.fid,
                                            )
                                        )
                                    }
                                    .padding(16.dp)
                            )
                        }

                        VerticalDivider(
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                            thickness = 2.dp
                        )
                    }
                }

                stickyHeader(key = "ThreadHeader") {
                    StickyHeader(
                        replyNum = viewModel.info!!.replyNum - 1,
                        isSeeLz = viewModel.seeLz,
                        onSeeLzChanged = { seeLz -> viewModel.requestLoadFirstPage(seeLz) }
                    )
                }

                if (state.sortType == ThreadSortType.BY_DESC && latestPosts.isNotEmpty()) {
                    items(items = latestPosts, key = { post -> "LatestPost_${post.id}" }) { post ->
                        PostCardItem(viewModel, post)
                    }
                    postTipItem(isDesc = true)    // DESC tip on bottom
                }

                if (state.hasPrevious) {
                    item(key = "LoadPreviousBtn") {
                        LoadPreviousButton(onClick = viewModel::requestLoadPrevious)
                    }
                }

                if (viewModel.data.isEmpty()) {
                    item(key = "EmptyTip") {
                        EmptyScreen(canReload, onReload = this@ThreadContent::reload)
                    }
                } else {
                    items(viewModel.data, key = { "$ITEM_POST_KEY_PREFIX${it.id}" }) { item ->
                        PostCardItem(viewModel, item)
                    }
                }

                if (state.sortType != ThreadSortType.BY_DESC && latestPosts.isNotEmpty()) {
                    postTipItem(isDesc = false)  // ASC Tip on top
                    items(items = latestPosts, key = { post -> "LatestPost_${post.id}" }) { post ->
                        PostCardItem(viewModel, post)
                    }
                }
            }
        }

        PullRefreshIndicator(
            refreshing = viewModel.isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = ExtendedTheme.colors.pullRefreshIndicator,
            contentColor = ExtendedTheme.colors.primary,
        )
    }
}

@Composable
fun UserNameText(
    userName: AnnotatedString,
    userLevel: Int,
    modifier: Modifier = Modifier,
    bawuType: String? = null,
) = Text(
    text = userName,
    inlineContent = mapOf(
        "Level" to buildChipInlineContent(
            "18",
            color = Color(getIconColorByLevel("$userLevel")),
            backgroundColor = Color(getIconColorByLevel("$userLevel")).copy(alpha = 0.25f)
        ),
        "Bawu" to buildChipInlineContent(
            bawuType ?: "",
            color = ExtendedTheme.colors.primary,
            backgroundColor = ExtendedTheme.colors.primary.copy(alpha = 0.1f)
        ),
        "Lz" to buildChipInlineContent(stringResource(id = R.string.tip_lz)),
    ),
    modifier = modifier
)

@NonRestartableComposable
@Composable
private fun LoadPreviousButton(onClick: () -> Unit) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)
        .padding(8.dp)
        .clip(MaterialTheme.shapes.medium),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically
) {
    Icon(
        imageVector = Icons.Rounded.AlignVerticalTop,
        contentDescription = stringResource(id = R.string.btn_load_previous),
        modifier = Modifier.size(16.dp)
    )
    Spacer(modifier = Modifier.width(16.dp))
    Text(
        text = stringResource(id = R.string.btn_load_previous),
        color = ExtendedTheme.colors.text,
        fontSize = 14.sp
    )
}

private fun LazyListScope.postTipItem(isDesc: Boolean) = this.item("LatestPostsTip") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VerticalDivider(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(if (isDesc) R.string.above_is_latest_post else R.string.below_is_latest_post),
            color = ExtendedTheme.colors.textSecondary,
            style = MaterialTheme.typography.caption,
        )
        VerticalDivider(modifier = Modifier.weight(1f))
    }
}

@Composable
fun PostCardItem(viewModel: ThreadViewModel, post: PostData) {
    val navigator = LocalNavigator.current
    PostCard(
        post = post,
        contentRenders = post.contentRenders,
        subPosts = post.subPosts,
        canDelete = post.author.id == viewModel.threadUiState.user?.id,
        immersiveMode = viewModel.isImmersiveMode,
        isCollected = post.id == viewModel.info?.collectMarkPid,
        onUserClick = {
            navigator.navigate(UserProfilePageDestination(post.author.id))
        },
        onAgree = { viewModel.onAgreePost(post) },
        onReplyClick = viewModel::onReplyPost,
        onSubPostReplyClick = { subPost -> viewModel.onReplySubPost(post, subPost) },
        onOpenSubPosts = { subPostId ->
            viewModel.onOpenSubPost(post, subPostId)
        },
        onMenuCopyClick = {
            navigator.navigate(CopyTextDialogPageDestination(it))
        },
        onMenuFavoriteClick = {
            val isPostCollected = post.id == viewModel.info?.collectMarkPid
            if (isPostCollected) {
                viewModel.requestRemoveFavorite()
            } else {
                viewModel.requestAddFavorite(post)
            }
        },
        onMenuDeleteClick = { viewModel.onDeletePost(post) }
    )
}

@Composable
private fun StickyHeader(replyNum: Int, isSeeLz: Boolean, onSeeLzChanged: (Boolean) -> Unit) =
    Row(
        modifier = Modifier
            .background(MaterialTheme.colors.background)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.title_thread_header, replyNum.toString()),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = ExtendedTheme.colors.text,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Spacer(modifier = Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(IntrinsicSize.Min)
        ) {
            Text(
                text = stringResource(R.string.text_all),
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = isSeeLz
                    ) {
                        onSeeLzChanged(false)
                    },
                fontSize = 13.sp,
                fontWeight = if (!isSeeLz) FontWeight.SemiBold else FontWeight.Normal,
                color = if (!isSeeLz) ExtendedTheme.colors.text else ExtendedTheme.colors.textSecondary,
            )
            HorizontalDivider()
            Text(
                text = stringResource(R.string.title_see_lz),
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = !isSeeLz
                    ) {
                        onSeeLzChanged(true)
                    },
                fontSize = 13.sp,
                fontWeight = if (isSeeLz) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSeeLz) ExtendedTheme.colors.text else ExtendedTheme.colors.textSecondary,
            )
        }
    }

@Composable
fun PostCard(
    post: PostData,
    contentRenders: ImmutableList<PbContentRender>,
    subPosts: ImmutableList<SubPostItemData> = persistentListOf(),
    canDelete: Boolean,
    immersiveMode: Boolean = false,
    isCollected: Boolean,
    onUserClick: () -> Unit = {},
    onAgree: () -> Unit = {},
    onReplyClick: (PostData) -> Unit = {},
    onSubPostReplyClick: ((SubPostItemData) -> Unit)? = null,
    onOpenSubPosts: (subPostId: Long) -> Unit = {},
    onMenuCopyClick: (String) -> Unit,
    onMenuFavoriteClick: (() -> Unit)? = null,
    onMenuDeleteClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val coroutineScope = rememberCoroutineScope()
    val hasPadding = post.floor > 1 && !immersiveMode

    val paddingModifier = Modifier.padding(start = if (hasPadding) Sizes.Small + 8.dp else 0.dp)
    val author = post.author
    val showTitle = post.title.isNotBlank() && post.floor <= 1 && !post.isNTitle
    val hasAgreed = post.hasAgree == 1
    val agreeNum = post.diffAgreeNum
    val menuState = rememberMenuState()
    val hideReply = remember { context.appPreferences.hideReply }

    BlockableContent(
        blocked = post.blocked,
        blockedTip = {
            BlockTip {
                Text(stringResource(id = R.string.tip_blocked_post, post.floor))
            }
        },
        hideBlockedContent = context.appPreferences.hideBlockedContent || immersiveMode,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        LongClickMenu(
            menuState = menuState,
            shape = MaterialTheme.shapes.medium,
            onClick = { if (!hideReply) onReplyClick(post) },
            menuContent = {
                if (!hideReply) {
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
                if (canDelete && onMenuDeleteClick != null) {
                    TextMenuItem(text = R.string.title_delete, onClick = onMenuDeleteClick)
                }
            }
        ) {
            Card(
                header = {
                    if (immersiveMode) return@Card

                    UserHeader(
                        avatar = {
                            Avatar(
                                data = StringUtil.getAvatarUrl(author.portrait),
                                size = Sizes.Small,
                                contentDescription = author.nameShow
                            )
                        },
                        name = {
                            UserNameText(
                                userName = author.annotatedName,
                                userLevel = author.levelId,
                                bawuType = author.bawuType,
                            )
                        },
                        desc = {
                            Row {
                                if (post.time > 0L) {
                                    Text(text = post.getFormattedTime(context))
                                }
                                Text(text = post.decoration)
                            }
                        },
                        onClick = onUserClick
                    ) {
                        if (post.floor > 1) {
                            PostAgreeBtn(agreed = hasAgreed, agreeNum = agreeNum, onClick = onAgree)
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
                                style = MaterialTheme.typography.subtitle1,
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

                        contentRenders.fastForEach { it.Render() }
                    }

                    if (subPosts.isEmpty() || post.subPostNumber <= 0 || immersiveMode) return@Card

                    val blockedStyle = MaterialTheme.typography.body2.copy(
                        color = ExtendedTheme.colors.textDisabled,
                        fontSize = 13.sp
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(paddingModifier)
                            .clip(RoundedCornerShape(6.dp))
                            .background(ExtendedTheme.colors.floorCard)
                            .padding(vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        subPosts.fastForEach { item ->
                            BlockableContent(
                                blocked = item.blocked,
                                blockedTip = {
                                    Text(
                                        text = stringResource(id = R.string.tip_blocked_sub_post),
                                        style = blockedStyle,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    )
                                },
                            ) {
                                SubPostItem(
                                    subPost = item,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp),
                                    onReplyClick = { onSubPostReplyClick?.invoke(it) },
                                    onOpenSubPosts = onOpenSubPosts,
                                    onMenuCopyClick = onMenuCopyClick
                                )
                            }
                        }

                        if (post.subPostNumber <= subPosts.size) return@Column

                        Text(
                            text = stringResource(R.string.open_all_sub_posts, post.subPostNumber),
                            style = MaterialTheme.typography.caption,
                            fontSize = 13.sp,
                            color = ExtendedTheme.colors.accent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenSubPosts(0) }
                                .padding(vertical = 2.dp, horizontal = 12.dp)
                        )
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
    onReplyClick: ((SubPostItemData) -> Unit)?,
    onOpenSubPosts: (Long) -> Unit,
    onMenuCopyClick: (String) -> Unit,
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val coroutineScope = rememberCoroutineScope()
    val menuState = rememberMenuState()

    LongClickMenu(
        menuState = menuState,
        menuContent = {
            if (!context.appPreferences.hideReply) {
                TextMenuItem(text = R.string.title_reply) {
                    onReplyClick?.invoke(subPost)
                }
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
        shape = RoundedCornerShape(0),
        onClick = { onOpenSubPosts(subPost.id) }
    ) {
        ProvideTextStyle(value = MaterialTheme.typography.body2.copy(color = ExtendedTheme.colors.text)) {
            PbContentText(
                text = subPost.content,
                modifier = modifier,
                fontSize = 13.sp,
                emoticonSize = 0.9f,
                overflow = TextOverflow.Ellipsis,
                maxLines = 4,
                lineSpacing = 0.4.sp,
                inlineContent = if (subPost.isLz) mapOf(
                    "Lz" to buildChipInlineContent(
                        stringResource(id = R.string.tip_lz),
                        backgroundColor = ExtendedTheme.colors.textSecondary.copy(alpha = 0.1f),
                        color = ExtendedTheme.colors.textSecondary
                    ),
                ) else emptyMap()
            )
        }
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
            Button(onClick = onReload, enabled = canReload) {
                Text(text = stringResource(id = R.string.btn_refresh))
            }
        },
        scrollable = false
    )

@Composable
private fun ThreadLoadMoreIndicator(
    isLoading: Boolean,
    loadMoreEnd: Boolean,
    willLoad: Boolean,
    hasMore: Boolean,
) {
    Surface(
        elevation = 8.dp,
        shape = RoundedCornerShape(100),
        color = ExtendedTheme.colors.loadMoreIndicator,
        contentColor = ExtendedTheme.colors.text
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(10.dp)
                .animateContentSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProvideTextStyle(value = MaterialTheme.typography.body2.copy(fontSize = 13.sp)) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 3.dp,
                            color = ExtendedTheme.colors.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.text_loading),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    loadMoreEnd -> {
                        Text(
                            text = stringResource(id = R.string.no_more),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    hasMore -> {
                        Text(
                            text = if (willLoad) stringResource(id = R.string.release_to_load) else stringResource(
                                id = R.string.pull_to_load
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    else -> {
                        Text(
                            text = if (willLoad) stringResource(id = R.string.release_to_load_latest_posts) else stringResource(
                                id = R.string.pull_to_load_latest_posts
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview("LoadPreviousButton")
@Composable
private fun LoadPreviousButtonPreview() {
    TiebaLiteTheme {
        LoadPreviousButton(onClick = {})
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
