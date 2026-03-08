package com.huanchengfly.tieba.post.ui.page.thread

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ChromeReaderMode
import androidx.compose.material.icons.automirrored.rounded.ChromeReaderMode
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Face6
import androidx.compose.material.icons.rounded.FaceRetouchingOff
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.VerticalAlignTop
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarExitDirection.Companion.Bottom
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.lerp
import androidx.compose.ui.util.trace
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.LocalUISettings
import com.huanchengfly.tieba.post.MacrobenchmarkConstant
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.collectUiEventWithLifecycle
import com.huanchengfly.tieba.post.arch.isOverlapping
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.components.glide.TbGlideUrl
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.theme.isTranslucent
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.FadedVisibility
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.common.theme.compose.onNotNull
import com.huanchengfly.tieba.post.ui.models.Like
import com.huanchengfly.tieba.post.ui.models.LikeZero
import com.huanchengfly.tieba.post.ui.models.PostData
import com.huanchengfly.tieba.post.ui.models.SimpleForum
import com.huanchengfly.tieba.post.ui.models.UserData
import com.huanchengfly.tieba.post.ui.page.Destination.Forum
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.setResult
import com.huanchengfly.tieba.post.ui.page.threadstore.ThreadStoreUiEvent
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.BlurScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.CenterAlignedTopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.ListMenuItem
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalHazeState
import com.huanchengfly.tieba.post.ui.widgets.compose.PlainTooltipBox
import com.huanchengfly.tieba.post.ui.widgets.compose.PromptDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.ProvideContentColor
import com.huanchengfly.tieba.post.ui.widgets.compose.StickyHeaderOverlay
import com.huanchengfly.tieba.post.ui.widgets.compose.StrongBox
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeToDismissSnackbarHost
import com.huanchengfly.tieba.post.ui.widgets.compose.VerticalGrid
import com.huanchengfly.tieba.post.ui.widgets.compose.collapsedFraction
import com.huanchengfly.tieba.post.ui.widgets.compose.defaultHazeStyle
import com.huanchengfly.tieba.post.ui.widgets.compose.defaultInputScale
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
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val ThreadToolbarContainerHeight = 48.dp

const val ThreadResultKey = "THREAD_PAGE"

private fun createResult(threadId: Long, like: Like?, markedPost: PostData?): ThreadResult? {
    return if (like != null) {
        ThreadResult(threadId, like.liked, like.count, markedPostId = markedPost?.id)
    } else {
        null
    }
}

