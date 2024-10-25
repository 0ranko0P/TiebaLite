package com.huanchengfly.tieba.post.ui.page.forum.rule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.forum.rule.ForumRuleDetailViewModel.Companion.ForumRuleDetailUiState
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen

@Composable
fun ForumRuleDetailPage(
    forumId: Long,
    navigator: NavController,
    viewModel: ForumRuleDetailViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState

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
            if (uiState !is ForumRuleDetailUiState.Success) return@StateScreen
            MyScaffold(
                topBar = {
                    TitleCentredToolbar(
                        title = stringResource(id = R.string.title_forum_rule),
                        navigationIcon = {
                            BackNavigationIcon(navigator::navigateUp)
                        }
                    )
                }
            ) {
                val publishTime = uiState.publishTime
                val preface = uiState.preface
                val data = uiState.data

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(text = uiState.title, style = MaterialTheme.typography.h5)
                    uiState.author?.let {
                        UserHeader(
                            name = it.get { user_name },
                            nameShow = it.get { name_show },
                            portrait = it.get { portrait },
                            onClick = {
                                navigator.navigate(Destination.UserProfile(uid = it.get { user_id }))
                            },
                            desc = publishTime.takeIf { time -> time.isNotEmpty() }
                        )
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProvideTextStyle(value = MaterialTheme.typography.body1) {
                            Text(text = preface)
                            data.fastForEach {
                                if (it.title.isNotEmpty()) {
                                    Text(
                                        text = it.title,
                                        style = MaterialTheme.typography.subtitle1
                                    )
                                }
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
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
}