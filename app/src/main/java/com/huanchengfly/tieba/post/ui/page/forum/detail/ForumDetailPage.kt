package com.huanchengfly.tieba.post.ui.page.forum.detail

import android.content.Context
import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.frsPage.ForumInfo
import com.huanchengfly.tieba.post.theme.Grey300
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.BebasFamily
import com.huanchengfly.tieba.post.ui.page.Destination.ForumDetail
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * 导航至 [ForumDetailPage], 因为ForumDetailFlow 未登录时返回的数据不全, 需额外提供ForumInfo.
 *
 * @see ForumDetail
 * */
fun NavController.navigateForumDetailPage(forum: ForumInfo/* Big Parcelable */, context: Context) {
    navigate(
        route = ForumDetail(
            forumId = forum.id,
            forumName = forum.name,
            avatar = forum.avatar,
            threadCount = forum.thread_num,
            postCount = forum.post_num,
            managers = forum.managers.mapTo(ArrayList(forum.managers.size)) {
                val displayName = StringUtil.getUserNameString(context, it.name, it.show_name)
                ManagerData(id = it.id, name = displayName, portrait = it.portrait)
            }
        )
    )
}

@Serializable
@Parcelize
data class ManagerData(
    val id: Long,
    val name: String,
    val portrait: String
): Parcelable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumDetailPage(
    avatar: String,
    postCount: Int,
    managers: List<ManagerData>,
    onBack: () -> Unit,
    viewModel: ForumDetailViewModel = hiltViewModel(),
    onManagerClicked: (ManagerData) -> Unit,
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    StateScreen(
        isError = uiState is ForumDetailUiState.Error,
        isLoading = uiState === ForumDetailUiState.Loading,
        onReload = viewModel::reload,
        errorScreen = {
            ErrorScreen(error = (uiState as ForumDetailUiState.Error).error)
        },
        modifier = Modifier.fillMaxSize(),
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(id = R.string.title_forum_info)) },
                    navigationIcon = { BackNavigationIcon(onBack) }
                )
            }
        ) { paddingValues ->
            val state = uiState as ForumDetailUiState.Success

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ForumDetailContent(
                    avatar = avatar,
                    name = state.name,
                    memberCount = state.memberCount,
                    threadCount = state.threadCount,
                    postCount = postCount
                )

                IntroItem(slogan = state.slogan, intro = state.intro)

                if (managers.isEmpty()) {
                    Chip(text = stringResource(id = R.string.title_forum_manager_none))
                    return@Scaffold
                }
                Chip(text = stringResource(id = R.string.title_forum_manager))

                LazyRow {
                    items(managers, key = { it.id }) {
                        ManagerItem(name = it.name, portrait = it.portrait) {
                            onManagerClicked(it)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ForumDetailContent(
    avatar: String,
    name: String,
    memberCount: Int,
    threadCount: Int,
    postCount: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(
            data = avatar,
            size = Sizes.Medium,
            contentDescription = name,
        )
        Text(
            text = stringResource(id = R.string.title_forum, name),
            style = MaterialTheme.typography.titleLarge
        )
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        ProvideTextStyle(
            value = MaterialTheme.typography.titleLarge.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp,
            )
        ) {
            Row(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .padding(top = 20.dp, bottom = 16.dp), // Visual aligned
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatCardItem(
                    statNum = memberCount,
                    statTitle = R.string.text_stat_follow
                )
                VerticalDivider(color = Grey300)
                StatCardItem(
                    statNum = threadCount,
                    statTitle = R.string.text_stat_threads
                )
                VerticalDivider(color = Grey300)
                StatCardItem(
                    statNum = postCount,
                    statTitle = R.string.title_stat_posts_num
                )
            }
        }
    }
}

@Composable
private fun IntroItem(modifier: Modifier = Modifier, slogan: String, intro: String) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Chip(text = stringResource(id = R.string.title_forum_intro))

        Text(
            text = slogan,
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = intro,
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun ManagerItem(
    modifier: Modifier = Modifier,
    name: String,
    portrait: String,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .semantics {
                role = Role.Image
                isTraversalGroup = true
                contentDescription = name
            }
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Avatar(
            data = remember { StringUtil.getAvatarUrl(portrait) },
            size = Sizes.Medium,
            contentDescription = null,
        )
        Text(text = name, modifier = Modifier.padding(4.dp))
    }
}

@Composable
private fun RowScope.StatCardItem(statNum: Int, @StringRes statTitle: Int) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = statNum.getShortNumString(),
            fontFamily = BebasFamily,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = stringResource(statTitle), fontSize = 14.sp)
    }
}

@Preview("ForumDetailPage", backgroundColor = 0xFFFFFFFF, showBackground = true)
@Composable
fun PreviewForumDetailPage() {
    TiebaLiteTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ForumDetailContent(
                avatar = "",
                name = "minecraft",
                memberCount = 2520287,
                threadCount = 31531580,
                postCount = 10297773,
            )
            IntroItem(
                slogan = "位于百度贴吧的像素点之家",
                intro = "minecraft……"
            )
        }
    }
}