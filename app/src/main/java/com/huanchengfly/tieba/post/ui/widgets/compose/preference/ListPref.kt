package com.huanchengfly.tieba.post.ui.widgets.compose.preference

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.fastRoundToInt
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import kotlinx.coroutines.flow.filterIsInstance

private val MenuItemLeadingIconSize: Dp
    get() = Sizes.Small

/**
 * Preference that shows a list of entries in a DropDownMenu where a single entry can be selected
 * at one time.
 *
 * @param modifier the [Modifier] to be applied on this preference.
 * @param value current selected value of this preference.
 * @param onValueChange callback to be invoked when user selected new option in [options]
 * @param options all available options of this preference with description
 * @param title text which describes this preference.
 * @param summary used to give some more information about what this preference is for.
 * @param useSelectedAsSummary set true to use the current selected option description as [summary]
 * @param optionsIconSupplier leading icon to be drawn at the beginning of option menu item.
 * @param leadingIcon leading icon to be drawn at the beginning of the preference, typically [Icon].
 * @param shapes the [ListItemShapes] that this list item will use to morph between depending on the
 *   user's interaction with the list item. The base shape depends on the index of the item within
 *   the overall list. See [ListItemDefaults.segmentedShapes].
 * @param enabled controls the enabled state of this list pref. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this preference.
 */
@Composable
fun <T> SegmentedListPreference(
    modifier: Modifier = Modifier,
    value: T,
    onValueChange: (T) -> Unit,
    options: StringLabelOptions<T>,
    title: String,
    summary: String? = null,
    useSelectedAsSummary: Boolean = summary == null,
    optionsIconSupplier: @Composable ((T) -> Unit)? = null,
    leadingIcon: ImageVector? = null,
    shapes: ListItemShapes = ListItemDefaults.shapes(),
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val onDismissRequest: () -> Unit = { expanded = !expanded }

    val menuPressOffsetY = LocalMinimumInteractiveComponentSize.current / 2
    var menuPressOffsetX by remember { mutableIntStateOf(0) }
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions
            .filterIsInstance<PressInteraction.Press>()
            .collect {
                menuPressOffsetX = it.pressPosition.x.fastRoundToInt()
            }
    }

    SegmentedPreference(
        title = title,
        shapes = shapes,
        enabled = enabled,
        leadingIcon = leadingIcon,
        summary = if (useSelectedAsSummary) options[value] else summary,
        interactionSource = interactionSource,
        onClick = onDismissRequest,
    )

    Box(
        modifier = Modifier.offset {
            IntOffset(x = menuPressOffsetX, y = -menuPressOffsetY.roundToPx())
        },
    ) {
        DropdownMenuPopup(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            offset = DpOffset.Zero,
        ) {
            val itemShapes = MenuDefaults.itemShapes() // Single group
            val itemContentPadding = if (optionsIconSupplier == null) {
                MenuDefaults.DropdownMenuSelectableItemContentPadding
            } else {
                MenuDefaults.DropdownMenuItemContentPadding
            }

            DropdownMenuGroup(
                shapes = MenuDefaults.groupShapes(),
            ) {
                options.forEach { (option: T, optionLabel: String) ->
                    val checked = option == value
                    DropdownMenuItem(
                        text = { Text(text = optionLabel) },
                        shapes = itemShapes,
                        leadingIcon = optionsIconSupplier?.let { {
                            Box(
                                modifier = Modifier.size(MenuItemLeadingIconSize),
                                contentAlignment = Alignment.Center,
                                content = { optionsIconSupplier(option) }
                            )
                        } },
                        trailingIcon = {
                            if (checked) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                                    contentDescription = null
                                )
                            } else {
                                Spacer(modifier = Modifier.size(MenuDefaults.LeadingIconSize))
                            }
                        },
                        checked = checked,
                        contentPadding = itemContentPadding,
                        onCheckedChange = {
                            if (it) onValueChange(option)
                            onDismissRequest()
                        },
                    )
                }
            }
        }
    }
}