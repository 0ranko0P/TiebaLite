package com.huanchengfly.tieba.post.ui.page.main.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.accompanist.placeholder.material.placeholder
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.block
import com.huanchengfly.tieba.post.arch.clickableNoIndication
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.rememberPreferenceAsMutableState
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.theme.DarkAmoledColors
import com.huanchengfly.tieba.post.theme.DefaultColors
import com.huanchengfly.tieba.post.ui.common.localSharedBounds
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.pullRefreshIndicator
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.main.emptyBlurBottomNavigation
import com.huanchengfly.tieba.post.ui.page.search.SearchToolbarSharedBoundsKey
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BlurScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Button
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.ForumAvatarSharedBoundsKey
import com.huanchengfly.tieba.post.ui.widgets.compose.ForumTitleSharedBoundsKey
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MenuState
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyVerticalGrid
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TextButton
import com.huanchengfly.tieba.post.ui.widgets.compose.TipScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.Toolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.accountNavIconIfCompact
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils.Companion.KEY_HOME_PAGE_SHOW_HISTORY
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils.Companion.KEY_HOME_SINGLE_FORUM_LIST
import com.huanchengfly.tieba.post.utils.LocalAccount
import com.huanchengfly.tieba.post.utils.TiebaUtil
import kotlinx.collections.immutable.persistentListOf

private val FORUM_AVATAR_SIZE = 40.dp

@Preview("SearchBoxPreview")
@Composable
fun SearchBoxPreview() {
    Column {
        TiebaLiteTheme(DefaultColors) {
            SearchBox(onClick =  {})
        }

        TiebaLiteTheme(DarkAmoledColors) {
            SearchBox(onClick =  {})
        }
    }
}

@Composable
private fun SearchBox(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        color = ExtendedTheme.colors.floorCard,
        contentColor = LocalContentColor.current,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                modifier = Modifier.size(Sizes.Tiny),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = stringResource(id = R.string.hint_search), fontSize = 14.sp)
        }
    }
}

@Composable
private fun Header(
    text: String,
    modifier: Modifier = Modifier,
    invert: Boolean = false
) {
    Chip(
        text = text,
        modifier = Modifier
            .padding(start = 16.dp)
            .then(modifier),
        invertColor = invert
    )
}

@Composable
private fun ForumItemPlaceholder(
    showAvatar: Boolean,
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = CenterVertically
    ) {
        if (showAvatar) {
            Box(
                modifier = Modifier
                    .size(FORUM_AVATAR_SIZE)
                    .placeholder(color = ExtendedTheme.colors.chip, shape = CircleShape),
            )
            Spacer(modifier = Modifier.width(14.dp))
        }

        Text(
            text = "",
            modifier = Modifier
                .weight(1.0f)
                .placeholder(visible = true, color = ExtendedTheme.colors.chip),
            fontSize = 15.sp,
        )

        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .width(54.dp)
                .padding(vertical = 4.dp)
                .placeholder(color = ExtendedTheme.colors.chip, shape = RoundedCornerShape(3.dp))
        ) {
            Text(text = "0", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ForumItemMenuContent(
    menuState: MenuState,
    isTopForum: Boolean,
    onDeleteTopForum: () -> Unit,
    onAddTopForum: () -> Unit,
    onCopyName: () -> Unit,
    onUnfollow: () -> Unit,
) {
    DropdownMenuItem(
        onClick = {
            if (isTopForum) {
                onDeleteTopForum()
            } else {
                onAddTopForum()
            }
            menuState.expanded = false
        }
    ) {
        if (isTopForum) {
            Text(text = stringResource(id = R.string.menu_top_del))
        } else {
            Text(text = stringResource(id = R.string.menu_top))
        }
    }
    DropdownMenuItem(
        onClick = {
            onCopyName()
            menuState.expanded = false
        }
    ) {
        Text(text = stringResource(id = R.string.title_copy_forum_name))
    }
    DropdownMenuItem(
        onClick = {
            onUnfollow()
            menuState.expanded = false
        }
    ) {
        Text(text = stringResource(id = R.string.button_unfollow))
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ForumItemContent(
    item: HomeUiState.Forum,
    showAvatar: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = CenterVertically
    ) {
        AnimatedVisibility(visible = showAvatar) {
            Row {
                Avatar(
                    modifier = Modifier
                        .localSharedBounds(key = ForumAvatarSharedBoundsKey(item.forumName, null)),
                    data = item.avatar,
                    size = FORUM_AVATAR_SIZE,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(14.dp))
            }
        }
        Text(
            color = ExtendedTheme.colors.text,
            text = item.forumName,
            modifier = Modifier.block { // Enable transition on List Mode (showAvatar) only
                if (showAvatar) {
                    localSharedBounds(key = ForumTitleSharedBoundsKey(item.forumName, null))
                } else {
                    this
                }
            },
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )

        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier
                .padding(start = 8.dp)
                .width(54.dp)
                .background(color = ExtendedTheme.colors.chip, shape = RoundedCornerShape(3.dp))
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = remember { "Lv.${item.levelId}" },
                color = ExtendedTheme.colors.onChip,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )

            if (item.isSign) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = stringResource(id = R.string.tip_signed),
                    modifier = Modifier.size(12.dp),
                    tint = ExtendedTheme.colors.onChip
                )
            }
        }
    }
}

