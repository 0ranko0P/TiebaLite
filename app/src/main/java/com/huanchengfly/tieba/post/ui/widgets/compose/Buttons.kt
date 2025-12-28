package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R

/**
 * Represents the container color for this button, depending on [enabled].
 *
 * @param enabled whether the button is enabled
 */
@Stable
internal fun ButtonColors.containerColor(enabled: Boolean): Color =
    if (enabled) containerColor else disabledContainerColor

/**
 * Represents the content color for this button, depending on [enabled].
 *
 * @param enabled whether the button is enabled
 */
@Stable
internal fun ButtonColors.contentColor(enabled: Boolean): Color =
    if (enabled) contentColor else disabledContentColor

@NonRestartableComposable
@Composable
fun NegativeButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit
) =
    TextButton(onClick = onClick, modifier = modifier) {
        Text(text = text, fontWeight = FontWeight.Bold)
    }

@NonRestartableComposable
@Composable
fun PositiveButton(
    @StringRes textRes: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    onClick: () -> Unit
) =
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors
    ) {
        Text(text = stringResource(textRes), fontWeight = FontWeight.Bold)
    }

@Composable
fun PositiveButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    onClick: () -> Unit
) =
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors
    ) {
        Text(text = text, fontWeight = FontWeight.Bold)
    }

@Composable
fun FavoriteButton(
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    favorite: Boolean,
    onClick: () -> Unit,
    favoriteCounter: @Composable RowScope.() -> Unit
) {
    val context = LocalContext.current
    val direction = LocalLayoutDirection.current

    Row(
        modifier = modifier
            .clip(shape = CircleShape)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                toggleableState = ToggleableState(favorite)
                contentDescription = context.getString(R.string.button_like)
            }
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val animatedColor by animateColorAsState(
            targetValue = if (favorite) MaterialTheme.colorScheme.primary else LocalContentColor.current,
        )

        ProvideContentColor(animatedColor) {
            if (direction == LayoutDirection.Ltr) favoriteCounter()
            Icon(
                imageVector = if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                modifier = Modifier.size(iconSize),
                contentDescription = null,
                tint = animatedColor
            )
            if (direction == LayoutDirection.Rtl) favoriteCounter()
        }
    }
}

@NonRestartableComposable
@Composable
fun OutlinedIconTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(
        contentColor = MaterialTheme.colorScheme.primary
    ),
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    icon: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) =
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        contentPadding = contentPadding
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier.size(ButtonDefaults.IconSize),
                contentAlignment = Alignment.Center,
                content = icon
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        content()
    }

@NonRestartableComposable
@Composable
fun OutlinedIconTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(
        contentColor = MaterialTheme.colorScheme.primary
    ),
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    vectorIcon: ImageVector? = null,
    text: String,
) =
    OutlinedIconTextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        contentPadding = contentPadding,
        icon = vectorIcon?.let {
            { Icon(vectorIcon, contentDescription = null, modifier = Modifier.matchParentSize()) }
        }
    ) {
        Text(text = text, fontSize = 13.sp) // Button default: MaterialTheme.typography.labelLarge
    }
