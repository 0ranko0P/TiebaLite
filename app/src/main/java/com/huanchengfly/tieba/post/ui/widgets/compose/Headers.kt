package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
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
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.utils.Util.getIconColorByLevel

@Composable
fun UserHeaderPlaceholder(
    avatarSize: Dp
) {
    UserHeader(
        avatar = {
            AvatarPlaceholder(avatarSize)
        },
        name = {
            Text(
                text = "Username",
                modifier = Modifier.placeholder(
                    visible = true,
                    highlight = PlaceholderHighlight.fade(),
                )
            )
        },
        desc = {
            Text(
                text = "Desc",
                modifier = Modifier.placeholder(
                    visible = true,
                    highlight = PlaceholderHighlight.fade(),
                )
            )
        }
    )
}

@Composable
fun UserHeader(
    avatar: @Composable () -> Unit,
    name: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    desc: @Composable (() -> Unit)? = null,
    content: @Composable (RowScope.() -> Unit)? = null,
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        )
    } else Modifier
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = clickableModifier,
        ) {
            avatar()
        }
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
                    value = MaterialTheme.typography.caption.merge(
                        TextStyle(
                            color = ExtendedTheme.colors.textSecondary,
                            fontSize = 11.sp
                        )
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

@Preview("UserNameText")
@Composable
fun UserNameTextPreview() = TiebaLiteTheme {
    ProvideTextStyle(MaterialTheme.typography.subtitle2) {
        UserNameText(Modifier.padding(10.dp), userName = "我是谁", userLevel = 5, isLz = true)
    }
}
