package com.huanchengfly.tieba.post.ui.page.user.post

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.PaddingNone
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.onCase
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.isWindowWidthCompact
import com.huanchengfly.tieba.post.ui.models.user.PostContent
import com.huanchengfly.tieba.post.ui.models.user.PostListItem
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.user.post.UserPostViewModel.Companion.UserPostVmFactory
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.Card
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultBackToTopFAB
import com.huanchengfly.tieba.post.ui.widgets.compose.ExtendedFabHeight
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.TopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.cardBottomDivider
import com.huanchengfly.tieba.post.ui.widgets.compose.defaultBottomIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen

@Composable
fun UserPostList(
    uid: Long,
    fluid: Boolean = false,
    lazyListState: LazyListState = rememberLazyListState(),
    onNavigate: (navKey: Destination) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues.Zero,
    viewModel: UserPostViewModel = hiltViewModel<UserPostViewModel, UserPostVmFactory> { it.create(uid) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    StateScreen(
        isEmpty = uiState.isEmpty,
        isLoading = uiState.isRefreshing,
        error = uiState.error,
        onReload = viewModel::onRefresh,
        screenPadding = contentPadding,
    ) {
        // initialize onClick listeners
        val onPostContentClicked: (PostListItem, PostContent) -> Unit = { post, content ->
            val threadId = post.threadId
            val forumId = post.forumId
            val navKey = if (content.isSubPost) {
                Destination.SubPosts(threadId, forumId, subPostId = content.postId)
            } else {
                Destination.Thread(threadId, forumId, postId = content.postId, scrollToReply = true)
            }
            onNavigate(navKey)
        }

        val onOriginThreadClicked: (PostListItem) -> Unit = {
            onNavigate(Destination.Thread(threadId = it.threadId))
        }

        Container(fluid = fluid) {
            val data = uiState.data
            SwipeUpLazyLoadColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
                contentPadding = contentPadding,
                isLoading = uiState.isLoadingMore,
                onLazyLoad = viewModel::onLoadMore.takeIf { uiState.hasMore },
                bottomIndicator = defaultBottomIndicator,
            ) {
                itemsIndexed(data, key = { _, it -> it.lazyListKey }) { i, post ->
                    UserPostItem(
                        modifier = Modifier.onCase(i < data.lastIndex) {
                            cardBottomDivider(DividerDefaults.color)
                        },
                        post = post,
                        onPostContentClicked = onPostContentClicked,
                        onOriginThreadClicked = onOriginThreadClicked
                    )
                }
            }
        }
    }
}

@Composable
fun UserPostPage(
    uid: Long,
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit,
    viewModel: UserPostViewModel = hiltViewModel<UserPostViewModel, UserPostVmFactory> { it.create(uid) },
) {
    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scaffoldContentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp + ExtendedFabHeight)

    val onBackToTopClicked: () -> Unit = remember { {
        listState.requestScrollToItem(0)
        scrollBehavior.state.contentOffset = 0f
        scrollBehavior.state.heightOffset = 0f
    } }

    MyScaffold(
        topBar = {
            TopAppBar(
                titleRes = R.string.text_stat_posts,
                navigationIcon = { BackNavigationIcon(onBackPressed = onBack) },
                actions = {
                    ActionItem(
                        icon = Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.btn_refresh)
                    ) {
                        onBackToTopClicked()
                        viewModel.onRefresh()
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        },
        bottomBarAtop = true,
        floatingActionButton = {
            val visible by remember {
                derivedStateOf { listState.canScrollBackward }
            }
            DefaultBackToTopFAB(visible = visible, onClick = onBackToTopClicked)
        }
    ) { paddingValues ->
        UserPostList(
            uid = uid,
            fluid = isWindowWidthCompact(),
            lazyListState = listState,
            onNavigate = onNavigate,
            contentPadding = paddingValues + scaffoldContentPadding,
            viewModel = viewModel,
        )
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
