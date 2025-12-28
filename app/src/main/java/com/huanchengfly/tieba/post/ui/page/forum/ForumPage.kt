package com.huanchengfly.tieba.post.ui.page.forum

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VerticalAlignTop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.LocalHabitSettings
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectCommonUiEventWithLifecycle
import com.huanchengfly.tieba.post.arch.collectUiEventWithLifecycle
import com.huanchengfly.tieba.post.arch.isOverlapping
import com.huanchengfly.tieba.post.arch.isScrolling
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.components.glide.TbGlideUrl
import com.huanchengfly.tieba.post.theme.FloatProducer
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.localSharedBounds
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.isWindowHeightCompact
import com.huanchengfly.tieba.post.ui.models.forum.ForumData
import com.huanchengfly.tieba.post.ui.models.forum.GoodClassify
import com.huanchengfly.tieba.post.ui.models.settings.ForumFAB
import com.huanchengfly.tieba.post.ui.page.Destination.ForumDetail
import com.huanchengfly.tieba.post.ui.page.Destination.ForumSearchPost
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.forum.threadlist.ForumThreadList
import com.huanchengfly.tieba.post.ui.page.forum.threadlist.ForumType
import com.huanchengfly.tieba.post.ui.page.main.explore.createThreadClickListeners
import com.huanchengfly.tieba.post.ui.page.thread.ThreadLikeUiEvent
import com.huanchengfly.tieba.post.ui.utils.rememberScrollOrientationConnection
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.BlurScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.CollapsingAvatarTopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultFabEnterTransition
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultFabExitTransition
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultInputScale
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCardPlaceholder
import com.huanchengfly.tieba.post.ui.widgets.compose.ForumAvatarSharedBoundsKey
import com.huanchengfly.tieba.post.ui.widgets.compose.ForumTitleSharedBoundsKey
import com.huanchengfly.tieba.post.ui.widgets.compose.LinearProgressIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.OutlinedIconTextButton
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeToDismissSnackbarHost
import com.huanchengfly.tieba.post.ui.widgets.compose.placeholder
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberPagerListStates
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.LocalAccount
import dev.chrisbanes.haze.ExperimentalHazeApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The default expanded height of a Forum TopAppBar */
private val ForumAppbarExpandHeight: Dp = 144.dp

