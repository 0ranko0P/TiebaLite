package com.huanchengfly.tieba.post.ui.common.prefs.widgets

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.stringPreferencesKey
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.putString
import com.huanchengfly.tieba.post.rememberPreferenceAsState
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
 * @param enabled If false, this Pref cannot be clicked.
 */
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
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val context = LocalContext.current
    val dialogState = rememberDialogState()

    // value should only change when save button is clicked
    val value by rememberPreferenceAsState(stringPreferencesKey(key), defaultValue)
    var displayValue by remember { mutableStateOf(value)}

    TextPref(
        title = title,
        modifier = modifier,
        summary = summary(value),
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
                context.dataStore.putString(key, it)
                displayValue = it
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
