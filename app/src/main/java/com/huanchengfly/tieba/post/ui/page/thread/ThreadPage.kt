package com.huanchengfly.tieba.post.ui.page.thread

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ChromeReaderMode
import androidx.compose.material.icons.automirrored.rounded.ChromeReaderMode
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Face6
import androidx.compose.material.icons.rounded.FaceRetouchingOff
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.VerticalAlignTop
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarExitDirection.Companion.Bottom
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.trace
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.MacrobenchmarkConstant
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.collectUiEventWithLifecycle
import com.huanchengfly.tieba.post.arch.isOverlapping
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.components.glide.TbGlideUrl
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.FadedVisibility
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.models.Like
import com.huanchengfly.tieba.post.ui.models.LikeZero
import com.huanchengfly.tieba.post.ui.models.PostData
import com.huanchengfly.tieba.post.ui.models.SimpleForum
import com.huanchengfly.tieba.post.ui.page.Destination.Forum
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.setResult
import com.huanchengfly.tieba.post.ui.page.threadstore.ThreadStoreUiEvent
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.BlurScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.CenterAlignedTopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalHazeState
import com.huanchengfly.tieba.post.ui.widgets.compose.MoreMenuItem
import com.huanchengfly.tieba.post.ui.widgets.compose.PlainTooltipBox
import com.huanchengfly.tieba.post.ui.widgets.compose.PromptDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.StickyHeaderOverlay
import com.huanchengfly.tieba.post.ui.widgets.compose.StrongBox
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeToDismissSnackbarHost
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.AnyPopDialogProperties
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.DirectionState
import com.huanchengfly.tieba.post.ui.widgets.compose.fixedTopBarPadding
import com.huanchengfly.tieba.post.ui.widgets.compose.hazeSource
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.useStickyHeaderWorkaround
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

const val ThreadResultKey = "THREAD_PAGE"

private fun createResult(threadId: Long, like: Like?, markedPost: PostData?): ThreadResult? {
    return if (like != null) {
        ThreadResult(threadId, like.liked, like.count, markedPostId = markedPost?.id)
    } else {
        null
    }
}

private fun LazyListState.middleVisiblePost(uiState: ThreadUiState): PostData? = layoutInfo.run {
    var postItem = visibleItemsInfo.getOrNull(visibleItemsInfo.size / 2)
    if (postItem == null || postItem.contentType !== Type.Post) {
        // Not found, search last visible post
        postItem = visibleItemsInfo.lastOrNull { it.contentType === Type.Post } ?: return uiState.firstPost
    }
    // item key is Post ID
    val postId = postItem.key as Long
    return uiState.data.fastFirstOrNull { p -> p.id == postId } ?: uiState.firstPost
}

