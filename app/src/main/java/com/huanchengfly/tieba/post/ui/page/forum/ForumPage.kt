package com.huanchengfly.tieba.post.ui.page.forum

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.fade
import com.google.accompanist.placeholder.material.placeholder
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.frsPage.ForumInfo
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.isScrolling
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.common.localSharedBounds
import com.huanchengfly.tieba.post.ui.common.localSharedElements
import com.huanchengfly.tieba.post.ui.common.theme.compose.block
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.page.Destination.ForumSearchPost
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.forum.detail.navigateForumDetailPage
import com.huanchengfly.tieba.post.ui.page.forum.threadlist.ClassifyTabsContent
import com.huanchengfly.tieba.post.ui.page.forum.threadlist.GoodThreadListPage
import com.huanchengfly.tieba.post.ui.page.forum.threadlist.NormalThreadListPage
import com.huanchengfly.tieba.post.ui.page.search.SearchIconSharedElementKey
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.AvatarPlaceholder
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultFabEnterTransition
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultFabExitTransition
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCardPlaceholder
import com.huanchengfly.tieba.post.ui.widgets.compose.ForumAvatarSharedBoundsKey
import com.huanchengfly.tieba.post.ui.widgets.compose.ForumTitleSharedBoundsKey
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeToDismissSnackbarHost
import com.huanchengfly.tieba.post.ui.widgets.compose.TwoRowsTopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberScrollStateConnection
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberSnackbarHostState
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils.Companion.ForumFabFunction
import com.huanchengfly.tieba.post.utils.LocalAccount
import kotlin.math.max
import kotlin.math.min