@Composable
private fun ToggleButton(
    text: String,
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector,
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = if (checked) colorScheme.secondaryContainer else colorScheme.surfaceContainerHigh,
        contentColor = if (checked) colorScheme.onSecondaryContainer else colorScheme.onSurface,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(imageVector = icon, contentDescription = text)
                Text(
                    text = text,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
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

    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val openBottomSheet: () -> Unit = {
        coroutineScope.launch {
            showBottomSheet = true
            bottomSheetState.show()
        }
    }
    val closeBottomSheet: () -> Unit = {
        coroutineScope
            .launch { bottomSheetState.hide() }
            .invokeOnCompletion { showBottomSheet = false }
    }

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

            is ThreadUiEvent.ToReplyDestination -> navigator.navigateDebounced(it.direction)

            is ThreadUiEvent.ToSubPostsDestination -> navigator.navigateDebounced(it.direction)

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

    state.thread?.like?.let { threadLike ->
        LaunchedEffect(state.thread?.like, newMarkedCollectionPost?.id) {
            val rec = createResult(threadId, threadLike, markedPost = newMarkedCollectionPost)
            navigator.setResult(ThreadResultKey, rec)
        }
    }

    val onBackPressedCallback: () -> Unit = {
        if (bottomSheetState.isVisible) {
            closeBottomSheet()
        } else {
            val lastVisiblePost = lazyListState.middleVisiblePost(state)
            // 更新收藏楼层
            val collectMarkPid: Long? = state.thread?.collectMarkPid
            val newCollectMarkPid: Long? = lastVisiblePost?.id
            if (collectMarkPid != null && collectMarkPid != newCollectMarkPid) {
                // Show CollectionsUpdateDialog now
                newMarkedCollectionPost = lastVisiblePost
            } else {
                navigator.navigateUp()
            }
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
            val interceptBack by remember {
                derivedStateOf {
                    bottomSheetState.isVisible || collectMarkPid != lazyListState.middleVisiblePost(state)?.id
                }
            }
            BackHandler(enabled = interceptBack, onBack = onBackPressedCallback)
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
                                navigator.navigateDebounced(route = Forum(forumName = forum.second))
                            }
                        }
                    },
                    navigationIcon = {
                        BackNavigationIcon(onBackPressed = onBackPressedCallback)
                    },
                    actions = {
                        val scrollToTopVisible by remember {
                            derivedStateOf { lazyListState.canScrollBackward }
                        }
                        FadedVisibility(visible = scrollToTopVisible) {
                            ActionItem(
                                icon = Icons.Rounded.VerticalAlignTop,
                                contentDescription = R.string.btn_back_to_top
                            ) {
                                if (lazyListState.canScrollBackward) {
                                    coroutineScope.launch { lazyListState.scrollToItem(0) }
                                    topAppBarScrollBehavior.state.contentOffset = 0f
                                    toolbarScrollBehavior.state.contentOffset = 0f
                                    toolbarScrollBehavior.state.offset = 0f
                                }
                            }
                        }
                    },
                    scrollBehavior = topAppBarScrollBehavior
                ) {
                    if (useStickyHeaderWorkaround && state.thread?.replyNum != null) {
                        Container {
                            StickyHeaderOverlay(state = lazyListState) {
                                ThreadHeader(uiState = state, viewModel = viewModel)
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            },
            bottomHazeBlock = { blurEnabled = false },
            snackbarHostState = snackbarHostState,
            snackbarHost = {
                SwipeToDismissSnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.graphicsLayer { // Offset up based on the collapsed fraction
                        translationY = toolbarScrollBehavior.state.run {
                            lerp(start = offsetLimit, stop = 0f, fraction = collapsedFraction)
                        }
                    }
                )
            },
        ) { padding ->
            val hazeState: HazeState? = LocalHazeState.current

            // ScreenOffset / 2 + Navigation bar
            val toolbarScreenOffset = -FloatingToolbarDefaults.ScreenOffset / 2 - padding.calculateBottomPadding()

            // Ignore Scaffold padding top changes if workaround enabled
            val contentPadding = padding.fixedTopBarPadding()

            Container(modifier = Modifier.clipToBounds()) {
                // The toolbar should receive focus before the screen content, so place it first.
                // Make sure to set its zIndex so it's above the screen content visually.
                ThreadFloatingToolbar(
                    modifier =
                        Modifier.align(Alignment.BottomCenter).offset(y = toolbarScreenOffset).zIndex(1f),
                    user = state.user,
                    onClickReply = viewModel::onReplyThread.takeUnless { viewModel.hideReply },
                    onClickMore =  openBottomSheet,
                    onJumpPage = jumpToPageDialogState::show,
                    like = state.thread?.like ?: LikeZero,
                    onLiked = viewModel::onThreadLikeClicked,
                    scrollBehavior = toolbarScrollBehavior
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

            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false },
                    sheetState = bottomSheetState,
                    containerColor = Color.Transparent, // Set background for blurring
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    scrimColor = Color.Transparent,
                    dragHandle = null,
                    contentWindowInsets = { WindowInsets() } // Handle it inside the content for blurring
                ) {
                    val isMyThread by remember(state.lz) {
                        derivedStateOf { state.user != null && state.lz?.id == state.user?.id }
                    }
                    val isDesc by remember { derivedStateOf { state.sortType == ThreadSortType.BY_DESC } }

                    ThreadMenu(
                        isSeeLz = state.seeLz,
                        isCollected = state.thread?.collected == true,
                        isImmersiveMode = viewModel.isImmersiveMode,
                        isDesc = isDesc,
                        onSeeLzClick = viewModel::onSeeLzChanged,
                        onCollectClick = {
                            if (state.user == null) {
                                context.toastShort(R.string.title_not_logged_in)
                            } else if (state.thread!!.collected) {
                                viewModel.removeFromCollections()
                            } else {
                                lazyListState.middleVisiblePost(state)?.let { post ->
                                    viewModel.updateCollections(markedPost = post)
                                }
                            }
                        },
                        onImmersiveModeClick = {
                            if (!viewModel.isImmersiveMode && !state.seeLz) {
                                viewModel.onSeeLzChanged()
                            }
                            viewModel.onImmersiveModeChanged()
                        },
                        onDescClick = {
                            val notDesc = state.sortType != ThreadSortType.BY_DESC
                            val sortType = if (notDesc) ThreadSortType.BY_DESC else ThreadSortType.DEFAULT
                            viewModel.onSortChanged(sortType)
                        },
                        onShareClick = viewModel::onShareThread,
                        onCopyLinkClick = viewModel::onCopyThreadLink,
                        onReportClick = { viewModel.onReportThread(navigator) },
                        onDeleteClick = viewModel::onDeleteThread.takeIf { isMyThread },
                        requestCloseMenu = closeBottomSheet,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = padding.calculateStartPadding(LocalLayoutDirection.current),
                                end = padding.calculateEndPadding(LocalLayoutDirection.current),
                            )
                            .onNotNull(hazeState) {
                                hazeEffect(state = it, style = defaultHazeStyle())
                            }
                            .background(TiebaLiteTheme.extendedColorScheme.sheetContainerColor)
                            .padding(top = 16.dp)
                            .windowInsetsPadding(BottomSheetDefaults.windowInsets)
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

            val forumStyle = MaterialTheme.typography.titleMedium
            Text(
                text = stringResource(id = R.string.title_forum, forum.second),
                modifier = Modifier.padding(horizontal = 8.dp),
                autoSize = TextAutoSize.StepBased(8.sp, forumStyle.fontSize),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = forumStyle
            )
        }
    }
}

