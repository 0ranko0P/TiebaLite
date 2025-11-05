package com.huanchengfly.tieba.post.ui.common.prefs.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.common.theme.compose.block
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes

private val ContentPadding: Dp = 16.dp
private const val DisabledTextOpacity = 0.38f

//TODO add single line title?

/**
 * Simple Text with title and summary.
 * Used to show some information to the user and is the basis of all other preferences.
 *
 * @param title Main text which describes this preference
 * @param modifier the [Modifier] to be applied on this preference
 * @param summary Used to give some more information about what this preference is for
 * Mostly for internal use with custom Prefs.
 * @param onClick called when this preference is clicked. Parse null to disable this Pref
 * @param enabled controls the enabled state of this preference
 * @param leadingIcon icon will be drawn at the start of the preference
 * @param trailingContent content will be drawn at the end of the preference
 */
@Composable
fun TextPref(
    modifier: Modifier = Modifier,
    title: String,
    summary: String? = null,
    onClick: (() -> Unit)? = null,
    leadingIcon: @Composable BoxScope.() -> Unit = {},
    enabled: Boolean = onClick != null,
    trailingContent: @Composable (RowScope.() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    val titleColor = if (enabled) colors.onSurface else colors.onSurface.copy(DisabledTextOpacity)
    val summaryColor = if (enabled) colors.onSurfaceVariant else colors.onSurfaceVariant.copy(DisabledTextOpacity)

    Surface(
        modifier = modifier,
        color = Color.Transparent,
        contentColor = titleColor,
    ) {
        Row(
            modifier = Modifier
                .block { onClick?.let { clickable(enabled, onClick = it) } }
                .padding(ContentPadding),
            horizontalArrangement = Arrangement.spacedBy(ContentPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Restrict minimum leading icon size
            Box(
                modifier = Modifier
                    .size(Sizes.Small)
                    .padding(6.dp),
                content = leadingIcon
            )

            Column(modifier = Modifier.weight(1.0f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)

                if (summary != null) {
                    Text(summary, color = summaryColor, style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (trailingContent != null) {
                trailingContent()
            }
        }
    }
}

@NonRestartableComposable
@Composable
fun TextPref(
    modifier: Modifier = Modifier,
    title: String,
    summary: String? = null,
    onClick: (() -> Unit)? = null,
    leadingIcon: ImageVector?,
    enabled: Boolean = onClick != null,
    trailingContent: @Composable (RowScope.() -> Unit)? = null
) =
    TextPref(
        modifier = modifier,
        title = title,
        summary = summary,
        onClick = onClick,
        leadingIcon = {
            leadingIcon?.let { Icon(it, title, Modifier.fillMaxSize()) }
        },
        enabled = enabled,
        trailingContent = trailingContent
    )

@Composable
fun TipPref(
    modifier: Modifier = Modifier,
    text: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth()
            .padding(ContentPadding)
            .padding(start = 8.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Box(modifier = Modifier.padding(12.dp), content = text)
    }
}