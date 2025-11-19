package com.huanchengfly.tieba.post.ui.page.history

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.models.database.ForumHistory
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.models.database.ThreadHistory
import com.huanchengfly.tieba.post.ui.common.localSharedBounds
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.thread.ThreadFrom
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.CenterAlignedTopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.FancyAnimatedIndicatorWithModifier
import com.huanchengfly.tieba.post.ui.widgets.compose.ForumAvatarSharedBoundsKey
import com.huanchengfly.tieba.post.ui.widgets.compose.ForumTitleSharedBoundsKey
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeaderPlaceholder
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberSnackbarHostState
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
            R.string.title_history_forum
        )
    }

    val onHistoryClickedListener: (History) -> Unit = {
        when(it) {
            is ThreadHistory -> {
                navigator.navigate(
                    Destination.Thread(threadId = it.id, postId = it.pid, seeLz = it.isSeeLz, from = ThreadFrom.History)
                )
            }

            is ForumHistory -> {
                navigator.navigate(route = Destination.Forum(forumName = it.name, avatar = it.avatar))
            }
        }
    }

    val pagerState = rememberPagerState { tabs.size }
    val pageMovableContent = remember {
        tabs.map { type ->
            movableContentOf {
                val pagedItems = when (type) {
                    R.string.title_history_thread -> viewModel.threadHistory.collectAsLazyPagingItems()

                    R.string.title_history_forum -> viewModel.forumHistory.collectAsLazyPagingItems()

                    else -> throw RuntimeException()
                }

                HistoryColumn(pagedItems, onDelete = viewModel::onDelete, onClick = onHistoryClickedListener)
            }
        }
    }

    MyScaffold(
        topBar = {
            CenterAlignedTopAppBar(
                titleRes = R.string.title_history,
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = navigator::navigateUp)
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.onDeleteAll()
                        coroutineScope.launch {
                            val message = context.getString(R.string.toast_clear_success)
                            snackbarHostState.showSnackbar(message)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(id = R.string.title_history_delete)
                        )
                    }
                },
                content = {
                    PrimaryTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        indicator = {
                            FancyAnimatedIndicatorWithModifier(pagerState.currentPage)
                        },
                        divider = {},
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
            )
        },
        snackbarHostState = snackbarHostState
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
private fun ThreadItem(
    modifier: Modifier = Modifier,
    avatar: String,
    name: String,
    time: String,
    title: String
) {
    Row(
        modifier = modifier.padding(16.dp)
    ) {
        Avatar(data = avatar, size = Sizes.Small)

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row {
                Text(name, style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.weight(1.0f))
                Text(time, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
            }

            Text(text = title)
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ForumItem(modifier: Modifier = Modifier, avatar: String, forum: String, time: String) {
    UserHeader(
        modifier = modifier.padding(16.dp),
        avatar = {
            Avatar(
                data = avatar,
                size = Sizes.Small,
                modifier = Modifier.localSharedBounds(key = ForumAvatarSharedBoundsKey(forum, null)),
            )
        },
        name = {
            Text(
                text = stringResource(R.string.title_forum, forum),
                modifier = Modifier.localSharedBounds(key = ForumTitleSharedBoundsKey(forum, null)),
            )
        },
    ) {
        Text(text = time, fontSize = 15.sp)
    }
}

@Composable
private fun <T : History> HistoryColumn(
    pagedItems: LazyPagingItems<T>,
    onDelete: (T) -> Unit,
    onClick: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        items(count = pagedItems.itemCount, key = pagedItems.itemKey { it.id }) {
            val item = pagedItems[it]
            if (item != null) {
                LongClickMenu(
                    menuContent = {
                        TextMenuItem(text = R.string.title_delete, onClick = { onDelete(item) })
                    },
                    modifier = Modifier.animateItem(placementSpec = null),
                    onClick = { onClick(item) }
                ) {
                    val context = LocalContext.current
                    val time = remember { DateTimeUtils.getRelativeTimeString(context, item.timestamp) }
                    when (item) {
                        is ThreadHistory ->  {
                            ThreadItem(avatar = item.avatar, name = item.name, title = item.title, time = time)
                        }

                        is ForumHistory -> {
                            ForumItem(avatar = item.avatar, forum = item.name, time = time)
                        }

                        else -> throw RuntimeException()
                    }
                }
            } else {
                UserHeaderPlaceholder(modifier = modifier.padding(16.dp))
            }
        }
    }
}
