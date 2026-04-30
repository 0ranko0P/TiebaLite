package com.huanchengfly.tieba.post.ui.page.history

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemElevation
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateSet
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.isScrolling
import com.huanchengfly.tieba.post.models.database.ForumHistory
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.models.database.ThreadHistory
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.plus
import com.huanchengfly.tieba.post.repository.UserHistory
import com.huanchengfly.tieba.post.theme.ProvideContentColorTextStyle
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.ForumAvatarSharedBoundsKey
import com.huanchengfly.tieba.post.ui.ForumTitleSharedBoundsKey
import com.huanchengfly.tieba.post.ui.common.FadedVisibility
import com.huanchengfly.tieba.post.ui.common.LocalAnimatedVisibilityScope
import com.huanchengfly.tieba.post.ui.common.LocalSharedTransitionScope
import com.huanchengfly.tieba.post.ui.common.animateEnterExit
import com.huanchengfly.tieba.post.ui.common.localSharedBounds
import com.huanchengfly.tieba.post.ui.common.theme.compose.onCase
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.thread.ThreadFrom
import com.huanchengfly.tieba.post.ui.page.user.sharedUserAvatar
import com.huanchengfly.tieba.post.ui.page.user.sharedUserNickname
import com.huanchengfly.tieba.post.ui.page.user.sharedUsername
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultBackToTopFAB
import com.huanchengfly.tieba.post.ui.widgets.compose.DeleteIconButton
import com.huanchengfly.tieba.post.ui.widgets.compose.ExtendedFabHeight
import com.huanchengfly.tieba.post.ui.widgets.compose.FancyAnimatedIndicatorWithModifier
import com.huanchengfly.tieba.post.ui.widgets.compose.MoreMenuItem
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TopAppBarPaged
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeaderPlaceholder
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SegmentedListItemColors
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberPagerListStates
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberSnackbarHostState
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import kotlinx.coroutines.launch

/**
 * Date Header -> Null
 * History Item -> String
 * else -> [androidx.paging.compose.PagingPlaceholderContentType]
 * */
private const val HistoryItemContentType = "history"

