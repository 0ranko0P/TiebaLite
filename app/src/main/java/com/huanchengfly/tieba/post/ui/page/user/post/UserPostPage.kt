package com.huanchengfly.tieba.post.ui.page.user.post

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.PaddingNone
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.ui.models.user.PostContent
import com.huanchengfly.tieba.post.ui.models.user.PostListItem
import com.huanchengfly.tieba.post.ui.page.Destination.SubPosts
import com.huanchengfly.tieba.post.ui.page.Destination.Thread
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.user.post.UserPostViewModel.Companion.UserPostVmFactory
import com.huanchengfly.tieba.post.ui.widgets.compose.Card
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadingIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen

@Composable
fun UserPostPage(
    uid: Long,
    fluid: Boolean = false,
    lazyListState: LazyListState = rememberLazyListState(),
    viewModel: UserPostViewModel = hiltViewModel<UserPostViewModel, UserPostVmFactory> { it.create(uid) },
) {
    val navigator = LocalNavController.current

    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = UserPostUiState::isRefreshing,
        initial = true
    )
    val isLoadingMore by viewModel.uiState.collectPartialAsState(
        prop1 = UserPostUiState::isLoadingMore,
        initial = false
    )
    val isEmpty by viewModel.uiState.collectPartialAsState(
        prop1 = UserPostUiState::isEmpty,
        initial = false
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = UserPostUiState::error,
        initial = null
    )

    StateScreen(
        isEmpty = isEmpty,
        isLoading = isRefreshing,
        error = error,
        onReload = viewModel::onRefresh,
        screenPadding = PaddingNone,
    ) {
        // initialize onClick listeners
        val onPostContentClicked: (PostListItem, PostContent) -> Unit = { post, content ->
            val threadId = post.threadId
            val forumId = post.forumId
            if (content.isSubPost) {
                navigator.navigate(SubPosts(threadId, forumId, subPostId = content.postId))
            } else {
                navigator.navigate(Thread(threadId, forumId, postId = content.postId, scrollToReply = true))
            }
        }

        val onOriginThreadClicked: (PostListItem) -> Unit = {
            navigator.navigate(Thread(threadId = it.threadId))
        }

        Container(fluid = fluid) {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val data = uiState.data
            val hasMore = uiState.hasMore

            SwipeUpLazyLoadColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
                isLoading = isLoadingMore,
                onLazyLoad = {
                    if (hasMore) viewModel.onLoadMore()
                },
                onLoad = null,
                bottomIndicator = { LoadingIndicator(isLoading = isLoadingMore) }
            ) {
                itemsIndexed(data, key = { _, it -> it.lazyListKey }) { i, post ->
                    Column {
                        UserPostItem(
                            post = post,
                            onPostContentClicked = onPostContentClicked,
                            onOriginThreadClicked = onOriginThreadClicked
                        )

                        if (i < data.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserPostItem(
    modifier: Modifier = Modifier,
    post: PostListItem,
    onPostContentClicked: (PostListItem, PostContent) -> Unit,
    onOriginThreadClicked: (PostListItem) -> Unit,
) =
    Card(
        header = {
            UserHeader(
                modifier = Modifier.padding(horizontal = 16.dp),
                name = post.author.name,
                avatar = post.author.avatarUrl
            )
        },
        content = {
            post.contents.fastForEach {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onPostContentClicked(post, it)
                        }
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = it.text, style = MaterialTheme.typography.bodyLarge)

                    Text(
                        text = it.timeDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = post.title,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable {
                        onOriginThreadClicked(post)
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                textDecoration = if (post.deleted) TextDecoration.LineThrough else null,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        modifier = modifier,
        contentPadding = PaddingNone,
    )