@Composable
private fun ForumAvatar(
    modifier: Modifier = Modifier,
    avatar: String?,
    forum: String,
    transitionKey: String?
) {
    if (avatar.isNullOrEmpty()) {
        Box(modifier = modifier.placeholder(shape = CircleShape))
    } else {
        Avatar(
            data = TbGlideUrl(avatar),
            modifier = modifier.localSharedBounds(ForumAvatarSharedBoundsKey(forum, transitionKey)),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeApi::class)
@Composable
fun ForumPage(
    forumName: String,
    avatarUrl: String?,
    transitionKey: String?,
    navigator: NavController,
    viewModel: ForumViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val loggedIn = LocalAccount.current != null
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = rememberSnackbarHostState()
    val onShowSnackbarShort: (CharSequence) -> Unit = {
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message = it.toString())
        }
    }

    val pagerState = rememberPagerState { ForumType.entries.size }
    val listStates = rememberPagerListStates(pagerState.pageCount)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollOrientationConnection = rememberScrollOrientationConnection()

    viewModel.uiEvent.collectUiEventWithLifecycle {
        val message = when (it) {
            is ForumUiEvent.SignIn.Success -> {
                getString(R.string.toast_sign_success, it.signBonusPoint, it.userSignRank)
            }

            is ForumUiEvent.SignIn.Failure -> getString(R.string.toast_sign_failed, it.errorMsg)

            is ForumUiEvent.Like.Success -> getString(R.string.toast_like_success, it.memberSum)

            is ForumUiEvent.Like.Failure -> getString(R.string.toast_like_failed, it.errorMsg)

            is ForumUiEvent.Dislike.Success -> getString(R.string.toast_unlike_success)

            is ForumUiEvent.Dislike.Failure -> getString(R.string.toast_unlike_failed, it.errorMsg)

            is ForumUiEvent.PinShortcut.Success -> getString(R.string.toast_send_to_desktop_success)

            is ForumUiEvent.PinShortcut.Failure -> getString(R.string.toast_unlike_failed, it.errorMsg)

            is ForumUiEvent.ScrollToTop -> {
                listStates[it.type.ordinal].scrollToItem(0)
                scrollBehavior.state.contentOffset = 0f
                scrollBehavior.state.heightOffset = 0f
            }

            else -> it.toString()
        }
        if (message is String) {
            onShowSnackbarShort(message)
        }
    }

    viewModel.uiEvent.collectCommonUiEventWithLifecycle(
        onToast = onShowSnackbarShort,
        onNavigateUp = navigator::navigateUp
    )

    onGlobalEvent<ThreadLikeUiEvent> {
        onShowSnackbarShort(it.toMessage(context))
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val forumData = uiState.forum

    val unlikeDialogState = rememberDialogState()
    if (unlikeDialogState.show) {
        ConfirmDialog(
            dialogState = unlikeDialogState,
            onConfirm = viewModel::onDislikeForum,
            title = {
                Text(text = stringResource(R.string.title_dialog_unfollow_forum, forumName))
            }
        )
    }

    val threadClickListeners = remember(navigator) {
        createThreadClickListeners(onNavigate = navigator::navigate)
    }
    val forumThreadPages = remember(threadClickListeners) {
        ForumType.entries.map { forumType ->
            movableContentOf<Modifier, PaddingValues, ForumData> { modifier, contentPadding, forum ->
                ForumThreadList(
                    modifier = modifier,
                    threadClickListeners = threadClickListeners,
                    forumId = forum.id,
                    forumName = forum.name,
                    forumRuleTitle = forum.forumRuleTitle.takeUnless { forumType == ForumType.Good },
                    type = forumType,
                    contentPadding = contentPadding,
                    listState = listStates[forumType.ordinal]
                )
            }
        }
    }

    BlurScaffold(
        topHazeBlock = {
            blurEnabled = (listStates[pagerState.currentPage].canScrollBackward ||
                    scrollBehavior.isOverlapping) && uiState.error == null
            inputScale = DefaultInputScale
        },
        topBar = {
            CollapsingAvatarTopAppBar(
                avatar = {
                    ForumAvatar(
                        modifier = Modifier.matchParentSize(),
                        avatar = avatarUrl ?: forumData?.avatar,
                        forum = forumName,
                        transitionKey = transitionKey
                    )
                },
                title = {
                    Text(
                        text = stringResource(id = R.string.title_forum, forumName),
                        modifier = Modifier
                            .localSharedBounds(ForumTitleSharedBoundsKey(forumName, transitionKey))
                            .clickableNoIndication(enabled = forumData != null) {
                                navigator.navigate(route = ForumDetail(forumName))
                            },
                        maxLines = 1,
                        overflow = TextOverflow.MiddleEllipsis,
                    )
                },
                subtitle = {
                    AnimatedVisibility(
                        visible = forumData != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        if (loggedIn) {
                            ForumExpProgress(forum = forumData!!)
                        } else if (!forumData!!.slogan.isNullOrEmpty()) {
                            Text(text = forumData.slogan, maxLines = 1)
                        }
                    }
                },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = navigator::navigateUp)
                },
                actions = {
                    if (forumData == null) return@CollapsingAvatarTopAppBar // Loading

                    if (loggedIn) {
                        ForumSignFollowActionButton(
                            forum = forumData,
                            onFollow = viewModel::onLikeForum,
                            onSignIn = viewModel::onSignIn,
                            collapsedFraction = { scrollBehavior.state.collapsedFraction }
                        )
                    }

                    ActionItem(
                        icon = Icons.Rounded.Search,
                        contentDescription = R.string.btn_search_in_forum,
                        onClick = { navigator.navigate(ForumSearchPost(forumName, forumData.id)) }
                    )

                    ClickMenu(
                        menuContent = {
                            TextMenuItem(text = R.string.title_share, onClick = viewModel::shareForum)

                            TextMenuItem(text = R.string.title_send_to_desktop) {
                                viewModel.sendToDesktop(context.getString(R.string.title_forum, forumData.name))
                            }

                            TextMenuItem(text = R.string.title_refresh) {
                                viewModel.onRefreshClicked(isGood = pagerState.currentPage == TAB_FORUM_GOOD)
                            }

                            // Is followed & logged in
                            if (forumData.liked && forumData.tbs != null) {
                                TextMenuItem(text = R.string.title_unfollow, onClick = unlikeDialogState::show)
                            }
                        },
                        triggerShape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(id = R.string.btn_more),
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        )
                    }
                },
                expandedHeight = ForumAppbarExpandHeight,
                colors = TiebaLiteTheme.topAppBarColors,
                scrollBehavior = scrollBehavior,
            )  {
                val sortType by viewModel.sortType.collectAsStateWithLifecycle()
                ForumTab(
                    modifier = Modifier.fillMaxWidth(),
                    pagerState = pagerState,
                    sortType = sortType,
                    onSortTypeChanged = viewModel::onSortTypeChanged
                )

                val classifyVisible by remember { derivedStateOf { pagerState.currentPage == TAB_FORUM_GOOD } }
                val goodClassifies = uiState.forum?.goodClassifies ?: return@CollapsingAvatarTopAppBar
                // Compose classify inside TopBar for background blur
                AnimatedVisibility(visible = classifyVisible) {
                    ClassifyTabs(
                        goodClassifies = goodClassifies,
                        selectedItem = uiState.goodClassifyId,
                        onSelected = viewModel::onGoodClassifyChanged
                    )
                }
            }
        },
        snackbarHostState = snackbarHostState,
        snackbarHost = { SwipeToDismissSnackbarHost(snackbarHostState) },
        floatingActionButton = {
            val fab = LocalHabitSettings.current.forumFAB
            if (fab == ForumFAB.HIDE || forumData == null) return@BlurScaffold
            // FAB visibility: no error, not onTop, scrolling forward, pager is not scrolling
            val fabVisibilityState = remember {
                derivedStateOf {
                    val notTop = listStates[pagerState.currentPage].canScrollBackward
                    uiState.error == null && notTop && scrollOrientationConnection.isScrollingForward && !pagerState.isScrolling
                }
            }

            ForumFab(fab = fab, visible = { fabVisibilityState.value }) {
                viewModel.onFabClicked(fab, isGood = pagerState.currentPage == TAB_FORUM_GOOD)
            }
        }
    ) { contentPadding ->
        StateScreen(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(connection = scrollOrientationConnection)
                .nestedScroll(connection = scrollBehavior.nestedScrollConnection),
            isLoading = forumData == null,
            error = uiState.error,
            loadingScreen = {
                ForumThreadsPlaceholder(threadCount = if (isWindowHeightCompact()) 4 else 8)
            },
            screenPadding = contentPadding
        ) {
            if (forumData == null) return@StateScreen

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                flingBehavior = PagerDefaults.flingBehavior(pagerState, snapPositionalThreshold = 0.75f),
                key = { it },
                verticalAlignment = Alignment.Top,
            ) { page ->
                ProvideNavigator(navigator = navigator) {
                    forumThreadPages[page](Modifier, contentPadding, forumData)
                }
            }
        }
    }
}

