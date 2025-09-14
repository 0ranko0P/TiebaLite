package com.huanchengfly.tieba.post.ui.page.user.post

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.huanchengfly.tieba.post.PaddingNone
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.PostInfoList
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.getOrNull
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.page.Destination.Forum
import com.huanchengfly.tieba.post.ui.page.Destination.SubPosts
import com.huanchengfly.tieba.post.ui.page.Destination.Thread
import com.huanchengfly.tieba.post.ui.page.Destination.UserProfile
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.widgets.compose.Card
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.TipScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import kotlinx.collections.immutable.persistentListOf

@Composable
fun TipScreenPostHide(modifier: Modifier = Modifier) {
    TipScreen(
        title = { Text(text = stringResource(id = R.string.title_user_hide_post)) },
        modifier = modifier,
        image = {
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_hide))
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .fillMaxWidth()
                    .aspectRatio(2.5f)
            )
        },
        scrollable = true,
    )
}

@Composable
fun TipScreenPostEmpty(modifier: Modifier = Modifier)  {
    TipScreen(
        title = { Text(text = stringResource(id = R.string.title_empty)) },
        modifier = modifier,
        image = {
            val composition by rememberLottieComposition(
                LottieCompositionSpec.RawRes(R.raw.lottie_empty_box)
            )
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f)
            )
        },
        scrollable = true
    )
}

@Composable
fun UserPostPage(
    uid: Long,
    isThread: Boolean = true,
    fluid: Boolean = false,
    viewModel: UserPostViewModel = pageViewModel(key = if (isThread) "user_thread_$uid" else "user_post_$uid"),
) {
    val navigator = LocalNavController.current

    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(UserPostUiIntent.Refresh(uid, isThread))
        viewModel.initialized = true
    }

    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = UserPostUiState::isRefreshing,
        initial = true
    )
    val isLoadingMore by viewModel.uiState.collectPartialAsState(
        prop1 = UserPostUiState::isLoadingMore,
        initial = false
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = UserPostUiState::error,
        initial = null
    )
    val currentPage by viewModel.uiState.collectPartialAsState(
        prop1 = UserPostUiState::currentPage,
        initial = 1
    )
    val hasMore by viewModel.uiState.collectPartialAsState(
        prop1 = UserPostUiState::hasMore,
        initial = false
    )
    val posts by viewModel.uiState.collectPartialAsState(
        prop1 = UserPostUiState::posts,
        initial = persistentListOf()
    )
    val hidePost by viewModel.uiState.collectPartialAsState(
        prop1 = UserPostUiState::hidePost,
        initial = false
    )

    val isEmpty by remember {
        derivedStateOf { posts.isEmpty() }
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
            viewModel.send(UserPostUiIntent.Refresh(uid, isThread))
        },
        errorScreen = { ErrorScreen(error = error.getOrNull()) },
        emptyScreen = {
            if (hidePost) {
                TipScreenPostHide()
            } else {
                TipScreenPostEmpty()
            }
        },
    ) {
        val lazyListState = rememberLazyListState()

        val onPostClickListener : (Long, Long?, Boolean) -> Unit = { threadId, postId, isSubPost ->
            if (postId == null) {
                navigator.navigate(Thread(threadId))
            } else if (isSubPost) {
                navigator.navigate(SubPosts(threadId, subPostId = postId))
            } else {
                navigator.navigate(Thread(threadId, postId = postId, scrollToReply = true))
            }
        }

        Container(fluid = fluid) {
            SwipeUpLazyLoadColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
                isLoading = isLoadingMore,
                onLazyLoad = {
                    if (hasMore) viewModel.send(UserPostUiIntent.LoadMore(uid, isThread, currentPage))
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
                items(items = posts, key = { it.data.item.run { "${thread_id}_$post_id" } }) { post ->
                    UserPostItem(
                        post = post,
                        onClick = onPostClickListener,
                        onAgree = {
                            viewModel.send(
                                UserPostUiIntent.Agree(it.thread_id, it.post_id, it.agree?.hasAgree ?: 0)
                            )
                        },
                        onClickReply = {
                            navigator.navigate(
                                Thread(it.thread_id, forumId = it.forum_id, scrollToReply = true)
                            )
                        },
                        onClickUser = {
                            navigator.navigate(UserProfile(it))
                        },
                        onClickForum = {
                            navigator.navigate(Forum(it))
                        },
                        onClickOriginThread = {
                            navigator.navigate(Thread(it))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun UserPostItem(
    post: PostListItemData,
    onAgree: (PostInfoList) -> Unit,
    modifier: Modifier = Modifier,
    onClick: (threadId: Long, postId: Long?, isSubPost: Boolean) -> Unit = { _, _, _ -> },
    onClickReply: (PostInfoList) -> Unit = {},
    onClickUser: (id: Long) -> Unit = {},
    onClickForum: (name: String) -> Unit = {},
    onClickOriginThread: (threadId: Long) -> Unit = {},
) {
    val item = post.data
    if (post.isThread) {
        FeedCard(
            item = item,
            onClick = { onClick(item.get { thread_id }, null, false) },
            onAgree = onAgree,
            modifier = modifier,
            onClickReply = onClickReply,
            onClickUser = onClickUser,
            onClickForum = onClickForum,
            onClickOriginThread = { onClickOriginThread(it.tid.toLong()) },
        )
    } else {
        val context = LocalContext.current
        Card(
            header = {
                UserHeader(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    name = item.get { user_name },
                    nameShow = item.get { name_show },
                    portrait = item.get { user_portrait },
                )
            },
            content = {
                Column {
                    post.contents.fastForEach {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onClick(item.get { thread_id }, it.postId, it.isSubPost)
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = it.contentText,
                                style = MaterialTheme.typography.bodyLarge,
                            )

                            Text(
                                text = remember {
                                    DateTimeUtils.getRelativeTimeString(context, it.createTime)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Text(
                    text = item.get { title },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable {
                            onClickOriginThread(item.get { thread_id })
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            modifier = modifier,
            contentPadding = PaddingNone,
        )
    }
}