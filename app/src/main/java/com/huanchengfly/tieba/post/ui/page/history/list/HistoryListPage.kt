package com.huanchengfly.tieba.post.ui.page.history.list

import androidx.annotation.StringRes
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.fromJson
import com.huanchengfly.tieba.post.models.ThreadHistoryInfoBean
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.repository.HistoryType
import com.huanchengfly.tieba.post.ui.common.localSharedBounds
import com.huanchengfly.tieba.post.ui.page.Destination.Forum
import com.huanchengfly.tieba.post.ui.page.Destination.Thread
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.history.list.HistoryListViewModel.Companion.HistoryVmFactory
import com.huanchengfly.tieba.post.ui.page.thread.ThreadFrom
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.ForumAvatarSharedBoundsKey
import com.huanchengfly.tieba.post.ui.widgets.compose.ForumTitleSharedBoundsKey
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.utils.DateTimeUtils

@Composable
fun HistoryListPage(
    @HistoryType type: Int,
    viewModel: HistoryListViewModel = hiltViewModel<HistoryListViewModel, HistoryVmFactory>(key = type.toString()) {
        it.create(type)
    }
) {
    onGlobalEvent<HistoryListUiEvent.DeleteAll> {
        viewModel.deleteAll()
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoadingMore = uiState.isLoadingMore
    val hasMore = uiState.hasMore
    val todayHistoryData = uiState.todayHistoryData
    val beforeHistoryData = uiState.beforeHistoryData

    val context = LocalContext.current
    val navigator = LocalNavController.current
    val snackbarHostState = LocalSnackbarHostState.current

    LaunchedEffect(viewModel) {
        viewModel.uiEvent.collect {
            when(it) {
                is HistoryListUiEvent.Failure -> {
                    snackbarHostState.showSnackbar(context.getString(R.string.delete_history_failure, it.errorMsg))
                }
            }
        }
    }

    val historyClickListener: (History) -> Unit = {
        when (it.type) {
            HistoryType.FORUM -> {
                navigator.navigate(route = Forum(forumName = it.data, avatar = it.avatar))
            }

            HistoryType.THREAD -> {
                val extra = it.extras?.fromJson<ThreadHistoryInfoBean>()
                navigator.navigate(
                    Thread(
                        it.data.toLong(),
                        postId = extra?.pid ?: 0L,
                        seeLz = extra?.isSeeLz == true,
                        from = ThreadFrom.History
                    )
                )
            }
        }
    }

    SwipeUpLazyLoadColumn(
        modifier = Modifier.fillMaxSize(),
        isLoading = isLoadingMore,
        onLazyLoad = {
            if (hasMore) viewModel.loadMore()
        },
        onLoad = null, // Refuse manual reload
        bottomIndicator = {
            LoadMoreIndicator(
                isLoading = isLoadingMore,
                noMore = !hasMore,
                onThreshold = false
            )
        }
    ) {
        if (todayHistoryData.isNotEmpty()) {
            stickyHeader(key = "TodayHistoryHeader") {
                TimeHintHeaderItem(timeHint = R.string.title_history_today, invertColor = true)
            }

            items(items = todayHistoryData, key = { it.id }) { info ->
                HistoryItem(info, onDelete = viewModel::delete, onClick = historyClickListener)
            }
        }

        if (beforeHistoryData.isNotEmpty()) {
            stickyHeader(key = "BeforeHistoryHeader") {
                TimeHintHeaderItem(timeHint = R.string.title_history_before)
            }

            items(items = beforeHistoryData, key = { it.id }) { info ->
                HistoryItem(info, onDelete = viewModel::delete, onClick = historyClickListener)
            }
        }
    }
}

@Composable
private fun TimeHintHeaderItem(@StringRes timeHint: Int, invertColor: Boolean = false) =
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Chip(text = stringResource(id = timeHint), invertColor = invertColor)
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
                contentDescription = forum
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
private fun HistoryItem(
    info: History,
    modifier: Modifier = Modifier,
    onClick: (History) -> Unit = {},
    onDelete: (History) -> Unit = {},
) {
    val context = LocalContext.current
    val menuState = rememberMenuState()
    LongClickMenu(
        menuContent = {
            TextMenuItem(text = R.string.title_delete) {
                onDelete(info)
            }
        },
        modifier = modifier,
        menuState = menuState,
        onClick = { onClick(info) }
    ) {
        val timestamp = remember { DateTimeUtils.getRelativeTimeString(context, info.timestamp) }

        if (info.type == HistoryType.THREAD) {
            ThreadItem(
                avatar = info.avatar!!,
                name = info.username.orEmpty(),
                time = timestamp,
                title = info.title
            )
        } else {
            ForumItem(avatar = info.avatar!!, forum = info.data, time = timestamp)
        }
    }
}