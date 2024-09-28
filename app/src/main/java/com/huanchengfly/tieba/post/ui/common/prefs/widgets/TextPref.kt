package com.huanchengfly.tieba.post.ui.common.prefs.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes

private val ContentPadding: Dp = 16.dp

//TODO add single line title?

/**
 * Simple Text with title and summary.
 * Used to show some information to the user and is the basis of all other preferences.
 *
 * @param title Main text which describes the Pref
 * @param modifier Modifier applied to the Text aspect of this Pref
 * @param summary Used to give some more information about what this Pref is for
 * Mostly for internal use with custom Prefs
 * @param onClick Will be called when user clicks on the Pref. Parse null to disable this Pref
 * @param enabled If false, this Pref cannot be checked/unchecked
 * @param leadingIcon Icon which is positioned at the start of the Pref
 * @param trailingContent Composable content which is positioned at the end of the Pref
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
    val alpha = if (onClick != null) 0.9f else ContentAlpha.disabled

    CompositionLocalProvider(LocalContentAlpha provides alpha) {
        val textColor = LocalContentColor.current.copy(LocalContentAlpha.current)

        Row(
            modifier = modifier
                .minimumInteractiveComponentSize()
                .then(if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier)
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
                Text(title, color = textColor, style = MaterialTheme.typography.subtitle1)

                if (summary != null) {
                    Text(
                        text = summary,
                        color = textColor.copy(ContentAlpha.medium),
                        style = MaterialTheme.typography.body2
                    )
                }
            }

            if (trailingContent != null) {
                trailingContent()
            }
        }
    }
}

@Composable
fun TextPref(
    modifier: Modifier = Modifier,
    title: String,
    summary: String? = null,
    onClick: (() -> Unit)? = null,
    leadingIcon: ImageVector?,
    enabled: Boolean = onClick != null,
    trailingContent: @Composable (RowScope.() -> Unit)? = null
) = TextPref(
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