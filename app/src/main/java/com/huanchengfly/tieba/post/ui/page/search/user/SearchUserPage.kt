package com.huanchengfly.tieba.post.ui.page.search.user

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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectCommonUiEventWithLifecycle
import com.huanchengfly.tieba.post.ui.models.search.SearchUser
import com.huanchengfly.tieba.post.ui.page.Destination.UserProfile
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen

@Composable
fun SearchUserPage(
    modifier: Modifier = Modifier,
    keyword: String,
    contentPadding: PaddingValues,
    listState: LazyListState = rememberLazyListState(),
    viewModel: SearchUserViewModel = hiltViewModel(),
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
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            val navigator = LocalNavController.current
            val headerContentType = Integer.MAX_VALUE

            val onUserClickedListener: (SearchUser) -> Unit = {
                navigator.navigate(UserProfile(uid = it.id))
            }

            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val exactMatchUser = uiState.exactMatch
            val fuzzyMatchUsers = uiState.fuzzyMatch

            MyLazyColumn(
                modifier = modifier.fillMaxSize(),
                state = listState,
                contentPadding = contentPadding,
            ) {
                if (exactMatchUser != null) {
                    item(key = "ExactMatchHeader", contentType = headerContentType) {
                        Chip(
                            text = stringResource(id = R.string.title_exact_match),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            invertColor = true
                        )
                    }

                    item(key = exactMatchUser.id) {
                        SearchUserItem(user = exactMatchUser, onClick = onUserClickedListener)
                    }
                }

                if (fuzzyMatchUsers.isNotEmpty()) {
                    item(key = "FuzzyMatchHeader", contentType = headerContentType) {
                        Chip(
                            text = stringResource(id = R.string.title_fuzzy_match_user),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(fuzzyMatchUsers, key = { it.id }) {
                        SearchUserItem(user = it, onClick = onUserClickedListener)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchUserItem(user: SearchUser, onClick: (SearchUser) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(user) }
            .semantics(mergeDescendants = true) {
                role = Role.DropdownList
                contentDescription = user.formattedName
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Avatar(data = user.avatar, size = Sizes.Medium)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = user.formattedName, style = MaterialTheme.typography.titleMedium)

            if (!user.intro.isNullOrEmpty()) {
                Text(
                    text = user.intro,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}