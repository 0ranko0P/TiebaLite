package com.huanchengfly.tieba.post.ui.common.prefs.widgets

import androidx.annotation.StringRes
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.huanchengfly.tieba.post.rememberPreferenceAsMutableState
import com.huanchengfly.tieba.post.ui.widgets.compose.Switch

/**
 * Simple preference with a trailing [Switch]
 *
 * @param key Key used to identify this Pref in the DataStore
 * @param title Main text which describes the Pref
 * @param modifier Modifier applied to the Text aspect of this Pref
 * @param summary Used to give some more information about what this Pref is for
 * @param defaultChecked If the switch should be checked by default. Only used if a value for this [key] doesn't already exist in the DataStore
 * @param onCheckedChange Will be called with the new state when the state changes
 * @param textColor Text colour of the [title] and [summary]
 * @param enabled If false, this Pref cannot be checked/unchecked
 * @param leadingIcon Icon which is positioned at the start of the Pref
 */
@Composable
fun SwitchPref(
    key: String,
    @StringRes title: Int,
    modifier: Modifier = Modifier,
    summary: (value: Boolean) -> Int? = { null },
    defaultChecked: Boolean = false,  // only used if it doesn't already exist in the datastore
    onCheckedChange: ((Boolean) -> Unit)? = null,
    textColor: Color = MaterialTheme.colors.onBackground,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    var checked by rememberPreferenceAsMutableState(booleanPreferencesKey(key), defaultChecked)

    TextPref(
        title = stringResource(title),
        modifier = modifier,
        textColor = textColor,
        summary = summary(checked)?.let { stringResource(it) },
        darkenOnDisable = true,
        leadingIcon = leadingIcon,
        enabled = enabled,
        onClick = {
            checked = !checked
            onCheckedChange?.invoke(checked)
        }
    ) {
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = null
        )
    }
}

@Composable
fun SwitchPref(
    key: String,
    @StringRes title: Int,
    modifier: Modifier = Modifier,
    @StringRes summaryOn: Int? = null,
    @StringRes summaryOff: Int? = null,
    defaultChecked: Boolean = false,  // only used if it doesn't already exist in the datastore
    onCheckedChange: ((Boolean) -> Unit)? = null,
    textColor: Color = MaterialTheme.colors.onBackground,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null
) = SwitchPref(
    key = key,
    title = title,
    modifier = modifier,
    summary = { switch -> if (switch) summaryOn else summaryOff },
    onCheckedChange = onCheckedChange,
    defaultChecked = defaultChecked,
    textColor = textColor,
    enabled = enabled,
    leadingIcon = leadingIcon
)