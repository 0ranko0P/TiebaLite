package com.huanchengfly.tieba.post.ui.page.thread

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ChromeReaderMode
import androidx.compose.material.icons.automirrored.rounded.ChromeReaderMode
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Face6
import androidx.compose.material.icons.rounded.FaceRetouchingOff
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.SimpleForum
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.copy
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.theme.compose.onNotNull
import com.huanchengfly.tieba.post.ui.models.Like
import com.huanchengfly.tieba.post.ui.models.LikeZero
import com.huanchengfly.tieba.post.ui.models.PostData
import com.huanchengfly.tieba.post.ui.models.ThreadUiState
import com.huanchengfly.tieba.post.ui.models.UserData
import com.huanchengfly.tieba.post.ui.page.Destination.Forum
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.BlurScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.CenterAlignedTopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultInputScale
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.FavoriteButton
import com.huanchengfly.tieba.post.ui.widgets.compose.LiftUpSpacer
import com.huanchengfly.tieba.post.ui.widgets.compose.ListMenuItem
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalHazeState
import com.huanchengfly.tieba.post.ui.widgets.compose.PromptDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.StickyHeaderOverlay
import com.huanchengfly.tieba.post.ui.widgets.compose.VerticalGrid
import com.huanchengfly.tieba.post.ui.widgets.compose.defaultHazeStyle
import com.huanchengfly.tieba.post.ui.widgets.compose.hazeSource
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.launch

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
        color = if (checked) colorScheme.secondaryContainer else colorScheme.surfaceContainer,
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

private fun LazyListState.lastVisiblePost(uiState: ThreadUiState): PostData? {
    val lastPostItem = layoutInfo.visibleItemsInfo.lastOrNull { item ->
        item.key is String && (item.key as String).startsWith(ITEM_POST_KEY_PREFIX)
    }?: return uiState.firstPost

    val lastPostItemKey = lastPostItem.key as String
    return uiState.data
        .fastFirstOrNull { post -> lastPostItemKey.endsWith(post.id.toString()) }
        ?: uiState.firstPost
}

