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
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.accompanist.placeholder.PlaceholderDefaults
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.theme.DefaultDarkColors
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.localSharedBounds
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.common.theme.compose.onCase
import com.huanchengfly.tieba.post.ui.models.LikedForum
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
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.LocalAccount
import com.huanchengfly.tieba.post.utils.TiebaUtil
import dev.chrisbanes.haze.ExperimentalHazeApi

private val FORUM_AVATAR_SIZE = 40.dp

@Preview("DummySearchBox")
@Composable
private fun DummySearchBoxPreview() {
    Column {
        TiebaLiteTheme {
            DummySearchBox(onClick = {})
        }

        TiebaLiteTheme(colorSchemeExt = DefaultDarkColors) {
            DummySearchBox(onClick =  {})
        }
    }
}

@Composable
private fun DummySearchBox(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        tonalElevation = 6.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
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
        verticalAlignment = Alignment.CenterVertically
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
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        avatar()
        Text(text = title, color = contentColor, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun HistoryRow(modifier: Modifier = Modifier, history: List<History>, onClick: (History) -> Unit) {
    var expandHistoryForum by rememberSaveable { mutableStateOf(true) }

    val degrees by animateFloatAsState(
        targetValue = if (expandHistoryForum) 90f else 0f,
        label = "ExpandRotateAnim"
    )
    val colorScheme = MaterialTheme.colorScheme

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
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
                items(items = history, key = { it.id }) {
                    HistoryItem(
                        title = it.name,
                        avatar = { Avatar(data = it.avatar, size = Sizes.Tiny) },
                        color = colorScheme.surfaceContainer,
                        contentColor = colorScheme.onSurface,
                        onClick = { onClick(it) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@NonRestartableComposable
@Composable
private fun ForumItemContent(forum: LikedForum, showAvatar: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showAvatar) {
            Avatar(
                data = forum.avatar,
                modifier = Modifier
                    .padding(end = 14.dp)
                    .size(FORUM_AVATAR_SIZE)
                    .localSharedBounds(key = ForumAvatarSharedBoundsKey(forum.name, null)),
            )
        }

        Box(modifier = Modifier.weight(1.0f)) {  // Boxing for transition animation
            Text(
                text = forum.name,
                modifier = Modifier.onCase(showAvatar) { // Enable transition on List Mode (showAvatar)
                    localSharedBounds(key = ForumTitleSharedBoundsKey(forum.name, null))
                },
                overflow = TextOverflow.MiddleEllipsis,
                maxLines = 1,
                style = MaterialTheme.typography.titleSmall
            )
        }

        Surface(
            modifier = Modifier.width(54.dp),
            shape = MaterialTheme.shapes.extraSmall,
            color = MaterialTheme.colorScheme.secondary,
        ) {
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = forum.level, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                if (forum.signed) {
                    Spacer(modifier = Modifier.width(4.dp))

                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = stringResource(id = R.string.tip_signed),
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ForumItem(
    modifier: Modifier = Modifier,
    forum: LikedForum,
    showAvatar: Boolean,
    onClick: (forum: LikedForum) -> Unit,
    onUnfollow: (forum: LikedForum) -> Unit,
    onPinnedForumChanged: (forum: LikedForum, isTop: Boolean) -> Unit,
    isTopForum: Boolean = false,
) {
    val context = LocalContext.current
    LongClickMenu(
        menuContent = {
            TextMenuItem(text = if (isTopForum) R.string.menu_top_del else R.string.menu_top) {
                onPinnedForumChanged(forum, !isTopForum)
            }
            TextMenuItem(text = R.string.title_copy_forum_name) {
                TiebaUtil.copyText(context, forum.name)
            }
            TextMenuItem(text = R.string.button_unfollow) {
                onUnfollow(forum)
            }
        },
        modifier = modifier,
        onClick = { onClick(forum) }
    ) {
        ForumItemContent(forum, showAvatar)
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
    viewModel: HomeViewModel = hiltViewModel<HomeViewModel>(),
    onOpenExplore: () -> Unit = {},
) {
    val account = LocalAccount.current
    val loggedIn by remember { derivedStateOf { account != null } }
    val context = LocalContext.current
    val navigator = LocalNavController.current
    val gridState = rememberLazyGridState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    var unfollowForum by remember { mutableStateOf<LikedForum?>(null) }
    val confirmUnfollowDialog = rememberDialogState()
    unfollowForum?.let {
        ConfirmDialog(
            dialogState = confirmUnfollowDialog,
            onConfirm = {
                viewModel.onDislikeForum(forum = it)
            },
            onDismiss = { unfollowForum = null },
            title = { Text(text = stringResource(R.string.button_unfollow)) }
        ) {
            Text(text = stringResource(R.string.title_dialog_unfollow_forum, it.name))
        }
    }

    val forumLists by viewModel.forumListsFlow.collectAsStateWithLifecycle()
    val (topForums, forums) = forumLists
    val hasTopForum = topForums.isNotEmpty()
    val isEmpty = !hasTopForum && forums.isEmpty()

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

                        ActionItem(
                            icon = Icons.Outlined.ViewAgenda,
                            contentDescription = stringResource(id = R.string.title_switch_list_single),
                            onClick = viewModel::onListModeChanged
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            ) {
                DummySearchBox(
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .localSharedBounds(key = SearchToolbarSharedBoundsKey),
                    onClick = { navigator.navigate(Destination.Search) }
                )
            }
        },
        topHazeBlock = {
            blurEnabled = !isEmpty && gridState.canScrollBackward
            inputScale = DefaultInputScale
        },
        bottomBar = emptyBlurBottomNavigation, // MainPage workaround when enabling BottomBar blurring
        bottomHazeBlock = {
            blurEnabled = !isEmpty && gridState.canScrollForward
            inputScale = DefaultInputScale
        },
    ) { contentPaddings ->

        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val listSingle = uiState.listSingle
        val gridCells = remember(listSingle) {
            if (listSingle) GridCells.Fixed(1) else GridCells.Adaptive(180.dp)
        }

        val contentType: (item: LikedForum) -> ForumType = {
            if (uiState.listSingle) ForumType.ListItem else ForumType.GridItem
        }

        // Initialize click listeners now
        val onForumClickedListener: (LikedForum) -> Unit = {
            navigator.navigate(route = Destination.Forum(forumName = it.name, avatar = it.avatar))
        }
        val onHistoryClickedListener: (History) -> Unit = {
            navigator.navigate(route = Destination.Forum(forumName = it.name))
        }

        val onUnfollow: (LikedForum) -> Unit = {
            unfollowForum = it
            confirmUnfollowDialog.show()
        }

        StateScreen(
            isEmpty = isEmpty,
            isError = uiState.error != null,
            isLoading = uiState.isLoading,
            modifier = Modifier.fillMaxSize(),
            onReload = viewModel::onRefresh,
            emptyScreen = {
                EmptyScreen(
                    modifier = Modifier.padding(contentPaddings),
                    loggedIn = loggedIn,
                    onOpenExplore = onOpenExplore
                )
            },
            loadingScreen = {
                HomePageSkeletonScreen(Modifier.padding(contentPaddings), listSingle, gridCells)
            },
            errorScreen = {
                ErrorScreen(error = uiState.error, Modifier.padding(contentPaddings))
            }
        ) {
            val history by viewModel.historyFlow.collectAsStateWithLifecycle()

            PullToRefreshBox(
                isRefreshing = uiState.isLoading,
                onRefresh = viewModel::onRefresh,
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
                    history?.takeUnless { it.isEmpty() }?.let {
                        item(key = ForumType.History.hashCode(), DefaultGridSpan, { ForumType.History }) {
                            HistoryRow(history = it, onClick = onHistoryClickedListener)
                        }
                    }

                    if (hasTopForum) {
                        item(key = R.string.title_top_forum, DefaultGridSpan, { ForumType.Header }) {
                            Header(
                                text = stringResource(id = R.string.title_top_forum),
                                modifier = Modifier.padding(vertical = 8.dp),
                                invert = true
                            )
                        }
                        items(items = topForums, key = { it.id }, contentType = contentType) {
                            ForumItem(
                                modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
                                forum = it,
                                showAvatar = listSingle,
                                onClick = onForumClickedListener,
                                onUnfollow = onUnfollow,
                                onPinnedForumChanged = viewModel::onPinnedForumChanged,
                                isTopForum = true
                            )
                        }
                    }
                    if (!history.isNullOrEmpty() || hasTopForum) {
                        item(key = R.string.forum_list_title, DefaultGridSpan, { ForumType.Header }) {
                            Header(text = stringResource(id = R.string.forum_list_title))
                        }
                    }

                    items(items = forums, key = { it.id }, contentType = contentType) {
                        ForumItem(
                            modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
                            forum = it,
                            showAvatar = listSingle,
                            onClick = onForumClickedListener,
                            onUnfollow = onUnfollow,
                            onPinnedForumChanged = viewModel::onPinnedForumChanged,
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
        items(24, key = { it }) {
            ForumItemPlaceholder(listSingle)
        }
    }
}

@Composable
fun EmptyScreen(
    modifier: Modifier = Modifier,
    loggedIn: Boolean,
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
            PositiveButton(
                onClick = onOpenExplore,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.filledTonalButtonColors(),
                textRes = R.string.button_go_to_explore
            )
        },
    )
}