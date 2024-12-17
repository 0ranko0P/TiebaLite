package com.huanchengfly.tieba.post.ui.page.thread

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Surface
import androidx.compose.material.Text
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
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.block
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.copy
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.invertChipBackground
import com.huanchengfly.tieba.post.ui.common.theme.compose.invertChipContent
import com.huanchengfly.tieba.post.ui.common.theme.compose.threadBottomBar
import com.huanchengfly.tieba.post.ui.models.PostData
import com.huanchengfly.tieba.post.ui.models.UserData
import com.huanchengfly.tieba.post.ui.page.Destination.Forum
import com.huanchengfly.tieba.post.ui.page.Destination.Reply
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.BlurScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.FavoriteButton
import com.huanchengfly.tieba.post.ui.widgets.compose.LiftUpSpacer
import com.huanchengfly.tieba.post.ui.widgets.compose.ListMenuItem
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalHazeState
import com.huanchengfly.tieba.post.ui.widgets.compose.PromptDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.StickyHeaderOverlay
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.VerticalGrid
import com.huanchengfly.tieba.post.ui.widgets.compose.defaultHazeStyle
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ToggleButton(
    text: String,
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    backgroundColor: Color = ExtendedTheme.colors.chip,
    contentColor: Color = ExtendedTheme.colors.text,
    selectedBackgroundColor: Color = ExtendedTheme.colors.invertChipBackground,
    selectedContentColor: Color = ExtendedTheme.colors.invertChipContent,
) {
    val animatedColor by animateColorAsState(
        if (checked) selectedContentColor else contentColor,
        label = "toggleBtnColor"
    )
    val animatedBackgroundColor by animateColorAsState(
        if (checked) selectedBackgroundColor else backgroundColor,
        label = "toggleBtnBackgroundColor"
    )

    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = true,
        shape = RoundedCornerShape(6.dp),
        color = animatedBackgroundColor,
        contentColor = animatedColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (icon != null) {
                    Icon(imageVector = icon, contentDescription = text)
                }
                ProvideTextStyle(
                    value = MaterialTheme.typography.subtitle1.copy(
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                ) {
                    Text(text = text)
                }
            }
        }
    }
}

private fun LazyListState.lastVisiblePost(viewModel: ThreadViewModel): PostData? {
    val lastPostItem = layoutInfo.visibleItemsInfo.lastOrNull { item ->
        item.key is String && (item.key as String).startsWith(ITEM_POST_KEY_PREFIX)
    }?: return viewModel.threadUiState.firstPost

    return viewModel.data.firstOrNull { post ->
        (lastPostItem.key as String).endsWith(post.id.toString())
    } ?: viewModel.threadUiState.firstPost
}

