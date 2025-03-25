package com.huanchengfly.tieba.post.ui.page.search.forum

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkUnreadChatAlt
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.Icon
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
import com.huanchengfly.tieba.post.arch.collectCommonUiEventWithLifecycle
import com.huanchengfly.tieba.post.ui.common.localSharedBounds
import com.huanchengfly.tieba.post.ui.models.search.SearchForum
import com.huanchengfly.tieba.post.ui.page.Destination.Forum
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
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

    viewModel.uiEvent.collectCommonUiEventWithLifecycle()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    StateScreen(
        isEmpty = uiState.isEmpty,
        isLoading = uiState.isRefreshing,
        error = uiState.error,
        onReload = viewModel::onRefresh,
        screenPadding = contentPadding,
    ) {
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = viewModel::onRefresh,
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

@Composable
fun SearchForumItem(forum: SearchForum, transitionKey: String? = null, onClick: (SearchForum) -> Unit) {
    val postNumSting = stringResource(id = R.string.forum_concern_num, forum.postNum)
    val concernNumString = stringResource(id = R.string.forum_concern_num, forum.concernNum)
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
            verticalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.CenterVertically)
        ) {
            Text(
                text = stringResource(id = R.string.title_forum, forum.name),
                modifier = Modifier.localSharedBounds(
                    key = ForumTitleSharedBoundsKey(forumName = forum.name, extraKey = transitionKey)
                ),
                maxLines = 1,
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Chip(
                    text = forum.concernNum,
                    //invertColor = true,
                    modifier = Modifier
                        .padding(end = 4.dp),
                    prefixIcon = {
                        Icon(
                            imageVector = Icons.Default.PersonOutline,
                            contentDescription = concernNumString
                        )
                    },
                )
                Chip(
                    text = forum.postNum,
                    //invertColor = true,
                    modifier = Modifier
                        .padding(start = 4.dp),
                    prefixIcon = {
                        Icon(
                            imageVector = Icons.Default.MarkUnreadChatAlt,
                            contentDescription = postNumSting
                        )
                    },
                )
            }

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