private val ForumHeaderHeight = 90.dp

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ForumHeader(
    forumInfoImmutableHolder: ImmutableHolder<ForumInfo>,
    transitionKey: String?,
    onOpenForumInfo: () -> Unit,
    onFollow: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (forum) = forumInfoImmutableHolder
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
                AnimatedVisibility(visible = forum.is_like == 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = {
                                max(0F, min(1F, forum.cur_score * 1.0F / (max(1.0F, forum.levelup_score * 1.0F))))
                            },
                            gapSize = Dp.Hairline,
                            drawStopIndicator = {}
                        )
                        Text(
                            text = stringResource(
                                id = R.string.tip_forum_header_liked,
                                forum.user_level,
                                forum.level_name
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                        )
                    }
                }
            }

            if (LocalAccount.current != null) {
                val signUser = forum.sign_in_info?.user_info
                val signed = signUser?.is_sign_in == 1
                val liked = forum.is_like == 1
                androidx.compose.material3.Button(
                    onClick = { if (!liked) onFollow() else onSignIn() },
                    enabled = !signed || !liked
                ) {
                    val text = when {
                        !liked -> stringResource(R.string.button_follow)
                        signed -> stringResource(R.string.button_signed_in, signUser.cont_sign_num)
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
            modifier = if (transitionEnabled) {
                Modifier.localSharedBounds(key = ForumTitleSharedBoundsKey(title, transitionKey))
            } else {
                Modifier
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
    viewModel: ForumViewModel = pageViewModel(),
) {
    val context = LocalContext.current
    val snackbarHostState = rememberSnackbarHostState()
    viewModel.onEvent<ForumUiEvent.SignIn.Success> {
        snackbarHostState.showSnackbar(
            message = context.getString(
                R.string.toast_sign_success,
                "${it.signBonusPoint}",
                "${it.userSignRank}"
            )
        )
    }
    viewModel.onEvent<ForumUiEvent.SignIn.Failure> {
        snackbarHostState.showSnackbar(
            message = context.getString(R.string.toast_sign_failed, it.errorMsg)
        )
    }
    viewModel.onEvent<ForumUiEvent.Like.Success> {
        snackbarHostState.showSnackbar(
            message = context.getString(R.string.toast_like_success, it.memberSum)
        )
    }
    viewModel.onEvent<ForumUiEvent.Like.Failure> {
        snackbarHostState.showSnackbar(
            message = context.getString(R.string.toast_like_failed, it.errorMsg)
        )
    }
    viewModel.onEvent<ForumUiEvent.Unlike.Success> {
        snackbarHostState.showSnackbar(
            message = context.getString(R.string.toast_unlike_success)
        )
    }
    viewModel.onEvent<ForumUiEvent.Unlike.Failure> {
        snackbarHostState.showSnackbar(
            message = context.getString(R.string.toast_unlike_failed, it.errorMsg)
        )
    }

    val isLoading by viewModel.uiState.collectPartialAsState(
        prop1 = ForumUiState::isLoading,
        initial = false
    )
    val forumInfo by viewModel.uiState.collectPartialAsState(
        prop1 = ForumUiState::forum,
        initial = null
    )
    val tbs by viewModel.uiState.collectPartialAsState(prop1 = ForumUiState::tbs, initial = null)

    val account = LocalAccount.current
    val loggedIn = account != null

    val pagerState = rememberPagerState { 2 }

    val isGood by remember { derivedStateOf { pagerState.currentPage == TAB_FORUM_GOOD } }
    var goodClassifyTabs: ClassifyTabsContent? by remember { mutableStateOf(null) }

    val unlikeDialogState = rememberDialogState()

    LaunchedEffect(forumInfo) {
        forumInfo?.item?.let { viewModel.saveHistory(it) }
    }

    if (loggedIn && forumInfo != null) {
        ConfirmDialog(
            dialogState = unlikeDialogState,
            onConfirm = {
                viewModel.send(
                    ForumUiIntent.Unlike(forumInfo!!.get { id }, forumName, tbs!!)
                )
            },
            title = {
                Text(text = stringResource(R.string.title_dialog_unfollow_forum, forumName))
            }
        )
    }

    val topBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // Listen scroll state changes to show/hide Fab
    val disableFab = viewModel.fab == ForumFabFunction.HIDE
    val scrollStateConnection = if (!disableFab) rememberScrollStateConnection() else null

    MyScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            val collapsed by remember {
                derivedStateOf { topBarScrollBehavior.state.collapsedFraction == 1.0f }
            }

            TwoRowsTopAppBar(
                title = {
                    val holder: ImmutableHolder<ForumInfo>? = forumInfo
                    when {
                        holder == null -> ForumHeaderPlaceholder(forumName, avatarUrl, transitionKey)

                        // remove from SharedBoundsNode when collapsed
                        collapsed -> Box(Modifier.fillMaxWidth().height(ForumHeaderHeight))

                        else -> {
                            ForumHeader(
                                forumInfoImmutableHolder = holder,
                                transitionKey = transitionKey,
                                onOpenForumInfo = {
                                    navigator.navigateForumDetailPage(holder.get(), context)
                                },
                                onFollow = {
                                    viewModel.onFollow(holder.get(), tbs!!)
                                },
                                onSignIn = {
                                    viewModel.onSignIn(holder.get(), tbs!!)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                smallTitle = {
                    ForumTitle(
                        modifier = Modifier
                            .clickableNoIndication {
                                forumInfo?.item?.let {
                                    navigator.navigateForumDetailPage(forum = it, context)
                                }
                            },
                        title = forumName,
                        avatar = avatarUrl ?: forumInfo?.item?.avatar,
                        transitionEnabled = collapsed,
                        transitionKey = transitionKey
                    )
                },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = navigator::navigateUp)
                },
                actions = {
                    forumInfo?.item?.id?.let { forumId ->
                        IconButton(
                            onClick = { navigator.navigate(ForumSearchPost(forumName, forumId)) }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = stringResource(id = R.string.btn_search_in_forum),
                                modifier = Modifier.localSharedElements(SearchIconSharedElementKey)
                            )
                        }
                    }

                    ClickMenu(
                        menuContent = {
                            TextMenuItem(text = stringResource(R.string.title_share)) {
                                viewModel.shareForum(context)
                            }

                            TextMenuItem(text = stringResource(R.string.title_send_to_desktop)) {
                                forumInfo?.item?.let { viewModel.sendToDesktop(context, forum = it) }
                            }

                            TextMenuItem(text = stringResource(R.string.title_refresh)) {
                                viewModel.onRefreshClicked(isGood = isGood)
                            }
                            // Is followed & logged in
                            if (forumInfo?.item?.is_like == 1 && tbs != null) {
                                TextMenuItem(text = stringResource(R.string.title_unfollow)) {
                                    unlikeDialogState.show()
                                }
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

                ForumTab(
                    modifier = Modifier.fillMaxWidth(),
                    pagerState = pagerState,
                    sortType = viewModel.sortType,
                    onSortTypeChanged = viewModel::onSortTypeChanged
                )

                AnimatedVisibility(visible = isGood) {
                    goodClassifyTabs?.invoke()
                }
            }
        },
        snackbarHostState = snackbarHostState,
        snackbarHost = { SwipeToDismissSnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (scrollStateConnection == null || forumInfo == null) return@MyScaffold

            val isFabVisible by remember {
                derivedStateOf { !scrollStateConnection.isScrolling && !pagerState.isScrolling }
            }

            ForumFab(
                visible = isFabVisible,
                fab = viewModel.fab,
                onClick = { viewModel.onFabClicked(context, isGood) }
            )
        }
    ) { contentPadding ->
        val info: ForumInfo? = forumInfo?.get()
        if (info == null) {
            Column(Modifier.padding(contentPadding)) {
                repeat(4) {
                    FeedCardPlaceholder()
                }
            }
            return@MyScaffold
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .nestedScroll(topBarScrollBehavior.nestedScrollConnection)
                .block { scrollStateConnection?.let { nestedScroll(it) } }
                .fillMaxSize(),
            flingBehavior = PagerDefaults.flingBehavior(pagerState, snapPositionalThreshold = 0.75f),
            key = { it },
            verticalAlignment = Alignment.Top,
        ) { page ->
            ProvideNavigator(navigator = navigator) {
                if (page == TAB_FORUM_LATEST) {
                    NormalThreadListPage(
                        modifier = Modifier.fillMaxSize(),
                        forumId = info.id,
                        forumName = info.name,
                        sortType = { viewModel.sortType },
                        contentPadding = contentPadding
                    )
                } else if (page == TAB_FORUM_GOOD) {
                    GoodThreadListPage(
                        modifier = Modifier.fillMaxSize(),
                        forumId = info.id,
                        forumName = info.name,
                        sortType = { viewModel.sortType },
                        contentPadding = contentPadding,
                        onComposeClassifyTab = { goodClassifyTabs = it }
                    )
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
private fun ForumFab(@ForumFabFunction fab: String, visible: Boolean, onClick: () -> Unit) {
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
                    ForumFabFunction.REFRESH-> Icons.Rounded.Refresh
                    ForumFabFunction.BACK_TO_TOP -> Icons.Rounded.VerticalAlignTop
                    ForumFabFunction.POST -> Icons.Rounded.Add
                    else -> throw IllegalStateException()
                },
                contentDescription = fab
            )
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
                        .placeholder(highlight = PlaceholderHighlight.fade(), shape = CircleShape)
                        .padding(horizontal = 18.dp, vertical = 6.dp)
                ) {
                    Text(text = stringResource(id = R.string.button_sign_in), fontSize = 13.sp)
                }
            }
        }
    }
}