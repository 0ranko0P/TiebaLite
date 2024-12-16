package com.huanchengfly.tieba.post.ui.common.prefs.widgets

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.toSize
import androidx.datastore.preferences.core.stringPreferencesKey
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.putString
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.ui.widgets.compose.PromptDialog
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
 * @param onValueChange Will be called every time the TextField value is changed.
 * @param enabled If false, this Pref cannot be clicked.
 */
@Composable
fun EditTextPref(
    key: String,
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    dialogTitle: String? = null,
    dialogMessage: String? = null,
    defaultValue: String = "",
    onValueChange: ((String) -> Unit) = {},
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val context = LocalContext.current
    val dialogState = rememberDialogState()

    //value should only change when save button is clicked
    val value by rememberPreferenceAsState(stringPreferencesKey(key), defaultValue)

    //value of the TextField which changes every time the text is modified
    var textVal by remember { mutableStateOf(value) }

    var dialogSize by remember { mutableStateOf(Size.Zero) }

    TextPref(
        modifier = modifier,
        title = title,
        summary = summary,
        onClick = { dialogState.show() },
        leadingIcon = leadingIcon,
        enabled = enabled
    )

    if (dialogState.show) {
        //reset
        LaunchedEffect(null) {
            textVal = value
        }

        PromptDialog(
            onConfirm = {
                if (it != defaultValue) {
                    context.dataStore.putString(key, it)
                    onValueChange(it)
                }
            },
            modifier = Modifier.onGloballyPositioned {
                dialogSize = it.size.toSize()
            },
            dialogState = dialogState,
            initialValue = textVal,
            onValueChange = { newVal, _ ->
                textVal = newVal
                true
            },
            title = {
                if (dialogTitle != null) {
                    Text(text = dialogTitle)
                }
            },
        ) {
            if (dialogMessage != null) {
                Text(text = dialogMessage)
            }
        }
    }
//        AlertDialog(
//            modifier = Modifier
//                .fillMaxWidth(0.9f)
//                .onGloballyPositioned {
//                    dialogSize = it.size.toSize()
//                },
//            onDismissRequest = { showDialog = false },
//            title = null,
//            text = null,
//            buttons = {
//                Column(
//                    verticalArrangement = Arrangement.SpaceBetween,
//                ) {
//                    DialogHeader(dialogTitle, dialogMessage)
//
//                    OutlinedTextField(
//                        value = textVal,
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(16.dp)
//                            .weight(1f, fill = false),
//                        onValueChange = {
//                            textVal = it
//                            onValueChange(it)
//                        }
//                    )
//
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.End,
//                        modifier = Modifier.width(with(LocalDensity.current) { dialogSize.width.toDp() })
//                    ) {
//                        TextButton(
//                            modifier = Modifier.padding(end = 16.dp),
//                            onClick = { showDialog = false }
//                        ) {
//                            Text("Cancel", style = MaterialTheme.typography.body1)
//                        }
//
//                        TextButton(
//                            modifier = Modifier.padding(end = 16.dp),
//                            onClick = {
//                                edit()
//                                showDialog = false
//                            }
//                        ) {
//                            Text("Save", style = MaterialTheme.typography.body1)
//                        }
//                    }
//
//                }
//
//            },
//            properties = DialogProperties(usePlatformDefaultWidth = false),
//            backgroundColor = dialogBackgroundColor,
//        )
}