package com.huanchengfly.tieba.post.ui.widgets.compose.dialogs

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DoubleArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.isWindowHeightCompact
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
    val androidColor by remember { derivedStateOf { color.toColor() } }
    val isHeightCompat = isWindowHeightCompact()

    Dialog(
        dialogState = state,
        title = if (!isHeightCompat && title != null) {
            { Text(text = stringResource(title)) }
        } else {
            null
        },
        buttons = {
            Row (
                Modifier.fillMaxWidth()
            ) {
                NegativeButton(
                    text = stringResource(id = R.string.button_cancel),
                    onClick = this@Dialog::dismiss
                )

                Spacer(Modifier.weight(1.0f))

                PositiveButton(
                    text = stringResource(id = R.string.button_finish),
                    onClick = {
                        onColorChanged(androidColor); dismiss()
                    }
                )
            }
        }
    ) {
        val density = LocalDensity.current
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            val focusManager = LocalFocusManager.current

            // Paddings to align widgets with button visually
            val paddingModifier = Modifier.padding(start = 8.dp)

            val isKeyboardOpen by rememberUpdatedState(WindowInsets.ime.getBottom(density) > 0)
            // Hide picker to make room for soft keyboard
            if (!isKeyboardOpen && !isHeightCompat) {
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
                    )
                }
            }

            ColorHexTextField(paddingModifier, initial = initial, color = androidColor) {
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
private fun ColorHexTextField(
    modifier: Modifier = Modifier,
    initial: Color = Color.Red,
    color: Color = initial,
    onColorChanged: (Color) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var isError: Boolean by remember { mutableStateOf(false) }
    var userInput by remember { mutableStateOf(color.toHexString()) }
    val userInputChangeListener: (String) -> Unit = {
        userInput = it.uppercase()

        try {
            val inputColor = Color(userInput.toColorInt())
            if (inputColor != color) {
                onColorChanged(inputColor)
            }
            isError = false
        } catch (_: Exception) {
            isError = true
        }
    }

    LaunchedEffect(color) { // Update hex text too if color changed
        userInput = color.toHexString().uppercase()
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)

        // Color box for initial color
        Box(
            modifier = Modifier
                .size(64.dp, 32.dp)
                .background(color = initial)
                .border(border),
        )

        Icon(imageVector = Icons.Rounded.DoubleArrow, contentDescription = null)

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
                .padding(start = 12.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp),
            colors = OutlinedTextFieldDefaults.colors(
                errorCursorColor = Color.Red,
                errorBorderColor = Color.Red
            )
        )
    }
}

@Preview("ColorHexTextField", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun ColorHexTextFiledPreview() = TiebaLiteTheme {
    ColorHexTextField(initial = Color.Cyan, color = Color.Blue) {  }
}
