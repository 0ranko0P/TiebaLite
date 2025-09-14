package com.huanchengfly.tieba.post.ui.common.prefs.widgets

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
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
 * Switchable preference with checked/unchecked states
 *
 * @param key key used to identify this preference in the DataStore
 * @param title Main text which describes the Pref
 * @param modifier the [Modifier] to be applied on this preference
 * @param summary Used to give some more information about what this Pref is for
 * @param defaultChecked default checked state when [key] doesn't exist in the DataStore
 * @param onCheckedChange called when this preference is clicked, parse null to disable this pref.
 * @param enabled controls the enabled state of this preference
 * @param leadingIcon icon will be drawn at the start of the preference
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

@NonRestartableComposable
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