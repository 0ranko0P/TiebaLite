package com.huanchengfly.tieba.post.ui.page.forum.rule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.getOrNull
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.StringUtil
import kotlinx.collections.immutable.persistentListOf

@Composable
fun ForumRuleDetailPage(
    forumId: Long,
    navigator: NavController,
    viewModel: ForumRuleDetailViewModel = pageViewModel(),
) {
    val isLoading by viewModel.uiState.collectPartialAsState(
        prop1 = ForumRuleDetailUiState::isLoading,
        initial = true
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = ForumRuleDetailUiState::error,
        initial = null
    )
    val title by viewModel.uiState.collectPartialAsState(
        prop1 = ForumRuleDetailUiState::title,
        initial = ""
    )
    val publishTime by viewModel.uiState.collectPartialAsState(
        prop1 = ForumRuleDetailUiState::publishTime,
        initial = ""
    )
    val preface by viewModel.uiState.collectPartialAsState(
        prop1 = ForumRuleDetailUiState::preface,
        initial = ""
    )
    val data by viewModel.uiState.collectPartialAsState(
        prop1 = ForumRuleDetailUiState::data,
        initial = persistentListOf()
    )
    val author by viewModel.uiState.collectPartialAsState(
        prop1 = ForumRuleDetailUiState::author,
        initial = null
    )

    ProvideNavigator(navigator = navigator) {
        StateScreen(
            modifier = Modifier.fillMaxSize(),
            isEmpty = data.isEmpty(),
            isError = error != null,
            isLoading = isLoading,
            onReload = {
                viewModel.send(ForumRuleDetailUiIntent.Load(forumId))
            },
            errorScreen = { ErrorScreen(error = error.getOrNull()) }
        ) {
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(text = title, style = MaterialTheme.typography.h5)
                    author?.let {
                        UserHeader(
                            avatar = {
                                Avatar(
                                    data = StringUtil.getAvatarUrl(it.get { portrait }),
                                    size = Sizes.Small,
                                    contentDescription = null
                                )
                            },
                            name = {
                                Text(
                                    text = StringUtil.getUserNameString(
                                        LocalContext.current,
                                        it.get { user_name },
                                        it.get { name_show },
                                    ),
                                    style = LocalTextStyle.current
                                )
                            },
                            desc = (@Composable {
                                Text(text = publishTime)
                            }).takeIf { publishTime.isNotEmpty() }
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