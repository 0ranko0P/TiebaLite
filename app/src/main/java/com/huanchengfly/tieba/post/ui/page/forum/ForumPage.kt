package com.huanchengfly.tieba.post.ui.page.forum

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.isScrolling
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.ui.common.localSharedBounds
import com.huanchengfly.tieba.post.ui.common.localSharedElements
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.common.theme.compose.onCase
import com.huanchengfly.tieba.post.ui.models.forum.ForumData
import com.huanchengfly.tieba.post.ui.models.forum.GoodClassify
import com.huanchengfly.tieba.post.ui.models.settings.ForumFAB
import com.huanchengfly.tieba.post.ui.page.Destination.ForumDetail
import com.huanchengfly.tieba.post.ui.page.Destination.ForumSearchPost
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.forum.threadlist.ForumThreadList
import com.huanchengfly.tieba.post.ui.page.forum.threadlist.ForumType
import com.huanchengfly.tieba.post.ui.page.main.explore.createThreadClickListeners
import com.huanchengfly.tieba.post.ui.page.search.SearchIconSharedElementKey
import com.huanchengfly.tieba.post.ui.page.thread.ThreadLikeUiEvent
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.AvatarPlaceholder
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultFabEnterTransition
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultFabExitTransition
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCardPlaceholder
import com.huanchengfly.tieba.post.ui.widgets.compose.ForumAvatarSharedBoundsKey
import com.huanchengfly.tieba.post.ui.widgets.compose.ForumTitleSharedBoundsKey
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeToDismissSnackbarHost
import com.huanchengfly.tieba.post.ui.widgets.compose.TwoRowsTopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.placeholder
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberScrollStateConnection
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.LocalAccount
import kotlin.math.max
import kotlin.math.min

