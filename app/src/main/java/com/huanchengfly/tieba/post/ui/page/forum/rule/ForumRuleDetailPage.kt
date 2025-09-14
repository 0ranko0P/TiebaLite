package com.huanchengfly.tieba.post.ui.page.forum.rule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.util.fastForEach
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen

@Composable
fun ForumRuleDetailPage(
    navigator: NavController,
    viewModel: ForumRuleDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ProvideNavigator(navigator = navigator) {
        StateScreen(
            modifier = Modifier.fillMaxSize(),
            isError = uiState is ForumRuleDetailUiState.Error,
            isLoading = uiState == ForumRuleDetailUiState.Loading,
            onReload = viewModel::reload,
            errorScreen = {
                ErrorScreen(error = (uiState as ForumRuleDetailUiState.Error).throwable)
            }
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

                val state = uiState as? ForumRuleDetailUiState.Success ?: return@Scaffold
                val publishTime = state.publishTime
                val preface = state.preface
                val data = state.data
                val ruleTitleStyle = MaterialTheme.typography.titleMedium

                LazyColumn(
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp),
                    contentPadding = contentPadding,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(text = state.title, style = MaterialTheme.typography.headlineSmall)
                    }

                    state.author?.item?.let {
                        item {
                            UserHeader(
                                name = it.user_name,
                                nameShow = it.name_show,
                                portrait = it.portrait,
                                onClick = {
                                    navigator.navigate(Destination.UserProfile(uid = it.user_id))
                                },
                                desc = publishTime.takeIf { time -> time.isNotEmpty() }
                            )
                        }
                    }

                    item {
                        Text(text = preface, style = MaterialTheme.typography.bodyLarge)
                    }

                    items(
                        items = data,
                        key = { it -> it.hashCode() },
                        contentType = { R.string.title_forum_rule }
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            data.fastForEach {
                                it.title?.let { title ->
                                    Text(text = title, style = ruleTitleStyle)
                                }

                                it.contentRenders.fastForEach { render ->
                                    render.Render()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}