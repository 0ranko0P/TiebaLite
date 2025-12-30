package com.huanchengfly.tieba.post.ui.page.forum.rule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.SharedTransitionUserHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen

@Composable
fun ForumRuleDetailPage(
    navigator: NavController,
    viewModel: ForumRuleDetailViewModel = hiltViewModel(),
) {
    val isLoading by viewModel.uiState.collectPartialAsState(
        prop1 = ForumRuleDetailUiState::isLoading,
        initial = true
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = ForumRuleDetailUiState::error,
        initial = null
    )

    ProvideNavigator(navigator = navigator) {
        StateScreen(
            isLoading = isLoading,
            error = error,
            onReload = viewModel::reload,
        ) {
            Scaffold(
                topBar = {
                    TitleCentredToolbar(
                        title = stringResource(id = R.string.title_forum_rule),
                        navigationIcon = {
                            BackNavigationIcon(navigator::navigateUp)
                        }
                    )
                }
            ) { contentPadding ->
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val forumRule = state.data ?: return@Scaffold

                val ruleTitleStyle = MaterialTheme.typography.titleMedium
                val ruleContentStyle = MaterialTheme.typography.bodyLarge

                LazyColumn(
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp),
                    contentPadding = contentPadding,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(text = forumRule.headLine, style = MaterialTheme.typography.titleLarge)
                    }

                    forumRule.author?.let {
                        item(key = it.id) {
                            SharedTransitionUserHeader(
                                user = it,
                                desc = forumRule.publishTime,
                                onClick = { navigator.navigate(Destination.UserProfile(user = it)) },
                            )
                        }
                    }

                    item {
                        Text(text = forumRule.preface, style = ruleContentStyle)
                    }

                    items(
                        items = forumRule.data,
                        key = { it -> it.hashCode() },
                        contentType = { R.string.title_forum_rule }
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = it.title, style = ruleTitleStyle)

                            it.content?.let { content ->
                                Text(text = content, style = ruleContentStyle)
                            }
                        }
                    }
                }
            }
        }
    }
}