@Composable
fun HistoryPage(
    navigator: NavController,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = rememberSnackbarHostState()
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val scaffoldContentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp + ExtendedFabHeight)

    val tabs = remember {
        listOf(
            R.string.title_history_thread,
            R.string.title_history_forum,
            R.string.title_history_user,
        )
    }

    val onHistoryClicked: (History) -> Unit = {
        val route = when(it) {
            is ThreadHistory -> {
                Destination.Thread(threadId = it.id, postId = it.pid, seeLz = it.isSeeLz, from = ThreadFrom.History)
            }

            is ForumHistory -> Destination.Forum(forumName = it.name, avatar = it.avatar)

            is UserHistory -> {
                Destination.UserProfile(
                    uid = it.id,
                    avatar = it.avatar,
                    nickname = it.name,
                    username = it.username,
                    transitionKey = it.id.toString(), // Avoid duplicate nickname
                    recordHistory = false
                )
            }

            else -> throw RuntimeException("Unknow history type: ${ it::class.simpleName }")
        }
        navigator.navigateDebounced(route = route)
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val isUpdating by viewModel.updating.collectAsStateWithLifecycle()

    val selectedItems = remember { mutableStateSetOf<History>() }
    var selectMode by remember { mutableStateOf(false) }
    BackHandler(enabled = selectMode) {
        selectMode = false
        selectedItems.clear()
    }

    val listStates = rememberPagerListStates(tabs.size)
    val pagerState = rememberPagerState { tabs.size }

    MyScaffold(
        topBar = {
            TopAppBarPaged(
                modifier = Modifier.animateEnterExit(
                    animatedVisibilityScope = LocalAnimatedVisibilityScope.current,
                    sharedTransitionScope = sharedTransitionScope,
                ),
                title = { Text(text = stringResource(R.string.title_history)) },
                navigationIcon = {
                    if (sharedTransitionScope?.isTransitionActive != true) {
                        BackNavigationIcon(onBackPressed = navigator::navigateUp)
                    }
                },
                actions = {
                    val deleteVisible by remember { derivedStateOf { isUpdating || selectMode } }
                    FadedVisibility(visible = deleteVisible) {
                        DeleteIconButton(deleting = isUpdating, enabled = selectedItems.isNotEmpty()) {
                            selectMode = false
                            viewModel.onDelete(selectedItems.toList())
                            selectedItems.clear()
                        }
                    }

                    ClickMenu(
                        menuContent = {
                            TextIconMenuItem(
                                text = stringResource(R.string.button_clear_all),
                                icon = Icons.Rounded.DeleteSweep,
                                enabled = !isUpdating,
                            ) {
                                viewModel.onDeleteAll()
                                context.toastShort(R.string.toast_clear_success)
                                navigator.navigateUp()
                            }
                        },
                        triggerShape = CircleShape,
                        content = MoreMenuItem,
                    )
                },
                scrollBehavior = scrollBehavior,
                canScrollBackward = {
                    sharedTransitionScope?.isTransitionActive != true && listStates[pagerState.currentPage].canScrollBackward
                },
                content = {
                    AnimatedVisibility(visible = !selectMode) {
                        PrimaryTabRow(
                            selectedTabIndex = pagerState.currentPage,
                            indicator = {
                                FancyAnimatedIndicatorWithModifier(pagerState.currentPage)
                            },
                            containerColor = Color.Transparent,
                        ) {
                            tabs.fastForEachIndexed { i, title ->
                                Tab(
                                    text = {
                                        Text(text = stringResource(id = title), letterSpacing = 0.75.sp)
                                    },
                                    selected = pagerState.currentPage == i,
                                    onClick = {
                                        coroutineScope.launch { pagerState.animateScrollToPage(i) }
                                    },
                                    unselectedContentColor = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                },
            )
        },
        snackbarHostState = snackbarHostState,
        floatingActionButton = {
            val visible by remember {
                derivedStateOf { !pagerState.isScrolling && listStates[pagerState.currentPage].canScrollBackward }
            }
            DefaultBackToTopFAB(visible = visible) {
                coroutineScope.launch {
                    listStates[pagerState.currentPage].scrollToItem(0)
                    scrollBehavior.state.contentOffset = 0f
                    scrollBehavior.state.heightOffset = 0f
                }
            }
        },
    ) { paddingValues ->
        val contentPadding = paddingValues + scaffoldContentPadding

        ProvideNavigator(navigator = navigator) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.onCase(!selectMode) {
                    nestedScroll(scrollBehavior.nestedScrollConnection)
                },
                key = { it },
                userScrollEnabled = !selectMode
            ) { index ->
                val pagingData = when (tabs[index]) {
                    R.string.title_history_thread -> viewModel.threadHistory

                    R.string.title_history_forum -> viewModel.forumHistory

                    R.string.title_history_user -> viewModel.userHistory

                    else -> throw RuntimeException()
                }

                HistoryColumn(
                    state = listStates[index],
                    contentPadding = contentPadding,
                    pagedItems = pagingData.collectAsLazyPagingItems(),
                    selectedItems = selectedItems,
                    onClick = { it: History ->
                        if (selectMode) {
                            if (selectedItems.contains(it)) selectedItems -= it else selectedItems += it
                        } else {
                            onHistoryClicked(it)
                        }
                    },
                    onLongClick = { history ->
                        if (!isUpdating && !selectMode) {
                            selectedItems += history
                            selectMode = true
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TimeText(modifier: Modifier = Modifier, time: Long) {
    val context = LocalContext.current
    Text(
        text = remember(time) { DateTimeUtils.getRelativeTimeString(context, time) },
        modifier = modifier
    )
}

@Composable
private fun HistoryBaseItem(
    modifier: Modifier = Modifier,
    selected: Boolean,
    avatar: @Composable BoxScope.() -> Unit,
    name: @Composable () -> Unit,
    etc: (@Composable () -> Unit)? = null,
    time: @Composable () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(Sizes.Small), contentAlignment = Alignment.Center) {
            avatar()
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(0.65f), CircleShape)
                        .padding(6.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge, content = name)

            if (etc != null) {
                ProvideContentColorTextStyle(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    content = etc
                )
            }
        }

        ProvideTextStyle(MaterialTheme.typography.bodyMedium, content = time)
    }
}

@NonRestartableComposable
@Composable
private fun ForumItem(modifier: Modifier = Modifier, item: ForumHistory, selected: Boolean) {
    HistoryBaseItem(
        modifier = modifier,
        selected = selected,
        avatar = {
            Avatar(
                modifier = Modifier
                    .matchParentSize()
                    .localSharedBounds(key = ForumAvatarSharedBoundsKey(item.name, null)),
                data = item.avatar,
            )
        },
        name = {
            Text(
                text = stringResource(R.string.title_forum, item.name),
                modifier = Modifier.localSharedBounds(key = ForumTitleSharedBoundsKey(item.name, null)),
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
            )
        },
        time = { TimeText(time = item.timestamp) }
    )
}

@NonRestartableComposable
@Composable
private fun ThreadItem(modifier: Modifier = Modifier, item: ThreadHistory, selected: Boolean) {
    HistoryBaseItem(
        modifier = modifier,
        selected = selected,
        avatar = {
            Avatar(modifier = Modifier.matchParentSize(), data = item.avatar)
        },
        name = { Text(text = item.name, maxLines = 1) },
        etc = {
            Text(text = item.title, overflow = TextOverflow.Ellipsis, maxLines = 1)
        },
        time = {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.End,
            ) {
                TimeText(time = item.timestamp)

                if (item.forum != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondary,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = stringResource(R.string.title_forum, item.forum),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    )
}

@NonRestartableComposable
@Composable
private fun UserItem(modifier: Modifier = Modifier, item: UserHistory, selected: Boolean) {
    val extraKey = item.id
    HistoryBaseItem(
        modifier = modifier,
        selected = selected,
        avatar = {
            Avatar(
                modifier = Modifier.matchParentSize().sharedUserAvatar(uid = item.id, extraKey),
                data = item.avatar,
            )
        },
        name = {
            Text(
                text = item.name,
                modifier = Modifier.sharedUserNickname(nickname = item.name, extraKey),
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
            )
        },
        etc = item.username?.let { {
            Text(
                text = remember { "($it)" },
                modifier = Modifier.sharedUsername(username = it, extraKey)
            )
        } },
        time = { TimeText(time = item.timestamp) }
    )
}

@NonRestartableComposable
@Composable
private fun DateHeader(modifier: Modifier = Modifier, time: String) {
    val isToday = time.length <= 5
    Text(
        text = time,
        modifier = modifier.padding(vertical = 12.dp),
        color = if (isToday) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.labelLarge
    )
}

@Composable
private fun <T : HistoryUiModel> HistoryColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues,
    pagedItems: LazyPagingItems<T>,
    selectedItems: SnapshotStateSet<History>,
    onClick: (History) -> Unit,
    onLongClick: (History) -> Unit,
) {
    val listItemColors = SegmentedListItemColors
    val listItemElevation = ListItemElevation(Dp.Hairline, Dp.Hairline)
    val listItemContentPadding = PaddingValues(10.dp) // ListItem.InteractiveListStartPadding

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = state,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(1.5.dp),
    ) {
        items(
            count = pagedItems.itemCount,
            key = pagedItems.itemKey { if (it is HistoryUiModel.Item) it.history.id else it.toString() },
            contentType = pagedItems.itemContentType {
                if (it is HistoryUiModel.Item) HistoryItemContentType else null
            }
        ) { i ->
            when (val item = pagedItems[i]) {
                is HistoryUiModel.Item -> item.history.let { history ->
                    val interactionSource = remember { MutableInteractionSource() }
                    val selected = selectedItems.contains(history)
                    SegmentedListItem(
                        selected = selected,
                        onClick = {
                            onClick(history)
                        },
                        shapes = historySegmentedShapes(index = i, pagedItems = pagedItems),
                        modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
                        verticalAlignment = Alignment.CenterVertically,
                        onLongClick = {
                            onLongClick(history)
                        },
                        colors = listItemColors,
                        elevation = listItemElevation,
                        contentPadding = listItemContentPadding,
                        interactionSource = interactionSource,
                    ) {
                        when (history) {
                            is ThreadHistory -> ThreadItem(item = history, selected = selected)

                            is ForumHistory -> ForumItem(item = history, selected = selected)

                            is UserHistory -> UserItem(item = history, selected = selected)

                            else -> throw RuntimeException()
                        }
                    }
                }

                is HistoryUiModel.DateHeader -> {
                    DateHeader(modifier = Modifier.animateItem(), time = item.date)
                }

                null -> UserHeaderPlaceholder(
                    modifier = Modifier
                        .background(listItemColors.containerColor, ListItemDefaults.shapes().shape)
                        .padding(listItemContentPadding)
                )
            }
        }
    }
}

@NonRestartableComposable
@Composable
private fun <T: HistoryUiModel> historySegmentedShapes(index: Int, pagedItems: LazyPagingItems<T>): ListItemShapes {
    val count = pagedItems.itemCount
    val isFirstItem = index > 0 && pagedItems.peek(index - 1) is HistoryUiModel.DateHeader
    val isLastItem = index + 1 == count || pagedItems.peek(index + 1) is HistoryUiModel.DateHeader

    return when {
        isFirstItem && isLastItem -> ListItemDefaults.shapes().run { copy(shape = selectedShape) }

        isFirstItem -> ListItemDefaults.segmentedShapes(index = 0, count = count)

        isLastItem -> ListItemDefaults.segmentedShapes(index = index, count = index + 1)

        else -> ListItemDefaults.segmentedShapes(index, count)
    }
}
