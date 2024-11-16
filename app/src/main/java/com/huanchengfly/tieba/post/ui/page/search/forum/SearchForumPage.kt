package com.huanchengfly.tieba.post.ui.page.search.forum

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.SearchForumBean
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.common.localSharedBounds
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.pullRefreshIndicator
import com.huanchengfly.tieba.post.ui.page.Destination.Forum
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.search.SearchUiEvent
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.ForumAvatarSharedBoundsKey
import com.huanchengfly.tieba.post.ui.widgets.compose.ForumTitleSharedBoundsKey
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalShouldLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SearchForumPage(
    keyword: String,
    viewModel: SearchForumViewModel = pageViewModel(),
) {
    val navigator = LocalNavController.current
    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(SearchForumUiIntent.Refresh(keyword))
        viewModel.initialized = true
    }
    val currentKeyword by viewModel.uiState.collectPartialAsState(
        prop1 = SearchForumUiState::keyword,
        initial = ""
    )
    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = SearchForumUiState::isRefreshing,
        initial = true
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = SearchForumUiState::error,
        initial = null
    )
    val exactMatchForum by viewModel.uiState.collectPartialAsState(
        prop1 = SearchForumUiState::exactMatchForum,
        initial = null
    )
    val fuzzyMatchForumList by viewModel.uiState.collectPartialAsState(
        prop1 = SearchForumUiState::fuzzyMatchForumList,
        initial = persistentListOf()
    )

    val showFuzzyMatchResult by remember {
        derivedStateOf { fuzzyMatchForumList.isNotEmpty() }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.send(SearchForumUiIntent.Refresh(keyword)) }
    )

    val isEmpty by remember {
        derivedStateOf { exactMatchForum == null && !showFuzzyMatchResult }
    }

    onGlobalEvent<SearchUiEvent.KeywordChanged> {
        viewModel.send(SearchForumUiIntent.Refresh(it.keyword))
    }
    val shouldLoad = LocalShouldLoad.current
    LaunchedEffect(currentKeyword) {
        if (currentKeyword.isNotEmpty() && keyword != currentKeyword) {
            if (shouldLoad) {
                viewModel.send(SearchForumUiIntent.Refresh(keyword))
            } else {
                viewModel.initialized = false
            }
        }
    }

    StateScreen(
        modifier = Modifier.fillMaxSize(),
        isEmpty = isEmpty,
        isError = error != null,
        isLoading = isRefreshing,
        onReload = { viewModel.send(SearchForumUiIntent.Refresh(keyword)) },
        errorScreen = {
            error?.let {
                val (e) = it
                ErrorScreen(error = e)
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            MyLazyColumn(modifier = Modifier.fillMaxSize()) {
                exactMatchForum?.let {
                    item(key = "ExactMatchHeader") {
                        Chip(
                            text = stringResource(id = R.string.title_exact_match),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            invertColor = true
                        )
                    }
                    item(key = "ExactMatch") {
                        SearchForumItem(
                            item = it,
                            onClick = {
                                val forumName = it.forumName ?: return@SearchForumItem
                                navigator.navigate(route = Forum(forumName, it.avatar))
                            }
                        )
                    }
                }
                if (showFuzzyMatchResult) {
                    item(key = "FuzzyMatchHeader") {
                        Chip(
                            text = stringResource(id = R.string.title_fuzzy_match),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            invertColor = false
                        )
                    }
                    items(fuzzyMatchForumList) {
                        SearchForumItem(
                            item = it,
                            onClick = {
                                val forumName = it.forumName ?: return@SearchForumItem
                                navigator.navigate(route = Forum(forumName, it.avatar))
                            }
                        )
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SearchForumItem(
    item: SearchForumBean.ForumInfoBean,
    onClick: () -> Unit,
) {
    val forumName = item.forumName ?: item.forumNameShow.orEmpty()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Avatar(
            data = item.avatar,
            size = Sizes.Medium,
            contentDescription = forumName,
            modifier = Modifier.localSharedBounds(
                key = ForumAvatarSharedBoundsKey(forumName = forumName, extraKey = null)
            )
        )
        Column(
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.title_forum, forumName),
                modifier = Modifier.localSharedBounds(
                    key = ForumTitleSharedBoundsKey(forumName = forumName, extraKey = null)
                ),
                style = MaterialTheme.typography.subtitle1
            )
            if (!item.intro.isNullOrEmpty()) {
                Text(
                    text = item.slogan.orEmpty(),
                    style = MaterialTheme.typography.body2,
                    maxLines = 1
                )
            }
        }
    }
}