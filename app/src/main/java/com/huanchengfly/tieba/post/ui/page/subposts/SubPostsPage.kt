package com.huanchengfly.tieba.post.ui.page.subposts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.isOverlapping
import com.huanchengfly.tieba.post.copy
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.models.Like
import com.huanchengfly.tieba.post.ui.models.PostData
import com.huanchengfly.tieba.post.ui.models.SubPostItemData
import com.huanchengfly.tieba.post.ui.models.UserData
import com.huanchengfly.tieba.post.ui.page.Destination.CopyText
import com.huanchengfly.tieba.post.ui.page.Destination.Reply
import com.huanchengfly.tieba.post.ui.page.Destination.SubPosts
import com.huanchengfly.tieba.post.ui.page.Destination.Thread
import com.huanchengfly.tieba.post.ui.page.Destination.UserProfile
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.thread.PostCard
import com.huanchengfly.tieba.post.ui.page.thread.SubPostBlockedTip
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.widgets.compose.BlurNavigationBarPlaceHolder
import com.huanchengfly.tieba.post.ui.widgets.compose.BlurScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.CenterAlignedTopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.FavoriteButton
import com.huanchengfly.tieba.post.ui.widgets.compose.LiftUpSpacer
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.StickyHeaderOverlay
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.UserDataHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
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

