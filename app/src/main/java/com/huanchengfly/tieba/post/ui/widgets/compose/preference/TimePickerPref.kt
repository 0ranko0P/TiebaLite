package com.huanchengfly.tieba.post.ui.widgets.compose.preference

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.huanchengfly.tieba.post.ui.widgets.compose.TimePickerDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.utils.HmTime

/**
 * Preference which shows a [TimePickerDialog]
 *
 * @param time initial picked time
 * @param onTimePicked called when user picked new time, in "HH:mm" format.
 * @param title main text which describes this preference
 * @param modifier the [Modifier] to be applied to this preference
 * @param summary Used to give some more information about what this Pref is for
 * @param dialogTitle Title shown in the dialog. No title if null.
 * @param enabled controls the enabled state of this preference
 */
@NonRestartableComposable
@Composable
fun TimePickerPerf(
    time: HmTime,
    onTimePicked: (HmTime) -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    summary: @Composable (value: HmTime) -> String? = { null },
    dialogTitle: String? = null,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val dialogState = rememberDialogState()

    TextPref(
        title = title,
        modifier = modifier,
        summary = summary(time),
        enabled = enabled,
        leadingIcon = leadingIcon,
        onClick = dialogState::show
    )

    if (dialogState.show) {
        TimePickerDialog(
            initialTime = time,
            title = {
                if (dialogTitle != null) {
                    Text(text = dialogTitle)
                }
            },
            onConfirm = { state ->
                val newTime = HmTime(hourOfDay = state.hour, minute = state.minute)
                if (newTime != time) {
                    onTimePicked(newTime)
                }
            },
            dialogState = dialogState,
        )
    }
}
