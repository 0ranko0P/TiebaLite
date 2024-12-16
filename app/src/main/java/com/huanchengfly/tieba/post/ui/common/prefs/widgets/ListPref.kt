package com.huanchengfly.tieba.post.ui.common.prefs.widgets

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.asyncEdit
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.picker.ListSinglePicker
import com.huanchengfly.tieba.post.ui.widgets.compose.picker.Options
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState

/**
 * Preference that shows a list of entries in a Dialog where a single entry can be selected at one time.
 *
 * @param key Preferences Key used to identify this Pref in the [DataStore]
 * @param title Main text which describes the Pref. Shown above the summary and in the Dialog.
 * @param modifier Modifier applied to the Text aspect of this Pref
 * @param summary Used to give some more information about what this Pref is for
 * @param defaultValue Value to use if this Pref does not exist in [DataStore].
 * @param onValueChange Callback to be invoked when user selected new option in [options]
 * @param useSelectedAsSummary If true, uses the current selected option description as [summary]
 * @param enabled If false, this Pref cannot be clicked and the Dialog cannot be shown.
 * @param options All available options of this [key] and their description
 */
@Composable
fun <T> ListPref(
    key: Preferences.Key<T>,
    @StringRes title: Int,
    modifier: Modifier = Modifier,
    @StringRes summary: Int? = null,
    defaultValue: T,
    onValueChange: ((T) -> Unit)? = null,
    useSelectedAsSummary: Boolean = summary == null,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    options: Options<T>,
    optionsIconSupplier: ((T) -> @Composable () -> Unit)? = null
) {
    val dialogState = rememberDialogState()
    val context = LocalContext.current
    val prefs: T by rememberPreferenceAsState(key, defaultValue)
    val summaryRes: Int? = if (useSelectedAsSummary) options[prefs] else summary

    TextPref(
        modifier = modifier,
        title = stringResource(title),
        summary = summaryRes?.let { stringResource(id = it) },
        onClick = { dialogState.show() },
        leadingIcon = leadingIcon,
        enabled = enabled
    )

    Dialog(
        dialogState = dialogState,
        title = { Text(text = stringResource(title)) },
        buttons = {
            DialogNegativeButton(text = stringResource(id = R.string.button_cancel))
        }
    ) {
        ListSinglePicker(
            items = options,
            selected = prefs,
            onItemSelected = { value: T, changed: Boolean ->
                if (changed) {
                    context.dataStore.asyncEdit(key, value.takeUnless { it == defaultValue })
                    onValueChange?.invoke(value)
                }
                dismiss()
            },
            modifier = Modifier.padding(bottom = 16.dp),
            itemIconSupplier = optionsIconSupplier
        )
    }
}