package com.huanchengfly.tieba.post.ui.widgets.compose.preference

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import com.huanchengfly.tieba.post.ui.widgets.compose.PromptDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState

/**
 * Preference which shows a TextField in a Dialog
 *
 * @param modifier the [Modifier] to be applied on this preference.
 * @param value Current value of this preference.
 * @param onValueChanged Will be called every time the TextField value is changed.
 * @param title Text which describes this preference.
 * @param summary Used to give some more information about what this Pref is for
 * @param dialogTitle Title shown in the dialog. No title if null.
 * @param dialogMessage Summary shown underneath [dialogTitle]. No summary if null.
 * @param enabled Controls the enabled state of this preference.
 */
@NonRestartableComposable
@Composable
fun EditTextPref(
    modifier: Modifier = Modifier,
    value: String,
    onValueChanged: (String) -> Unit,
    title: String,
    summary: String? = null,
    dialogTitle: String? = null,
    dialogMessage: String? = null,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val dialogState = rememberDialogState()

    BasePreference(
        modifier = modifier.clickable(enabled, role = Role.Button, onClick = dialogState::show),
        title = title,
        summary = summary,
        enabled = enabled,
        leadingIcon = leadingIcon
    )

    if (dialogState.show) {
        PromptDialog(
            onConfirm = {
                if (it.trim() != value) {
                    onValueChanged(it)
                }
            },
            dialogState = dialogState,
            initialValue = value,
            title = dialogTitle?.let { { Text(text = dialogTitle) } },
        ) {
            if (dialogMessage != null) {
                Text(text = dialogMessage)
            }
        }
    }
}