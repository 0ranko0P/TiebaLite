package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.theme.ProvideContentColorTextStyle
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.common.theme.compose.onNotNull
import com.huanchengfly.tieba.post.ui.models.UserData
import com.huanchengfly.tieba.post.utils.ColorUtils.getIconColorByLevel

@Composable
fun UserHeaderPlaceholder(
    modifier: Modifier = Modifier,
    avatarSize: Dp = Sizes.Small
) = UserHeader(
    modifier = modifier,
    avatar = {
        AvatarPlaceholder(avatarSize)
    },
    name = {
        Text(
            text = "Username",
            modifier = Modifier.placeholder(highlight = PlaceholderHighlight.fade())
        )
    },
    desc = {
        Text(
            text = "Desc",
            modifier = Modifier.placeholder(highlight = PlaceholderHighlight.fade())
        )
    }
)

@Composable
fun UserHeader(
    modifier: Modifier = Modifier,
    avatar: @Composable BoxScope.() -> Unit,
    name: @Composable () -> Unit,
    desc: @Composable (() -> Unit)? = null,
    content: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(contentAlignment = Alignment.Center, content = avatar)

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            ProvideTextStyle(value = MaterialTheme.typography.labelLarge, content = name)

            if (desc != null) {
                ProvideContentColorTextStyle(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    content = desc
                )
            }
        }
        content?.invoke(this)
    }
}

@Composable
fun UserHeader(
    modifier: Modifier = Modifier,
    name: String,
    avatar: String?,
    onClick: (() -> Unit)? = null,
    desc: String? = null,
    content: (@Composable RowScope.() -> Unit)? = null
) = UserHeader(
    modifier = modifier,
    avatar = {
        Avatar(
            data = avatar,
            size = Sizes.Small,
            modifier = Modifier.onNotNull(onClick) { clickableNoIndication(onClick = it) },
            contentDescription = name
        )
    },
    name = {
        Text(text = name)
    },
    desc = desc?.let { { Text(text = desc) } },
    content = content
)

@NonRestartableComposable
@Composable
fun UserDataHeader(
    modifier: Modifier = Modifier,
    author: UserData,
    desc: String,
    onClick: (() -> Unit)? = null,
    content: @Composable (RowScope.() -> Unit)? = null,
) = UserHeader(
    modifier = modifier,
    avatar = {
        Avatar(
            data = author.avatarUrl,
            size = Sizes.Small,
            modifier = Modifier.onNotNull(onClick) { clickableNoIndication(onClick = it) },
            contentDescription = author.name
        )
    },
    name = {
        UserNameText(
            userName = author.userShowBothName ?: author.name,
            userLevel = author.levelId,
            isLz = author.isLz,
            bawuType = author.bawuType,
        )
    },
    desc = { Text(text = desc) },
    content = content
)

@Composable
fun UserNameText(
    modifier: Modifier = Modifier,
    userName: String,
    userLevel: Int,
    isLz: Boolean,
    bawuType: String? = null
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = userName)

        val levelColor = Color(getIconColorByLevel(userLevel))

        TextChip(
            text = userLevel.toString(),
            fontSize = 11.sp,
            color = levelColor,
            backgroundColor = levelColor.copy(0.25f),
        )

        if (isLz) {
            TextChip(text = stringResource(id = R.string.tip_lz))
        }

        if (!bawuType.isNullOrBlank()) {
            TextChip(
                text = bawuType,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                backgroundColor = MaterialTheme.colorScheme.primaryContainer
            )
        }
    }
}

private val DefaultChipTextStyle = TextStyle(
    fontSize = 10.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 0.5.sp,
    lineHeight = 12.sp,
)

@NonRestartableComposable
@Composable
private fun TextChip(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    backgroundColor: Color = MaterialTheme.colorScheme.tertiary,
    color: Color = MaterialTheme.colorScheme.onTertiary,
) = Text(
    text = text,
    modifier = modifier
        .background(color = backgroundColor, CircleShape)
        .padding(horizontal = 8.dp, vertical = 1.5.dp),
    color = color,
    fontSize = fontSize,
    textAlign = TextAlign.Center,
    style = LocalTextStyle.current.merge(DefaultChipTextStyle)
)

@Preview("UserNameText")
@Composable
private fun UserNameTextPreview() = TiebaLiteTheme {
    ProvideTextStyle(MaterialTheme.typography.labelLarge) {
        UserNameText(Modifier.padding(10.dp), userName = "我是谁", userLevel = 5, isLz = true)
    }
}
