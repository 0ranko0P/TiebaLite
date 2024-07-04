package com.huanchengfly.tieba.post.ui.page.forum

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.DropdownMenuItem
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.fade
import com.google.accompanist.placeholder.material.placeholder
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.frsPage.ForumInfo
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.ForumDetailPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ForumSearchPostPageDestination
import com.huanchengfly.tieba.post.ui.page.forum.detail.StatCardItem
import com.huanchengfly.tieba.post.ui.page.forum.threadlist.ForumThreadListPage
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.AvatarPlaceholder
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.Button
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCardPlaceholder
import com.huanchengfly.tieba.post.ui.widgets.compose.HorizontalDivider
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.MenuScope
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.Toolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberCollapseConnection
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils.Companion.ForumFabFunction
import com.huanchengfly.tieba.post.utils.LocalAccount
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlin.math.max
import kotlin.math.min

@Composable
private fun ForumHeader(
    forumInfoImmutableHolder: ImmutableHolder<ForumInfo>,
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
                contentDescription = forum.name
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenForumInfo
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.title_forum, forum.name),
                        style = MaterialTheme.typography.h6,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                AnimatedVisibility(visible = forum.is_like == 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = max(
                                0F,
                                min(1F, forum.cur_score * 1.0F / (max(1.0F, forum.levelup_score * 1.0F)))
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(100))
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
                        contentColor = ExtendedTheme.colors.onAccent
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
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(color = ExtendedTheme.colors.chip)
                .padding(top = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatCardItem(
                statNum = forum.member_num,
                statText = stringResource(id = R.string.text_stat_follow)
            )
            HorizontalDivider(color = Color(if (ExtendedTheme.colors.isNightMode) 0xFF808080 else 0xFFDEDEDE))
            StatCardItem(
                statNum = forum.thread_num,
                statText = stringResource(id = R.string.text_stat_threads)
            )
            HorizontalDivider(color = Color(if (ExtendedTheme.colors.isNightMode) 0xFF808080 else 0xFFDEDEDE))
            StatCardItem(
                statNum = forum.post_num,
                statText = stringResource(id = R.string.title_stat_posts_num)
            )
        }
    }
}

@Composable
private fun ForumToolbar(
    title: @Composable () -> Unit,
    menuContent: @Composable (MenuScope.() -> Unit)? = null,
    onBackAction: () -> Unit,
    onSearchAction: () -> Unit,
    isLoading: Boolean,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    Toolbar(
        title = title,
        navigationIcon = { BackNavigationIcon(onBackPressed = onBackAction) },
        actions = {
            if (!isLoading) {
                IconButton(onClick = onSearchAction) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = stringResource(id = R.string.btn_search_in_forum)
                    )
                }
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
        content = content
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Destination(
    deepLinks = [
        DeepLink(uriPattern = "tblite://forum/{forumName}")
    ]
)
@Composable
fun ForumPage(
    forumName: String,
    viewModel: ForumViewModel = pageViewModel(),
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    LazyLoad(loaded = viewModel.initialized) {
        viewModel.requestLoadForm(context, forumName)
        viewModel.initialized = true
    }

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
    val pagerState = rememberPagerState { 2 }

    val isGood by remember { derivedStateOf { pagerState.currentPage == TAB_FORUM_GOOD } }

    val coroutineScope = rememberCoroutineScope()

    val unlikeDialogState = rememberDialogState()

    LaunchedEffect(forumInfo) {
        forumInfo?.item?.let { viewModel.saveHistory(it) }
    }

    if (account != null && forumInfo != null) {
        ConfirmDialog(
            dialogState = unlikeDialogState,
            onConfirm = {
                viewModel.send(
                    ForumUiIntent.Unlike(forumInfo!!.get { id }, forumName, tbs ?: account.tbs)
                )
            },
            title = {
                Text(text = stringResource(R.string.title_dialog_unfollow_forum, forumName))
            }
        )
    }

    val connection = rememberCollapseConnection(coroutineScope)
    val collapsed by remember { derivedStateOf { connection.ratio == 0.0f } }

    MyScaffold(
        scaffoldState = scaffoldState,
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
                        Text(text = stringResource(R.string.title_forum, forumName))
                    }
                },
                menuContent = {
                    TextMenuItem(
                        onClick = { viewModel.shareForum(context) },
                        text = stringResource(R.string.title_share)
                    )
                    TextMenuItem(
                        onClick = {
                            forumInfo?.item?.let {
                                viewModel.sendToDesktop(context = context, forum = it)
                            }
                        },
                        text = stringResource(R.string.title_send_to_desktop)
                    )
                    TextMenuItem(
                        onClick = { unlikeDialogState.show() },
                        text = stringResource(R.string.title_unfollow)
                    )
                },
                onBackAction = navigator::navigateUp,
                onSearchAction = {
                    forumInfo?.get { id }?.let {
                        navigator.navigate(ForumSearchPostPageDestination(forumName, it))
                    }
                },
                isLoading = isLoading
            ) {
                val holder: ImmutableHolder<ForumInfo>? = forumInfo
                if (holder == null) {
                    ForumHeaderPlaceholder(forumName)
                    return@ForumToolbar
                }

                this@ForumToolbar.AnimatedVisibility(visible = collapsed) {
                    ForumHeader(
                        forumInfoImmutableHolder = holder,
                        onOpenForumInfo = {
                            navigator.navigate(ForumDetailPageDestination(forumId = holder.get().id))
                        },
                        onFollow = {
                            viewModel.onFollow(holder.get(), tbs ?: account!!.tbs)
                        },
                        onSignIn = {
                            viewModel.onSignIn(holder.get(), tbs ?: account!!.tbs)
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
            val fabFunction = viewModel.fab
            if (fabFunction == ForumFabFunction.HIDE || forumInfo == null) return@MyScaffold

            FloatingActionButton(
                onClick = {
                    viewModel.onFabClicked(context, isGood)
                },
                backgroundColor = ExtendedTheme.colors.windowBackground,
                contentColor = ExtendedTheme.colors.primary,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Icon(
                    imageVector = when (fabFunction) {
                        ForumFabFunction.REFRESH-> Icons.Rounded.Refresh
                        ForumFabFunction.BACK_TO_TOP -> Icons.Rounded.VerticalAlignTop
                        ForumFabFunction.POST -> Icons.Rounded.Add
                        else -> throw IllegalStateException()
                    },
                    contentDescription = fabFunction
                )
            }
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
            modifier = Modifier.nestedScroll(connection).fillMaxSize(),
            key = { it },
            verticalAlignment = Alignment.Top,
            userScrollEnabled = true,
        ) {
            ProvideNavigator(navigator = navigator) {
                ForumThreadListPage(
                    Modifier.nestedScroll(connection),
                    forumId = info.id,
                    forumName = info.name,
                    isGood = isGood,
                    sortType = { viewModel.sortType }
                )
            }
        }
    }
}

@Composable
private fun MenuScope.TextMenuItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String
) = DropdownMenuItem(
    onClick = {
        onClick()
        dismiss()
    },
    modifier = modifier,
    content = { Text(text = text) }
)

@Composable
fun ForumHeaderPlaceholder(forumName: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarPlaceholder(size = Sizes.Large)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.title_forum, forumName),
                    style = MaterialTheme.typography.h6,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (LocalAccount.current != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100))
                        .placeholder(
                            visible = true,
                            highlight = PlaceholderHighlight.fade(),
                        )
                        .padding(horizontal = 18.dp, vertical = 6.dp)
                ) {
                    Text(text = stringResource(id = R.string.button_sign_in), fontSize = 13.sp)
                }
            }
        }
    }
}