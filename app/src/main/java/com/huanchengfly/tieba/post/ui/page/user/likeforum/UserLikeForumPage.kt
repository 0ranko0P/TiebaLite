package com.huanchengfly.tieba.post.ui.page.user.likeforum

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.api.models.UserLikeForumBean
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.getOrNull
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.pullRefreshIndicator
import com.huanchengfly.tieba.post.ui.page.Destination.Forum
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.user.post.TipScreenPostHide
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import kotlinx.collections.immutable.persistentListOf

@Composable
fun UserLikeForumPageHide() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        TipScreenPostHide()
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun UserLikeForumPage(
    uid: Long,
    fluid: Boolean = false,
    enablePullRefresh: Boolean = false,
    viewModel: UserLikeForumViewModel = pageViewModel(),
) {
    val navigator = LocalNavController.current

    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(UserLikeForumUiIntent.Refresh(uid))
        viewModel.initialized = true
    }

    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = UserLikeForumUiState::isRefreshing,
        initial = true
    )
    val isLoadingMore by viewModel.uiState.collectPartialAsState(
        prop1 = UserLikeForumUiState::isLoadingMore,
        initial = false
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = UserLikeForumUiState::error,
        initial = null
    )
    val currentPage by viewModel.uiState.collectPartialAsState(
        prop1 = UserLikeForumUiState::currentPage,
        initial = 1
    )
    val hasMore by viewModel.uiState.collectPartialAsState(
        prop1 = UserLikeForumUiState::hasMore,
        initial = false
    )
    val forums by viewModel.uiState.collectPartialAsState(
        prop1 = UserLikeForumUiState::forums,
        initial = persistentListOf()
    )

    val isEmpty by remember {
        derivedStateOf { forums.isEmpty() }
    }
    val isError by remember {
        derivedStateOf { error != null }
    }

    StateScreen(
        modifier = Modifier.fillMaxSize(),
        isEmpty = isEmpty,
        isError = isError,
        isLoading = isRefreshing,
        onReload = {
            viewModel.send(UserLikeForumUiIntent.Refresh(uid))
        },
        errorScreen = {
            val throwable = error.getOrNull()
            ErrorScreen(error = throwable, showReload = throwable !is TiebaNotLoggedInException)
        },
    ) {
        val pullRefreshState = rememberPullRefreshState(
            refreshing = isRefreshing,
            onRefresh = ::reload
        )

        val lazyListState = rememberLazyListState()

        val pullRefreshModifier =
            if (enablePullRefresh) Modifier.pullRefresh(pullRefreshState) else Modifier

        Container(modifier = pullRefreshModifier, fluid = fluid) {
            SwipeUpLazyLoadColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
                isLoading = isLoadingMore,
                onLazyLoad = {
                    if (hasMore && !isEmpty) {
                        viewModel.send(UserLikeForumUiIntent.LoadMore(uid, currentPage))
                    }
                },
                onLoad = {
                    viewModel.send(UserLikeForumUiIntent.Refresh(uid))
                },
                bottomIndicator = { onThreshold ->
                    LoadMoreIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        isLoading = isLoadingMore,
                        noMore = !hasMore,
                        onThreshold = onThreshold
                    )
                }
            ) {
                items(items = forums, key = { it.id }) { forumBean ->
                    UserLikeForumItem(
                        item = forumBean,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        forumBean.name?.let { navigator.navigate(Forum(forumName = it)) }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = ExtendedTheme.colors.pullRefreshIndicator,
                contentColor = ExtendedTheme.colors.primary,
            )
        }
    }
}

@Composable
private fun UserLikeForumItem(
    item: UserLikeForumBean.ForumBean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Avatar(
            data = item.avatar,
            size = Sizes.Medium,
            contentDescription = item.name
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = item.name.orEmpty(), style = MaterialTheme.typography.subtitle1)
            item.slogan.takeUnless { it.isNullOrEmpty() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.body2,
                    color = ExtendedTheme.colors.textSecondary
                )
            }
        }
    }
}