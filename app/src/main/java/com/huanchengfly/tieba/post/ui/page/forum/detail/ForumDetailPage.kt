package com.huanchengfly.tieba.post.ui.page.forum.detail

import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.protos.frsPage.ForumInfo
import com.huanchengfly.tieba.post.api.models.protos.plainText
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.HorizontalDivider
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Navigate to [ForumDetailPage]
 * */
fun DestinationsNavigator.navigateForumDetailPage(forumInfo: ForumInfo) {
    this.navigate(
        ForumDetailPageDestination(
            forumId = forumInfo.id,
            avatar = forumInfo.avatar,
            name = forumInfo.name,
            slogan = forumInfo.slogan,
            memberCount = forumInfo.member_num,
            threadCount = forumInfo.thread_num,
            postCount = forumInfo.post_num
        )
    )
}

private fun getForumIntro(forumId: Long): Flow<String> = TiebaApi.getInstance()
    .getForumDetailFlow(forumId)
    .map {
        return@map it.data_?.forum_info?.content?.plainText
            ?: throw NullPointerException("Data is null: ${it.error?.error_msg}, forumId: $forumId")
    }

@Destination
@Composable
fun ForumDetailPage(
    forumId: Long,
    avatar: String,
    name: String,
    slogan: String,
    memberCount: Int,
    threadCount: Int,
    postCount: Int,
    navigator: DestinationsNavigator,
) = MyScaffold(
    topBar = {
        TitleCentredToolbar(
            title = {
                Text(text = stringResource(id = R.string.title_forum_info))
            },
            navigationIcon = {
                BackNavigationIcon { navigator.navigateUp() }
            }
        )
    }
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ForumDetailContent(avatar, name, memberCount, threadCount, postCount)
        IntroItem(forumId = forumId, slogan = slogan)
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
            style = MaterialTheme.typography.h6
        )
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color = ExtendedTheme.colors.chip)
            .padding(vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatCardItem(
            statNum = memberCount,
            statText = stringResource(id = R.string.text_stat_follow)
        )
        HorizontalDivider(color = Color(if (ExtendedTheme.colors.isNightMode) 0xFF808080 else 0xFFDEDEDE))
        StatCardItem(
            statNum = threadCount,
            statText = stringResource(id = R.string.text_stat_threads)
        )
        HorizontalDivider(color = Color(if (ExtendedTheme.colors.isNightMode) 0xFF808080 else 0xFFDEDEDE))
        StatCardItem(
            statNum = postCount,
            statText = stringResource(id = R.string.title_stat_posts_num)
        )
    }
}

@Composable
private fun IntroItem(modifier: Modifier = Modifier, slogan: String, intro: String?) = Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    Chip(text = stringResource(id = R.string.title_forum_intro))
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(text = slogan, style = MaterialTheme.typography.body1)
        if (!intro.isNullOrEmpty()) {
            Text(text = intro, style = MaterialTheme.typography.body1)
        }
    }
}

@Composable
private fun IntroItem(modifier: Modifier = Modifier, forumId: Long, slogan: String) {
    val introFlow = remember(forumId) { getForumIntro(forumId) }
    val intro by introFlow.collectAsState(initial = null) // ignore errors

    IntroItem(modifier, slogan, intro)
}

@Composable
private fun RowScope.StatCardItem(statNum: Int, statText: String) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = statNum.getShortNumString(),
            fontSize = 20.sp,
            fontFamily = FontFamily(
                Typeface.createFromAsset(
                    LocalContext.current.assets,
                    "bebas.ttf"
                )
            ),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = statText,
            fontSize = 12.sp,
            color = ExtendedTheme.colors.textSecondary
        )
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