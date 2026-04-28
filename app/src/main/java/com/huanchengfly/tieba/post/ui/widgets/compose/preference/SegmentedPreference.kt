package com.huanchengfly.tieba.post.ui.widgets.compose.preference

import androidx.annotation.StringRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

val PreferenceItemPaddingCompact = PaddingValues(horizontal = 16.dp, vertical = 12.dp)

val PreferenceItemPadding = PaddingValues(16.dp)

val SegmentedListItemColors: ListItemColors
    @Composable get() = MaterialTheme.colorScheme.run {
        ListItemDefaults.segmentedColors(
            containerColor = secondaryContainer.copy(alpha = 0.4f),
            disabledContainerColor = secondaryContainer.copy(0.28f),
        )
    }

@NonRestartableComposable
@Composable
fun SegmentedPreference(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    shapes: ListItemShapes = ListItemDefaults.shapes(),
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    colors: ListItemColors = SegmentedListItemColors,
    summary: @Composable (() -> Unit)? = null,
    contentPadding: PaddingValues = if (summary == null) {
        PreferenceItemPadding
    } else {
        PreferenceItemPaddingCompact
    },
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit = {},
) =
    androidx.compose.material3.SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
        modifier = modifier,
        enabled = enabled,
        leadingContent = leadingIcon,
        trailingContent = trailingContent,
        supportingContent = summary,
        verticalAlignment = Alignment.CenterVertically,
        colors = colors,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
    ) {
        ProvideTextStyle(MaterialTheme.typography.titleMedium, content = title)
    }

@Composable
fun SegmentedPreference(
    modifier: Modifier = Modifier,
    title: String,
    shapes: ListItemShapes = ListItemDefaults.shapes(),
    leadingIcon: ImageVector? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    colors: ListItemColors = SegmentedListItemColors,
    summary: String? = null,
    contentPadding: PaddingValues = if (summary == null) {
        PreferenceItemPadding
    } else {
        PreferenceItemPaddingCompact
    },
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit = {},
) =
    SegmentedPreference(
        modifier = modifier,
        title = {
            Text(text = title)
        },
        shapes = shapes,
        leadingIcon = leadingIcon?.let { imageVector ->
            { Icon(imageVector, contentDescription = null) }
        },
        trailingContent = trailingContent,
        colors = colors,
        summary = summary?.let { { Text(text = it) } },
        contentPadding = contentPadding,
        enabled = enabled,
        interactionSource = interactionSource,
        onClick = onClick,
    )

@Composable
fun SegmentedPreference(
    modifier: Modifier = Modifier,
    @StringRes title: Int,
    shapes: ListItemShapes = ListItemDefaults.shapes(),
    leadingIcon: ImageVector? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    colors: ListItemColors = SegmentedListItemColors,
    @StringRes summary: Int? = null,
    contentPadding: PaddingValues = if (summary == null) {
        PreferenceItemPadding
    } else {
        PreferenceItemPaddingCompact
    },
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit,
) =
    SegmentedPreference(
        modifier = modifier,
        title = {
            Text(text = stringResource(id = title))
        },
        shapes = shapes,
        leadingIcon = leadingIcon?.let { imageVector ->
            { Icon(imageVector, contentDescription = null) }
        },
        trailingContent = trailingContent,
        colors = colors,
        summary = summary?.let {
            { Text(text = stringResource(id = it)) }
        },
        contentPadding = contentPadding,
        enabled = enabled,
        interactionSource = interactionSource,
        onClick = onClick,
    )