@OptIn(ExperimentalHazeApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ThreadPage(
    threadId: Long,
    postId: Long = 0,
    extra: ThreadFrom? = null,
    scrollToReply: Boolean = false,
    navigator: NavController,
    viewModel: ThreadViewModel = hiltViewModel(),
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = rememberSnackbarHostState()

    val state by viewModel.threadUiState.collectAsStateWithLifecycle()
    val isRefreshing by remember { derivedStateOf { state.isRefreshing } }
    val isError by remember {derivedStateOf { state.error != null } }
    val isEmpty by remember {
        derivedStateOf { state.data.isEmpty() && state.firstPost == null }
    }

    var waitLoadSuccessAndScrollToFirstReply by remember { mutableStateOf(scrollToReply) }

    val lazyListState = rememberLazyListState()
    val topAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val openBottomSheet = {
        coroutineScope.launch {
            showBottomSheet = true
            bottomSheetState.show()
        }
    }
    val closeBottomSheet = {
        coroutineScope
            .launch { bottomSheetState.hide() }
            .invokeOnCompletion { showBottomSheet = false }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect {
            val unhandledEvent = it?: return@collect
            when(unhandledEvent) {
                is CommonUiEvent.Toast -> {
                    snackbarHostState.showSnackbar(unhandledEvent.message.toString())
                }

                is ThreadUiEvent.ScrollToFirstReply -> lazyListState.animateScrollToItem(1)

                is ThreadUiEvent.ScrollToLatestReply -> {
                    if (state.sortType != ThreadSortType.BY_DESC) {
                        lazyListState.animateScrollToItem(2 + state.data.size)
                    } else {
                        lazyListState.animateScrollToItem(1)
                    }
                }

                is ThreadUiEvent.LoadSuccess -> {
                    if (unhandledEvent.page > 1 || waitLoadSuccessAndScrollToFirstReply) {
                        waitLoadSuccessAndScrollToFirstReply = false
                        lazyListState.animateScrollToItem(1)
                    }
                }

                is ThreadUiEvent.ToReplyDestination -> navigator.navigate(unhandledEvent.direction)

                is ThreadUiEvent.ToSubPostsDestination -> navigator.navigate(unhandledEvent.direction)
            }

            viewModel.onUiEventReceived()
        }
    }

    onGlobalEvent<GlobalEvent.ReplySuccess>(filter = { it.threadId == threadId }) { event ->
        viewModel.requestLoadMyLatestReply(event.newPostId)
    }

    val updateCollectMarkDialogState = rememberDialogState()

    ConfirmDialog(
        dialogState = updateCollectMarkDialogState,
        onConfirm = {
            lazyListState.lastVisiblePost(state)?.let { post ->
                if (post.id != 0L) viewModel.requestAddFavorite(post)
            }
            navigator.navigateUp()
        },
        onCancel = { navigator.navigateUp() }
    ) {
        val lastVisibleFloor = lazyListState.lastVisiblePost(state)?.floor ?: 0
        Text(stringResource(R.string.message_update_collect_mark, lastVisibleFloor))
    }

    val confirmDeleteState = rememberDialogState()
    ConfirmDialog(dialogState = confirmDeleteState, onConfirm = viewModel::onDeleteConfirmed) {
        val deletePost = viewModel.deletePost ?: return@ConfirmDialog
        val deleteType = if (deletePost != state.firstPost) {
            stringResource(R.string.tip_post_floor, deletePost.floor) // post
        } else {
            stringResource(id = R.string.this_thread) // thread
        }
        Text(text = stringResource(id = R.string.message_confirm_delete, deleteType))
    }

    LaunchedEffect(viewModel.deletePost) {
        if (viewModel.deletePost == null) return@LaunchedEffect
        confirmDeleteState.show()
    }

    val jumpToPageDialogState = rememberDialogState()
    PromptDialog(
        onConfirm = {
            viewModel.requestLoad(it.toInt(), postId = 0L)
        },
        dialogState = jumpToPageDialogState,
        onValueChange = { newVal, _ -> "^[0-9]*$".toRegex().matches(newVal) },
        title = { Text(text = stringResource(id = R.string.title_jump_page)) },
        content = {
            with(state) {
                Text(text = stringResource(R.string.tip_jump_page, currentPageMax, totalPage))
            }
        }
    )

    LaunchedEffect(Unit) {
        if (extra is ThreadFrom.Store && extra.maxPid != postId) {
            val result = snackbarHostState.showSnackbar(
                context.getString(R.string.message_store_thread_update, extra.maxFloor),
                context.getString(R.string.button_load_new),
                true,
                SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.requestLoad(page = 0, postId = extra.maxPid)
            }
        }
    }

    BackHandler(enabled = true) {
        if (bottomSheetState.isVisible) { // Close bottom sheet now
            closeBottomSheet(); return@BackHandler
        }

        val lastVisiblePost = lazyListState.lastVisiblePost(state)?.apply {
            if (id == 0L) return@apply
            viewModel.onLastPostVisibilityChanged(pid = id, floor = floor)
        }

        if (viewModel.info?.collected == true && lastVisiblePost?.floor != 0) {
            updateCollectMarkDialogState.show()
        } else {
            navigator.navigateUp()
        }
    }

    StateScreen(
        modifier = Modifier.fillMaxSize(),
        isEmpty =  isEmpty,
        isError = isError,
        isLoading = isRefreshing,
        errorScreen = {
            ErrorScreen(error = viewModel.error, modifier = Modifier.safeContentPadding())
        },
        onReload = { viewModel.requestLoad(0, postId) }
    ) {

        // Workaround to make StickyHeader respect content padding
        var useStickyHeaderWorkaround by remember { mutableStateOf(false) }

        BlurScaffold(
            topHazeBlock = {
                blurEnabled = lazyListState.canScrollBackward
                inputScale = DefaultInputScale
            },
            attachHazeContentState = false, // Attach manually since we're blurring the BottomSheet
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        val forum = state.forum ?: return@CenterAlignedTopAppBar // Initializing
                        ForumTitleChip(forumItem = forum) {
                            navigator.navigate(route = Forum(forum.item.name))
                        }
                    },
                    navigationIcon = { BackNavigationIcon(navigator::navigateUp) },
                    scrollBehavior = topAppBarScrollBehavior
                ) {
                    if (useStickyHeaderWorkaround) {
                        Container {
                            StickyHeaderOverlay(state = lazyListState) {
                                ThreadHeader(viewModel = viewModel)
                            }
                        }
                    }
                }
            },
            bottomBar = {
                BottomBar(
                    user = state.user,
                    onClickReply = viewModel::onReplyThread.takeUnless { viewModel.hideReply },
                    onClickMore = {
                        if (bottomSheetState.isVisible) {
                            closeBottomSheet()
                        } else {
                            openBottomSheet()
                        }
                    },
                    like = viewModel.info?.like ?: LikeZero,
                    onLiked = viewModel::onThreadLikeClicked
                )
            },
            bottomHazeBlock = {
                inputScale = DefaultInputScale
            },
            snackbarHostState = snackbarHostState
        ) { padding ->
            val hazeState: HazeState? = LocalHazeState.current

            useStickyHeaderWorkaround = padding.calculateTopPadding() != Dp.Hairline

            // Ignore Scaffold padding changes if workaround enabled
            val direction = LocalLayoutDirection.current
            val contentPadding = if (useStickyHeaderWorkaround) remember { padding.copy(direction) } else padding

            val enablePullRefresh by remember {
                derivedStateOf { state.hasPrevious || state.sortType == ThreadSortType.BY_DESC }
            }

            ProvideNavigator(navigator = navigator) {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::requestLoadFirstPage,
                    modifier = Modifier.hazeSource(hazeState),
                    enabled = enablePullRefresh,
                    contentPadding = contentPadding
                ) {
                    ThreadContent(
                        modifier = Modifier.nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
                        viewModel = viewModel,
                        lazyListState = lazyListState,
                        contentPadding = contentPadding,
                        useStickyHeader = !useStickyHeaderWorkaround
                    )
                }
            }

            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false },
                    sheetState = bottomSheetState,
                    shape = MaterialTheme.shapes.large,
                    containerColor = Color.Transparent, // Set background for blurring
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    scrimColor = Color.Transparent,
                    dragHandle = null
                ) {
                    val localUid by remember { derivedStateOf { state.user?.id } }
                    val isDesc by remember { derivedStateOf { state.sortType == ThreadSortType.BY_DESC } }

                    ThreadMenu(
                        isSeeLz = viewModel.seeLz,
                        isCollected = viewModel.info!!.collected,
                        isImmersiveMode = viewModel.isImmersiveMode,
                        isDesc = isDesc,
                        onSeeLzClick = {
                            viewModel.requestLoadFirstPage(seeLz = !viewModel.seeLz)
                            closeBottomSheet()
                        },
                        onCollectClick = {
                            if (localUid == null) {
                                context.toastShort(R.string.title_not_logged_in)
                            } else if (viewModel.info!!.collected) {
                                viewModel.requestRemoveFavorite()
                            } else {
                                lazyListState.lastVisiblePost(state)?.let { post ->
                                    viewModel.requestAddFavorite(markedPost = post)
                                }
                            }
                            closeBottomSheet()
                        },
                        onImmersiveModeClick = {
                            if (!viewModel.isImmersiveMode && !viewModel.seeLz) {
                                viewModel.requestLoadFirstPage(seeLz = true)
                            }
                            viewModel.onImmersiveModeChanged()
                            closeBottomSheet()
                        },
                        onDescClick = {
                            val curSortType = state.sortType
                            viewModel.requestLoadFirstPage(
                                sortType = if (curSortType != ThreadSortType.BY_DESC) ThreadSortType.BY_DESC else ThreadSortType.DEFAULT
                            )
                            closeBottomSheet()
                        },
                        onJumpPageClick = {
                            closeBottomSheet()
                            jumpToPageDialogState.show()
                        },
                        onShareClick = viewModel::onShareThread,
                        onCopyLinkClick = viewModel::onCopyThreadLink,
                        onReportClick = { viewModel.onReportThread(context, navigator) },
                        onDeleteClick = viewModel::onDeleteThread.takeIf { state.lz?.id == localUid },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onNotNull(hazeState) {
                                hazeEffect(state = it, style = defaultHazeStyle)
                            }
                            .background(TiebaLiteTheme.extendedColorScheme.sheetContainerColor)
                            .padding(top = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ForumTitleChip(forumItem: ImmutableHolder<SimpleForum>, onForumClick: () -> Unit) {
    val forum = forumItem.get()
    Surface(
        onClick = onForumClick,
        modifier = Modifier
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = forum.name
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
                data = forum.avatar,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
            )

            Text(
                text = stringResource(id = R.string.title_forum, forum.name),
                modifier = Modifier.padding(horizontal = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun BottomBar(
    modifier: Modifier = Modifier,
    user: UserData?,
    onClickReply: (() -> Unit)?,
    onClickMore: () -> Unit,
    like: Like,
    onLiked: () -> Unit,
) {
    Column(
        modifier = modifier
            .background(TiebaLiteTheme.extendedColorScheme.navigationContainer)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (user != null && onClickReply != null) { // User logged in && not HideReply
                Avatar(
                    data = user.avatarUrl,
                    size = Sizes.Tiny,
                    contentDescription = user.name,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Surface(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .weight(1f),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    tonalElevation = 2.dp,
                    onClick = onClickReply
                ) {
                    Text(
                        text = stringResource(id = R.string.tip_reply_thread),
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            } else {
                Spacer(modifier = Modifier
                    .weight(1f)
                    .height(40.dp))
            }

            FavoriteButton(
                modifier = Modifier.minimumInteractiveComponentSize(),
                favorite = like.liked,
                onClick = onLiked
            ) {
                AnimatedVisibility(visible = like.count > 0) {
                    Text(
                        text = like.count.getShortNumString(),
                        modifier = Modifier.align(Alignment.Top),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            IconButton(onClick = onClickMore) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = stringResource(id = R.string.btn_more),
                )
            }
        }

        LiftUpSpacer()
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
    onJumpPageClick: () -> Unit,
    onShareClick: () -> Unit,
    onCopyLinkClick: () -> Unit,
    onReportClick: () -> Unit,
    onDeleteClick: (() -> Unit)?,
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
                .background(color = MaterialTheme.colorScheme.outlineVariant, shape = CircleShape)
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
                    onClick = onSeeLzClick,
                    icon = if (isSeeLz) Icons.Rounded.Face6 else Icons.Rounded.FaceRetouchingOff,
                    modifier = Modifier.fillMaxSize()
                )
            }
            item {
                ToggleButton(
                    text = stringResource(id = if (isCollected) R.string.title_collected else R.string.title_uncollected),
                    checked = isCollected,
                    onClick = onCollectClick,
                    icon = if (isCollected) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    modifier = Modifier.fillMaxSize()
                )
            }
            item {
                ToggleButton(
                    text = stringResource(id = R.string.title_pure_read),
                    checked = isImmersiveMode,
                    onClick = onImmersiveModeClick,
                    icon = if (isImmersiveMode) Icons.AutoMirrored.Rounded.ChromeReaderMode else Icons.AutoMirrored.Outlined.ChromeReaderMode,
                    modifier = Modifier.fillMaxSize()
                )
            }
            item {
                ToggleButton(
                    text = stringResource(id = R.string.title_sort),
                    checked = isDesc,
                    onClick = onDescClick,
                    icon = Icons.AutoMirrored.Rounded.Sort,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Column {
            ListMenuItem(
                icon = Icons.Rounded.RocketLaunch,
                text = stringResource(id = R.string.title_jump_page),
                onClick = onJumpPageClick,
                modifier = Modifier.fillMaxWidth(),
            )
            ListMenuItem(
                icon = Icons.Rounded.Share,
                text = stringResource(id = R.string.title_share),
                onClick = onShareClick,
                modifier = Modifier.fillMaxWidth(),
            )
            ListMenuItem(
                icon = Icons.Rounded.ContentCopy,
                text = stringResource(id = R.string.title_copy_link),
                onClick = onCopyLinkClick,
                modifier = Modifier.fillMaxWidth(),
            )
            ListMenuItem(
                icon = Icons.Rounded.Report,
                text = stringResource(id = R.string.title_report),
                onClick = onReportClick,
                modifier = Modifier.fillMaxWidth(),
            )
            if (onDeleteClick != null) {
                ListMenuItem(
                    icon = Icons.Rounded.Delete,
                    text = stringResource(id = R.string.title_delete),
                    onClick = onDeleteClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}