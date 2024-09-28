package com.huanchengfly.tieba.post.ui.common.prefs.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.huanchengfly.tieba.post.rememberPreferenceAsMutableState
import kotlinx.collections.immutable.ImmutableMap

/**
 * Preference that shows a list of entries in a DropDown
 *
 * @param key Key used to identify this Pref in the [DataStore], null when it's not a DataStore pref
 * @param title Main text which describes the Pref
 * @param modifier Modifier applied to the Text aspect of this Pref
 * @param summary Used to give some more information about what this Pref is for
 * @param defaultValue Value to use if this Pref does not exist in [DataStore].
 * @param onValueChange Callback to be invoked when user selected new option in [options]
 * @param useSelectedAsSummary If true, uses the current selected item as the summary. Equivalent of useSimpleSummaryProvider in androidx.
 * @param options All available options of this [key] and their description
 */
@Composable
fun <T> DropDownPref(
    key: Preferences.Key<T>?,
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    defaultValue: T,
    onValueChange: ((T) -> Unit)? = null,
    useSelectedAsSummary: Boolean = false,
    leadingIcon: ImageVector? = null,
    options: ImmutableMap<T, String>
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    var prefs: T by if (key != null) {
        rememberPreferenceAsMutableState(key, defaultValue)
    } else {
        remember { mutableStateOf(defaultValue) }
    }

    Column {
        TextPref(
            title = title,
            modifier = modifier,
            summary = if (useSelectedAsSummary) options[prefs] else summary,
            leadingIcon = leadingIcon,
            onClick = {
                expanded = true
            },
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
                        onClick = {
                            expanded = false
                            if (item.key != defaultValue) {
                                prefs = item.key
                                onValueChange?.invoke(item.key)
                            }
                        }
                    ) {
                        Text(
                            text = item.value,
                            style = MaterialTheme.typography.body1
                        )
                    }
                }
            }
        }
    }
}