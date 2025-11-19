package com.huanchengfly.tieba.post.ui.widgets.compose.preference

import androidx.annotation.StringRes
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.huanchengfly.tieba.post.ui.widgets.compose.Switch

/**
 * Switchable preference.
 *
 * @param modifier the [Modifier] to be applied on this preference.
 * @param checked check states of this preference
 * @param onCheckedChange called when this preference is clicked, parse null to disable this pref.
 * @param title text which describes this preference.
 * @param summary used to give some more information about what this Pref is for.
 * @param enabled controls the enabled state of this preference.
 * @param leadingIcon optional leading icon to be drawn at the beginning of the preference.
 */
@NonRestartableComposable
@Composable
fun SwitchPref(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    @StringRes title: Int,
    @StringRes summary: Int?,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    BasePreference(
        modifier = modifier.toggleable(checked, enabled = enabled) {
            onCheckedChange(!checked)
        },
        title = stringResource(title),
        summary = summary?.let { stringResource(id = it) },
        enabled = enabled,
        leadingIcon = leadingIcon,
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
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    @StringRes title: Int,
    modifier: Modifier = Modifier,
    @StringRes summaryOn: Int? = null,
    @StringRes summaryOff: Int? = null,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) = SwitchPref(
    modifier = modifier,
    checked = checked,
    onCheckedChange = onCheckedChange,
    title = title,
    summary = if (checked) summaryOn else summaryOff,
    enabled = enabled,
    leadingIcon = leadingIcon
)