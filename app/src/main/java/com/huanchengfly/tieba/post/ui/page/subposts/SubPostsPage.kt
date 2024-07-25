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
import androidx.compose.material.DropdownMenuItem
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.common.PbContentText
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.threadBottomBar
import com.huanchengfly.tieba.post.ui.models.SubPostItemData
import com.huanchengfly.tieba.post.ui.models.UserData
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.CopyTextDialogPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.huanchengfly.tieba.post.ui.page.reply.ReplyArgs
import com.huanchengfly.tieba.post.ui.page.reply.ReplyDialog
import com.huanchengfly.tieba.post.ui.page.thread.PostCard
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockTip
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.widgets.compose.Card
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.FavoriteButton
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.UserNameText
import com.huanchengfly.tieba.post.ui.widgets.compose.VerticalDivider
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.DateTimeUtils.getRelativeTimeString
import com.huanchengfly.tieba.post.utils.LocalAccount
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.spec.DestinationStyleBottomSheet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Destination
@Composable
fun SubPostsPage(
    navigator: DestinationsNavigator,
    threadId: Long,
    forumId: Long = 0L,
    postId: Long = 0L,
    subPostId: Long = 0L,
    loadFromSubPost: Boolean = false,
    viewModel: SubPostsViewModel = pageViewModel()
) {
    ProvideNavigator(navigator) {
        SubPostsContent(
            viewModel = viewModel,
            forumId = forumId,
            threadId = threadId,
            postId = postId,
            subPostId = subPostId,
            loadFromSubPost = loadFromSubPost,
            onNavigateUp = { navigator.navigateUp() }
        )
    }
}

