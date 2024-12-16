package com.huanchengfly.tieba.post.ui.common.prefs.widgets

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.putBoolean
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.ui.widgets.compose.Switch

/**
 * Simple preference with a trailing [Switch]
 *
 * @param key Key used to identify this Pref in the DataStore
 * @param title Main text which describes the Pref
 * @param modifier Modifier applied to the Text aspect of this Pref
 * @param summary Used to give some more information about what this Pref is for
 * @param defaultChecked If the switch should be checked by default. Only used if a value for this [key] doesn't already exist in the DataStore
 * @param onCheckedChange Callback to be invoked when [SwitchPref] is being clicked, Parse null to disable this pref.
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
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    val context = LocalContext.current
    val checked by rememberPreferenceAsState(booleanPreferencesKey(key), defaultChecked)

    TextPref(
        title = stringResource(title),
        modifier = modifier,
        summary = summary(checked)?.let { stringResource(it) },
        leadingIcon = leadingIcon,
        enabled = enabled,
        onClick = {
            val newState = !checked
            context.dataStore.putBoolean(key, value = newState.takeUnless { it == defaultChecked })
            onCheckedChange?.invoke(newState)
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
    defaultChecked: Boolean = false,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) = SwitchPref(
    key = key,
    title = title,
    modifier = modifier,
    summary = { switch -> if (switch) summaryOn else summaryOff },
    onCheckedChange = onCheckedChange,
    defaultChecked = defaultChecked,
    enabled = enabled,
    leadingIcon = leadingIcon
)