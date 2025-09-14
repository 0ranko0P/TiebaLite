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
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.accompanist.placeholder.PlaceholderDefaults
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.isOverlapping
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.putBoolean
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.theme.DefaultDarkColors
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.localSharedBounds
import com.huanchengfly.tieba.post.ui.common.theme.compose.block
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.main.emptyBlurBottomNavigation
import com.huanchengfly.tieba.post.ui.page.search.SearchToolbarSharedBoundsKey
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BlurScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultInputScale
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.ForumAvatarSharedBoundsKey
import com.huanchengfly.tieba.post.ui.widgets.compose.ForumTitleSharedBoundsKey
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyVerticalGrid
import com.huanchengfly.tieba.post.ui.widgets.compose.PositiveButton
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TipScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.TopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.accountNavIconIfCompact
import com.huanchengfly.tieba.post.ui.widgets.compose.color
import com.huanchengfly.tieba.post.ui.widgets.compose.placeholder
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils.Companion.KEY_HOME_PAGE_SHOW_HISTORY
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils.Companion.KEY_HOME_SINGLE_FORUM_LIST
import com.huanchengfly.tieba.post.utils.LocalAccount
import com.huanchengfly.tieba.post.utils.TiebaUtil
import dev.chrisbanes.haze.ExperimentalHazeApi
import kotlinx.collections.immutable.persistentListOf

private val FORUM_AVATAR_SIZE = 40.dp

@Preview("DummySearchBox")
@Composable
private fun DummySearchBoxPreview() {
    Column {
        TiebaLiteTheme {
            SearchBox(onClick = {})
        }

        TiebaLiteTheme(colorSchemeExt = DefaultDarkColors) {
            SearchBox(onClick =  {})
        }
    }
}

