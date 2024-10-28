package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.fade
import com.google.accompanist.placeholder.material.placeholder
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.models.UserData
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.Util.getIconColorByLevel

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
            ProvideTextStyle(value = MaterialTheme.typography.subtitle2) {
                name()
            }

            if (desc != null) {
                ProvideTextStyle(
                    value = MaterialTheme.typography.caption.copy(
                        color = ExtendedTheme.colors.textSecondary,
                        fontSize = 11.sp
                    )
                ) {
                    desc()
                }
            }
        }
        content?.invoke(this)
    }
}

@Composable
fun UserHeader(
    modifier: Modifier = Modifier,
    name: String,
    nameShow: String?,
    portrait: String?,
    onClick: (() -> Unit)? = null,
    desc: String? = null,
    content: (@Composable RowScope.() -> Unit)? = null
) = UserHeader(
    modifier = modifier,
    name = remember { StringUtil.getUserNameString(App.INSTANCE, name, nameShow) },
    avatar = remember { StringUtil.getAvatarUrl(portrait) },
    onClick = onClick,
    desc = desc,
    content = content
)

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
            modifier = onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier,
            contentDescription = name
        )
    },
    name = {
        Text(text = name)
    },
    desc = desc?.let { { Text(text = desc) } },
    content = content
)

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
            modifier = onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier,
            contentDescription = author.name
        )
    },
    name = {
        UserNameText(
            userName = StringUtil.getUserNameString(LocalContext.current, author.name, author.nameShow),
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
        Text(text = userName, color = LocalContentColor.current)

        val levelColor = Color(getIconColorByLevel("$userLevel"))

        TextChip(
            text = userLevel.toString(),
            fontSize = 11.sp,
            color = levelColor,
            backgroundColor = levelColor.copy(0.25f)
        )

        if (isLz) {
            TextChip(text = stringResource(id = R.string.tip_lz), fontSize = 9.sp)
        }

        if (!bawuType.isNullOrBlank()) {
            TextChip(
                text = bawuType,
                fontSize = 9.sp,
                color = ExtendedTheme.colors.primary,
                backgroundColor = ExtendedTheme.colors.primary.copy(0.1f)
            )
        }
    }
}

@Composable
private fun TextChip(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    color: Color = ExtendedTheme.colors.onChip,
    backgroundColor: Color = ExtendedTheme.colors.chip,
    shape: Shape = RoundedCornerShape(100),
) = Text(
    text = text,
    modifier = modifier
        .fillMaxHeight()
        .clip(shape)
        .background(color = backgroundColor)
        .padding(horizontal = 8.dp, vertical = 1.dp),
    color = color,
    fontSize = fontSize,
    textAlign = TextAlign.Center,
    style = style
)

@Preview("UserDataHeader")
@Composable
fun UserDataHeaderPreview() = TiebaLiteTheme {
    UserDataHeader(
        author = UserData.Empty.copy(name = "我是谁", levelId = 99, bawuType = "小吧主"),
        desc = "一分钟前 · 第 10 楼 · 来自中国)"
    )
}

@Preview("UserNameText")
@Composable
fun UserNameTextPreview() = TiebaLiteTheme {
    ProvideTextStyle(MaterialTheme.typography.subtitle2) {
        UserNameText(Modifier.padding(10.dp), userName = "我是谁", userLevel = 5, isLz = true)
    }
}
