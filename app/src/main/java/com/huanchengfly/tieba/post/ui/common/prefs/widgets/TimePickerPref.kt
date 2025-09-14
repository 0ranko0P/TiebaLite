package com.huanchengfly.tieba.post.ui.common.prefs.widgets

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
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.stringPreferencesKey
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.putString
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.ui.widgets.compose.TimePickerDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Preference which shows a [TimePickerDialog]
 *
 * @param key key used to identify this preference in the DataStore
 * @param title main text which describes this preference
 * @param modifier the [Modifier] to be applied to this preference
 * @param summary Used to give some more information about what this Pref is for
 * @param dialogTitle Title shown in the dialog. No title if null.
 * @param defaultValue default time when [key] doesn't exist in the DataStore.
 * @param onValueSaved called when user picked new time, in "HH:mm" format.
 * @param enabled controls the enabled state of this preference
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerPerf(
    key: String,
    title: String,
    modifier: Modifier = Modifier,
    summary: @Composable (value: String) -> String? = { null },
    dialogTitle: String? = null,
    defaultValue: String = "07:00",
    onValueSaved: (String) -> Unit = {},
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val context = LocalContext.current
    val dialogState = rememberDialogState()
    val formatter = remember { SimpleDateFormat("HH:mm", Locale.US) }
    val calendar = remember { Calendar.getInstance(Locale.US) }

    // value should only change when save button is clicked
    val value by rememberPreferenceAsState(stringPreferencesKey(key), defaultValue)
    var editHour by remember { mutableIntStateOf(0) }
    var editMinute by remember { mutableIntStateOf(0) }

    LaunchedEffect(value) {
        calendar.time = formatter.parse(value) ?: throw NullPointerException("Null on parse $value")
        editHour = calendar.get(Calendar.HOUR_OF_DAY)
        editMinute = calendar.get(Calendar.MINUTE)
    }

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
            initialHour = editHour,
            initialMinute = editMinute,
            title = {
                if (dialogTitle != null) {
                    Text(text = dialogTitle)
                }
            },
            onConfirm = { state ->
                calendar.set(Calendar.HOUR_OF_DAY, state.hour)
                calendar.set(Calendar.MINUTE, state.minute)
                val newTime = formatter.format(calendar.time)
                context.dataStore.putString(key, newTime)
                onValueSaved(newTime)
            },
            dialogState = dialogState,
        )
    }
}