@Composable
private fun SearchBox(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
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
            Text(text = stringResource(id = R.string.hint_search), style = MaterialTheme.typography.bodyMedium)
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
private fun ForumItemPlaceholder(showAvatar: Boolean) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = CenterVertically
    ) {
        val placeholderColor = PlaceholderDefaults.color()

        if (showAvatar) {
            Box(
                modifier = Modifier
                    .size(FORUM_AVATAR_SIZE)
                    .placeholder(color = placeholderColor, shape = CircleShape),
            )
            Spacer(modifier = Modifier.width(14.dp))
        }

        Text(
            text = "",
            modifier = Modifier
                .weight(1.0f)
                .placeholder(color = placeholderColor),
            fontSize = 15.sp,
        )

        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .width(54.dp)
                .padding(vertical = 4.dp)
                .placeholder(color = placeholderColor)
        ) {
            Text(text = "0", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun HistoryItem(
    modifier: Modifier = Modifier,
    title: String,
    avatar: @Composable RowScope.() -> Unit,
    color: Color,
    contentColor: Color,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .clip(shape = CircleShape)
            .background(color = color)
            .clickable(onClick = onClick)
            .padding(start = 4.dp, top = 4.dp, end = 8.dp, bottom = 4.dp),
        verticalAlignment = CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        avatar()
        Text(text = title, color = contentColor, style = MaterialTheme.typography.labelMedium)
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
    val colorScheme = MaterialTheme.colorScheme

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
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(items = forums, key = { it.timestamp }) {
                    HistoryItem(
                        title = it.data,
                        avatar = { Avatar(data = it.avatar, size = Sizes.Tiny) },
                        color = colorScheme.surfaceContainer,
                        contentColor = colorScheme.onSurface,
                        onClick = {
                            onHistoryClicked(it)
                        }
                    )
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
            )
        } else {
            Spacer(modifier = Modifier.width(4.dp))
        }

        Text(
            text = item.forumName,
            modifier = Modifier.block { // Enable transition on List Mode (showAvatar) only
                if (showAvatar) {
                    localSharedBounds(key = ForumTitleSharedBoundsKey(item.forumName, null))
                } else {
                    null
                }
            },
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
        )

        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier
                .padding(start = 8.dp)
                .width(54.dp)
                .background(
                    color = MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(3.dp)
                )
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.level,
                color = MaterialTheme.colorScheme.onSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )

            if (item.isSign) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = stringResource(id = R.string.tip_signed),
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.secondary
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
    object Header: ForumType
    object History: ForumType
    object ListItem: ForumType
    object GridItem: ForumType
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class, ExperimentalHazeApi::class)
@Composable
fun HomePage(
    viewModel: HomeViewModel = pageViewModel<HomeUiIntent, HomeViewModel>(listOf(HomeUiIntent.Refresh)),
    canOpenExplore: Boolean = false,
    onOpenExplore: () -> Unit = {},
) {
    val account = LocalAccount.current
    val loggedIn by remember { derivedStateOf { account != null } }
    val context = LocalContext.current
    val navigator = LocalNavController.current
    val gridState = rememberLazyGridState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

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
    unfollowForum?.let {
        ConfirmDialog(
            dialogState = confirmUnfollowDialog,
            onConfirm = {
                viewModel.send(HomeUiIntent.Unfollow(it.forumId, it.forumName))
            },
            onDismiss = { unfollowForum = null },
            title = { Text(text = stringResource(R.string.button_unfollow)) }
        ) {
            Text(text = stringResource(R.string.title_dialog_unfollow_forum, it.forumName))
        }
    }

    LaunchedEffect(Unit) {
        if (viewModel.initialized) viewModel.send(HomeUiIntent.RefreshHistory)
    }

    BlurScaffold(
        topBar = {
            TopAppBar(
                titleRes = R.string.title_main,
                navigationIcon = accountNavIconIfCompact,
                actions = {
                    if (loggedIn) {
                        ActionItem(
                            icon = ImageVector.vectorResource(id = R.drawable.ic_oksign),
                            contentDescription = stringResource(id = R.string.title_oksign)
                        ) {
                            TiebaUtil.startSign(context)
                        }
                    }

                    ActionItem(
                        icon = Icons.Outlined.ViewAgenda,
                        contentDescription = stringResource(id = R.string.title_switch_list_single),
                    ) {
                        context.dataStore.putBoolean(KEY_HOME_SINGLE_FORUM_LIST, !listSingle)
                    }
                },
                scrollBehavior = scrollBehavior
            ) {
                SearchBox(
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .localSharedBounds(key = SearchToolbarSharedBoundsKey)
                    ,
                    onClick = { navigator.navigate(Destination.Search) }
                )
            }
        },
        topHazeBlock = {
            blurEnabled = !isEmpty && scrollBehavior.isOverlapping
            inputScale = DefaultInputScale
        },
        bottomBar = emptyBlurBottomNavigation, // MainPage workaround when enabling BottomBar blurring
        bottomHazeBlock = {
            blurEnabled = !isEmpty && gridState.canScrollForward
            inputScale = DefaultInputScale
        },
    ) { contentPaddings ->
        // Initialize click listeners now
        val onRefreshClick: () -> Unit = remember { { viewModel.send(HomeUiIntent.Refresh) } }
        val onForumClick: (HomeUiState.Forum) -> Unit = {
            navigator.navigate(route = Destination.Forum(it.forumName, it.avatar))
        }

        val onUnfollow: (HomeUiState.Forum) -> Unit = {
            unfollowForum = it
            confirmUnfollowDialog.show()
        }

        StateScreen(
            isEmpty = isEmpty,
            isError = isError,
            isLoading = isLoading,
            modifier = Modifier.fillMaxSize(),
            onReload = onRefreshClick,
            emptyScreen = {
                EmptyScreen(
                    modifier = Modifier.padding(contentPaddings),
                    loggedIn = loggedIn,
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

            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = onRefreshClick,
                contentPadding = contentPaddings
            ) {
                MyLazyVerticalGrid(
                    columns = gridCells,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    state = gridState,
                    contentPadding = contentPaddings,
                ) {
                    if (showHistoryForum) {
                        item(key = ForumType.History.hashCode(), DefaultGridSpan, { ForumType.History }) {
                            HistoryForums(forums = historyForums) {
                                navigator.navigate(Destination.Forum(forumName = it.data))
                            }
                        }
                    }

                    if (hasTopForum) {
                        item(key = "TopForumHeader", span = DefaultGridSpan, { ForumType.Header }) {
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
                        item(key = "ForumHeader", span = DefaultGridSpan, { ForumType.Header }) {
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
                    modifier = Modifier.placeholder(color = MaterialTheme.colorScheme.surfaceContainerHigh),
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
                Text(text = stringResource(R.string.home_empty_login), textAlign = TextAlign.Center)
            }
        },
        actions = {
            if (!loggedIn) {
                val navigator = LocalNavController.current
                PositiveButton(
                    modifier = Modifier.fillMaxWidth(),
                    textRes = R.string.button_login,
                    onClick = { navigator.navigate(Destination.Login) },
                )
            }
            if (canOpenExplore) {
                PositiveButton(
                    onClick = onOpenExplore,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    textRes = R.string.button_go_to_explore
                )
            }
        },
    )
}