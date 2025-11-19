package com.huanchengfly.tieba.post.ui.widgets.compose.preference

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.huanchengfly.tieba.post.ui.widgets.compose.TimePickerDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerPerf(
    time: String,
    onTimePicked: (String) -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    summary: @Composable (value: String) -> String? = { null },
    dialogTitle: String? = null,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val dialogState = rememberDialogState()
    val formatter = remember { SimpleDateFormat("HH:mm", Locale.US) }
    val calendar = remember { Calendar.getInstance(Locale.US) }

    var editHour by remember { mutableIntStateOf(0) }
    var editMinute by remember { mutableIntStateOf(0) }

    LaunchedEffect(time) {
        calendar.time = formatter.parse(time) ?: throw NullPointerException("Null on parse $time")
        editHour = calendar.get(Calendar.HOUR_OF_DAY)
        editMinute = calendar.get(Calendar.MINUTE)
    }

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
            initialHour = editHour,
            initialMinute = editMinute,
            title = {
                if (dialogTitle != null) {
                    Text(text = dialogTitle)
                }
            },
            onConfirm = { state ->
                if (state.hour == editHour && state.minute == editMinute) return@TimePickerDialog
                calendar.set(Calendar.HOUR_OF_DAY, state.hour)
                calendar.set(Calendar.MINUTE, state.minute)
                val newTime = formatter.format(calendar.time)
                onTimePicked(newTime)
            },
            dialogState = dialogState,
        )
    }
}
