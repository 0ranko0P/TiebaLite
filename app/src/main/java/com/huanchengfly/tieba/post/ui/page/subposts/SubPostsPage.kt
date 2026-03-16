package com.huanchengfly.tieba.post.ui.page.subposts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.VerticalAlignTop
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.LocalHabitSettings
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.collectUiEventWithLifecycle
import com.huanchengfly.tieba.post.arch.isOverlapping
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.FadedVisibility
import com.huanchengfly.tieba.post.ui.common.LocalAnimatedVisibilityScope
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
import com.huanchengfly.tieba.post.ui.utils.rememberScrollOrientationConnection
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.widgets.compose.BlurScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.CenterAlignedTopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.FavoriteButton
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadingIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.PlainTooltipBox
import com.huanchengfly.tieba.post.ui.widgets.compose.SharedTransitionUserHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.StickyHeaderOverlay
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.AnyPopDialogProperties
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.DirectionState
import com.huanchengfly.tieba.post.ui.widgets.compose.fixedTopBarPadding
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.scrollToItemWithHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.stickyHeaderBackground
import com.huanchengfly.tieba.post.ui.widgets.compose.useStickyHeaderWorkaround
import com.huanchengfly.tieba.post.utils.DateTimeUtils.getRelativeTimeString
import com.huanchengfly.tieba.post.utils.LocalAccount
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import com.huanchengfly.tieba.post.utils.TiebaUtil
import kotlinx.coroutines.Job
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
            SubPostsContent(viewModel, threadId, postId, true, navigator::navigateUp)
        }
    }
}

@NonRestartableComposable
@Composable
fun SubPostsPage(
    params: SubPosts,
    navigator: NavController,
    viewModel: SubPostsViewModel = hiltViewModel()
) {
    ProvideNavigator(navigator) {
        with(params) {
            SubPostsContent(viewModel, threadId, postId, isSheet = false, navigator::navigateUp)
        }
    }
}

private const val PostContentType = 0
private val HeaderContentType = Unit
// SubpostContentType use Null by default

@Composable
private fun SubPostsContent(
    viewModel: SubPostsViewModel,
    threadId: Long,
    postId: Long,
    isSheet: Boolean = false,
    onNavigateUp: () -> Unit = {},
) {
    val context = LocalContext.current
    val navigator = LocalNavController.current
    val useStickyHeaderWorkaround = useStickyHeaderWorkaround()
    val account = LocalAccount.current
    val myUid = account?.uid
    val canReply = account != null && !LocalHabitSettings.current.hideReply

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoadingMore = uiState.isLoadingMore
    val hasMore = uiState.page.hasMore
    val forumName = uiState.forumName
    val forumId = viewModel.forumId
    val postId = uiState.post?.id ?: postId

    val lazyListState = rememberLazyListState()

    viewModel.uiEvent.collectUiEventWithLifecycle {
        when (it) {
            is ThreadLikeUiEvent -> toastShort(text = it.toMessage(context))

            is CommonUiEvent.Toast -> toastShort(text = it.message)

            is SubPostsUiEvent.ScrollToSubPosts -> {
                val index = 2 + it.index // Post + Sticky Header + Subpost index
                if (useStickyHeaderWorkaround) {
                    lazyListState.scrollToItemWithHeader(index) { item ->
                        item.contentType == HeaderContentType
                    }
                } else {
                    lazyListState.animateScrollToItem(index)
                }
            }

            is SubPostsUiEvent.DeletePostFailed -> toastShort(R.string.toast_delete_failure, it.message)

            else -> toastShort(it::class.java.simpleName) // or throw
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
        isEmpty = uiState.subPosts.isEmpty(),
        isLoading = uiState.isRefreshing,
        error = uiState.error,
        onReload = viewModel::onRefresh,
    ) {
        val coroutineScope = rememberCoroutineScope()
        val topAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
        val scrollOrientationConnection = rememberScrollOrientationConnection()

        val onScrollToTopClicked: () -> Unit = {
            coroutineScope.launch { lazyListState.scrollToItem(0) }
            topAppBarScrollBehavior.state.contentOffset = 0f
        }

        // Initialize nullable click listeners:
        val onReplySubPostClickedListener: ((SubPostItemData) -> Unit)? = { item: SubPostItemData ->
            navigator.navigateDebounced(
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
        }.takeIf { canReply && forumId > 0 }

        val onReplyPostClickedListener: ((PostData) -> Unit)? =  { it: PostData ->
            navigator.navigateDebounced(
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
        }.takeIf { canReply && forumId > 0 }

        val onOpenThreadClickedListener: (() -> Unit)? = {
            navigator.navigateDebounced(route = Thread(threadId, forumId, postId = postId))
        }

        // non-nullable, initialize here for convenience
        val onCopyClickedListener: (String) -> Unit = { navigator.navigate(CopyText(it)) }

        BlurScaffold(
            topHazeBlock = {
                blurEnabled = lazyListState.canScrollBackward || topAppBarScrollBehavior.isOverlapping
            },
            topBar = {
                TitleBar(
                    post = uiState.post,
                    onBack = onNavigateUp,
                    onOpenThread = onOpenThreadClickedListener.takeIf { !isSheet },
                    onScrollToTop = onScrollToTopClicked.takeIf { lazyListState.canScrollBackward && canReply },
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
                Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            },
            bottomHazeBlock = { blurEnabled = false },
            floatingActionButton = {
                if (forumName.isNullOrEmpty()) return@BlurScaffold
                val fabVisible by remember {
                    derivedStateOf {
                        if (canReply) {
                            !lazyListState.canScrollBackward || scrollOrientationConnection.isScrollingForward
                        } else {
                            lazyListState.canScrollBackward && scrollOrientationConnection.isScrollingForward
                        }
                    }
                }
                SubpostsFAB(
                    visible = fabVisible,
                    onScrollToTop = onScrollToTopClicked,
                    onReply = {
                        if (uiState.post != null && onReplyPostClickedListener != null) {
                            onReplyPostClickedListener(uiState.post!!)
                        }
                    }.takeIf { canReply }
                )
            }
        ) { padding ->
            val contentPadding = padding.fixedTopBarPadding()

            SwipeUpLazyLoadColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollOrientationConnection)
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
                bottomIndicator = { LoadingIndicator(isLoading = isLoadingMore) }
            ) {
                val postItem = uiState.post ?: return@SwipeUpLazyLoadColumn
                item(key = "Post$postId", contentType = PostContentType) {
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides null) {
                        Column {
                            PostCard(
                                post = postItem,
                                onUserClick = {
                                    navigator.navigateDebounced(UserProfile(postItem.author))
                                },
                                onReplyClick = onReplyPostClickedListener,
                                onMenuCopyClick = onCopyClickedListener,
                                onMenuDeleteClick = viewModel::onDeletePost.takeIf { postItem.author.id == myUid } // Check is my Post
                            )
                            HorizontalDivider(thickness = 2.dp)
                        }
                    }
                }

                if (useStickyHeaderWorkaround) {
                    item(contentType = HeaderContentType) {
                        SubPostsHeader(postNum = uiState.page.postCount)
                    }
                } else {
                    stickyHeader(contentType = HeaderContentType) {
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
                            navigator.navigateDebounced(
                                route = UserProfile(user = it, transitionKey = item.id.toString())
                            )
                        },
                        onAgree = viewModel::onSubPostLikeClicked,
                        onMenuReplyClick = onReplySubPostClickedListener,
                        onMenuCopyClick = onCopyClickedListener,
                        onMenuReportClick = {
                            coroutineScope.launch {
                                TiebaUtil.reportPost(context, navigator, postId = it.id.toString())
                            }
                        },
                        onMenuDeleteClick = viewModel::onDeleteSubPost.takeIf { item.authorId == myUid } // Check is my SubPost
                    )
                }
            }
        }
    }
}

