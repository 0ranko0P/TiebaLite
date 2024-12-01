package com.huanchengfly.tieba.post.ui.page.forum

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VerticalAlignTop
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.huanchengfly.tieba.post.arch.block
import com.huanchengfly.tieba.post.arch.clickableNoIndication
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.common.localSharedBounds
import com.huanchengfly.tieba.post.ui.common.localSharedElements
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.Destination.ForumSearchPost
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.forum.detail.navigateForumDetailPage
import com.huanchengfly.tieba.post.ui.page.forum.threadlist.GoodThreadListPage
import com.huanchengfly.tieba.post.ui.page.forum.threadlist.NormalThreadListPage
import com.huanchengfly.tieba.post.ui.page.search.SearchIconSharedElementKey
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.AvatarPlaceholder
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.BlurScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Button
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCardPlaceholder
import com.huanchengfly.tieba.post.ui.widgets.compose.ForumAvatarSharedBoundsKey
import com.huanchengfly.tieba.post.ui.widgets.compose.ForumTitleSharedBoundsKey
import com.huanchengfly.tieba.post.ui.widgets.compose.MenuScope
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.Toolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberCollapseConnection
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberPagerListStates
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberScrollStateConnection
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils.Companion.ForumFabFunction
import com.huanchengfly.tieba.post.utils.LocalAccount
import kotlin.math.max
import kotlin.math.min

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
        modifier = modifier.padding(16.dp),
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
                            progress = max(
                                0F,
                                min(1F, forum.cur_score * 1.0F / (max(1.0F, forum.levelup_score * 1.0F)))
                            ),
                            modifier = Modifier
                                .clip(CircleShape)
                                .height(8.dp),
                            color = ExtendedTheme.colors.primary,
                            backgroundColor = ExtendedTheme.colors.primary.copy(alpha = 0.25f)
                        )
                        Text(
                            text = stringResource(
                                id = R.string.tip_forum_header_liked,
                                forum.user_level.toString(),
                                forum.level_name
                            ),
                            style = MaterialTheme.typography.caption,
                            color = ExtendedTheme.colors.textSecondary,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
            val signed = forum.sign_in_info?.user_info?.is_sign_in == 1
            val liked = forum.is_like == 1
            if (LocalAccount.current != null) {
                Button(
                    onClick = { if (!liked) onFollow() else onSignIn() },
                    elevation = null,
                    shape = RoundedCornerShape(100),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = ExtendedTheme.colors.primary,
                        contentColor = ExtendedTheme.colors.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                    enabled = !signed || !liked
                ) {
                    val text = when {
                        !liked -> stringResource(id = R.string.button_follow)
                        forum.sign_in_info?.user_info?.is_sign_in == 1 -> stringResource(
                            id = R.string.button_signed_in,
                            forum.sign_in_info.user_info.cont_sign_num
                        )

                        else -> stringResource(id = R.string.button_sign_in)
                    }
                    Text(text = text, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ForumTitle(modifier: Modifier = Modifier, title: String?, avatar: String?) =
    Row(
        modifier = modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.small),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (avatar == null) {
            AvatarPlaceholder(size = Sizes.Small)
        } else {
            Avatar(data = avatar, size = Sizes.Small, contentDescription = title)
        }

        Text(text = stringResource(R.string.title_forum, title ?: ""), maxLines = 1)
    }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ForumToolbar(
    title: @Composable () -> Unit,
    menuContent: @Composable (MenuScope.() -> Unit)? = null,
    onBackAction: () -> Unit,
    onSearchAction: () -> Unit,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    Toolbar(
        title = title,
        navigationIcon = { BackNavigationIcon(onBackPressed = onBackAction) },
        actions = {
            IconButton(onClick = onSearchAction) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = stringResource(id = R.string.btn_search_in_forum),
                    modifier = Modifier.localSharedElements(SearchIconSharedElementKey)
                )
            }
            if (menuContent == null) return@Toolbar

            Box {
                val menuState = rememberMenuState()
                ClickMenu(
                    menuContent = menuContent,
                    menuState = menuState,
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
            }
        },
        elevation = Dp.Hairline,
        content = content
    )
}

@Composable
fun ForumPage(
    forumName: String,
    avatarUrl: String?,
    transitionKey: String?,
    navigator: NavController,
    viewModel: ForumViewModel = pageViewModel(),
) {
    val context = LocalContext.current
    val scaffoldState = rememberScaffoldState()
    val snackbarHostState = scaffoldState.snackbarHostState
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
    val listStates = rememberPagerListStates(size = pagerState.pageCount)

    val isGood by remember { derivedStateOf { pagerState.currentPage == TAB_FORUM_GOOD } }

    val coroutineScope = rememberCoroutineScope()

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

    // Listen scroll state changes to show/hide Fab
    val disableFab = viewModel.fab == ForumFabFunction.HIDE
    val scrollStateConnection = if (!disableFab) rememberScrollStateConnection() else null

    val connection = rememberCollapseConnection(coroutineScope)
    // Toolbar only collapsible if logged in
    val collapsed by remember { derivedStateOf { loggedIn && connection.ratio == 0.0f } }

    BlurScaffold(
        scaffoldState = scaffoldState,
        topHazeBlock = remember { {
            blurEnabled = with(pagerState) {
                currentPageOffsetFraction != 0f || listStates[currentPage].canScrollBackward == true
            }
        } },
        backgroundColor = Color.Transparent,
        modifier = Modifier.fillMaxSize(),
        topBar = {
            ForumToolbar(
                title = {
                    AnimatedVisibility(
                        visible = collapsed.not(),
                        enter =  fadeIn() + expandVertically(expandFrom = Alignment.Top),
                        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
                    ) {
                        ForumTitle(
                            modifier = Modifier.clickableNoIndication {
                                forumInfo?.item?.let { navigator.navigateForumDetailPage(it) }
                            },
                            title = forumInfo?.get { name },
                            avatar = forumInfo?.get { avatar }
                        )
                    }
                },
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
                onBackAction = navigator::navigateUp,
                onSearchAction = {
                    val forumId = forumInfo?.get { id } ?: return@ForumToolbar
                    navigator.navigate(ForumSearchPost(forumName, forumId))
                }
            ) {
                this@ForumToolbar.AnimatedVisibility(visible = collapsed) {
                    val holder: ImmutableHolder<ForumInfo>? = forumInfo
                    if (holder == null) {
                        ForumHeaderPlaceholder(forumName, avatarUrl, transitionKey)
                        return@AnimatedVisibility
                    }

                    ForumHeader(
                        forumInfoImmutableHolder = holder,
                        transitionKey = transitionKey,
                        onOpenForumInfo = {
                            navigator.navigateForumDetailPage(holder.get())
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
                ForumTab(
                    modifier = Modifier.fillMaxWidth(),
                    pagerState = pagerState,
                    sortType = viewModel.sortType,
                    onSortTypeChanged = viewModel::onSortTypeChanged
                )
            }
        },
        floatingActionButton = {
            if (disableFab || forumInfo == null) return@BlurScaffold

            // Not scrolling & Not top
            val isFabVisible by remember { derivedStateOf {
                !scrollStateConnection!!.isScrolling.value && (listStates[pagerState.currentPage]?.firstVisibleItemIndex ?: 0) > 0
            } }

            ForumFab(
                visible = isFabVisible,
                fab = viewModel.fab,
                onClick = {
                    viewModel.onFabClicked(context, isGood)
                }
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
            return@BlurScaffold
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .nestedScroll(connection)
                .block { scrollStateConnection?.let { Modifier.nestedScroll(it) } }
                .fillMaxSize(),
            key = { it },
            verticalAlignment = Alignment.Top,
            userScrollEnabled = true,
        ) { page ->
            val listState = listStates[page]

            ProvideNavigator(navigator = navigator) {
                if (page == TAB_FORUM_LATEST) {
                    NormalThreadListPage(
                        modifier = Modifier.fillMaxSize(),
                        forumId = info.id,
                        forumName = info.name,
                        sortType = { viewModel.sortType },
                        listState = listState,
                        contentPadding = contentPadding
                    )
                } else if (page == TAB_FORUM_GOOD) {
                    GoodThreadListPage(
                        modifier = Modifier.fillMaxSize(),
                        forumId = info.id,
                        forumName = info.name,
                        sortType = { viewModel.sortType },
                        listState = listState,
                        contentPadding = contentPadding
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
        color = ExtendedTheme.colors.onTopBar,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.h6,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )

@Composable
private fun ForumFab(@ForumFabFunction fab: String, visible: Boolean, onClick: () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter =  fadeIn() + scaleIn(animationSpec = tween()),
        exit = scaleOut(animationSpec = tween()) + fadeOut()
    ) {
        FloatingActionButton(
            onClick = onClick,
            backgroundColor = ExtendedTheme.colors.windowBackground,
            contentColor = ExtendedTheme.colors.primary,
            modifier = Modifier.navigationBarsPadding()
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
                        .clip(CircleShape)
                        .placeholder(highlight = PlaceholderHighlight.fade())
                        .padding(horizontal = 18.dp, vertical = 6.dp)
                ) {
                    Text(text = stringResource(id = R.string.button_sign_in), fontSize = 13.sp)
                }
            }
        }
    }
}