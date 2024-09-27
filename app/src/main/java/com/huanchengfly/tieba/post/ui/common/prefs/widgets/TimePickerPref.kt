package com.huanchengfly.tieba.post.ui.common.prefs.widgets

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.stringPreferencesKey
import com.huanchengfly.tieba.post.rememberPreferenceAsMutableState
import com.huanchengfly.tieba.post.ui.widgets.compose.TimePickerDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState

/**
 * Preference which shows a TextField in a Dialog
 *
 * @param key Key used to identify this Pref in the DataStore
 * @param title Main text which describes the Pref
 * @param modifier Modifier applied to the Text aspect of this Pref
 * @param summary Used to give some more information about what this Pref is for
 * @param dialogTitle Title shown in the dialog. No title if null.
 * @param dialogMessage Summary shown underneath [dialogTitle]. No summary if null.
 * @param defaultValue Default value that will be set in the TextField when the dialog is shown for the first time.
 * @param onValueSaved Will be called with new TextField value when the confirm button is clicked. It is NOT called every time the value changes. Use [onValueChange] for that.
 * @param textColor Text colour of the [title] and [summary]
 * @param enabled If false, this Pref cannot be clicked.
 */
@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
fun TimePickerPerf(
    key: String,
    title: String,
    modifier: Modifier = Modifier,
    summary: @Composable (value: String) -> String? = { null },
    dialogTitle: String? = null,
    dialogMessage: String? = null,
    defaultValue: String = "07:00",
    onValueSaved: (String) -> Unit = {},
    textColor: Color = MaterialTheme.colors.onBackground,
    leadingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val dialogState = rememberDialogState()

    // value should only change when save button is clicked
    var value by rememberPreferenceAsMutableState(stringPreferencesKey(key), defaultValue)
    var displayValue by remember { mutableStateOf(value)}

    TextPref(
        title = title,
        modifier = modifier,
        summary = summary(value),
        textColor = textColor,
        enabled = enabled,
        leadingIcon = leadingIcon,
        onClick = dialogState::show
    )

    if (dialogState.show) {
        TimePickerDialog(
            title = {
                if (dialogTitle != null) {
                    Text(text = dialogTitle)
                }
            },
            currentTime = value,
            onConfirm = {
                value = it
                displayValue = value
                onValueSaved(it)
            },
            dialogState = dialogState,
            onValueChange = { displayValue = it },
            onCancel = {
                displayValue = value // reset
            }
        ) {
            if (dialogMessage != null) {
                Text(text = dialogMessage)
            }
        }
    }
}
