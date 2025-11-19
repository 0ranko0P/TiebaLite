package com.huanchengfly.tieba.post.ui.widgets.compose.preference

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes

private const val DisabledTextOpacity = 0.38f

private val ContentPadding: Dp = 16.dp

/**
 * Basic preference component.
 *
 * @param title text which describes this preference.
 * @param modifier the [Modifier] to be applied on this preference.
 * @param summary used to give some more information about what this preference is for.
 *   Mostly for internal use with custom Prefs.
 * @param enabled controls the enabled state of this preference.
 * @param leadingContent content will be drawn at the beginning of the
 *   preference, expected to be an [Icon].
 * @param trailingContent content will be drawn at the end of the preference.
 */
@Composable
fun BasePreference(
    modifier: Modifier = Modifier,
    title: String,
    summary: String? = null,
    enabled: Boolean,
    leadingContent: @Composable (BoxScope.() -> Unit)? = null,
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
            modifier = Modifier.padding(ContentPadding),
            horizontalArrangement = Arrangement.spacedBy(ContentPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Restrict minimum leading icon size
            if (leadingContent != null) {
                Box(
                    modifier = Modifier
                        .size(Sizes.Small)
                        .padding(6.dp),
                    content = leadingContent
                )
            } else {
                Spacer(modifier = Modifier.size(Sizes.Small))
            }

            Column(modifier = Modifier.weight(1.0f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)

                if (!summary.isNullOrEmpty()) {
                    Text(summary, color = summaryColor, style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (trailingContent != null) {
                trailingContent()
            }
        }
    }
}

/**
 * Basic preference component.
 *
 * @param title text which describes this preference.
 * @param modifier the [Modifier] to be applied on this preference.
 * @param summary used to give some more information about what this preference is for.
 *   Mostly for internal use with custom Prefs.
 * @param enabled controls the enabled state of this preference.
 * @param leadingIcon optional leading icon to be drawn at the beginning of the preference.
 * @param trailingContent optional content will be drawn at the end of the preference.
 */
@NonRestartableComposable
@Composable
fun BasePreference(
    modifier: Modifier = Modifier,
    title: String,
    summary: String? = null,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingContent: @Composable (RowScope.() -> Unit)? = null
) =
    BasePreference(
        modifier = modifier,
        title = title,
        summary = summary,
        leadingContent = leadingIcon?.let { icon ->
            { Icon(imageVector = icon, contentDescription = null, Modifier.fillMaxSize()) }
        },
        enabled = enabled,
        trailingContent = trailingContent
    )