@Composable
fun ThreadPage(
    threadId: Long,
    postId: Long = 0,
    extra: ThreadFrom? = null,
    scrollToReply: Boolean = false,
    navigator: NavController,
    viewModel: ThreadViewModel = hiltViewModel(),
) {

    val scaffoldState = rememberScaffoldState()
    val state = viewModel.threadUiState

    val forum = state.forum
    val user = state.user

    val currentPageMax = state.currentPageMax
    val curSortType = state.sortType

    var waitLoadSuccessAndScrollToFirstReply by remember { mutableStateOf(scrollToReply) }

    val lazyListState = rememberLazyListState()
    val bottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val openBottomSheet = {
        coroutineScope.launch { bottomSheetState.show() }
    }
    val closeBottomSheet = {
        coroutineScope.launch { bottomSheetState.hide() }
    }

    val uiEvent by viewModel.uiEvent

    LaunchedEffect(uiEvent) {
        val unhandledEvent = uiEvent?: return@LaunchedEffect
        when(unhandledEvent) {
            is CommonUiEvent.Toast -> {
                scaffoldState.snackbarHostState.showSnackbar(unhandledEvent.message.toString())
            }

            is ThreadUiEvent.ScrollToFirstReply -> {
                lazyListState.animateScrollToItem(1)
            }

            is ThreadUiEvent.ScrollToLatestReply -> {
                if (curSortType != ThreadSortType.BY_DESC) {
                    lazyListState.animateScrollToItem(2 + viewModel.data.size)
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

    onGlobalEvent<GlobalEvent.ReplySuccess>(filter = { it.threadId == threadId }) { event ->
        viewModel.requestLoadMyLatestReply(event.newPostId)
    }

    val updateCollectMarkDialogState = rememberDialogState()

    ConfirmDialog(
        dialogState = updateCollectMarkDialogState,
        onConfirm = {
            lazyListState.lastVisiblePost(viewModel)?.let { post ->
                if (post.id != 0L) viewModel.requestAddFavorite(post)
            }
            navigator.navigateUp()
        },
        onCancel = { navigator.navigateUp() }
    ) {
        val lastVisibleFloor = lazyListState.lastVisiblePost(viewModel)?.floor ?: 0
        Text(stringResource(R.string.message_update_collect_mark, lastVisibleFloor))
    }

    val confirmDeleteState = rememberDialogState()
    ConfirmDialog(dialogState = confirmDeleteState, onConfirm = viewModel::onDeleteConfirmed) {
        val deletePost = viewModel.deletePost ?: return@ConfirmDialog
        val deleteType = if (deletePost != viewModel.threadUiState.firstPost) {
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
            Text(text = stringResource(R.string.tip_jump_page, currentPageMax, state.totalPage))
        }
    )

    LaunchedEffect(Unit) {
        if (extra is ThreadFrom.Store && extra.maxPid != postId) {
            val result = scaffoldState.snackbarHostState.showSnackbar(
                context.getString(R.string.message_store_thread_update, extra.maxFloor),
                context.getString(R.string.button_load_new),
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

        val lastVisiblePost = lazyListState.lastVisiblePost(viewModel)?.apply {
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
        isEmpty =  viewModel.data.isEmpty() && viewModel.threadUiState.firstPost == null,
        isError = viewModel.error != null,
        isLoading = viewModel.isRefreshing,
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
            },
            scaffoldState = scaffoldState,
            attachHazeContentState = false, // Attach manually since we're blurring the BottomSheet
            topBar = {
                TopBar(
                    name = forum?.item?.name,
                    avatar = forum?.item?.avatar,
                    onBack = navigator::navigateUp,
                    onForumClick = {
                        forum?.item?.name?.let { navigator.navigate(Forum(it)) }
                    }
                ) {
                    if (useStickyHeaderWorkaround) {
                        StickyHeaderOverlay(state = lazyListState) {
                            ThreadHeader(viewModel = viewModel)
                        }
                    }
                }
            },
            bottomBar = {
                BottomBar(
                    user = user,
                    onClickReply = {
                        navigator.navigate(
                            Reply(
                                forumId = viewModel.curForumId ?: 0,
                                forumName = forum?.get { name }.orEmpty(),
                                threadId = threadId,
                            )
                        )
                    }.takeUnless { viewModel.hideReply },
                    onAgree = viewModel::onAgreeThreadClicked,
                    onClickMore = {
                        if (bottomSheetState.isVisible) {
                            closeBottomSheet()
                        } else {
                            openBottomSheet()
                        }
                    },
                    agreed = viewModel.info?.hasAgree == true,
                    agreeNum = viewModel.info?.diffAgreeNum ?: 0L
                )
            },
        ) { padding ->
            val hazeState: HazeState? = LocalHazeState.current

            useStickyHeaderWorkaround = padding.calculateTopPadding() != Dp.Hairline

            // Ignore Scaffold padding changes if workaround enabled
            val direction = LocalLayoutDirection.current
            val contentPadding = if (useStickyHeaderWorkaround) remember { padding.copy(direction) } else padding

            ModalBottomSheetLayout(
                sheetState = bottomSheetState,
                sheetShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                sheetBackgroundColor = Color.Unspecified, // Set background on SheetContent for blurring
                sheetContentColor = ExtendedTheme.colors.text,
                sheetContent = {
                    ThreadMenu(
                        isSeeLz = viewModel.seeLz,
                        isCollected = viewModel.info!!.collected,
                        isImmersiveMode = viewModel.isImmersiveMode,
                        isDesc = state.sortType == ThreadSortType.BY_DESC,
                        canDelete = { viewModel.lz?.id == user?.id },
                        onSeeLzClick = {
                            viewModel.requestLoadFirstPage(seeLz = !viewModel.seeLz)
                            closeBottomSheet()
                        },
                        onCollectClick = {
                            if (viewModel.info!!.collected) {
                                viewModel.requestRemoveFavorite()
                            } else {
                                lazyListState.lastVisiblePost(viewModel)?.let { post ->
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
                        onDeleteClick = viewModel::onDeleteThread,
                        modifier = Modifier
                            .fillMaxWidth()
                            .block {
                                hazeState?.let { hazeChild(it, defaultHazeStyle, null) }
                            }
                            .background(color = ExtendedTheme.colors.threadBottomBar)
                            .padding(vertical = 16.dp)
                            .padding(bottom = contentPadding.calculateBottomPadding())
                    )
                },
                scrimColor = Color.Transparent,
            ) {
                ProvideNavigator(navigator = navigator) {
                    ThreadContent(
                        modifier = Modifier.block { hazeState?.let { haze(it) } },
                        viewModel = viewModel,
                        lazyListState = lazyListState,
                        contentPadding = contentPadding,
                        useStickyHeader = !useStickyHeaderWorkaround
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    name: String?,
    avatar: String?,
    onBack: () -> Unit,
    onForumClick: () -> Unit,
    content: (@Composable ColumnScope.() -> Unit)? = null
) =
    TitleCentredToolbar(
        title = {
            if (avatar == null || name == null) return@TitleCentredToolbar // Initializing
            Row(
                modifier = Modifier
                    .padding(horizontal = 48.dp)
                    .height(IntrinsicSize.Min)
                    .clip(CircleShape)
                    .background(ExtendedTheme.colors.chip)
                    .clickable(onClick = onForumClick)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(
                    data = avatar,
                    contentDescription = name,
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                )

                Text(
                    text = stringResource(id = R.string.title_forum, name),
                    fontSize = 14.sp,
                    color = ExtendedTheme.colors.text,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        elevation = Dp.Hairline,
        navigationIcon = {
            BackNavigationIcon(onBack)
        },
        content = content
    )

@Composable
private fun BottomBar(
    user: UserData?,
    onClickReply: (() -> Unit)?,
    onAgree: () -> Unit,
    onClickMore: () -> Unit,
    modifier: Modifier = Modifier,
    agreed: Boolean = false,
    agreeNum: Long = 0,
) {
    Column(
        modifier = modifier
            .background(ExtendedTheme.colors.threadBottomBar)
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

                Row(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(ExtendedTheme.colors.floorCard)
                        .clickable(onClick = onClickReply)
                        .padding(8.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.tip_reply_thread),
                        style = MaterialTheme.typography.caption,
                        color = ExtendedTheme.colors.textSecondary,
                    )
                }
            } else {
                Spacer(modifier = Modifier
                    .weight(1f)
                    .height(40.dp))
            }

            FavoriteButton(Modifier.fillMaxHeight(), favorite = agreed, onClick = onAgree) { color ->
                AnimatedVisibility(visible = agreeNum > 0) {
                    Text(
                        text = agreeNum.getShortNumString(),
                        modifier = Modifier.align(Alignment.Top),
                        color = color,
                        style = MaterialTheme.typography.caption,
                        fontSize = 12.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(MaterialTheme.shapes.small)
                    .clickable(onClick = onClickMore)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = stringResource(id = R.string.btn_more),
                    tint = ExtendedTheme.colors.textSecondary,
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
    canDelete: () -> Boolean,
    onSeeLzClick: () -> Unit,
    onCollectClick: () -> Unit,
    onImmersiveModeClick: () -> Unit,
    onDescClick: () -> Unit,
    onJumpPageClick: () -> Unit,
    onShareClick: () -> Unit,
    onCopyLinkClick: () -> Unit,
    onReportClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .height(4.dp)
                .fillMaxWidth(0.25f)
                .clip(CircleShape)
                .background(ExtendedTheme.colors.chip)
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
                iconColor = ExtendedTheme.colors.text,
                onClick = onJumpPageClick,
                modifier = Modifier.fillMaxWidth(),
            )
            ListMenuItem(
                icon = Icons.Rounded.Share,
                text = stringResource(id = R.string.title_share),
                iconColor = ExtendedTheme.colors.text,
                onClick = onShareClick,
                modifier = Modifier.fillMaxWidth(),
            )
            ListMenuItem(
                icon = Icons.Rounded.ContentCopy,
                text = stringResource(id = R.string.title_copy_link),
                iconColor = ExtendedTheme.colors.text,
                onClick = onCopyLinkClick,
                modifier = Modifier.fillMaxWidth(),
            )
            ListMenuItem(
                icon = Icons.Rounded.Report,
                text = stringResource(id = R.string.title_report),
                iconColor = ExtendedTheme.colors.text,
                onClick = onReportClick,
                modifier = Modifier.fillMaxWidth(),
            )
            if (canDelete()) {
                ListMenuItem(
                    icon = Icons.Rounded.Delete,
                    text = stringResource(id = R.string.title_delete),
                    iconColor = ExtendedTheme.colors.text,
                    onClick = onDeleteClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}