@Destination(
    style = DestinationStyleBottomSheet::class
)
@Composable
fun SubPostsSheetPage(
    navigator: DestinationsNavigator,
    threadId: Long,
    forumId: Long = 0L,
    postId: Long = 0L,
    subPostId: Long = 0L,
    loadFromSubPost: Boolean = false,
    viewModel: SubPostsViewModel = pageViewModel()
) {
    ProvideNavigator(navigator) {
        SubPostsContent(
            viewModel = viewModel,
            forumId = forumId,
            threadId = threadId,
            postId = postId,
            subPostId = subPostId,
            loadFromSubPost = loadFromSubPost,
            isSheet = true,
            onNavigateUp = { navigator.navigateUp() }
        )
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
    loadFromSubPost: Boolean = false,
    isSheet: Boolean = false,
    onNavigateUp: () -> Unit = {},
) {
    val navigator = LocalNavigator.current
    val account = LocalAccount.current

    LazyLoad(key = viewModel, loaded = viewModel.initialized) {
        viewModel.send(
            SubPostsUiIntent.Load(
                forumId,
                threadId,
                postId,
                subPostId.takeIf { loadFromSubPost } ?: 0L
            )
        )
    }

    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::isRefreshing,
        initial = false
    )
    val isLoading by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::isLoading,
        initial = false
    )
    val anti by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::anti,
        initial = null
    )
    val forum by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::forum,
        initial = null
    )
    val post by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::post,
        initial = null
    )
    val postContentRenders by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::postContentRenders,
        initial = persistentListOf()
    )
    val subPosts by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::subPosts,
        initial = persistentListOf()
    )
    val currentPage by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::currentPage,
        initial = 1
    )
    val totalCount by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::totalCount,
        initial = 0
    )
    val hasMore by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::hasMore,
        initial = true
    )

    val lazyListState = rememberLazyListState()

    viewModel.onEvent<SubPostsUiEvent.ScrollToSubPosts> {
        delay(20)
        lazyListState.scrollToItem(2 + subPosts.indexOfFirst { it.id == subPostId })
    }

    val confirmDeleteDialogState = rememberDialogState()
    var deleteSubPost by remember { mutableStateOf<SubPostItemData?>(null) }
    ConfirmDialog(
        dialogState = confirmDeleteDialogState,
        onConfirm = {
            if (deleteSubPost == null) {
                val isSelfPost = post?.author?.id == account?.uid?.toLongOrNull()
                viewModel.send(
                    SubPostsUiIntent.DeletePost(
                        forumId = forumId,
                        forumName = forum?.get { name }.orEmpty(),
                        threadId = threadId,
                        postId = postId,
                        deleteMyPost = isSelfPost,
                        tbs = anti?.get { tbs },
                    )
                )
            } else {
                val isSelfSubPost = deleteSubPost!!.authorId == account?.uid?.toLongOrNull()
                viewModel.send(
                    SubPostsUiIntent.DeletePost(
                        forumId = forumId,
                        forumName = forum?.get { name }.orEmpty(),
                        threadId = threadId,
                        postId = postId,
                        subPostId = deleteSubPost!!.id,
                        deleteMyPost = isSelfSubPost,
                        tbs = anti?.get { tbs },
                    )
                )
            }
        }
    ) {
        val deleteType = if (deleteSubPost == null && post != null) {
            stringResource(id = R.string.tip_post_floor, post!!.floor) // floor
        } else {
            stringResource (id = R.string.this_reply)    // reply
        }
        Text(text = stringResource(id = R.string.message_confirm_delete, deleteType))
    }

    val replyDialogState = rememberDialogState()
    var currentReplyArgs by remember { mutableStateOf<ReplyArgs?>(null) }
    if (currentReplyArgs != null) {
        ReplyDialog(args = currentReplyArgs!!, state = replyDialogState)
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

    fun showReplyDialog(args: ReplyArgs) {
        currentReplyArgs = args
        replyDialogState.show()
    }

    StateScreen(
        modifier = Modifier.fillMaxSize(),
        isEmpty = subPosts.isEmpty(),
        isError = false,
        isLoading = isRefreshing
    ) {
        MyScaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TitleCentredToolbar(
                    title = {
                        Text(text = post?.let {
                            stringResource(id = R.string.title_sub_posts, it.floor)
                        } ?: stringResource(id = R.string.title_sub_posts_default),
                            fontWeight = FontWeight.Bold, style = MaterialTheme.typography.h6
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateUp) {
                            Icon(
                                imageVector = if (isSheet) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(id = R.string.btn_close)
                            )
                        }
                    },
                    actions = {
                        if (!isSheet) {
                            IconButton(onClick = {
                                navigator.navigate(
                                    ThreadPageDestination(
                                        forumId = forumId,
                                        threadId = threadId,
                                        postId = postId
                                    )
                                )
                            }) {
                                Icon(
                                    imageVector = Icons.Rounded.OpenInBrowser,
                                    contentDescription = stringResource(id = R.string.btn_open_origin_thread)
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (account != null && !LocalContext.current.appPreferences.hideReply) {
                    Column(
                        modifier = Modifier.background(ExtendedTheme.colors.threadBottomBar)
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
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ExtendedTheme.colors.bottomBarSurface)
                                    .clickable {
                                        val fid = forum?.get { id } ?: forumId
                                        val forumName = forum?.get { name }
                                        if (!forumName.isNullOrEmpty()) {
                                            showReplyDialog(
                                                ReplyArgs(
                                                    forumId = fid,
                                                    forumName = forumName,
                                                    threadId = threadId,
                                                    postId = postId,
                                                )
                                            )
                                        }
                                    }
                                    .padding(8.dp),
                            ) {
                                Text(
                                    text = stringResource(id = R.string.tip_reply_thread),
                                    style = MaterialTheme.typography.caption,
                                    color = ExtendedTheme.colors.onBottomBarSurface,
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .requiredHeightIn(min = if (LocalContext.current.appPreferences.liftUpBottomBar) 16.dp else 0.dp)
                        ) {
                            Spacer(
                                modifier = Modifier
                                    .windowInsetsBottomHeight(WindowInsets.navigationBars)
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            SwipeUpLazyLoadColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = paddingValues,
                isLoading = isLoading,
                onLazyLoad = {
                    if (!hasMore || post == null || subPosts.isEmpty()) return@SwipeUpLazyLoadColumn
                    viewModel.send(
                        SubPostsUiIntent.LoadMore(forumId, threadId, postId, page = currentPage + 1)
                    )
                },
                onLoad = {
                    viewModel.send(
                        SubPostsUiIntent.LoadMore(forumId, threadId, postId, page = currentPage + 1)
                    )
                },
                bottomIndicator = { onThreshold ->
                    LoadMoreIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        isLoading = isLoading,
                        noMore = !hasMore,
                        onThreshold = onThreshold
                    )
                }
            ) {
                val postItem = post ?: return@SwipeUpLazyLoadColumn
                item(key = "Post$postId") {
                    Column {
                        PostCard(
                            post = postItem,
                            contentRenders = postContentRenders,
                            canDelete = postItem.author.id == account?.uid?.toLongOrNull(),
                            isCollected = false,
                            onUserClick = {
                                navigator.navigate(UserProfilePageDestination(postItem.author.id))
                            },
                            onAgree = {
                                val hasAgreed = postItem.hasAgree != 0
                                viewModel.send(
                                    SubPostsUiIntent.Agree(forumId, threadId, postId, agree = !hasAgreed)
                                )
                            },
                            onReplyClick = {
                                showReplyDialog(
                                    ReplyArgs(
                                        forumId = forumId,
                                        forumName = forum?.get { name } ?: "",
                                        threadId = threadId,
                                        postId = postId,
                                        replyUserId = it.author.id,
                                        replyUserName = it.author.nameShow.takeIf { name -> name.isNotEmpty() }?: it.author.name,
                                        replyUserPortrait = it.author.portrait
                                    )
                                )
                            },
                            onMenuCopyClick = {
                                navigator.navigate(CopyTextDialogPageDestination(it))
                            },
                        ) {
                            deleteSubPost = null
                            confirmDeleteDialogState.show()
                        }
                        VerticalDivider(thickness = 2.dp)
                    }
                } // End of post card

                stickyHeader(key = "SubPostsHeader") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ExtendedTheme.colors.background)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.title_sub_posts_header, totalCount),
                            style = MaterialTheme.typography.subtitle1
                        )
                    }
                }

                items(items = subPosts, key = { subPost -> subPost.id }) { item ->
                    SubPostItem(
                        item = item,
                        canDelete = item.authorId == account?.uid?.toLongOrNull(),
                        onUserClick = {
                            navigator.navigate(UserProfilePageDestination(it.id))
                        },
                        onAgree = {
                            viewModel.send(
                                SubPostsUiIntent.Agree(forumId, threadId, postId, subPostId = it.id, agree = !it.hasAgree)
                            )
                        },
                        onReplyClick = {
                            showReplyDialog(
                                ReplyArgs(
                                    forumId = forumId,
                                    forumName = forum?.get { name } ?: "",
                                    threadId = threadId,
                                    postId = postId,
                                    subPostId = it.id,
                                    replyUserId = it.author.id,
                                    replyUserName = it.author.nameShow.takeIf { name -> name.isNotEmpty() }
                                        ?: it.author.name,
                                    replyUserPortrait = it.author.portrait,
                                )
                            )
                        },
                        onMenuCopyClick = {
                            navigator.navigate(CopyTextDialogPageDestination(it))
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
private fun SubPostItem(
    item: SubPostItemData,
    canDelete: Boolean,
    onUserClick: (UserData) -> Unit = {},
    onAgree: (SubPostItemData) -> Unit = {},
    onReplyClick: (SubPostItemData) -> Unit = {},
    onMenuCopyClick: ((String) -> Unit)? = null,
    onMenuDeleteClick: ((SubPostItemData) -> Unit)? = null,
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val coroutineScope = rememberCoroutineScope()
    val menuState = rememberMenuState()
    BlockableContent(
        blocked = item.blocked,
        blockedTip = { BlockTip(text = { Text(text = stringResource(id = R.string.tip_blocked_sub_post)) }) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        LongClickMenu(
            menuState = menuState,
            indication = null,
            menuContent = {
                if (!context.appPreferences.hideReply) {
                    DropdownMenuItem(
                        onClick = {
                            onReplyClick(item)
                            menuState.expanded = false
                        }
                    ) {
                        Text(text = stringResource(id = R.string.btn_reply))
                    }
                }
                if (onMenuCopyClick != null) {
                    DropdownMenuItem(
                        onClick = {
                            onMenuCopyClick(item.plainText)
                            menuState.expanded = false
                        }
                    ) {
                        Text(text = stringResource(id = R.string.menu_copy))
                    }
                }
                DropdownMenuItem(
                    onClick = {
                        coroutineScope.launch {
                            TiebaUtil.reportPost(context, navigator, item.id.toString())
                        }
                        menuState.expanded = false
                    }
                ) {
                    Text(text = stringResource(id = R.string.title_report))
                }
                if (canDelete && onMenuDeleteClick != null) {
                    DropdownMenuItem(
                        onClick = {
                            onMenuDeleteClick(item)
                            menuState.expanded = false
                        }
                    ) {
                        Text(text = stringResource(id = R.string.title_delete))
                    }
                }
            },
            onClick = { onReplyClick(item) }.takeUnless { context.appPreferences.hideReply }
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
                        fontSize = 13.sp,
                        emoticonSize = 0.9f,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 4,
                        lineSpacing = 0.4.sp
                    )
                }
            )
        }
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