@Composable
fun ThreadPage(
    threadId: Long,
    postId: Long = 0,
    extra: ThreadFrom? = null,
    navigator: NavController,
    viewModel: ThreadViewModel,
) = trace(MacrobenchmarkConstant.TRACE_THREAD) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = rememberSnackbarHostState()
    val useStickyHeaderWorkaround = useStickyHeaderWorkaround()

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isEmpty by remember {
        derivedStateOf { state.data.isEmpty() && state.firstPost == null }
    }

    val lazyListState = rememberLazyListState()
    val topAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val toolbarScrollBehavior = FloatingToolbarDefaults.exitAlwaysScrollBehavior(exitDirection = Bottom)

    viewModel.uiEvent.collectUiEventWithLifecycle {
        val message = when (it) {
            is CommonUiEvent.Toast -> it.message.toString()

            is CommonUiEvent.NavigateUp -> navigator.navigateUp()

            is ThreadUiEvent.DeletePostFailed -> getString(R.string.toast_delete_failure, it.message)

            is ThreadUiEvent.DeletePostSuccess -> getString(R.string.toast_delete_success)

            is ThreadUiEvent.ScrollToFirstReply -> lazyListState.scrollToItem(1)

            is ThreadUiEvent.ScrollToLatestReply -> {
                if (state.sortType != ThreadSortType.BY_DESC) {
                    lazyListState.animateScrollToItem(2 + state.data.size)
                } else {
                    lazyListState.animateScrollToItem(1)
                }
            }

            // Workaround for broken scroll position preservation
            is ThreadUiEvent.LoadPreviousSuccess -> {
                val nonDataItems = if (state.pageData.hasPrevious) 3 else 2 // FirstPost + StickyHeader + PreviousButton
                lazyListState.scrollToItem(nonDataItems + it.previousIndex, it.offset)
            }

            is ThreadUiEvent.LoadSuccess -> {
                if (it.postId != 0L || it.page > 1) {
                    lazyListState.animateScrollToItem(1)
                } else {
                    // Scroll to bottom when sorting by DESC
                    val index = if (state.sortType != ThreadSortType.BY_DESC) 1 else 2 + state.data.size
                    lazyListState.animateScrollToItem(index)
                }
            }

            is ThreadUiEvent.ToReplyDestination -> navigator.navigate(it.direction)

            is ThreadUiEvent.ToSubPostsDestination -> navigator.navigate(it.direction)

            is ThreadLikeUiEvent -> it.toMessage(context)

            is ThreadStoreUiEvent -> it.toMessage(context)

            else -> Unit
        }
        if (message is String) {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    onGlobalEvent<GlobalEvent.ReplySuccess>(filter = { it.threadId == threadId }) { event ->
        viewModel.requestLoadMyLatestReply(event.newPostId)
    }

    if (extra != null && extra is ThreadFrom.Store && extra.maxPid != postId) {
        CollectionsUpdatedSnack(snackbarHostState, extra) {
            viewModel.requestLoad(page = 0, postId = extra.maxPid)
        }
    }

    var newMarkedCollectionPost: PostData? by remember { mutableStateOf(null) }

    newMarkedCollectionPost?.let {
        CollectionsUpdateDialog(
            markedPost = it,
            onUpdate = viewModel::updateCollections,
            onBack = navigator::navigateUp
        )
    }

    val markedDeletionPost: PostData? by viewModel.deletePost.collectAsStateWithLifecycle()
    ThreadOrPostDeleteDialog(
        deletePost = markedDeletionPost,
        firstPost = state.firstPost,
        onConfirm = viewModel::onDeleteConfirmed,
        onCancel = viewModel::onDeleteCancelled
    )

    val jumpToPageDialogState = rememberDialogState()
    PromptDialog(
        onConfirm = {
            viewModel.requestLoad(it.toInt())
        },
        dialogState = jumpToPageDialogState,
        keyboardType = KeyboardType.Number,
        isError = {
            it.isEmpty() || (it.toIntOrNull() ?: -1) !in 1..state.pageData.total
        },
        title = { Text(text = stringResource(id = R.string.title_jump_page)) },
        content = {
            with(state.pageData) {
                Text(text = stringResource(R.string.tip_jump_page, current, total))
            }
        }
    )

    val onRefreshClicked: () -> Unit = {
        viewModel.requestLoad(0, postId)
    }
    val onScrollToTopClicked: () -> Unit = {
        if (lazyListState.canScrollBackward) {
            coroutineScope.launch { lazyListState.scrollToItem(0) }
            topAppBarScrollBehavior.state.contentOffset = 0f
        }
    }

    state.thread?.like?.let { threadLike ->
        LaunchedEffect(state.thread?.like, newMarkedCollectionPost?.id) {
            val rec = createResult(threadId, threadLike, markedPost = newMarkedCollectionPost)
            navigator.setResult(ThreadResultKey, rec)
        }
    }

    val onBackPressedCallback: () -> Unit = {
        val lastVisiblePost = lazyListState.middleVisiblePost(state)
        // 更新收藏楼层
        val collectMarkPid: Long? = viewModel.info?.collectMarkPid
        val newCollectMarkPid: Long? = lastVisiblePost?.id
        if (collectMarkPid != null && collectMarkPid != newCollectMarkPid) {
            // Show CollectionsUpdateDialog now
            newMarkedCollectionPost = lastVisiblePost
        } else {
            navigator.navigateUp()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val lastVisiblePost = lazyListState.middleVisiblePost(state)
            viewModel.onSaveHistory(lastVisiblePost)
        }
    }

    state.thread?.collectMarkPid?.let { collectMarkPid ->
        StrongBox {
            val showBackDialog by remember {
                derivedStateOf { collectMarkPid != lazyListState.middleVisiblePost(state)?.id }
            }
            BackHandler(enabled = showBackDialog, onBack = onBackPressedCallback)
        }
    }

    StateScreen(
        isEmpty =  isEmpty,
        isLoading = state.isRefreshing,
        error = state.error,
        onReload = onRefreshClicked,
    ) {
        BlurScaffold(
            topHazeBlock = {
                blurEnabled = lazyListState.canScrollBackward || topAppBarScrollBehavior.isOverlapping
            },
            attachHazeContentState = false, // Attach manually since we're blurring the BottomSheet
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        state.forum?.let { forum ->
                            ForumTitleChip(forum = forum) {
                                navigator.navigate(route = Forum(forumName = forum.second))
                            }
                        }
                    },
                    navigationIcon = {
                        BackNavigationIcon(onBackPressed = onBackPressedCallback)
                    },
                    actions = {
                        val scrollToTopVisible by remember {
                            derivedStateOf { lazyListState.canScrollBackward && state.user != null && !viewModel.hideReply }
                        }
                        FadedVisibility(visible = scrollToTopVisible) {
                            ActionItem(
                                icon = Icons.Rounded.VerticalAlignTop,
                                contentDescription = R.string.btn_back_to_top,
                                onClick = onScrollToTopClicked
                            )
                        }

                        ClickMenu(
                            menuContent = {
                                if (!state.isRefreshing) {
                                    TextMenuItem(text = R.string.btn_refresh, onClick = onRefreshClicked)
                                }
                                TextMenuItem(
                                    text = R.string.title_share,
                                    onClick = viewModel::onShareThread
                                )

                                TextMenuItem(
                                    text = R.string.title_copy_link,
                                    onClick = viewModel::onCopyThreadLink
                                )

                                TextMenuItem(text = R.string.title_report) {
                                    viewModel.onReportThread(navigator)
                                }

                                if (state.user != null && state.lz?.id == state.user?.id) {
                                    TextMenuItem(text = R.string.title_delete, onClick = viewModel::onDeleteThread)
                                }
                            },
                            triggerShape = CircleShape,
                            content = MoreMenuItem,
                        )
                    },
                    scrollBehavior = topAppBarScrollBehavior
                ) {
                    val replyNum = state.thread?.replyNum
                    if (useStickyHeaderWorkaround && replyNum != null) {
                        Container {
                            StickyHeaderOverlay(state = lazyListState) {
                                ThreadHeader(uiState = state, viewModel = viewModel)
                            }
                        }
                    }
                }
            },
            bottomHazeBlock = { blurEnabled = lazyListState.canScrollForward },
            snackbarHostState = snackbarHostState,
            snackbarHost = { SwipeToDismissSnackbarHost(snackbarHostState) },
        ) { padding ->
            val hazeState: HazeState? = LocalHazeState.current

            // Ignore Scaffold padding top changes if workaround enabled
            val contentPadding = padding.fixedTopBarPadding()

            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalFloatingToolbar(
                    expanded = true,
                    floatingActionButton = {
                        val reply = state.user != null && !viewModel.hideReply
                        val tip = stringResource(if (reply) R.string.tip_reply_thread else R.string.btn_back_to_top)
                        val icon = if (reply) Icons.Rounded.Edit else Icons.Rounded.VerticalAlignTop
                        PlainTooltipBox(
                            contentDescription = tip,
                        ) {
                            FloatingToolbarDefaults.VibrantFloatingActionButton(
                                onClick = if (reply) viewModel::onReplyThread else onScrollToTopClicked
                            ) {
                                Icon(imageVector = icon, contentDescription = tip)
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = contentPadding.calculateBottomPadding())
                        .offset(y = -FloatingToolbarDefaults.ScreenOffset / 2)
                        .zIndex(1f),
                    colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                    scrollBehavior = toolbarScrollBehavior,
                    content = {
                        ActionItem(
                            icon = Icons.Rounded.RocketLaunch,
                            contentDescription = R.string.title_jump_page,
                            onClick = jumpToPageDialogState::show,
                        )

                        ActionItem(
                            icon = if (state.seeLz) Icons.Rounded.Face6 else Icons.Rounded.FaceRetouchingOff,
                            contentDescription = R.string.title_see_lz,
                            activated = state.seeLz,
                            onClick = viewModel::onSeeLzChanged
                        )

                        if (state.user != null) {
                            val isCollected = viewModel.info?.collected == true
                            ActionItem(
                                icon = if (isCollected) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                contentDescription = if (isCollected) R.string.title_collected else R.string.title_uncollected,
                                activated = isCollected,
                            ) {
                                if (viewModel.info!!.collected) {
                                    viewModel.removeFromCollections()
                                } else {
                                    lazyListState.middleVisiblePost(state)?.let { post ->
                                        viewModel.updateCollections(markedPost = post)
                                    }
                                }
                            }
                        }

                        ActionItem(
                            icon = if (viewModel.isImmersiveMode) {
                                Icons.AutoMirrored.Rounded.ChromeReaderMode
                            } else {
                                Icons.AutoMirrored.Outlined.ChromeReaderMode
                            },
                            contentDescription = R.string.title_pure_read,
                            activated = viewModel.isImmersiveMode,
                            onClick = viewModel::onImmersiveModeChanged,
                        )

                        LikeAction(
                            like = viewModel.info?.like ?: LikeZero,
                            onClick = viewModel::onThreadLikeClicked
                        )
                    },
                )

                ProvideNavigator(navigator = navigator) {
                    ThreadContent(
                        modifier = Modifier
                            .hazeSource(hazeState)
                            .nestedScroll(toolbarScrollBehavior),
                        viewModel = viewModel,
                        lazyListState = lazyListState,
                        contentPadding = contentPadding,
                        topAppBarScrollBehavior = topAppBarScrollBehavior,
                        useStickyHeader = !useStickyHeaderWorkaround
                    )
                }
            }
        }
    }
}

