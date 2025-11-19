package com.huanchengfly.tieba.post.ui.widgets.compose.preference

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableMap

/**
 * Preference that shows a list of entries in a DropDown
 *
 * @param modifier the [Modifier] to be applied on this preference.
 * @param value current selected value of this preference.
 * @param title text which describes this preference.
 * @param summary used to give some more information about what this Preference is for.
 * @param onValueChange Callback to be invoked when user selected new option in [options]
 * @param useSelectedAsSummary If true, uses the current selected item as the summary. Equivalent of useSimpleSummaryProvider in androidx.
 * @param options All available options of this preference with description
 */
@Composable
fun <T> DropDownPref(
    modifier: Modifier = Modifier,
    value: T?,
    title: String,
    summary: String? = null,
    onValueChange: ((T) -> Unit)? = null,
    useSelectedAsSummary: Boolean = false,
    leadingIcon: ImageVector? = null,
    options: ImmutableMap<T, String>
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column {
        BasePreference(
            modifier = modifier.clickable(role = Role.DropdownList) { expanded = true },
            title = title,
            summary = if (useSelectedAsSummary) options[value] else summary,
            leadingIcon = leadingIcon
        )

        Box(
            modifier = Modifier.padding(start = 16.dp)
        ) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Text(text = item.value)
                        },
                        onClick = {
                            expanded = false
                            if (onValueChange != null && item.key != value) {
                                onValueChange(item.key)
                            }
                        }
                    )
                }
            }
        }
    }
}