private val ForumHeaderHeight = 90.dp

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ForumHeader(
    forum: ForumData,
    transitionKey: String?,
    onOpenForumInfo: () -> Unit,
    onFollow: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(ForumHeaderHeight)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(
                data = forum.avatar,
                size = Sizes.Large,
                contentDescription = forum.name,
                modifier = Modifier
                    .localSharedBounds(key = ForumAvatarSharedBoundsKey(forum.name, transitionKey))
                    .clickable(onClick = onOpenForumInfo)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ForumTitleText(
                    modifier = Modifier
                        .localSharedBounds(key = ForumTitleSharedBoundsKey(forum.name, transitionKey))
                        .clickable(onClick = onOpenForumInfo),
                    name = forum.name
                )
                AnimatedVisibility(visible = forum.liked) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = {
                                max(0F, min(1F, forum.score / (max(1.0F, forum.scoreLevelUp * 1.0F))))
                            },
                            gapSize = Dp.Hairline,
                            drawStopIndicator = {}
                        )
                        Text(
                            text = stringResource(R.string.tip_forum_header_liked, forum.level, forum.levelName),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (LocalAccount.current != null) {
                Button(
                    onClick = if (forum.liked) onSignIn else onFollow,
                    enabled = !forum.signed || !forum.liked
                ) {
                    val text = when {
                        !forum.liked -> stringResource(R.string.button_follow)
                        forum.signed -> stringResource(R.string.button_signed_in, forum.signedDays)
                        else -> stringResource(R.string.button_sign_in)
                    }
                    Text(text = text, fontSize = 13.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ForumTitle(
    modifier: Modifier = Modifier,
    title: String,
    avatar: String?,
    transitionEnabled: Boolean,
    transitionKey: String?
) {
    val avatarModifier = if (transitionEnabled) {
        Modifier.localSharedBounds(key = ForumAvatarSharedBoundsKey(title, transitionKey))
    } else {
        Modifier
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (avatar == null) {
            AvatarPlaceholder(size = Sizes.Small, modifier = avatarModifier)
        } else {
            Avatar(avatar, size = Sizes.Small, contentDescription = title, modifier = avatarModifier)
        }

        Text(
            text = stringResource(R.string.title_forum, title),
            modifier = Modifier
                .onCase(condition = transitionEnabled) {
                    localSharedBounds(key = ForumTitleSharedBoundsKey(title, transitionKey))
                },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
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
    val snackbarHostState = rememberSnackbarHostState()

    LaunchedEffect(viewModel) {
        viewModel.uiEvent.collect {
            val message = when(it) {
                is CommonUiEvent.Toast -> it.message.toString()

                is ForumUiEvent.SignIn.Success -> {
                    context.getString(R.string.toast_sign_success, it.signBonusPoint, it.userSignRank)
                }
                is ForumUiEvent.SignIn.Failure -> context.getString(R.string.toast_sign_failed, it.errorMsg)

                is ForumUiEvent.Like.Success -> context.getString(R.string.toast_like_success, it.memberSum)

                is ForumUiEvent.Like.Failure -> context.getString(R.string.toast_like_failed, it.errorMsg)

                is ForumUiEvent.Dislike.Success -> context.getString(R.string.toast_unlike_success)

                is ForumUiEvent.Dislike.Failure -> context.getString(R.string.toast_unlike_failed, it.errorMsg)

                else -> it.toString() // not gonna happen
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    onGlobalEvent<ThreadLikeUiEvent> {
        snackbarHostState.showSnackbar(it.toMessage(context))
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val forumData = uiState.forum

    val pagerState = rememberPagerState { ForumType.entries.size }

    val unlikeDialogState = rememberDialogState()
    if (loggedIn && forumData != null) {
        ConfirmDialog(
            dialogState = unlikeDialogState,
            onConfirm = viewModel::onDislikeForum,
            title = {
                Text(text = stringResource(R.string.title_dialog_unfollow_forum, forumName))
            }
        )
    }

    val topBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // Listen scroll state changes to show/hide Fab
    val scrollStateConnection = rememberScrollStateConnection()

    val threadClickListeners = remember(navigator) {
        createThreadClickListeners(onNavigate = navigator::navigate)
    }
    val forumThreadPages = remember(threadClickListeners) {
        ForumType.entries.map { forumType ->
            movableContentOf<Modifier, PaddingValues, ForumData> { modifier, contentPadding, forum ->
                ForumThreadList(
                    modifier = Modifier.nestedScroll(topBarScrollBehavior.nestedScrollConnection),
                    threadClickListeners = threadClickListeners,
                    forumId = forum.id,
                    forumName = forum.name,
                    forumRuleTitle = forum.forumRuleTitle.takeUnless { forumType == ForumType.Good },
                    type = forumType,
                    contentPadding = contentPadding
                )
            }
        }
    }

    MyScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            val onNavigateForumDetail: () -> Unit = {
                uiState.forum?.let { navigator.navigate(route = ForumDetail(it.name)) }
            }

            val collapsed by remember {
                derivedStateOf { topBarScrollBehavior.state.collapsedFraction == 1.0f }
            }

            TwoRowsTopAppBar(
                title = {
                    when {
                        forumData == null -> ForumHeaderPlaceholder(forumName, avatarUrl, transitionKey)

                        // remove from SharedBoundsNode when collapsed
                        collapsed -> Box(Modifier.fillMaxWidth().height(ForumHeaderHeight))

                        else -> {
                            ForumHeader(
                                forum = forumData,
                                transitionKey = transitionKey,
                                onOpenForumInfo = onNavigateForumDetail,
                                onFollow = viewModel::onLikeForum,
                                onSignIn = viewModel::onSignIn,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                smallTitle = {
                    ForumTitle(
                        modifier = Modifier.clickableNoIndication(onClick = onNavigateForumDetail) ,
                        title = forumName,
                        avatar = avatarUrl ?: forumData?.avatar,
                        transitionEnabled = collapsed,
                        transitionKey = transitionKey
                    )
                },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = navigator::navigateUp)
                },
                actions = {
                    if (forumData == null) return@TwoRowsTopAppBar // Loading
                    IconButton(
                        onClick = { navigator.navigate(ForumSearchPost(forumName, forumData.id)) }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = stringResource(id = R.string.btn_search_in_forum),
                            modifier = Modifier.localSharedElements(SearchIconSharedElementKey)
                        )
                    }

                    ClickMenu(
                        menuContent = {
                            TextMenuItem(text = stringResource(R.string.title_share), onClick = viewModel::shareForum)

                            TextMenuItem(text = stringResource(R.string.title_send_to_desktop), onClick = viewModel::sendToDesktop)

                            TextMenuItem(text = stringResource(R.string.title_refresh)) {
                                viewModel.onRefreshClicked(isGood = pagerState.currentPage == TAB_FORUM_GOOD)
                            }

                            // Is followed & logged in
                            if (forumData.liked && forumData.tbs != null) {
                                TextMenuItem(
                                    text = stringResource(R.string.title_unfollow),
                                    onClick = unlikeDialogState::show
                                )
                            }
                        },
                        triggerShape = CircleShape
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = stringResource(id = R.string.btn_more)
                            )
                        }
                    }
                },
                scrollBehavior = topBarScrollBehavior
            )  {
                val sortType by viewModel.sortType.collectAsStateWithLifecycle()
                ForumTab(
                    modifier = Modifier.fillMaxWidth(),
                    pagerState = pagerState,
                    sortType = sortType,
                    onSortTypeChanged = viewModel::onSortTypeChanged
                )

                val classifyVisible by remember { derivedStateOf { pagerState.currentPage == TAB_FORUM_GOOD } }
                val goodClassifies = uiState.forum?.goodClassifies ?: return@TwoRowsTopAppBar
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
            val fab by viewModel.fab.collectAsStateWithLifecycle()
            if (fab == ForumFAB.HIDE || forumData == null) return@MyScaffold

            val isFabVisible by remember {
                derivedStateOf { !scrollStateConnection.isScrolling && !pagerState.isScrolling }
            }

            ForumFab(fab = fab, visible = isFabVisible) {
                viewModel.onFabClicked(pagerState.currentPage == TAB_FORUM_GOOD)
            }
        }
    ) { contentPadding ->
        StateScreen(
            modifier = Modifier.fillMaxSize(),
            isEmpty = false,
            isError = uiState.error != null,
            isLoading = forumData == null,
            loadingScreen = {
                Column(Modifier.padding(contentPadding)) {
                    repeat(4) {
                        FeedCardPlaceholder()
                    }
                }
            },
            errorScreen = { ErrorScreen(uiState.error, Modifier.padding(contentPadding)) }
        ) {
            if (forumData == null) return@StateScreen
            val pageModifier = Modifier.nestedScroll(topBarScrollBehavior.nestedScrollConnection)

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(connection = scrollStateConnection),
                flingBehavior = PagerDefaults.flingBehavior(pagerState, snapPositionalThreshold = 0.75f),
                key = { it },
                verticalAlignment = Alignment.Top,
            ) { page ->
                ProvideNavigator(navigator = navigator) {
                    forumThreadPages[page](pageModifier, contentPadding, forumData)
                }
            }
        }
    }
}

@Composable
private fun ForumTitleText(modifier: Modifier = Modifier, name: String) =
    Text(
        text = stringResource(id = R.string.title_forum, name),
        modifier = modifier,
        style = MaterialTheme.typography.titleLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )

@Composable
private fun ForumFab(@ForumFAB fab: Int, visible: Boolean, onClick: () -> Unit) {
    AnimatedVisibility(
        visible = visible,
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ForumHeaderPlaceholder(
    forumName: String,
    avatarUrl: String?,
    transitionKey: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val avatarModifier = Modifier.localSharedBounds(
            key = ForumAvatarSharedBoundsKey(forumName = forumName, extraKey = transitionKey)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (avatarUrl != null) {
                Avatar(data = avatarUrl, size = Sizes.Large, modifier = avatarModifier)
            } else {
                AvatarPlaceholder(size = Sizes.Large, modifier = avatarModifier)
            }

            ForumTitleText(
                modifier = Modifier.localSharedBounds(ForumTitleSharedBoundsKey(forumName, transitionKey)),
                name = forumName
            )

            if (LocalAccount.current != null) {
                Spacer(modifier = Modifier.weight(1.0f))
                Box(
                    modifier = Modifier
                        .placeholder(shape = CircleShape)
                        .padding(horizontal = 18.dp, vertical = 6.dp)
                ) {
                    Text(text = stringResource(id = R.string.button_sign_in), fontSize = 13.sp)
                }
            }
        }
    }
}