@Composable
private fun ForumTitleChip(forum: SimpleForum, onForumClick: () -> Unit) {
    Surface(
        onClick = onForumClick,
        modifier = Modifier
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = forum.second
            },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier
                .height(intrinsicSize = IntrinsicSize.Min)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(
                data = forum.third?.let { TbGlideUrl(url = it) },
                contentDescription = null,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
            )

            Text(
                text = stringResource(id = R.string.title_forum, forum.second),
                modifier = Modifier.padding(horizontal = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun CollectionsUpdatedSnack(
    snackbarHostState: SnackbarHostState,
    extra: ThreadFrom.Store,
    onLoadLatest: () -> Unit
) {
    var showed by rememberSaveable { mutableStateOf(false) }
    if (showed) return // Display only once

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val result = snackbarHostState.showSnackbar(
            context.getString(R.string.message_store_thread_update, extra.maxFloor),
            context.getString(R.string.button_load_new),
            true,
            SnackbarDuration.Long
        )
        if (result == SnackbarResult.ActionPerformed) {
            onLoadLatest()
        }
        showed = true
    }
}

@Composable
private fun CollectionsUpdateDialog(markedPost: PostData, onUpdate: (PostData) -> Unit, onBack: () -> Unit) {
    val updateCollectMarkDialogState = rememberDialogState()
    LaunchedEffect(markedPost) {
        updateCollectMarkDialogState.show()
    }

    if (!updateCollectMarkDialogState.show) return
    ConfirmDialog(
        dialogState = updateCollectMarkDialogState,
        onConfirm = {
            onUpdate(markedPost)
        },
        onDismiss = onBack,
    ) {
        Text(stringResource(R.string.message_update_collect_mark, markedPost.floor))
    }
}

@Composable
private fun ThreadOrPostDeleteDialog(
    deletePost: PostData?,
    firstPost: PostData?,
    onConfirm: () -> Job,
    onCancel: () -> Unit
) {
    val dialogState = rememberDialogState()
    LaunchedEffect(deletePost) {
        if (deletePost != null) dialogState.show()
    }

    if (!dialogState.show || firstPost == null) return

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
        if (deletePost == null || deleting) {
            Text(text = stringResource(id = R.string.dialog_content_wait))
        } else {
            val postType = if (deletePost.id == firstPost.id) {
                stringResource(id = R.string.this_thread)
            } else {
                stringResource(R.string.tip_post_floor, deletePost.floor)
            }
            Text(text = stringResource(id = R.string.message_confirm_delete, postType))
        }
    }
}

@Composable
private fun LikeAction(modifier: Modifier = Modifier, like: Like, onClick: () -> Unit) {
    val contentDescription = stringResource(R.string.button_like)
    PlainTooltipBox(
        modifier = modifier.clickableNoIndication(onClick = onClick),
        contentDescription = contentDescription,
        hasAction = true,
    ) {
        BadgedBox(
            badge = {
                if (like.count > 0) {
                    Surface(
                        modifier = Modifier.graphicsLayer {
                            if (like.count > 999) {
                                translationX = -size.width * 0.15f
                            }
                            translationY = -size.height * 0.2f
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiary,
                    ) {
                        Text(
                            text = remember(like.count) { like.count.getShortNumString() },
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            autoSize = TextAutoSize.StepBased(4.sp, 9.sp),
                            lineHeight = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            },
            modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
            val animatedColor by animateColorAsState(
                targetValue = if (like.liked) MaterialTheme.colorScheme.primary else LocalContentColor.current,
            )
            Icon(
                imageVector = if (like.liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                modifier = Modifier.size(24.dp),
                contentDescription = null,
                tint = animatedColor
            )
        }
    }
}

@Preview("LikeAction")
@Composable
private fun LikeActionPreview() = TiebaLiteTheme {
    LikeAction(like = Like(liked = true, count = 999)) { }
}
