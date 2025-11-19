package com.huanchengfly.tieba.post.ui.widgets.compose.preference

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.widgets.compose.AlertDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.picker.ListSinglePicker
import com.huanchengfly.tieba.post.ui.widgets.compose.picker.Options
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState

/**
 * Preference that shows a list of entries in a Dialog where a single entry can be selected at one time.
 *
 * @param value Current value of this preference.
 * @param title Preference title.
 * @param modifier Modifier applied to the Text aspect of this Pref
 * @param summary Used to give some more information about what this Pref is for
 * @param onValueChange Callback to be invoked when user selected new option in [options]
 * @param useSelectedAsSummary If true, uses the current selected option description as [summary]
 * @param enabled If false, this Pref cannot be clicked and the Dialog cannot be shown.
 * @param leadingIcon Optional leading icon to be drawn at the beginning of the preference.
 * @param options All available options of this preference with description.
 */
@NonRestartableComposable
@Composable
fun <T> ListPref(
    modifier: Modifier = Modifier,
    value: T,
    @StringRes title: Int,
    @StringRes summary: Int? = null,
    onValueChange: ((T) -> Unit)? = null,
    useSelectedAsSummary: Boolean = summary == null,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    options: Options<T>,
    optionsIconSupplier: (@Composable (T) -> Unit)? = null
) {
    val dialogState = rememberDialogState()
    val summaryRes: Int? = if (useSelectedAsSummary) options[value] else summary

    BasePreference(
        modifier = modifier.clickable(role = Role.DropdownList, onClick = dialogState::show),
        title = stringResource(title),
        summary = summaryRes?.let { stringResource(id = it) },
        enabled = enabled,
        leadingIcon = leadingIcon,
    )

    if (!dialogState.show) return

    AlertDialog(
        dialogState = dialogState,
        title = { Text(text = stringResource(title)) },
        buttons = {
            DialogNegativeButton(text = stringResource(id = R.string.button_cancel))
        }
    ) {
        ListSinglePicker(
            items = options,
            selected = value,
            onItemSelected = { value: T, changed: Boolean ->
                if (changed) {
                    onValueChange?.invoke(value)
                }
                dismiss()
            },
            itemIconSupplier = optionsIconSupplier
        )
    }
}