package com.huanchengfly.tieba.post.ui.page.search.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.SearchUserBean
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.page.Destination.UserProfile
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.search.SearchUiEvent
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalShouldLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.StringUtil
import kotlinx.collections.immutable.persistentListOf


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchUserPage(
    modifier: Modifier = Modifier,
    keyword: String,
    contentPadding: PaddingValues,
    viewModel: SearchUserViewModel = pageViewModel(),
) {
    val navigator = LocalNavController.current
    val listState = rememberLazyListState()

    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(SearchUserUiIntent.Refresh(keyword))
        viewModel.initialized = true
    }
    val currentKeyword by viewModel.uiState.collectPartialAsState(
        prop1 = SearchUserUiState::keyword,
        initial = ""
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = SearchUserUiState::error,
        initial = null
    )
    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = SearchUserUiState::isRefreshing,
        initial = true
    )
    val exactMatch by viewModel.uiState.collectPartialAsState(
        prop1 = SearchUserUiState::exactMatch,
        initial = null
    )
    val fuzzyMatch by viewModel.uiState.collectPartialAsState(
        prop1 = SearchUserUiState::fuzzyMatch,
        initial = persistentListOf()
    )

    val showExactMatchResult by remember {
        derivedStateOf { exactMatch != null }
    }
    val showFuzzyMatchResult by remember {
        derivedStateOf { fuzzyMatch.isNotEmpty() }
    }

    onGlobalEvent<SearchUiEvent.KeywordChanged> {
        viewModel.send(SearchUserUiIntent.Refresh(it.keyword))
    }
    val shouldLoad = LocalShouldLoad.current
    LaunchedEffect(currentKeyword) {
        if (currentKeyword.isNotEmpty() && keyword != currentKeyword) {
            if (shouldLoad) {
                viewModel.send(SearchUserUiIntent.Refresh(keyword))
            } else {
                viewModel.initialized = false
            }
        }
    }

    val isEmpty by remember {
        derivedStateOf { !showExactMatchResult && !showFuzzyMatchResult }
    }

    val onReload: () -> Unit = { viewModel.send(SearchUserUiIntent.Refresh(keyword)) }

    StateScreen(
        modifier = Modifier.fillMaxSize(),
        isEmpty = isEmpty,
        isError = error != null,
        isLoading = isRefreshing,
        onReload = onReload,
        errorScreen = {
            error?.item?.let {
                ErrorScreen(error = it)
            }
        }
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onReload,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            MyLazyColumn(
                modifier = modifier.fillMaxSize(),
                state = listState,
                contentPadding = contentPadding,
            ) {
                if (showExactMatchResult) {
                    exactMatch?.let {
                        item(key = "ExactMatchHeader") {
                            Chip(
                                text = stringResource(id = R.string.title_exact_match),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                invertColor = true
                            )
                        }

                        item(key = "ExactMatch") {
                            SearchUserItem(
                                item = it,
                                onClick = {
                                    val uid = it.id?.toLongOrNull() ?: return@SearchUserItem
                                    navigator.navigate(UserProfile(uid))
                                }
                            )
                        }
                    }
                }
                if (showFuzzyMatchResult) {
                    item(key = "FuzzyMatchHeader") {
                        Chip(
                            text = stringResource(id = R.string.title_fuzzy_match_user),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(fuzzyMatch) {
                        SearchUserItem(
                            item = it,
                            onClick = {
                                val uid = it.id?.toLongOrNull() ?: return@SearchUserItem
                                navigator.navigate(UserProfile(uid))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchUserItem(
    item: SearchUserBean.UserBean,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Avatar(
            data = remember { StringUtil.getAvatarUrl(item.portrait) },
            size = Sizes.Medium,
            contentDescription = item.name
        )
        Column(
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = remember {
                    StringUtil.getUserNameString(context, item.name.orEmpty(), item.showNickname)
                },
                style = MaterialTheme.typography.titleMedium
            )

            if (!item.intro.isNullOrEmpty()) {
                Text(
                    text = item.intro,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
            }
        }
    }
}