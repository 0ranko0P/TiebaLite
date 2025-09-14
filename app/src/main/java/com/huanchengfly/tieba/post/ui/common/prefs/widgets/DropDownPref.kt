package com.huanchengfly.tieba.post.ui.common.prefs.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.huanchengfly.tieba.post.asyncEdit
import com.huanchengfly.tieba.post.dataStore
import kotlinx.collections.immutable.ImmutableMap

/**
 * Preference that shows a list of entries in a DropDown
 *
 * @param key Key used to identify this Pref in the [DataStore], null when it's not a DataStore pref
 * @param title Main text which describes the Pref
 * @param modifier the [Modifier] to be applied on this preference
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
    val context = LocalContext.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    var prefs: T by remember { mutableStateOf(defaultValue) }

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
                        text = {
                            Text(text = item.value)
                        },
                        onClick = {
                            expanded = false
                            if (item.key != defaultValue) {
                                prefs = item.key
                                if (key != null) {
                                    context.dataStore.asyncEdit(key, prefs.takeUnless { it == defaultValue })
                                }
                                onValueChange?.invoke(item.key)
                            }
                        }
                    )
                }
            }
        }
    }
}