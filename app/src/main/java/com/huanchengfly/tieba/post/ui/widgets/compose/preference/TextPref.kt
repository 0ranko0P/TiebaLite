package com.huanchengfly.tieba.post.ui.widgets.compose.preference

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.common.theme.compose.block

/**
 * Simple Text Preference. Used to show some information to the user.
 *
 * @param title Text which describes this preference.
 * @param modifier the [Modifier] to be applied on this preference.
 * @param summary Used to give some more information about what this preference is for.
 * @param onClick called when this preference is clicked. Parse null to disable this Pref
 * @param enabled controls the enabled state of this preference
 * @param leadingContent content will be drawn at the beginning of the
 *   preference, expected to be an [Icon].
 * @param trailingContent content will be drawn at the end of the preference
 */
@NonRestartableComposable
@Composable
fun TextPref(
    modifier: Modifier = Modifier,
    title: String,
    summary: String? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = onClick != null,
    leadingContent: @Composable (BoxScope.() -> Unit)? = null,
    trailingContent: @Composable (RowScope.() -> Unit)? = null
) {
    BasePreference(
        modifier = modifier.block {
            if (onClick != null) {
                clickable(enabled, role = Role.Button, onClick = onClick)
            } else {
                semantics(mergeDescendants = true) { isTraversalGroup = true }
            }
        },
        title = title,
        summary = summary,
        enabled = enabled,
        leadingContent = leadingContent,
        trailingContent = trailingContent
    )
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
        enabled = enabled,
        leadingContent = leadingIcon?.let { icon ->
            { Icon(imageVector = icon, contentDescription = null, Modifier.fillMaxSize()) }
        },
        trailingContent = trailingContent
    )

@Composable
fun TipPref(
    modifier: Modifier = Modifier,
    text: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth()
            .padding(16.dp)
            .padding(start = 8.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Box(modifier = Modifier.padding(12.dp), content = text)
    }
}