@NonRestartableComposable
@Composable
fun SubPostsSheetPage(
    params: SubPosts,
    navigator: NavController,
    viewModel: SubPostsViewModel = hiltViewModel()
) {
    ProvideNavigator(navigator) {
        with(params) {
            SubPostsContent(viewModel, forumId, threadId, postId, subPostId, true, navigator::navigateUp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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

    val state by viewModel.state.collectAsStateWithLifecycle()
    val forum = state.forum

    val lazyListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect {
            val unhandledEvent = it?: return@collect
            when(unhandledEvent) {
                is CommonUiEvent.Toast -> {
                    context.toastShort(unhandledEvent.message.toString())
                }

                is SubPostsUiEvent.ScrollToSubPosts -> {
                    val targetIndex = 2 + state.subPosts.indexOfFirst { s -> s.id == subPostId }
                    delay(20)
                    lazyListState.scrollToItem(targetIndex.coerceIn(0, state.subPosts.lastIndex))
                }
            }
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
            state.error?.let { err -> ErrorScreen(error = err) }
        },
        isLoading = state.isRefreshing
    ) {
        val topAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
        val hideReply by rememberPreferenceAsState(booleanPreferencesKey(KEY_REPLY_HIDE), false)
        val canReply by remember { derivedStateOf { !(hideReply || account == null) } }

        // Workaround to make StickyHeader respect content padding
        var useStickyHeaderWorkaround by remember { mutableStateOf(false) }

        // Initialize nullable click listeners:
        val onReplySubPost: ((SubPostItemData) -> Unit)? = { item: SubPostItemData ->
            navigator.navigate(
                Reply(
                    forumId = forumId,
                    forumName = forum?.get { name } ?: "",
                    threadId = threadId,
                    postId = postId,
                    subPostId = item.id,
                    replyUserId = item.author.id,
                    replyUserName = item.author.getDisplayName(context),
                    replyUserPortrait = item.author.portrait,
                )
            )
        }.takeIf { canReply }

        // Null when not my SubPost
        val onDeleteSubPost: (SubPostItemData) -> Unit = { item: SubPostItemData ->
            deleteSubPost = item
            confirmDeleteDialogState.show()
        }

        // This is non-nullable, initialize here just for convenience
        val onCopyClick: (String) -> Unit = { navigator.navigate(CopyText(it)) }

        BlurScaffold(
            topHazeBlock = {
                blurEnabled = topAppBarScrollBehavior.isOverlapping
            },
            topBar = {
                TitleBar(
                    isSheet = isSheet,
                    post = state.post,
                    onBack = onNavigateUp,
                    onAction = {
                        navigator.navigate(Thread(forumId = forumId, threadId = threadId, postId = postId))
                    },
                    scrollBehavior = topAppBarScrollBehavior
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
            val hideBlockedContent: Boolean = context.appPreferences.hideBlockedContent
            val isLoading by remember { derivedStateOf { state.isLoading } }

            SwipeUpLazyLoadColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
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
                            onUserClick = {
                                navigator.navigate(UserProfile(postItem.author.id))
                            },
                            onLikeClick = viewModel::onPostLikeClicked,
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
                            onMenuCopyClick = onCopyClick,
                            onMenuDeleteClick = {
                                deleteSubPost = null
                                confirmDeleteDialogState.show()
                            }
                            .takeIf { postItem.author.id == account?.uid?.toLongOrNull() } // Check is my Post
                        )
                        HorizontalDivider(thickness = 2.dp)
                    }
                } // End of post card

                if (useStickyHeaderWorkaround) {
                    item(key = "SubPostsHeader", contentType = Unit) {
                        SubPostsHeader(postNum = state.totalCount)
                    }
                } else {
                    stickyHeader(key = "SubPostsHeader", contentType = Unit) {
                        SubPostsHeader(
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer),
                            postNum = state.totalCount
                        )
                    }
                }

                items(items = state.subPosts, key = { subPost -> subPost.id }) { item ->
                    SubPostItem(
                        item = item,
                        hideBlockedContent = hideBlockedContent,
                        onUserClick = {
                            navigator.navigate(UserProfile(it.id))
                        },
                        onAgree = viewModel::onSubPostLikeClicked,
                        onMenuReplyClick = onReplySubPost,
                        onMenuCopyClick = onCopyClick,
                        onMenuDeleteClick = onDeleteSubPost.takeIf {
                            item.authorId == account?.uid?.toLongOrNull() // Check is my SubPost
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@NonRestartableComposable
@Composable
private fun TitleBar(
    isSheet: Boolean,
    post: PostData?,
    onBack: () -> Unit,
    onAction: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior?,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = if (post != null) {
                    stringResource(id = R.string.title_sub_posts, post.floor)
                } else {
                    stringResource(id = R.string.title_sub_posts_default)
                }
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
        scrollBehavior = scrollBehavior,
        content = content
    )
}

@Composable
private fun BottomBar(modifier: Modifier = Modifier, account: Account, onReply: () -> Unit) =
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
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
            Surface(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .weight(1f),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                tonalElevation = 2.dp,
                onClick = onReply
            ) {
                Text(
                    text = stringResource(id = R.string.tip_reply_thread),
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        LiftUpSpacer()
    }

@Composable
private fun SubPostItem(
    item: SubPostItemData,
    hideBlockedContent: Boolean = false,
    onUserClick: (UserData) -> Unit = {},
    onAgree: (SubPostItemData) -> Unit = {},
    onMenuReplyClick: ((SubPostItemData) -> Unit)?,
    onMenuCopyClick: ((String) -> Unit)? = null,
    onMenuDeleteClick: ((SubPostItemData) -> Unit)? = null,
) =
    BlockableContent(
        blocked = item.blocked,
        blockedTip = SubPostBlockedTip,
        hideBlockedContent = hideBlockedContent,
    )
{
    val context = LocalContext.current
    val navigator = LocalNavController.current
    val coroutineScope = rememberCoroutineScope()

    LongClickMenu(
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
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            UserDataHeader(
                author = item.author,
                desc = remember { getRelativeTimeString(context, item.time) },
                onClick = { onUserClick(item.author) }
            ) {
                PostLikeButton(like = item.like, onClick = { onAgree(item) })
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 44.dp, top = 8.dp)
            ) {
                item.pbContent!!.fastForEach {
                    it.Render()
                }
            }
        }
    }
}

@Composable
private fun SubPostsHeader(modifier: Modifier = Modifier, postNum: Int) {
    Text(
        text = stringResource(R.string.title_sub_posts_header, postNum),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.titleMedium
    )
}

@NonRestartableComposable
@Composable
fun PostLikeButton(like: Like, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FavoriteButton(modifier, iconSize = 18.dp, favorite = like.liked, onClick = onClick) {
        if (like.count > 0) {
            Text(
                text = remember(like.count) { like.count.getShortNumString() },
                modifier = Modifier.padding(horizontal = 4.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Preview("PostFavoriteButton")
@Composable
private fun PostAgreeBtnPreview() = TiebaLiteTheme {
    PostLikeButton(like = Like(true, 99999), onClick = {})
}