private const val SignActionVisibilityThreshold = 0.1f // 10% Collapsing

@Composable
private fun ForumSignFollowActionButton(
    modifier: Modifier = Modifier,
    forum: ForumData,
    onFollow: () -> Unit,
    onSignIn: () -> Unit,
    collapsedFraction: FloatProducer,
) {
    val visibility by remember {
        derivedStateOf { collapsedFraction() < SignActionVisibilityThreshold }
    }
    if (!visibility) return

    OutlinedIconTextButton (
        onClick = if (forum.liked) onSignIn else onFollow,
        modifier = modifier.graphicsLayer {
            alpha = lerp(1f, 0f, collapsedFraction() * (1 / SignActionVisibilityThreshold))
        },
        enabled = !forum.signed || !forum.liked,
        vectorIcon = when {
            !forum.liked -> ImageVector.vectorResource(id = R.drawable.ic_favorite)
            forum.signed -> null
            else -> ImageVector.vectorResource(id = R.drawable.ic_oksign)
        },
        text = when {
            !forum.liked -> stringResource(R.string.button_follow)
            forum.signed -> stringResource(R.string.button_signed_in, forum.signedDays)
            else -> stringResource(R.string.button_sign_in)
        }
    )
}

@Composable
private fun ForumExpProgress(modifier: Modifier = Modifier, forum: ForumData) {
    AnimatedVisibility(
        visible = forum.liked,
        modifier = modifier.fillMaxWidth(0.75f),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        val progressAnimatable = remember { Animatable(forum.levelProgress) }

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            LinearProgressIndicator(
                progress = { progressAnimatable.value },
                modifier = Modifier.height(6.dp).clip(CircleShape),
            )
            Text(
                text = stringResource(R.string.tip_forum_header_liked, forum.level, forum.levelName),
            )
        }

        if (forum.signed) {
            LaunchedEffect(Unit) {
                if (forum.levelProgress != progressAnimatable.targetValue) { // Skip signed forum
                    progressAnimatable.snapTo(0f)
                    delay(AnimationConstants.DefaultDurationMillis.toLong())
                    progressAnimatable.animateTo(forum.levelProgress, spring(stiffness = Spring.StiffnessLow))
                }
            }
        }
    }
}

@Composable
private fun ForumFab(@ForumFAB fab: Int, visible: () -> Boolean, onClick: () -> Unit) {
    AnimatedVisibility(
        visible = visible(),
        enter = DefaultFabEnterTransition,
        exit = DefaultFabExitTransition
    ) {
        FloatingActionButton(
            onClick = onClick,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = Dp.Hairline // Buggy shadow when visibility changes
            ),
        ) {
            Icon(
                imageVector = when (fab) {
                    ForumFAB.REFRESH-> Icons.Rounded.Refresh
                    ForumFAB.BACK_TO_TOP -> Icons.Rounded.VerticalAlignTop
                    ForumFAB.POST -> Icons.Rounded.Add
                    else -> throw IllegalStateException()
                },
                contentDescription = null
            )
        }
    }
}

@Composable
private fun ClassifyTabs(
    goodClassifies: List<GoodClassify>,
    selectedItem: Int?,
    onSelected: (Int) -> Unit,
) {
    LazyRow(
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = goodClassifies, key = { it.second /* class_id */ }) { (name, id) ->
            Chip(text = name, invertColor = selectedItem == id) {
                if (selectedItem != id) {
                    onSelected(id)
                }
            }
        }
    }
}

@Composable
private fun ForumThreadsPlaceholder(modifier: Modifier = Modifier, threadCount: Int) {
    Container(modifier = modifier) {
        Column {
            repeat(times = threadCount) {
                FeedCardPlaceholder()
            }
        }
    }
}