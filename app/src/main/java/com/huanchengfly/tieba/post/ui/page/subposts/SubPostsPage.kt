package com.huanchengfly.tieba.post.ui.page.subposts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.AnimationConstants
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
import androidx.compose.material3.Button
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.LocalHabitSettings
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.models.database.Account
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
import com.huanchengfly.tieba.post.ui.page.thread.ThreadLikeUiEvent
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.widgets.compose.BlurNavigationBarPlaceHolder
import com.huanchengfly.tieba.post.ui.widgets.compose.BlurScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.CenterAlignedTopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultInputScale
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.FavoriteButton
import com.huanchengfly.tieba.post.ui.widgets.compose.LiftUpSpacer
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.StickyHeaderOverlay
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.UserDataHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.AnyPopDialogProperties
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.DirectionState
import com.huanchengfly.tieba.post.ui.widgets.compose.fixedTopBarPadding
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.stickyHeaderBackground
import com.huanchengfly.tieba.post.ui.widgets.compose.useStickyHeaderWorkaround
import com.huanchengfly.tieba.post.utils.DateTimeUtils.getRelativeTimeString
import com.huanchengfly.tieba.post.utils.LocalAccount
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import com.huanchengfly.tieba.post.utils.TiebaUtil
import dev.chrisbanes.haze.ExperimentalHazeApi
import kotlinx.coroutines.Job
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

private const val PostContentType = 0
private val HeaderContentType = Unit
// SubpostContentType use Null by default

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeApi::class)
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
    val context = LocalContext.current
    val navigator = LocalNavController.current
    val account = LocalAccount.current
    val myUid = account?.uid
    val canReply = account != null && !LocalHabitSettings.current.hideReply

    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val isLoadingMore = uiState.isLoadingMore
    val hasMore = uiState.page.hasMore
    val forumName = uiState.forumName

    val lazyListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect {
            when (it) {
                is ThreadLikeUiEvent -> context.toastShort(it.toMessage(context))

                is CommonUiEvent.Toast -> context.toastShort(it.message.toString())

                is SubPostsUiEvent.ScrollToSubPosts -> {
                    val targetIndex = 2 + uiState.subPosts.indexOfFirst { s -> s.id == subPostId }
                    delay(AnimationConstants.DefaultDurationMillis.toLong())
                    lazyListState.animateScrollToItem(targetIndex.coerceIn(0, uiState.subPosts.lastIndex))
                }

                is SubPostsUiEvent.DeletePostFailed -> context.toastShort(R.string.toast_delete_failure, it.message)

                else ->  {/* Unknown UI event */}
            }
        }
    }

    val deleteTarget by viewModel.delete.collectAsStateWithLifecycle()
    DeletePostSubPostDialog(
        deleteTarget = deleteTarget,
        onCancel = viewModel::onDeleteCancelled,
        onConfirm = viewModel::onDeleteConfirmed
    )

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
        isEmpty = uiState.subPosts.isEmpty(),
        isError = uiState.error != null,
        onReload = viewModel::onRefresh,
        errorScreen = { ErrorScreen(error = uiState.error) },
        isLoading = uiState.isRefreshing
    ) {
        val useStickyHeaderWorkaround = useStickyHeaderWorkaround()
        val topAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

        // Initialize nullable click listeners:
        val onReplySubPostClickedListener: ((SubPostItemData) -> Unit)? = { item: SubPostItemData ->
            navigator.navigate(
                Reply(
                    forumId = forumId,
                    forumName = forumName.orEmpty(),
                    threadId = threadId,
                    postId = postId,
                    subPostId = item.id,
                    replyUserId = item.author.id,
                    replyUserName = item.author.nameShow,
                    replyUserPortrait = item.author.portrait,
                )
            )
        }.takeIf { canReply }

        // non-nullable, initialize here for convenience
        val onCopyClickedListener: (String) -> Unit = { navigator.navigate(CopyText(it)) }

        BlurScaffold(
            topHazeBlock = {
                blurEnabled = lazyListState.canScrollBackward
                inputScale = DefaultInputScale
            },
            topBar = {
                TitleBar(
                    isSheet = isSheet,
                    post = uiState.post,
                    onBack = onNavigateUp,
                    onAction = {
                        navigator.navigate(route = Thread(threadId, forumId, postId = postId))
                    },
                    scrollBehavior = topAppBarScrollBehavior
                ) {
                    if (useStickyHeaderWorkaround) {
                        StickyHeaderOverlay(state = lazyListState) {
                            SubPostsHeader(postNum = uiState.page.postCount)
                        }
                    }
                }
            },
            bottomBar = {
                if (account == null || !canReply) {
                    BlurNavigationBarPlaceHolder()
                } else {
                    BottomBar(
                        account = account,
                        onReply = {
                            if (forumName != null ) {
                                navigator.navigate(Reply(forumId, forumName, threadId, postId))
                            }
                        }
                    )
                }
            },
            bottomHazeBlock = {
                blurEnabled = lazyListState.canScrollForward
                inputScale = DefaultInputScale
            }
        ) { padding ->
            val contentPadding = padding.fixedTopBarPadding()

            SwipeUpLazyLoadColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
                state = lazyListState,
                contentPadding = contentPadding,
                isLoading = isLoadingMore,
                onLazyLoad = {
                    if (hasMore && uiState.subPosts.isNotEmpty()) {
                        viewModel.onLoadMore()
                    }
                },
                onLoad = null,
                bottomIndicator = {
                    LoadMoreIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        isLoading = isLoadingMore,
                        noMore = !hasMore,
                        onThreshold = false
                    )
                }
            ) {
                val postItem = uiState.post ?: return@SwipeUpLazyLoadColumn
                item(key = "Post$postId", contentType = PostContentType) {
                    Column {
                        PostCard(
                            post = postItem,
                            onUserClick = {
                                navigator.navigate(UserProfile(postItem.author.id))
                            },
                            onReplyClick = { it: PostData ->
                                navigator.navigate(
                                    Reply(
                                        forumId = forumId,
                                        forumName = forumName.orEmpty(),
                                        threadId = threadId,
                                        postId = postId,
                                        replyUserId = it.author.id,
                                        replyUserName = it.author.nameShow,
                                        replyUserPortrait = it.author.portrait
                                    )
                                )
                            }.takeIf { canReply },
                            onMenuCopyClick = onCopyClickedListener,
                            onMenuDeleteClick = viewModel::onDeletePost.takeIf { postItem.author.id == myUid } // Check is my Post
                        )
                        HorizontalDivider(thickness = 2.dp)
                    }
                } // End of post card

                if (useStickyHeaderWorkaround) {
                    item(key = "SubPostsHeader", contentType = HeaderContentType) {
                        SubPostsHeader(postNum = uiState.page.postCount)
                    }
                } else {
                    stickyHeader(key = "SubPostsHeader", contentType = HeaderContentType) {
                        SubPostsHeader(
                            modifier = Modifier.stickyHeaderBackground(topAppBarScrollBehavior.state, lazyListState),
                            postNum = uiState.page.postCount
                        )
                    }
                }

                items(items = uiState.subPosts, key = { subPost -> subPost.id }) { item ->
                    SubPostItem(
                        item = item,
                        onUserClick = {
                            navigator.navigate(UserProfile(it.id))
                        },
                        onAgree = viewModel::onSubPostLikeClicked,
                        onMenuReplyClick = onReplySubPostClickedListener,
                        onMenuCopyClick = onCopyClickedListener,
                        onMenuDeleteClick = viewModel::onDeleteSubPost.takeIf { item.authorId == myUid } // Check is my SubPost
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
            .background(TiebaLiteTheme.extendedColorScheme.navigationContainer)
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
                data = remember { StringUtil.getAvatarUrl(account.portrait) },
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
    onUserClick: (UserData) -> Unit = {},
    onAgree: (SubPostItemData) -> Unit = {},
    onMenuReplyClick: ((SubPostItemData) -> Unit)?,
    onMenuCopyClick: ((String) -> Unit)? = null,
    onMenuDeleteClick: ((SubPostItemData) -> Unit)? = null,
) =
    BlockableContent(
        blocked = item.blocked,
        blockedTip = SubPostBlockedTip,
        hideBlockedContent = false,
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
                item.content!!.fastForEach { it.Render() }
            }
        }
    }
}

