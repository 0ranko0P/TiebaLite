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
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.arch.block
import com.huanchengfly.tieba.post.arch.clickableNoIndication
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.putBoolean
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
import java.util.Objects

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
private fun Header(text: String, modifier: Modifier = Modifier, invert: Boolean = false) {
    Box(modifier = modifier) {
        Chip(text = text, modifier = Modifier.padding(start = 16.dp), invertColor = invert)
    }
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
private fun HistoryForums(
    modifier: Modifier = Modifier,
    forums: List<History>,
    onHistoryClicked: (History) -> Unit
) {
    var expandHistoryForum by rememberSaveable { mutableStateOf(true) }

    val degrees by animateFloatAsState(
        targetValue = if (expandHistoryForum) 90f else 0f,
        label = "ExpandRotateAnim"
    )
    Column(modifier = modifier) {
        Row(
            verticalAlignment = CenterVertically,
            modifier = Modifier
                .clickableNoIndication { expandHistoryForum = !expandHistoryForum }
                .padding(vertical = 8.dp)
                .padding(end = 16.dp)
        ) {
            Header(text = stringResource(id = R.string.title_history_forum))

            Spacer(modifier = Modifier.weight(1f))

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = stringResource(id = R.string.desc_show),
                modifier = Modifier.graphicsLayer {
                    rotationZ = degrees
                }
            )
        }

        AnimatedVisibility(visible = expandHistoryForum) {
            LazyRow(
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 8.dp),
            ) {
                items(items = forums, key = { it.timestamp }) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(IntrinsicSize.Min)
                            .clip(CircleShape)
                            .background(color = ExtendedTheme.colors.chip)
                            .clickable {
                                onHistoryClicked(it)
                            }
                            .padding(4.dp),
                        verticalAlignment = CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Avatar(
                            data = it.avatar,
                            contentDescription = null,
                            size = Sizes.Tiny
                        )
                        Text(
                            text = it.data,
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
        if (showAvatar) {
            Avatar(
                data = item.avatar,
                modifier = Modifier
                    .padding(end = 14.dp)
                    .size(FORUM_AVATAR_SIZE)
                    .localSharedBounds(key = ForumAvatarSharedBoundsKey(item.forumName, null)),
                transition = null
            )
        } else {
            Spacer(modifier = Modifier.width(4.dp))
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
                text = item.level,
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
    modifier: Modifier = Modifier,
    item: HomeUiState.Forum,
    showAvatar: Boolean,
    onClick: (HomeUiState.Forum) -> Unit,
    onUnfollow: (HomeUiState.Forum) -> Unit,
    onTopStateChanged: (HomeUiState.Forum, Boolean) -> Unit,
    isTopForum: Boolean = false,
) {
    val context = LocalContext.current
    val menuState = rememberMenuState()
    LongClickMenu(
        menuContent = {
            TextMenuItem(text = if (isTopForum) R.string.menu_top_del else R.string.menu_top) {
                onTopStateChanged(item, isTopForum)
            }
            TextMenuItem(text = R.string.title_copy_forum_name) {
                TiebaUtil.copyText(context, item.forumName)
            }
            TextMenuItem(text = R.string.button_unfollow) {
                onUnfollow(item)
            }
        },
        modifier = modifier,
        menuState = menuState,
        onClick = { onClick(item) }
    ) {
        ForumItemContent(item = item, showAvatar = showAvatar)
    }
}

private val DefaultGridSpan: LazyGridItemSpanScope.() -> GridItemSpan = {
    GridItemSpan(maxLineSpan)
}

private sealed interface ForumType {
    object History: ForumType
    object ListItem: ForumType
    object GridItem: ForumType
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
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = HomeUiState::error,
        initial = null
    )
    val isEmpty by remember { derivedStateOf { forums.isEmpty() } }
    val hasTopForum by remember { derivedStateOf { topForums.isNotEmpty() } }

    val listSingle by rememberPreferenceAsState(
        key = booleanPreferencesKey(KEY_HOME_SINGLE_FORUM_LIST),
        defaultValue = false
    )
    val gridCells = if (listSingle) GridCells.Fixed(1) else GridCells.Adaptive(180.dp)

    val isError by remember { // Show EmptyScreen when not logged in
        derivedStateOf { error != null && error !is TiebaNotLoggedInException }
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
        onDismiss = { unfollowForum = null }
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
        topHazeBlock = {
            blurEnabled = !isEmpty && gridState.canScrollBackward
        },
        bottomHazeBlock = {
            blurEnabled = !isEmpty
        },
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
                    ) {
                        context.dataStore.putBoolean(KEY_HOME_SINGLE_FORUM_LIST, !listSingle)
                    }
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

        val onRefreshClick: () -> Unit = remember { { viewModel.send(HomeUiIntent.Refresh) } }
        val onForumClick: (HomeUiState.Forum) -> Unit = remember { {
            navigator.navigate(route = Destination.Forum(it.forumName, it.avatar))
        } }

        val onUnfollow: (HomeUiState.Forum) -> Unit = remember { {
            unfollowForum = it
            confirmUnfollowDialog.show()
        } }

        val pullRefreshState = rememberPullRefreshState(refreshing = isLoading, onRefreshClick)

        Box(
            modifier = Modifier.pullRefresh(pullRefreshState)
        ) {
            StateScreen(
                isEmpty = isEmpty,
                isError = isError,
                isLoading = isLoading,
                modifier = Modifier.fillMaxSize(),
                onReload = onRefreshClick,
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

                val contentType: (item: HomeUiState.Forum) -> ForumType = remember {
                    { if (listSingle) ForumType.ListItem else ForumType.GridItem }
                }

                MyLazyVerticalGrid(
                    columns = gridCells,
                    modifier = Modifier.fillMaxSize(),
                    state = gridState,
                    contentPadding = contentPaddings,
                ) {
                    if (showHistoryForum) {
                        item(key = Objects.hashCode(ForumType.History), DefaultGridSpan, { ForumType.History }) {
                            HistoryForums(forums = historyForums) {
                                navigator.navigate(Destination.Forum(forumName = it.data))
                            }
                        }
                    }

                    if (hasTopForum) {
                        item(key = "TopForumHeader", span = DefaultGridSpan) {
                            Header(
                                text = stringResource(id = R.string.title_top_forum),
                                modifier = Modifier.padding(vertical = 8.dp),
                                invert = true
                            )
                        }
                        items(items = topForums, key = { it.forumId }, contentType = contentType) {
                            ForumItem(
                                modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
                                item = it,
                                showAvatar = listSingle,
                                onClick = onForumClick,
                                onUnfollow = onUnfollow,
                                onTopStateChanged = viewModel::onTopStateChanged,
                                isTopForum = true
                            )
                        }
                    }
                    if (showHistoryForum || hasTopForum) {
                        item(key = "ForumHeader", span = DefaultGridSpan) {
                            Header(text = stringResource(id = R.string.forum_list_title))
                        }
                    }

                    items(items = forums, key = { it.forumId }, contentType = contentType) {
                        ForumItem(
                            modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
                            item = it,
                            showAvatar = listSingle,
                            onClick = onForumClick,
                            onUnfollow = onUnfollow,
                            onTopStateChanged = viewModel::onTopStateChanged,
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
        item(key = "TopForumHeaderPlaceholder", span = DefaultGridSpan) {
            Header(
                text = stringResource(id = R.string.title_top_forum),
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .placeholder(color = ExtendedTheme.colors.chip),
            )
        }
        items(6, key = { "TopPlaceholder$it" }) {
            ForumItemPlaceholder(listSingle)
        }

        item(key = "Spacer", span = DefaultGridSpan) {
            Spacer(modifier = Modifier.height(16.dp))
        }

        item(key = "ForumHeaderPlaceholder", span = DefaultGridSpan) {
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
                Text(text = stringResource(id = R.string.title_not_logged_in))
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