package com.huanchengfly.tieba.post.ui.page.subposts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.ui.common.PbContentText
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.threadBottomBar
import com.huanchengfly.tieba.post.ui.models.PostData
import com.huanchengfly.tieba.post.ui.models.SubPostItemData
import com.huanchengfly.tieba.post.ui.models.UserData
import com.huanchengfly.tieba.post.ui.page.Destination.CopyText
import com.huanchengfly.tieba.post.ui.page.Destination.Reply
import com.huanchengfly.tieba.post.ui.page.Destination.SubPosts
import com.huanchengfly.tieba.post.ui.page.Destination.Thread
import com.huanchengfly.tieba.post.ui.page.Destination.UserProfile
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.thread.PostCard
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockTip
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.widgets.compose.Card
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.FavoriteButton
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.OneTimeMeasurer
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.UserNameText
import com.huanchengfly.tieba.post.ui.widgets.compose.VerticalDivider
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils.Companion.KEY_REPLY_HIDE
import com.huanchengfly.tieba.post.utils.DateTimeUtils.getRelativeTimeString
import com.huanchengfly.tieba.post.utils.LocalAccount
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SubPostsSheetPage(
    params: SubPosts,
    navigator: NavController,
    viewModel: SubPostsViewModel = hiltViewModel()
) {
    CompositionLocalProvider(LocalNavController provides navigator) {
        with(params) {
            SubPostsContent(viewModel, forumId, threadId, postId, subPostId, true, navigator::navigateUp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SubPostsContent(
    viewModel: SubPostsViewModel,
    forumId: Long,
    threadId: Long,
    postId: Long,
    subPostId: Long = 0L,
    isSheet: Boolean = false,
    onNavigateUp: () -> Unit = {},
) {
    val navigator = LocalNavController.current
    val account = LocalAccount.current
    val context = LocalContext.current

    LazyLoad(key = viewModel, loaded = viewModel.initialized) {
        viewModel.initialize()
    }

    val isRefreshing = viewModel.refreshing

    val isLoading = viewModel.loading

    val state by viewModel.state

    val forum = state.forum

    val lazyListState = rememberLazyListState()

    val uiEvent by viewModel.uiEvent

    LaunchedEffect(uiEvent) {
        val unhandledEvent = uiEvent?: return@LaunchedEffect
        if (unhandledEvent is SubPostsUiEvent.ScrollToSubPosts) {
            delay(20)
            lazyListState.scrollToItem(2 + state.subPosts.indexOfFirst { it.id == subPostId })
            viewModel.onUiEventReceived()
        }
    }

    val confirmDeleteDialogState = rememberDialogState()
    var deleteSubPost by remember { mutableStateOf<SubPostItemData?>(null) }
    ConfirmDialog(
        dialogState = confirmDeleteDialogState,
        onConfirm = {
            if (deleteSubPost == null) {
                val isSelfPost = state.post?.author?.id == account?.uid?.toLongOrNull()
                viewModel.requestDeletePost(deleteMyPost = isSelfPost)
            } else {
                val isSelfSubPost = deleteSubPost!!.authorId == account?.uid?.toLongOrNull()
                viewModel.requestDeletePost(deleteMyPost = isSelfSubPost)
            }
        }
    ) {
        val deleteType = if (deleteSubPost == null && state.post != null) {
            stringResource(id = R.string.tip_post_floor, state.post!!.floor) // floor
        } else {
            stringResource (id = R.string.this_reply)    // reply
        }
        Text(text = stringResource(id = R.string.message_confirm_delete, deleteType))
    }

//    onGlobalEvent<GlobalEvent.ReplySuccess>(
//        filter = { it.threadId == threadId && it.postId == postId }
//    ) { event ->
//        viewModel.send(
//            SubPostsUiIntent.Load(
//                forumId,
//                threadId,
//                postId,
//                subPostId.takeIf { loadFromSubPost } ?: 0L
//            )
//        )
//    }

    StateScreen(
        modifier = Modifier.fillMaxSize(),
        isEmpty = state.subPosts.isEmpty(),
        isError = state.error != null,
        onReload = viewModel::requestLoad,
        errorScreen = {
            viewModel.error?.let { err -> ErrorScreen(error = err) }
        },
        isLoading = isRefreshing
    ) {
        var bottomBarHeight: Dp by remember { mutableStateOf(Dp.Hairline) }
        val hideReply by rememberPreferenceAsState(booleanPreferencesKey(KEY_REPLY_HIDE), false)

        MyScaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TitleBar(isSheet = isSheet, post = state.post, onBack = onNavigateUp) {
                    navigator.navigate(
                        Thread(forumId = forumId, threadId = threadId, postId = postId)
                    )
                }
            },
            bottomBar = {
                if (account == null || hideReply) return@MyScaffold
                OneTimeMeasurer { size: IntSize? ->
                    BottomBar(
                        account = account,
                        onReply = {
                            val forumName = forum?.get { name } ?: return@BottomBar
                            if (forumName.isNotEmpty()) {
                                navigator.navigate(
                                    Reply(
                                        forumId = forum.get { id },
                                        forumName = forumName,
                                        threadId = threadId,
                                        postId = postId)
                                )
                            }
                        }
                    )
                    // Update BottomBar's height
                    if (size != null && bottomBarHeight == Dp.Hairline) {
                        with(LocalDensity.current) { bottomBarHeight = size.height.toDp() }
                    }
                }
            }
        ) { paddingValues ->
            SwipeUpLazyLoadColumn(
                modifier = Modifier.fillMaxSize().padding(bottom = bottomBarHeight),
                contentPadding = paddingValues,
                isLoading = isLoading,
                onLazyLoad = {
                    if (state.hasMore && state.post != null && state.subPosts.isNotEmpty()) {
                        viewModel.requestLoadMore()
                    }
                },
                onLoad = null,
                bottomIndicator = {
                    LoadMoreIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        isLoading = isLoading,
                        noMore = !state.hasMore,
                        onThreshold = false
                    )
                }
            ) {
                val postItem = state.post ?: return@SwipeUpLazyLoadColumn
                item(key = "Post$postId") {
                    Column {
                        PostCard(
                            post = postItem,
                            contentRenders = state.postContentRenders,
                            canDelete = postItem.author.id == account?.uid?.toLongOrNull(),
                            isCollected = false,
                            onUserClick = {
                                navigator.navigate(UserProfile(postItem.author.id))
                            },
                            onAgree = {
                                val hasAgreed = postItem.hasAgree != 0
                                viewModel.onAgreePost(!hasAgreed)
                            },
                            onReplyClick = { it: PostData ->
                                navigator.navigate(
                                    Reply(
                                        forumId = forumId,
                                        forumName = forum?.get { name } ?: "",
                                        threadId = threadId,
                                        postId = postId,
                                        replyUserId = it.author.id,
                                        replyUserName = it.author.getDisplayName(context),
                                        replyUserPortrait = it.author.portrait
                                    )
                                )
                            }.takeUnless { hideReply },
                            onMenuCopyClick = {
                                navigator.navigate(CopyText(it))
                            },
                        ) {
                            deleteSubPost = null
                            confirmDeleteDialogState.show()
                        }
                        VerticalDivider(thickness = 2.dp)
                    }
                } // End of post card

                stickyHeader(key = "SubPostsHeader") {
                    Text(
                        text = stringResource(R.string.title_sub_posts_header, state.totalCount),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ExtendedTheme.colors.background.copy(0.96f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.subtitle1
                    )
                }

                items(items = state.subPosts, key = { subPost -> subPost.id }) { item ->
                    SubPostItem(
                        item = item,
                        canDelete = item.authorId == account?.uid?.toLongOrNull(),
                        onUserClick = {
                            navigator.navigate(UserProfile(it.id))
                        },
                        onAgree = {
                            viewModel.onAgreeSubPost(subPostId = it.id, !it.hasAgree)
                        },
                        onReplyClick = { it: SubPostItemData ->
                            navigator.navigate(
                                Reply(
                                    forumId = forumId,
                                    forumName = forum?.get { name } ?: "",
                                    threadId = threadId,
                                    postId = postId,
                                    subPostId = it.id,
                                    replyUserId = it.author.id,
                                    replyUserName = it.author.getDisplayName(context),
                                    replyUserPortrait = it.author.portrait,
                                )
                            )
                        }.takeUnless { hideReply },
                        onMenuCopyClick = {
                            navigator.navigate(CopyText(it))
                        },
                        onMenuDeleteClick = {
                            deleteSubPost = it
                            confirmDeleteDialogState.show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TitleBar(isSheet: Boolean, post: PostData?, onBack: () -> Unit, onAction: () -> Unit) =
    TitleCentredToolbar(
        title = {
            Text(text = post?.let {
                stringResource(id = R.string.title_sub_posts, it.floor)
            } ?: stringResource(id = R.string.title_sub_posts_default),
                fontWeight = FontWeight.Bold, style = MaterialTheme.typography.h6
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = if (isSheet) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(id = R.string.btn_close)
                )
            }
        },
        actions = {
            if (!isSheet) {
                IconButton(onClick = onAction) {
                    Icon(
                        imageVector = Icons.Rounded.OpenInBrowser,
                        contentDescription = stringResource(id = R.string.btn_open_origin_thread)
                    )
                }
            }
        }
    )

@Composable
private fun BottomBar(modifier: Modifier = Modifier, account: Account, onReply: () -> Unit) =
    Column(
        modifier = modifier.background(ExtendedTheme.colors.threadBottomBar)
    ) {
        val context = LocalContext.current
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Avatar(
                data = StringUtil.getAvatarUrl(account.portrait),
                size = Sizes.Tiny,
                contentDescription = account.name,
            )
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(ExtendedTheme.colors.floorCard)
                    .clickable(onClick = onReply)
                    .padding(8.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.tip_reply_thread),
                    style = MaterialTheme.typography.caption,
                    color = ExtendedTheme.colors.textSecondary,
                )
            }
        }

        val spacerMinHeight = remember {
            if (context.appPreferences.liftUpBottomBar) 16.dp else Dp.Hairline
        }
        Box(
            modifier = Modifier.requiredHeightIn(min = spacerMinHeight)
        ) {
            Spacer(
                modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)
            )
        }
    }

@Composable
private fun SubPostItem(
    item: SubPostItemData,
    canDelete: Boolean,
    onUserClick: (UserData) -> Unit = {},
    onAgree: (SubPostItemData) -> Unit = {},
    onReplyClick: ((SubPostItemData) -> Unit)?,
    onMenuCopyClick: ((String) -> Unit)? = null,
    onMenuDeleteClick: ((SubPostItemData) -> Unit)? = null,
) = BlockableContent(
    blocked = item.blocked,
    blockedTip = { BlockTip() },
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
    )
{
    val context = LocalContext.current
    val navigator = LocalNavController.current
    val coroutineScope = rememberCoroutineScope()
    val menuState = rememberMenuState()

    LongClickMenu(
        menuState = menuState,
        indication = null,
        menuContent = {
            if (onReplyClick != null) {
                TextMenuItem(text = stringResource(id = R.string.btn_reply)) {
                    onReplyClick(item)
                }
            }

            if (onMenuCopyClick != null) {
                TextMenuItem(text = stringResource(id = R.string.menu_copy)) {
                    onMenuCopyClick(item.plainText)
                }
            }

            TextMenuItem(text = stringResource(id = R.string.title_report)) {
                coroutineScope.launch {
                    TiebaUtil.reportPost(context, navigator, item.id.toString())
                }
            }

            if (canDelete && onMenuDeleteClick != null) {
                TextMenuItem(text = stringResource(id = R.string.title_delete)) {
                    onMenuDeleteClick(item)
                }
            }
        }
    ) {
        Card(
            header = {
                val author = item.author
                UserHeader(
                    avatar = {
                        Avatar(
                            data = author.avatarUrl,
                            size = Sizes.Small,
                            contentDescription = author.name
                        )
                    },
                    name = {
                        UserNameText(
                            userName = author.getDisplayName(context),
                            userLevel = author.levelId,
                            isLz = author.isLz,
                            bawuType = author.bawuType,
                        )
                    },
                    desc = {
                        Text(text = getRelativeTimeString(context, item.time))
                    },
                    onClick = { onUserClick(author) }
                ) {
                    PostAgreeBtn(agreed = item.hasAgree, agreeNum = item.agreeNum) {
                        onAgree(item)
                    }
                }
            },
            content = {
                PbContentText(
                    text = item.content,
                    modifier = Modifier
                        .padding(start = 44.dp)
                        .fillMaxWidth(),
                    style = MaterialTheme.typography.body1
                )
            }
        )
    }
}

@Preview("PostAgreeBtn")
@Composable
private fun PostAgreeBtnPreview() {
    TiebaLiteTheme {
        Surface(Modifier.padding(12.dp)) {
            PostAgreeBtn(agreed = true, agreeNum = 999) { /*** NO-OP ***/ }
        }
    }
}

@Composable
fun PostAgreeBtn(modifier: Modifier = Modifier, agreed: Boolean, agreeNum: Long, onClick: () -> Unit) {
    FavoriteButton(modifier, iconSize = 18.dp, favorite = agreed, onClick = onClick) { color ->
        if (agreeNum > 0) {
            Text(
                text = agreeNum.getShortNumString(),
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(horizontal = 4.dp),
                color = color,
                style = MaterialTheme.typography.caption
            )
        }
    }
}