@Composable
private fun ForumItem(
    item: HomeUiState.Forum,
    showAvatar: Boolean,
    onClick: (HomeUiState.Forum) -> Unit,
    onUnfollow: (HomeUiState.Forum) -> Unit,
    onAddTopForum: (HomeUiState.Forum) -> Unit,
    onDeleteTopForum: (HomeUiState.Forum) -> Unit,
    isTopForum: Boolean = false,
) {
    val context = LocalContext.current
    val menuState = rememberMenuState()
    LongClickMenu(
        menuContent = {
            ForumItemMenuContent(
                menuState = menuState,
                isTopForum = isTopForum,
                onDeleteTopForum = { onDeleteTopForum(item) },
                onAddTopForum = { onAddTopForum(item) },
                onCopyName = {
                    TiebaUtil.copyText(context, item.forumName)
                },
                onUnfollow = { onUnfollow(item) }
            )
        },
        menuState = menuState,
        onClick = {
            onClick(item)
        }
    ) {
        ForumItemContent(item = item, showAvatar = showAvatar)
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HomePage(
    viewModel: HomeViewModel = pageViewModel<HomeUiIntent, HomeViewModel>(listOf(HomeUiIntent.Refresh)),
    canOpenExplore: Boolean = false,
    onOpenExplore: () -> Unit = {},
) {
    val account = LocalAccount.current
    val context = LocalContext.current
    val navigator = LocalNavController.current
    val gridState = rememberLazyGridState()

    val isLoading by viewModel.uiState.collectPartialAsState(
        prop1 = HomeUiState::isLoading,
        initial = true
    )
    val forums by viewModel.uiState.collectPartialAsState(
        prop1 = HomeUiState::forums,
        initial = persistentListOf()
    )
    val topForums by viewModel.uiState.collectPartialAsState(
        prop1 = HomeUiState::topForums,
        initial = persistentListOf()
    )
    val historyForums by viewModel.uiState.collectPartialAsState(
        prop1 = HomeUiState::historyForums,
        initial = persistentListOf()
    )
    val expandHistoryForum by viewModel.uiState.collectPartialAsState(
        prop1 = HomeUiState::expandHistoryForum,
        initial = true
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = HomeUiState::error,
        initial = null
    )
    val isEmpty by remember { derivedStateOf { forums.isEmpty() } }
    val hasTopForum by remember { derivedStateOf { topForums.isNotEmpty() } }

    var listSingle by rememberPreferenceAsMutableState(
        key = booleanPreferencesKey(KEY_HOME_SINGLE_FORUM_LIST),
        defaultValue = false
    )
    val gridCells = if (listSingle) GridCells.Fixed(1) else GridCells.Adaptive(180.dp)

    val isError by remember { derivedStateOf { error != null } }

    onGlobalEvent<GlobalEvent.Refresh>(
        filter = { it.key == "home" }
    ) {
        viewModel.send(HomeUiIntent.Refresh)
    }

    var unfollowForum by remember { mutableStateOf<HomeUiState.Forum?>(null) }
    val confirmUnfollowDialog = rememberDialogState()
    ConfirmDialog(
        dialogState = confirmUnfollowDialog,
        onConfirm = {
            unfollowForum?.let {
                viewModel.send(HomeUiIntent.Unfollow(it.forumId, it.forumName))
            }
        },
    ) {
        Text(
            text = stringResource(
                id = R.string.title_dialog_unfollow_forum,
                unfollowForum?.forumName.orEmpty()
            )
        )
    }

    LaunchedEffect(Unit) {
        if (viewModel.initialized) viewModel.send(HomeUiIntent.RefreshHistory)
    }

    BlurScaffold(
        backgroundColor = Color.Transparent,
        topHazeBlock = remember { {
            blurEnabled = !isEmpty && gridState.canScrollBackward
        } },
        bottomHazeBlock = remember { {
            blurEnabled = !isEmpty
        } },
        topBar = {
            Toolbar(
                modifier = Modifier.localSharedBounds(key = SearchToolbarSharedBoundsKey),
                title = stringResource(id = R.string.title_main),
                navigationIcon = accountNavIconIfCompact(),
                actions = {
                    ActionItem(
                        icon = ImageVector.vectorResource(id = R.drawable.ic_oksign),
                        contentDescription = stringResource(id = R.string.title_oksign)
                    ) {
                        TiebaUtil.startSign(context)
                    }
                    ActionItem(
                        icon = Icons.Outlined.ViewAgenda,
                        contentDescription = stringResource(id = R.string.title_switch_list_single),
                        onClick = { listSingle = !listSingle }
                    )
                },
                elevation = Dp.Hairline
            ) {
                SearchBox(
                    modifier = Modifier.padding(bottom = 4.dp),
                    onClick = { navigator.navigate(Destination.Search) }
                )
            }
        },
        bottomBar = emptyBlurBottomNavigation,
        modifier = Modifier.fillMaxSize(),
    ) { contentPaddings ->
        val pullRefreshState = rememberPullRefreshState(
            refreshing = isLoading,
            onRefresh = { viewModel.send(HomeUiIntent.Refresh) }
        )

        val onForumClick: (HomeUiState.Forum) -> Unit = remember { {
            navigator.navigate(Destination.Forum(it.forumName, it.avatar))
        } }

        Box(
            modifier = Modifier.pullRefresh(pullRefreshState)
        ) {
            StateScreen(
                isEmpty = isEmpty,
                isError = isError,
                isLoading = isLoading,
                modifier = Modifier.fillMaxSize(),
                onReload = {
                    viewModel.send(HomeUiIntent.Refresh)
                },
                emptyScreen = {
                    EmptyScreen(
                        modifier = Modifier.padding(contentPaddings),
                        loggedIn = account != null,
                        canOpenExplore = canOpenExplore,
                        onOpenExplore = onOpenExplore
                    )
                },
                loadingScreen = {
                    HomePageSkeletonScreen(Modifier.padding(contentPaddings), listSingle, gridCells)
                },
                errorScreen = {
                    ErrorScreen(error = error, Modifier.padding(contentPaddings))
                }
            ) {
                val showHistoryOnHome by rememberPreferenceAsState(booleanPreferencesKey(KEY_HOME_PAGE_SHOW_HISTORY), true)
                val showHistoryForum by remember { derivedStateOf {
                    showHistoryOnHome && historyForums.isNotEmpty()
                } }

                MyLazyVerticalGrid(
                    columns = gridCells,
                    modifier = Modifier.fillMaxSize(),
                    state = gridState,
                    contentPadding = contentPaddings,
                ) {
                    if (showHistoryForum) {
                        item(key = "HistoryForums", span = { GridItemSpan(maxLineSpan) }) {
                            val rotate by animateFloatAsState(
                                targetValue = if (expandHistoryForum) 90f else 0f,
                                label = "rotate"
                            )
                            Column {
                                Row(
                                    verticalAlignment = CenterVertically,
                                    modifier = Modifier
                                        .clickableNoIndication {
                                            viewModel.send(
                                                HomeUiIntent.ToggleHistory(expandHistoryForum)
                                            )
                                        }
                                        .padding(vertical = 8.dp)
                                        .padding(end = 16.dp)
                                ) {
                                    Header(
                                        text = stringResource(id = R.string.title_history_forum),
                                        invert = false
                                    )

                                    Spacer(modifier = Modifier.weight(1f))

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = stringResource(id = R.string.desc_show),
                                        modifier = Modifier
                                            .size(24.dp)
                                            .rotate(rotate)
                                    )
                                }
                                AnimatedVisibility(visible = expandHistoryForum) {
                                    LazyRow(
                                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 8.dp),
                                    ) {
                                        items(
                                            historyForums,
                                            key = { it.data }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .padding(horizontal = 4.dp)
                                                    .height(IntrinsicSize.Min)
                                                    .clip(RoundedCornerShape(100))
                                                    .background(color = ExtendedTheme.colors.chip)
                                                    .clickable {
                                                        navigator.navigate(Destination.Forum(forumName = it.data))
                                                    }
                                                    .padding(4.dp),
                                                verticalAlignment = CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Avatar(
                                                    data = it.avatar,
                                                    contentDescription = null,
                                                    size = Sizes.Tiny,
                                                    shape = CircleShape
                                                )
                                                Text(
                                                    text = it.title,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(end = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (hasTopForum) {
                        item(key = "TopForumHeader", span = { GridItemSpan(maxLineSpan) }) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Header(text = stringResource(id = R.string.title_top_forum), invert = true)
                            }
                        }
                        items(
                            items = topForums,
                            key = { "Top${it.forumId}" }
                        ) { item ->
                            ForumItem(
                                item,
                                listSingle,
                                onClick = onForumClick,
                                onUnfollow = {
                                    unfollowForum = it
                                    confirmUnfollowDialog.show()
                                },
                                onAddTopForum = {
                                    viewModel.send(HomeUiIntent.TopForums.Add(it))
                                },
                                onDeleteTopForum = {
                                    viewModel.send(HomeUiIntent.TopForums.Delete(it.forumId))
                                },
                                isTopForum = true
                            )
                        }
                    }
                    if (showHistoryForum || hasTopForum) {
                        item(key = "ForumHeader", span = { GridItemSpan(maxLineSpan) }) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Header(text = stringResource(id = R.string.forum_list_title))
                            }
                        }
                    }

                    items(items = forums, key = { it.forumId }) { item ->
                        ForumItem(
                            item,
                            listSingle,
                            onClick = onForumClick,
                            onUnfollow = {
                                unfollowForum = it
                                confirmUnfollowDialog.show()
                            },
                            onAddTopForum = {
                                viewModel.send(HomeUiIntent.TopForums.Add(it))
                            },
                            onDeleteTopForum = {
                                viewModel.send(HomeUiIntent.TopForums.Delete(it.forumId))
                            }
                        )
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = isLoading,
                state = pullRefreshState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(contentPaddings),
                backgroundColor = ExtendedTheme.colors.pullRefreshIndicator,
                contentColor = ExtendedTheme.colors.primary,
            )
        }
    }
}

@Composable
private fun HomePageSkeletonScreen(
    modifier: Modifier = Modifier,
    listSingle: Boolean,
    gridCells: GridCells
) {
    MyLazyVerticalGrid(
        columns = gridCells,
        modifier = modifier
    ) {
        item(key = "TopForumHeaderPlaceholder", span = { GridItemSpan(maxLineSpan) }) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Header(
                    text = stringResource(id = R.string.title_top_forum),
                    modifier = Modifier.placeholder(color = ExtendedTheme.colors.chip),
                    invert = true
                )
            }
        }
        items(6, key = { "TopPlaceholder$it" }) {
            ForumItemPlaceholder(listSingle)
        }

        item(key = "Spacer", span = { GridItemSpan(maxLineSpan) }) {
            Spacer(modifier = Modifier.height(16.dp))
        }

        item(key = "ForumHeaderPlaceholder", span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Header(
                    text = stringResource(id = R.string.forum_list_title),
                    modifier = Modifier.placeholder(color = ExtendedTheme.colors.chip),
                    invert = true
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        items(12, key = { "Placeholder$it" }) {
            ForumItemPlaceholder(listSingle)
        }
    }
}

@Composable
fun EmptyScreen(
    modifier: Modifier = Modifier,
    loggedIn: Boolean,
    canOpenExplore: Boolean,
    onOpenExplore: () -> Unit
) {
    val navigator = LocalNavController.current
    TipScreen(
        title = {
            if (!loggedIn) {
                Text(text = stringResource(id = R.string.title_empty_login))
            } else {
                Text(text = stringResource(id = R.string.title_empty))
            }
        },
        modifier = modifier,
        image = {
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_astronaut))
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f)
            )
        },
        message = {
            if (!loggedIn) {
                Text(
                    text = stringResource(id = R.string.home_empty_login),
                    style = MaterialTheme.typography.body1,
                    color = ExtendedTheme.colors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        },
        actions = {
            if (!loggedIn) {
                Button(
                    onClick = { navigator.navigate(Destination.Login) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.button_login))
                }
            }
            if (canOpenExplore) {
                TextButton(
                    onClick = onOpenExplore,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.button_go_to_explore))
                }
            }
        },
    )
}