package com.huanchengfly.tieba.post.ui.page.user.thread

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.PbContentText
import com.huanchengfly.tieba.post.ui.models.Author
import com.huanchengfly.tieba.post.ui.models.SimpleForum
import com.huanchengfly.tieba.post.ui.models.ThreadItem
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.main.explore.createThreadClickListeners
import com.huanchengfly.tieba.post.ui.page.thread.ThreadLikeUiEvent
import com.huanchengfly.tieba.post.ui.page.user.thread.UserThreadViewModel.Companion.UserThreadVmFactory
import com.huanchengfly.tieba.post.ui.widgets.compose.Card
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.ForumInfoChip
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.ThreadContentType
import com.huanchengfly.tieba.post.ui.widgets.compose.ThreadMedia
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.DateTimeUtils

@Composable
fun UserThreadPage(
    uid: Long,
    fluid: Boolean = false,
    lazyListState: LazyListState = rememberLazyListState(),
    viewModel: UserThreadViewModel = hiltViewModel<UserThreadViewModel, UserThreadVmFactory> { it.create(uid) },
) {
    val context = LocalContext.current
    val navigator = LocalNavController.current

    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = UserThreadUiState::isRefreshing,
        initial = true
    )
    val isLoadingMore by viewModel.uiState.collectPartialAsState(
        prop1 = UserThreadUiState::isLoadingMore,
        initial = false
    )
    val isEmpty by viewModel.uiState.collectPartialAsState(
        prop1 = UserThreadUiState::isEmpty,
        initial = false
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = UserThreadUiState::error,
        initial = null
    )

    onGlobalEvent<ThreadLikeUiEvent> {
        context.toastShort(it.toMessage(context))
    }

    StateScreen(
        modifier = Modifier.fillMaxSize(),
        isEmpty = isEmpty,
        isError = error != null,
        isLoading = isRefreshing,
        onReload = viewModel::onRefresh,
        errorScreen = { ErrorScreen(error = error) },
    ) {
        val threadClickListeners = remember(navigator) {
            createThreadClickListeners(onNavigate = navigator::navigate)
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
                bottomIndicator = {
                    LoadMoreIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        isLoading = isLoadingMore,
                        noMore = !hasMore,
                        onThreshold = false
                    )
                }
            ) {
                itemsIndexed(data, key = { _, it -> it.id }, ThreadContentType) { i, thread ->
                    Column {
                        UserThread(
                            thread = thread,
                            onClick = threadClickListeners.onClicked,
                            onClickForum = threadClickListeners.onForumClicked,
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
private fun UserThread(
    modifier: Modifier = Modifier,
    thread: ThreadItem,
    onClick: (ThreadItem) -> Unit,
    onClickForum: (ThreadItem) -> Unit,
) {
    val context = LocalContext.current
    val (forumId, forumName, forumAvatar) = thread.simpleForum

    Card(
        modifier = modifier,
        header = {
            UserHeader(
                name = thread.author.name,
                avatar = thread.author.avatarUrl,
                desc = remember { DateTimeUtils.getRelativeTimeString(context, thread.lastTimeMill) },
            ) {
                ForumInfoChip(
                    forumName = forumName,
                    avatarUrl = forumAvatar,
                    transitionKey = thread.id.toString(),
                    onClick = { onClickForum(thread) }
                )
            }
        },
        content = {
            if (!thread.content.isNullOrEmpty()) {
                PbContentText(
                    text = thread.content,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(modifier),
                    fontSize = 15.sp,
                    lineSpacing = 0.8.sp,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 5,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            ThreadMedia(
                forumId = forumId,
                forumName = forumName,
                threadId = thread.id,
                medias = thread.medias ?: emptyList(),
                videoInfo = thread.video,
            )
        },
        onClick = { onClick(thread) },
    )
}

@Preview("UserThreadPreview")
@Composable
private fun UserThreadPreview() = TiebaLiteTheme {
    Surface {
        UserThread(
            thread = ThreadItem(
                author = Author(0, name = "User", avatarUrl = ""),
                title = "预览",
                lastTimeMill = System.currentTimeMillis(),
                simpleForum = SimpleForum(-1, "测试", null)
            ),
            onClick = {},
            onClickForum = {},
        )
    }
}
