package com.huanchengfly.tieba.post.ui.page.search.forum

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.ui.common.localSharedBounds
import com.huanchengfly.tieba.post.ui.models.search.SearchForum
import com.huanchengfly.tieba.post.ui.page.Destination.Forum
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.search.UiState
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.ForumAvatarSharedBoundsKey
import com.huanchengfly.tieba.post.ui.widgets.compose.ForumTitleSharedBoundsKey
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen

@Composable
fun SearchForumPage(
    modifier: Modifier = Modifier,
    keyword: String,
    contentPadding: PaddingValues,
    listState: LazyListState = rememberLazyListState(),
    viewModel: SearchForumViewModel = hiltViewModel(),
) {

    LaunchedEffect(keyword) {
        viewModel.onKeywordChanged(keyword)
    }

    val isEmpty by viewModel.uiState.collectPartialAsState(
        prop1 = UiState<SearchForum>::isEmpty,
        initial = false
    )
    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = UiState<SearchForum>::isRefreshing,
        initial = true
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = UiState<SearchForum>::error,
        initial = null
    )

    StateScreen(
        modifier = Modifier.fillMaxSize(),
        isEmpty = isEmpty,
        isError = error != null,
        isLoading = isRefreshing,
        onReload = viewModel::onRefresh,
        errorScreen = {
            ErrorScreen(error = error, modifier = Modifier.padding(contentPadding))
        }
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::onRefresh,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            val navigator = LocalNavController.current
            val headerContentType = Integer.MAX_VALUE

            val onForumClickedListener: (SearchForum) -> Unit = {
                navigator.navigate(route = Forum(forumName = it.name, avatar = it.avatar))
            }

            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val exactMatchForum = uiState.exactMatch
            val fuzzyMatchForums = uiState.fuzzyMatch

            MyLazyColumn(
                modifier = modifier.fillMaxSize(),
                state = listState,
                contentPadding = contentPadding,
            ) {
                if (exactMatchForum != null) {
                    item(key = "ExactMatchHeader", contentType = headerContentType) {
                        Chip(
                            text = stringResource(id = R.string.title_exact_match),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            invertColor = true
                        )
                    }
                    item(key = exactMatchForum.id) {
                        SearchForumItem(forum = exactMatchForum, onClick = onForumClickedListener)
                    }
                }

                if (fuzzyMatchForums.isNotEmpty()) {
                    item(key = "FuzzyMatchHeader", contentType = headerContentType) {
                        Chip(
                            text = stringResource(id = R.string.title_fuzzy_match),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(fuzzyMatchForums, key = { it.id }) {
                        SearchForumItem(forum = it, onClick = onForumClickedListener)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SearchForumItem(forum: SearchForum, transitionKey: String? = null, onClick: (SearchForum) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(forum) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Avatar(
            data = forum.avatar,
            size = Sizes.Medium,
            modifier = Modifier.localSharedBounds(
                key = ForumAvatarSharedBoundsKey(forumName = forum.name, extraKey = transitionKey)
            )
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.title_forum, forum.name),
                modifier = Modifier.localSharedBounds(
                    key = ForumTitleSharedBoundsKey(forumName = forum.name, extraKey = transitionKey)
                ),
                style = MaterialTheme.typography.titleMedium
            )

            if (forum.slogan != null) {
                Text(
                    text = forum.slogan,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}