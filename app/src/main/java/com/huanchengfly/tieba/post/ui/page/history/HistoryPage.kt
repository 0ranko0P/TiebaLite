package com.huanchengfly.tieba.post.ui.page.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.isScrolling
import com.huanchengfly.tieba.post.models.database.ForumHistory
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.models.database.ThreadHistory
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.repository.UserHistory
import com.huanchengfly.tieba.post.theme.ProvideContentColorTextStyle
import com.huanchengfly.tieba.post.ui.common.localSharedBounds
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.thread.ThreadFrom
import com.huanchengfly.tieba.post.ui.page.user.sharedUserAvatar
import com.huanchengfly.tieba.post.ui.page.user.sharedUserNickname
import com.huanchengfly.tieba.post.ui.page.user.sharedUsername
import com.huanchengfly.tieba.post.ui.utils.rememberScrollOrientationConnection
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultBackToTopFAB
import com.huanchengfly.tieba.post.ui.widgets.compose.FancyAnimatedIndicatorWithModifier
import com.huanchengfly.tieba.post.ui.widgets.compose.ForumAvatarSharedBoundsKey
import com.huanchengfly.tieba.post.ui.widgets.compose.ForumTitleSharedBoundsKey
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TopAppBarPaged
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeaderPlaceholder
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberPagerListStates
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberSnackbarHostState
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import kotlinx.coroutines.launch

@Composable
fun HistoryPage(
    navigator: NavController,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = rememberSnackbarHostState()

    val tabs = remember {
        listOf(
            R.string.title_history_thread,
            R.string.title_history_forum,
            R.string.title_history_user,
        )
    }

    val onHistoryClickedListener: (History) -> Unit = {
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

    val scrollOrientationConnection = rememberScrollOrientationConnection()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val listStates = rememberPagerListStates(tabs.size)
    val pagerState = rememberPagerState { tabs.size }
    val pageMovableContent = remember {
        tabs.mapIndexed { i, type ->
            movableContentOf {
                val pagedItems = when (type) {
                    R.string.title_history_thread -> viewModel.threadHistory.collectAsLazyPagingItems()

                    R.string.title_history_forum -> viewModel.forumHistory.collectAsLazyPagingItems()

                    R.string.title_history_user -> viewModel.userHistory.collectAsLazyPagingItems()

                    else -> throw RuntimeException()
                }

                HistoryColumn(
                    modifier = Modifier
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .nestedScroll(scrollOrientationConnection),
                    state = listStates[i],
                    pagedItems = pagedItems,
                    onDelete = viewModel::onDelete,
                    onClick = onHistoryClickedListener,
                )
            }
        }
    }

    MyScaffold(
        topBar = {
            TopAppBarPaged(
                title = { Text(text = stringResource(R.string.title_history)) },
                titleHorizontalAlignment = Alignment.CenterHorizontally,
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = navigator::navigateUp)
                },
                actions = {
                    ActionItem(
                        icon = Icons.Outlined.Delete,
                        contentDescription = stringResource(id = R.string.title_history_delete)
                    ) {
                        viewModel.onDeleteAll()
                        coroutineScope.launch {
                            val message = context.getString(R.string.toast_clear_success)
                            snackbarHostState.showSnackbar(message)
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                canScrollBackward = {
                    listStates[pagerState.currentPage].canScrollBackward
                },
                content = {
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
                },
            )
        },
        snackbarHostState = snackbarHostState,
        floatingActionButton = {
            // FAB visibility: scrolling forward, pager not scrolling, not top
            val visible by remember {
                derivedStateOf {
                    scrollOrientationConnection.isScrollingForward && !pagerState.isScrolling && listStates[pagerState.currentPage].canScrollBackward
                }
            }
            DefaultBackToTopFAB(visible = visible) {
                coroutineScope.launch {
                    listStates[pagerState.currentPage].scrollToItem(0)
                    scrollBehavior.state.contentOffset = 0f
                }
            }
        }
    ) { contentPadding ->
        ProvideNavigator(navigator = navigator) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.padding(contentPadding),
                key = { it },
                verticalAlignment = Alignment.Top,
            ) { index ->
                pageMovableContent[index]()
            }
        }
    }
}

@Composable
private fun HistoryBaseItem(
    modifier: Modifier = Modifier,
    avatar: @Composable BoxScope.() -> Unit,
    name: @Composable () -> Unit,
    etc: (@Composable () -> Unit)? = null,
    time: Long,
) {
    val context = LocalContext.current
    val relativeTimeString = remember {
        DateTimeUtils.getRelativeTimeString(context, time)
    }

    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(Sizes.Small), contentAlignment = Alignment.Center, content = avatar)

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Row {
                Box(modifier = Modifier.weight(1.0f)) {
                    ProvideTextStyle(MaterialTheme.typography.labelLarge, content = name)
                }

                Text(
                    text = relativeTimeString,
                    modifier = Modifier.padding(start = 6.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (etc != null) {
                ProvideContentColorTextStyle(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    content = etc
                )
            }
        }
    }
}

@NonRestartableComposable
@Composable
private fun ForumItem(modifier: Modifier = Modifier, item: ForumHistory) {
    HistoryBaseItem(
        modifier = modifier,
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
        time = item.timestamp
    )
}

@NonRestartableComposable
@Composable
private fun ThreadItem(modifier: Modifier = Modifier, item: ThreadHistory) {
    HistoryBaseItem(
        modifier = modifier,
        avatar = {
            Avatar(modifier = Modifier.matchParentSize(), data = item.avatar)
        },
        name = { Text(text = item.name, maxLines = 1) },
        etc = {
            Row (
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    modifier = Modifier.weight(1.0f),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )

                if (item.forum != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondary,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = stringResource(R.string.title_forum, item.forum),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        },
        time = item.timestamp
    )
}

@NonRestartableComposable
@Composable
private fun UserItem(modifier: Modifier = Modifier, item: UserHistory) {
    val extraKey = item.id
    HistoryBaseItem(
        modifier = modifier,
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
        time = item.timestamp
    )
}

@NonRestartableComposable
@Composable
private fun DateHeader(modifier: Modifier = Modifier, time: String) {
    val isToday = time.length <= 5
    Text(
        text = time,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        color = if (isToday) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.labelLarge
    )
}

@Composable
private fun <T : HistoryUiModel> HistoryColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    pagedItems: LazyPagingItems<T>,
    onDelete: (History) -> Unit,
    onClick: (History) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = state,
    ) {
        items(
            count = pagedItems.itemCount,
            key = pagedItems.itemKey { if (it is HistoryUiModel.Item) it.history.id else it.hashCode() }
        ) { i ->
            when (val item = pagedItems[i]) {
                is HistoryUiModel.Item -> item.history.let { history ->
                    LongClickMenu(
                        menuContent = {
                            TextMenuItem(text = R.string.title_delete, onClick = { onDelete(history) })
                        },
                        modifier = Modifier.animateItem(),
                        onClick = { onClick(history) }
                    ) {
                        when (history) {
                            is ThreadHistory -> ThreadItem(item = history)

                            is ForumHistory -> ForumItem(item = history)

                            is UserHistory -> UserItem(item = history)

                            else -> throw RuntimeException()
                        }
                    }
                }

                is HistoryUiModel.DateHeader -> {
                    DateHeader(modifier = Modifier.animateItem(), time = item.date)
                }

                null -> UserHeaderPlaceholder(modifier = Modifier.padding(16.dp))
            }
        }
    }
}
