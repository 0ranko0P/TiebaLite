package com.huanchengfly.tieba.post.ui.page.subposts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.copy
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.ui.common.PbContentText
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.LocalExtendedColors
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
import com.huanchengfly.tieba.post.ui.widgets.compose.BlurNavigationBarPlaceHolder
import com.huanchengfly.tieba.post.ui.widgets.compose.BlurScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Card
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.FavoriteButton
import com.huanchengfly.tieba.post.ui.widgets.compose.LiftUpSpacer
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.StickyHeaderOverlay
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.UserDataHeader
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
        val hideReply by rememberPreferenceAsState(booleanPreferencesKey(KEY_REPLY_HIDE), false)
        val canReply by remember { derivedStateOf { !(hideReply || account == null) } }

        // Workaround to make StickyHeader respect content padding
        var useStickyHeaderWorkaround by remember { mutableStateOf(false) }

        BlurScaffold(
            topHazeBlock = remember { {
                blurEnabled = lazyListState.canScrollBackward
            } },
            topBar = {
                TitleBar(
                    isSheet = isSheet,
                    post = state.post,
                    onBack = onNavigateUp,
                    onAction = {
                        navigator.navigate(Thread(forumId = forumId, threadId = threadId, postId = postId))
                    }
                ) {
                    if (useStickyHeaderWorkaround) {
                        StickyHeaderOverlay(state = lazyListState) {
                            SubPostsHeader(postNum = state.totalCount)
                        }
                    }
                }
            },
            bottomBar = {
                if (account == null || hideReply) {
                    BlurNavigationBarPlaceHolder()
                    return@BlurScaffold
                }

                BottomBar(
                    account = account,
                    onReply = {
                        val forumName = forum?.get { name } ?: return@BottomBar
                        navigator.navigate(
                            route = Reply(forumId = forum.get { id }, forumName, threadId, postId)
                        )
                    }
                )
            }
        ) { padding ->

            useStickyHeaderWorkaround = padding.calculateTopPadding() != Dp.Hairline

            // Ignore Scaffold padding changes if workaround enabled
            val direction = LocalLayoutDirection.current
            val contentPadding = if (useStickyHeaderWorkaround) remember { padding.copy(direction) } else padding

            SwipeUpLazyLoadColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
                contentPadding = contentPadding,
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
                            }.takeIf { canReply },
                            onMenuCopyClick = {
                                navigator.navigate(CopyText(it))
                            },
                            onMenuDeleteClick = {
                                deleteSubPost = null
                                confirmDeleteDialogState.show()
                            }
                            .takeIf { postItem.author.id == account?.uid?.toLongOrNull() } // Check is my Post
                        )
                        VerticalDivider(thickness = 2.dp)
                    }
                } // End of post card

                if (useStickyHeaderWorkaround) {
                    item(key = "SubPostsHeader", contentType = Unit) {
                        SubPostsHeader(postNum = state.totalCount)
                    }
                } else {
                    stickyHeader(key = "SubPostsHeader", contentType = Unit) {
                        SubPostsHeader(
                            modifier = Modifier.background(LocalExtendedColors.current.topBar),
                            postNum = state.totalCount
                        )
                    }
                }

                items(items = state.subPosts, key = { subPost -> subPost.id }) { item ->
                    SubPostItem(
                        item = item,
                        onUserClick = {
                            navigator.navigate(UserProfile(it.id))
                        },
                        onAgree = {
                            viewModel.onAgreeSubPost(subPostId = it.id, !it.hasAgree)
                        },
                        onMenuReplyClick = { it: SubPostItemData ->
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
                        }.takeIf { canReply },
                        onMenuCopyClick = {
                            navigator.navigate(CopyText(it))
                        },
                        onMenuDeleteClick = { it: SubPostItemData ->
                            deleteSubPost = it
                            confirmDeleteDialogState.show()
                        }.takeIf { item.authorId == account?.uid?.toLongOrNull() } // Check is my SubPost
                    )
                }
            }
        }
    }
}

@Composable
private fun TitleBar(
    isSheet: Boolean,
    post: PostData?,
    onBack: () -> Unit,
    onAction: () -> Unit,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
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
        },
        elevation = Dp.Hairline,
        content = content
    )
}

@Composable
private fun BottomBar(modifier: Modifier = Modifier, account: Account, onReply: () -> Unit) =
    Column(
        modifier = modifier
            .background(ExtendedTheme.colors.threadBottomBar)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
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

        LiftUpSpacer()
    }

@Composable
private fun SubPostItem(
    item: SubPostItemData,
    onUserClick: (UserData) -> Unit = {},
    onAgree: (SubPostItemData) -> Unit = {},
    onMenuReplyClick: ((SubPostItemData) -> Unit)?,
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
            if (onMenuReplyClick != null) {
                TextMenuItem(text = stringResource(id = R.string.btn_reply)) {
                    onMenuReplyClick(item)
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

            if (onMenuDeleteClick != null) {
                TextMenuItem(text = stringResource(id = R.string.title_delete)) {
                    onMenuDeleteClick(item)
                }
            }
        }
    ) {
        Card(
            header = {
                val author = item.author
                UserDataHeader(
                    author = author,
                    desc = remember { getRelativeTimeString(context, item.time) },
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

@Composable
private fun SubPostsHeader(modifier: Modifier = Modifier, postNum: Int) {
    Text(
        text = stringResource(R.string.title_sub_posts_header, postNum),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.subtitle1
    )
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