@Composable
private fun TitleBar(
    post: PostData?,
    onBack: () -> Unit,
    onOpenThread: (() -> Unit)? = null,
    onScrollToTop: (() -> Unit)? = null,
    isSheet: Boolean = onOpenThread != null,
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
            ActionItem(
                icon = if (isSheet) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = R.string.btn_close,
                onClick = onBack
            )
        },
        actions = {
            FadedVisibility(visible = onScrollToTop != null) {
                ActionItem(
                    icon = Icons.Rounded.VerticalAlignTop,
                    contentDescription = R.string.btn_back_to_top,
                    onClick = onScrollToTop ?: {}
                )
            }

            if (onOpenThread != null) {
                ActionItem(
                    icon = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = R.string.btn_open_origin_thread,
                    onClick = onOpenThread
                )
            }
        },
        scrollBehavior = scrollBehavior,
        content = content
    )
}

@Composable
private fun SubpostsFAB(
    modifier: Modifier = Modifier,
    visible: Boolean,
    onScrollToTop: () -> Unit,
    onReply: (() -> Unit)?,
) {
    val canReply = onReply != null
    val tip = stringResource(if (canReply) R.string.tip_reply_thread else R.string.btn_back_to_top)
    val icon = if (canReply) Icons.Rounded.Edit else Icons.Rounded.VerticalAlignTop

    PlainTooltipBox(
        modifier = modifier,
        contentDescription = tip,
    ) {
        FloatingActionButton(
            modifier = Modifier.animateFloatingActionButton(visible, alignment = Alignment.Center),
            onClick = if (canReply) onReply else onScrollToTop
        ) {
            Icon(imageVector = icon, contentDescription = tip)
        }
    }
}

@Composable
private fun SubPostItem(
    item: SubPostItemData,
    onUserClick: (UserData) -> Unit = {},
    onAgree: (SubPostItemData) -> Unit = {},
    onMenuReplyClick: ((SubPostItemData) -> Unit)?,
    onMenuCopyClick: ((String) -> Unit)? = null,
    onMenuReportClick: (SubPostItemData) -> Unit = {},
    onMenuDeleteClick: ((SubPostItemData) -> Unit)? = null,
) =
    BlockableContent(
        blocked = item.blocked,
        blockedTip = {
            SubPostBlockedTip(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        },
        hideBlockedContent = false,
    )
{
    val context = LocalContext.current

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
                onMenuReportClick(item)
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
            SharedTransitionUserHeader(
                author = item.author,
                desc = remember { getRelativeTimeString(context, item.time) },
                extraKey = item.id,
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
                        content = { Text(text = stringResource(R.string.button_sure)) }
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
