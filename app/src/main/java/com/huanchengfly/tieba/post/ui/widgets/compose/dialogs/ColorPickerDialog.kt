package com.huanchengfly.tieba.post.ui.widgets.compose.dialogs

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DoubleArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.godaddy.android.colorpicker.HsvColor
import com.godaddy.android.colorpicker.harmony.ColorHarmonyMode
import com.godaddy.android.colorpicker.harmony.HarmonyColorPicker
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.NegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.PositiveButton
import com.huanchengfly.tieba.post.ui.widgets.compose.RoundedSlider
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.utils.extension.toHexString

@Composable
fun ColorPickerDialog(
    state: DialogState = rememberDialogState(),
    @StringRes title: Int? = null,
    initial: Color = Color.Red,
    onColorChanged: (Color) -> Unit,
    extraContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    var color by remember { mutableStateOf(HsvColor.from(initial)) }

    Dialog(
        dialogState = state,
        title = title?.let { { Text(text = stringResource(it)) } },
        buttons = {
            Box(Modifier.fillMaxWidth()) {
                NegativeButton(
                    modifier = Modifier.align(Alignment.CenterStart),
                    text = stringResource(id = R.string.button_cancel),
                    onClick = this@Dialog::dismiss
                )

                PositiveButton(
                    text = stringResource(id = R.string.button_finish),
                    modifier = Modifier.align(Alignment.CenterEnd),
                    onClick = {
                        onColorChanged(color.toColor()); dismiss()
                    }
                )
            }
        }
    ) {
        val density = LocalDensity.current
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            val focusManager = LocalFocusManager.current

            // Paddings to align widgets with color picker visually
            val paddingModifier = Modifier.padding(horizontal = 24.dp)

            val isKeyboardOpen by rememberUpdatedState(WindowInsets.ime.getBottom(density) > 0)
            // Hide picker to make room for soft keyboard
            if (!isKeyboardOpen) {
                HarmonyColorPicker(
                    harmonyMode = ColorHarmonyMode.ANALOGOUS,
                    color = color,
                    onColorChanged = { color = it },
                    showBrightnessBar = false,
                    modifier = Modifier
                        .sizeIn(maxWidth = 280.dp, maxHeight = 280.dp)
                        .align(Alignment.CenterHorizontally)
                )

                Row(
                    modifier = paddingModifier.height(24.dp)
                ) {
                    Text(text = stringResource(R.string.brightness))
                    RoundedSlider(
                        value = color.value,
                        onValueChange = { color = color.copy(value = it) },
                        modifier = Modifier.padding(start = 12.dp),
                        trackHeight = 10.dp
                    )
                }
            }

            ColorHexTextFiled(paddingModifier, initial = initial, color = color.toColor()) {
                color = HsvColor.from(it)
            }

            extraContent?.invoke(this)

            if (!isKeyboardOpen) { // Clear focus on soft keyboard close
                focusManager.clearFocus()
            }
        }
    }
}

@Composable
private fun ColorHexTextFiled(
    modifier: Modifier = Modifier,
    initial: Color = Color.Red,
    color: Color = initial,
    onColorChanged: (Color) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var isError: Boolean by remember { mutableStateOf(false) }
    var userInput by remember { mutableStateOf( color.toHexString()) }
    val userInputChangeListener: (String) -> Unit = {
        userInput = it.uppercase()

        try {
            val inputColor = Color(it.toColorInt())
            if (inputColor != color) {
                onColorChanged(inputColor)
            }
            isError = false
        } catch (e: Exception) {
            isError = true
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val border = BorderStroke(0.5.dp, MaterialTheme.colors.onSurface)

        // Color box for initial color
        Box(
            modifier = Modifier
                .size(64.dp, 32.dp)
                .background(color = initial)
                .border(border),
        )

        Image(
            imageVector = Icons.Rounded.DoubleArrow,
            contentDescription = null,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        // Color box for picked color
        Box(
            modifier = Modifier
                .size(64.dp, 32.dp)
                .background(color = color)
                .border(border)
        )

        TextField(
            value = userInput,
            onValueChange = userInputChangeListener,
            isError = isError,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                userInputChangeListener(userInput)
            }),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp),
            textStyle = MaterialTheme.typography.body2.copy(fontSize = 18.sp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                cursorColor = MaterialTheme.colors.primary,
                errorCursorColor = Color.Red,
                errorBorderColor = Color.Red
            )
        )
    }
}

@Preview("ColorHexTextFiled", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun ColorHexTextFiledPreview() = TiebaLiteTheme {
    ColorHexTextFiled(initial = Color.Cyan, color = Color.Blue) {  }
}