@Composable
private fun ThreadMenu(
    isSeeLz: Boolean,
    isCollected: Boolean,
    isImmersiveMode: Boolean,
    isDesc: Boolean,
    onSeeLzClick: () -> Unit,
    onCollectClick: () -> Unit,
    onImmersiveModeClick: () -> Unit,
    onDescClick: () -> Unit,
    onShareClick: () -> Unit,
    onCopyLinkClick: () -> Unit,
    onReportClick: () -> Unit,
    onDeleteClick: (() -> Unit)?,
    requestCloseMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .height(4.dp)
                .fillMaxWidth(0.2f)
                .background(color = MaterialTheme.colorScheme.onSurfaceVariant, shape = CircleShape)
        )
        VerticalGrid(
            column = 2,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            rowModifier = Modifier.height(IntrinsicSize.Min),
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            item {
                ToggleButton(
                    text = stringResource(id = R.string.title_see_lz),
                    checked = isSeeLz,
                    onClick = {
                        requestCloseMenu()
                        onSeeLzClick()
                    },
                    icon = if (isSeeLz) Icons.Rounded.Face6 else Icons.Rounded.FaceRetouchingOff,
                    modifier = Modifier.fillMaxSize()
                )
            }
            item {
                ToggleButton(
                    text = stringResource(id = if (isCollected) R.string.title_collected else R.string.title_uncollected),
                    checked = isCollected,
                    onClick = {
                        requestCloseMenu()
                        onCollectClick()
                    },
                    icon = if (isCollected) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    modifier = Modifier.fillMaxSize()
                )
            }
            item {
                ToggleButton(
                    text = stringResource(id = R.string.title_pure_read),
                    checked = isImmersiveMode,
                    onClick = {
                        requestCloseMenu()
                        onImmersiveModeClick()
                    },
                    icon = if (isImmersiveMode) Icons.AutoMirrored.Rounded.ChromeReaderMode else Icons.AutoMirrored.Outlined.ChromeReaderMode,
                    modifier = Modifier.fillMaxSize()
                )
            }
            item {
                ToggleButton(
                    text = stringResource(id = R.string.title_sort),
                    checked = isDesc,
                    onClick = {
                        requestCloseMenu()
                        onDescClick()
                    },
                    icon = Icons.AutoMirrored.Rounded.Sort,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Column {
            ListMenuItem(
                icon = Icons.Rounded.Share,
                text = stringResource(id = R.string.title_share),
                onClick = {
                    requestCloseMenu()
                    onShareClick()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            ListMenuItem(
                icon = Icons.Rounded.ContentCopy,
                text = stringResource(id = R.string.title_copy_link),
                onClick = {
                    requestCloseMenu()
                    onCopyLinkClick()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            ListMenuItem(
                icon = Icons.Rounded.Report,
                text = stringResource(id = R.string.title_report),
                onClick = {
                    requestCloseMenu()
                    onReportClick()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            if (onDeleteClick != null) {
                ListMenuItem(
                    icon = Icons.Rounded.Delete,
                    text = stringResource(id = R.string.title_delete),
                    onClick = {
                        requestCloseMenu()
                        onDeleteClick()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
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
private fun ThreadFloatingToolbar(
    modifier: Modifier = Modifier,
    user: UserData? = null,
    onClickReply: (() -> Unit)? = null,
    onClickMore: () -> Unit = {},
    onJumpPage: () -> Unit = {},
    like: Like = LikeZero,
    onLiked: () -> Unit = {},
    scrollBehavior: FloatingToolbarScrollBehavior? = null,
    shadowElevation: Dp = FloatingToolbarDefaults.ContainerExpandedElevationWithFab,
) {
    val colorScheme = MaterialTheme.colorScheme
    // Default: FloatingToolbarTokens.VibrantContainerColor
    val toolbarContainerColor = colorScheme.primaryContainer.let {
        if (!colorScheme.isTranslucent && !LocalUISettings.current.reduceEffect) it.copy(alpha = 0.7f) else it
    }

    ProvideContentColor(colorScheme.onPrimaryContainer) {
        Row(
            modifier = modifier
                .height(ThreadToolbarContainerHeight)
                .padding(horizontal = 16.dp)
                .onNotNull(scrollBehavior) {
                    with(it) { floatingScrollBehavior() }
                }
                .graphicsLayer {
                    this.shadowElevation = shadowElevation.toPx()
                    this.shape = CircleShape
                    this.clip = true
                }
                .onNotNull(LocalHazeState.current) {
                    val hazeInputScale = defaultInputScale()
                    hazeEffect(it, defaultHazeStyle()) { inputScale = hazeInputScale }
                }
                .background(color = toolbarContainerColor, shape = CircleShape)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val avatarContentDescription = user?.name ?: stringResource(R.string.title_not_logged_in)
            PlainTooltipBox(
                positionProvider = rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                contentDescription = avatarContentDescription
            ) {
                Avatar(
                    modifier = Modifier.size(40.dp),
                    data = user?.avatarUrl ?: R.drawable.ic_launcher_new_round,
                    contentDescription = avatarContentDescription
                )
            }

            if (onClickReply != null) {
                Text(
                    text = stringResource(id = R.string.tip_reply_thread),
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .weight(1.0f)
                        .clickableNoIndication(onClick = onClickReply),
                    color = LocalContentColor.current.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            ActionItem(
                icon = Icons.Rounded.RocketLaunch,
                contentDescription = stringResource(R.string.title_jump_page),
                positionProvider = rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                onClick = onJumpPage,
            )

            LikeAction(like = like, onClick = onLiked)

            ActionItem(
                icon = Icons.Rounded.MoreVert,
                contentDescription = stringResource(id = R.string.btn_more),
                positionProvider = rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                onClick = onClickMore
            )
        }
    }
}

@Composable
private fun LikeAction(modifier: Modifier = Modifier, like: Like, onClick: () -> Unit) {
    val contentDescription = stringResource(R.string.button_like)
    PlainTooltipBox(
        modifier = modifier,
        positionProvider = rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        contentDescription = contentDescription,
        hasAction = true,
    ) {
        BadgedBox(
            badge = {
                if (like.count > 0) {
                    Surface(
                        modifier = Modifier.graphicsLayer {
                            translationX = -size.width * if (like.count > 999) 0.45f else 0.3f
                            translationY = size.height * 0.25f
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
        ) {
            val animatedColor by animateColorAsState(
                targetValue = if (like.liked) MaterialTheme.colorScheme.primary else LocalContentColor.current,
            )
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = if (like.liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    modifier = Modifier.size(24.dp),
                    contentDescription = null,
                    tint = animatedColor
                )
            }
        }
    }
}

@Preview("LikeAction")
@Composable
private fun LikeActionPreview() = TiebaLiteTheme {
    LikeAction(like = Like(liked = true, count = 999)) { }
}
