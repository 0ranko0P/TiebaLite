package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ButtonElevation
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    elevation: ButtonElevation? = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
    shape: Shape = RoundedCornerShape(100),
    border: BorderStroke? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        backgroundColor = ExtendedTheme.colors.primary,
        contentColor = ExtendedTheme.colors.onAccent
    ),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val contentColor by colors.contentColor(enabled)
    Surface(
        modifier = Modifier
            .clip(shape)
            .clickable(
                onClick = onClick,
                enabled = enabled,
                interactionSource = interactionSource,
                indication = LocalIndication.current
            )
            .then(modifier),
        shape = shape,
        color = colors.backgroundColor(enabled).value,
        contentColor = contentColor.copy(alpha = 1f),
        border = border,
        elevation = elevation?.elevation(enabled, interactionSource)?.value ?: 0.dp,
    ) {
        CompositionLocalProvider(LocalContentAlpha provides contentColor.alpha) {
            ProvideTextStyle(
                value = MaterialTheme.typography.button
            ) {
                Row(
                    Modifier.padding(contentPadding),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }
        }
    }
}

@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = RoundedCornerShape(100),
    border: BorderStroke? = null,
    color: Color = ExtendedTheme.colors.text,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = shape,
        border = border,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = color.copy(alpha = 0.1f),
            contentColor = color,
            disabledBackgroundColor = color.copy(alpha = ContentAlpha.disabled * 0.1f),
            disabledContentColor = color.copy(alpha = ContentAlpha.disabled)
        ),
        contentPadding = contentPadding,
        content = content
    )
}

@Composable
fun NegativeButton(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = MaterialTheme.colors.onPrimary,
    onClick: () -> Unit
) = Button(
    onClick = onClick,
    modifier = modifier,
    colors = ButtonDefaults.buttonColors(
        backgroundColor = Color.Transparent,
        contentColor = color,
        disabledBackgroundColor = color.copy(alpha = ContentAlpha.disabled * 0.1f),
        disabledContentColor = color.copy(alpha = ContentAlpha.disabled)
    )
) {
    Text(text = text, color = color, fontWeight = FontWeight.Bold)
}


@Composable
fun PositiveButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onClick: () -> Unit
) = Button(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    colors = ButtonDefaults.buttonColors(
        backgroundColor = MaterialTheme.colors.primary,
        contentColor = MaterialTheme.colors.onPrimary,
        disabledBackgroundColor = Color.Transparent,
        disabledContentColor = MaterialTheme.colors.onSurface.copy(0.1f)
    )
) {
    Text(text = text, fontWeight = FontWeight.Bold)
}

@Composable
fun FavoriteButton(
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    backgroundColor: Color = Color.Transparent,
    favorite: Boolean,
    onClick: () -> Unit,
    favoriteCounter: @Composable RowScope.(contentColor: Color) -> Unit
) {
    val animatedColor by animateColorAsState(
        targetValue = if (favorite) ExtendedTheme.colors.accent else ExtendedTheme.colors.textSecondary,
        label = "FavoriteButtonColor"
    )
    val direction = LocalLayoutDirection.current

    Button(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor,
            contentColor = animatedColor
        )
    ) {
        Row(modifier = Modifier.align(Alignment.CenterVertically)) {
            if (direction == LayoutDirection.Ltr) favoriteCounter(animatedColor)

            Icon(
                imageVector = if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = stringResource(id = R.string.title_agree),
                modifier = Modifier.size(iconSize),
                tint = animatedColor
            )

            if (direction == LayoutDirection.Rtl) favoriteCounter(animatedColor)
        }
    }
}