@Composable
private fun DeletePostSubPostDialog(
    dialogState: DialogState = rememberDialogState(),
    deleteTarget: Any?, // Post or Subpost
    onCancel: () -> Unit,
    onConfirm: () -> Job
) {
    LaunchedEffect(deleteTarget) {
        if (deleteTarget != null) dialogState.show()
    }

    if (!dialogState.show) return

    var deleting by remember { mutableStateOf(false) }

    Dialog(
        dialogState = dialogState,
        dialogProperties = AnyPopDialogProperties(
            direction = DirectionState.CENTER,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        title = { Text(text = stringResource(R.string.title_delete)) },
        buttons = {
            AnimatedVisibility(visible = !deleting) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    DialogNegativeButton(text = stringResource(R.string.button_cancel), onClick = onCancel)

                    Button(
                        onClick = {
                            deleting = true
                            onConfirm().invokeOnCompletion {
                                dismiss()
                                deleting = false
                            }
                        },
                        content = { Text(text = stringResource(R.string.button_sure_default)) }
                    )
                }
            }
        }
    ) {
        if (deleteTarget == null || deleting) {
            Text(text = stringResource(id = R.string.dialog_content_wait))
        } else {
            val type = if (deleteTarget is PostData) {
                stringResource(R.string.tip_post_floor, deleteTarget.floor)
            } else {
                stringResource(R.string.this_reply)
            }
            Text(text = stringResource(R.string.message_confirm_delete, type))
        }
    }
}

@NonRestartableComposable
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
