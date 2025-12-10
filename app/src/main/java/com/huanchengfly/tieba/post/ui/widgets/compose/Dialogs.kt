package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.isWindowHeightCompact
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.isWindowWidthCompact
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.AnyPopDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.AnyPopDialogProperties
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.DirectionState
import com.huanchengfly.tieba.post.utils.HmTime

val DefaultDialogContentPadding = 20.dp
val DefaultDialogMargin = 16.dp

val DefaultDialogProperties = AnyPopDialogProperties(
    direction = DirectionState.CENTER,
    dismissOnBackPress = true,
    dismissOnClickOutside = true,
    imePadding = true
)

val dialogAdaptiveFraction: Float
    @ReadOnlyComposable @Composable get() = if (isWindowWidthCompact()) 1.0f else 0.6f

@Composable
fun DialogScope.DialogPositiveButton(
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    Button(
        onClick = {
            onClick()
            dismiss()
        },
        modifier = modifier,
        enabled = enabled,
        content = { Text(text = text, maxLines = 1) }
    )
}

@Composable
fun DialogScope.DialogNegativeButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: (() -> Unit)? = null
) {
    TextButton(
        onClick = {
            dismiss()
            onClick?.invoke()
        },
        modifier = modifier,
        content = { Text(text = text, maxLines = 1) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    modifier: Modifier = Modifier,
    initialTime: HmTime,
    title: @Composable (() -> Unit)? = null,
    onConfirm: (TimePickerState) -> Unit,
    dialogState: DialogState = rememberDialogState(),
    onCancel: (() -> Unit)? = null,
    confirmText: String = stringResource(id = R.string.button_sure_default),
    cancelText: String = stringResource(id = R.string.button_cancel),
) {
    val isWindowHeightCompact = isWindowHeightCompact()
    var pickerStyle by remember { mutableStateOf(!isWindowHeightCompact) }
    val timePickerState = rememberTimePickerState(initialTime.hourOfDay, initialTime.minute)

    Dialog(
        modifier = modifier,
        dialogState = dialogState,
        onDismiss = onCancel,
        title = title.takeUnless { isWindowHeightCompact },
        buttons = {
            IconButton(onClick = { pickerStyle = !pickerStyle }) {
                Icon(
                    imageVector = if (pickerStyle) Icons.Filled.EditCalendar else Icons.Filled.AccessTime,
                    contentDescription = "Time picker type",
                )
            }

            with(it)  { Spacer(modifier = Modifier.weight(1.0f)) }

            DialogNegativeButton(text = cancelText, onClick = onCancel)
            DialogPositiveButton(text = confirmText) {
                onConfirm(timePickerState)
            }
        },
    ) {
        if (pickerStyle) {
            TimePicker(state = timePickerState)
        } else {
            TimeInput(state = timePickerState)
        }

        LaunchedEffect(isWindowHeightCompact) {
            pickerStyle = !isWindowHeightCompact
        }
    }
}

@Composable
fun AlertDialog(
    modifier: Modifier = Modifier,
    dialogState: DialogState,
    dialogProperties: AnyPopDialogProperties = DefaultDialogProperties,
    onDismiss: (() -> Unit)? = null,
    title: @Composable () -> Unit = {},
    buttons: @Composable (DialogScope.(RowScope) -> Unit) = {},
    content: @Composable (DialogScope.(ColumnScope) -> Unit) = {},
) {
    BaseDialog(
        modifier = modifier,
        dialogState = dialogState,
        dialogProperties = dialogProperties,
        onDismiss = onDismiss,
    ) {
        Surface(
            shape = AlertDialogDefaults.shape,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(fraction = dialogAdaptiveFraction)
                .padding(DefaultDialogMargin),
        ) {
            Column(
                modifier = Modifier.padding(vertical = DefaultDialogContentPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ProvideTextStyle(MaterialTheme.typography.headlineSmall, title)
                Spacer(modifier = Modifier.height(DefaultDialogContentPadding))

                ProvideTextStyle(MaterialTheme.typography.bodyLarge) {
                    content(this)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DefaultDialogContentPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.End)
                ) {
                    buttons(this)
                }
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    dialogState: DialogState,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    onCancel: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    confirmText: String = stringResource(id = R.string.button_sure_default),
    cancelText: String = stringResource(id = R.string.button_cancel),
    title: @Composable (() -> Unit)? = null,
    content: @Composable (DialogScope.() -> Unit)? = null,
) {
    Dialog(
        modifier = modifier,
        dialogState = dialogState,
        onDismiss = onDismiss,
        title = title,
        buttons = {
            DialogNegativeButton(text = cancelText, onClick = onCancel)
            DialogPositiveButton(text = confirmText, onClick = onConfirm)
        },
    ) {
        if (content != null)  {
            content()
        }
    }
}

/**
 * 带输入框的对话框
 *
 * @param onValueChange 输入框内容变化时的回调，返回true表示允许变化，false表示不允许变化
 */
@Composable
fun PromptDialog(
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier,
    dialogState: DialogState = rememberDialogState(),
    keyboardType: KeyboardType = KeyboardType.Unspecified,
    initialValue: String = "",
    onValueChange: (newVal: String, oldVal: String) -> Boolean = { _, _ -> true },
    isError: ((String) -> Boolean)? = null,
    onCancel: (() -> Unit)? = null,
    confirmText: String = stringResource(id = R.string.button_sure_default),
    cancelText: String = stringResource(id = R.string.button_cancel),
    title: @Composable (() -> Unit)? = null,
    content: @Composable (DialogScope.() -> Unit) = {},
) {
    var textVal by remember { mutableStateOf(initialValue) }
    val isErrorState by remember { derivedStateOf { isError?.invoke(textVal) == true } }

    // 每次显示时重置输入框内容
    LaunchedEffect(dialogState.show) {
        textVal = initialValue
    }

    Dialog(
        modifier = modifier,
        dialogState = dialogState,
        onDismiss = onCancel,
        title = title,
        buttons = {
            DialogNegativeButton(text = cancelText, onClick = onCancel)
            DialogPositiveButton(text = confirmText, enabled = !isErrorState) {
                onConfirm(textVal)
            }
        },
    ) {
        val focusRequester = remember { FocusRequester() }
        val softwareKeyboardController = LocalSoftwareKeyboardController.current
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()

            OutlinedTextField(
                value = textVal,
                onValueChange = {
                    if (onValueChange.invoke(it, textVal)) textVal = it
                },
                isError = isErrorState,
                maxLines = 1,
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        softwareKeyboardController?.hide()
                        if (!isErrorState) {
                            dismiss()
                            onConfirm(textVal)
                        }
                    }
                ),
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .fillMaxWidth(),
                colors = TextFieldDefaults.colors()
            )
            LaunchedEffect(focusRequester) {
                focusRequester.requestFocus()
            }
        }
    }
}

@Composable
fun BaseDialog(
    modifier: Modifier = Modifier,
    dialogState: DialogState = rememberDialogState(),
    dialogProperties: AnyPopDialogProperties = DefaultDialogProperties,
    onDismiss: (() -> Unit)? = null,
    content: @Composable DialogScope.() -> Unit,
) {
    var showDialog by remember {
        mutableStateOf(false)
    }
    var isActiveClose by remember {
        mutableStateOf(false)
    }
    LaunchedEffect(dialogState.show) {
        if (dialogState.show) {
            showDialog = true
            isActiveClose = false
        } else {
            isActiveClose = true
        }
    }
    if (showDialog) {
        val dialogScope = DialogScope(
            onDismiss = {
                isActiveClose = true
            },
        )
        AnyPopDialog(
            onDismiss = {
                onDismiss?.invoke()
                dialogState.show = false
                showDialog = false
            },
            modifier = modifier,
            isActiveClose = isActiveClose,
            properties = dialogProperties
        ) {
            dialogScope.content()
        }
    }
}

@Composable
fun Dialog(
    modifier: Modifier = Modifier,
    dialogState: DialogState = rememberDialogState(),
    dialogProperties: AnyPopDialogProperties = DefaultDialogProperties,
    onDismiss: (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    buttons: @Composable (DialogScope.(RowScope) -> Unit) = {},
    content: @Composable (DialogScope.(ColumnScope) -> Unit),
) {
    BaseDialog(
        modifier = modifier,
        dialogState = dialogState,
        dialogProperties = dialogProperties,
        onDismiss = onDismiss,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(fraction = dialogAdaptiveFraction)
                .padding(DefaultDialogMargin),
            shape = AlertDialogDefaults.shape,
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 6.dp
        ) {
            ConstraintLayout(
                modifier = Modifier
                    .padding(vertical = DefaultDialogContentPadding)
                    .animateContentSize(),
            ) {
                val (titleRef, buttonsRef) = createRefs()

                // To make buttons visually aligned, apply horizontal paddings separately
                Column(
                    modifier = Modifier
                        .constrainAs(titleRef) {
                            top.linkTo(parent.top)
                            start.linkTo(parent.start, margin = DefaultDialogContentPadding)
                            end.linkTo(parent.end, margin = DefaultDialogContentPadding)
                            bottom.linkTo(buttonsRef.top)
                            width = Dimension.fillToConstraints
                        },
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val typography = MaterialTheme.typography

                    if (title != null) {
                        ProvideTextStyle(typography.titleLarge, content = title)
                    }

                    ProvideTextStyle(typography.bodyLarge) {
                        content(this)
                    }
                }

                Row (
                    modifier = Modifier
                        // Apply custom margin to align buttons with content (visually)
                        .constrainAs(buttonsRef) {
                            start.linkTo(parent.start, margin = 18.dp)
                            end.linkTo(parent.end, margin = DefaultDialogContentPadding)
                            bottom.linkTo(parent.bottom)
                            top.linkTo(titleRef.bottom, margin = 12.dp)
                            width = Dimension.fillToConstraints
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.End)
                ) {
                    buttons(this)
                }
            }
        }
    }
}

@Composable
fun rememberDialogState(): DialogState {
    return rememberSaveable(saver = DialogState.Saver) {
        DialogState()
    }
}

@Stable
class DialogState private constructor(
    show: Boolean
) {
    constructor() : this(show = false)

    var show by mutableStateOf(show)

    fun show() {
        show = true
    }

    companion object {
        val Saver: Saver<DialogState, *> = listSaver(
            save = {
                listOf<Any>(
                    it.show,
                )
            },
            restore = {
                DialogState(
                    show = it[0] as Boolean
                )
            }
        )
    }
}

class DialogScope(
    private val onDismiss: () -> Unit,
) {
    fun dismiss() {
        onDismiss()
    }
}