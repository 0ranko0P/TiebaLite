package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonSkippableComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun BaseTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    placeholder: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    isFocused: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: TextFieldColors = TextFieldDefaults.colors(),
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit =
        { innerTextField ->
            Box(
                contentAlignment = Alignment.CenterStart
            ) {
                PlaceholderDecoration(
                    show = value.isEmpty(),
                    placeholderColor = colors.placeholderColor(enabled, isError, isFocused),
                    placeholder = placeholder
                )
                innerTextField()
            }
        },
) {
    // If color is not provided via the text style, use content color as a default
    val textColor =
        textStyle.color.takeOrElse {
            val focused = interactionSource.collectIsFocusedAsState().value
            colors.textColor(enabled, isError, focused)
        }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.background(color = colors.containerColor(enabled, isError, isFocused)),
        enabled = enabled,
        readOnly = readOnly,
        textStyle = mergedTextStyle,
        cursorBrush = SolidColor(colors.cursorColor(isError)),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        singleLine = singleLine,
        maxLines = maxLines,
        decorationBox = decorationBox
    )
}

@Composable
fun OutlineCounterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxLength: Int = Int.MAX_VALUE,
    onLengthBeyondRestrict: ((String) -> Unit)? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
) {
    val maxLengthRestrictEnable = maxLength < Int.MAX_VALUE
    OutlinedTextField(
        value = value,
        onValueChange = {
            if (!maxLengthRestrictEnable || it.length <= maxLength) {
                onValueChange(it)
            } else {
                onValueChange(it.substring(0 until maxLength))
                onLengthBeyondRestrict?.invoke(it)
            }
        },
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        supportingText = {
            Text(
                text = if (maxLengthRestrictEnable) {
                   String.format(null, "%d/%d", value.length, maxLength)
                } else {
                    value.length.toString()
                },
            )
        },
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        singleLine = singleLine,
        maxLines = maxLines,
        colors = colors,
    )
}

@Composable
@NonSkippableComposable
fun PlaceholderDecoration(
    show: Boolean,
    placeholderColor: Color,
    placeholder: @Composable (() -> Unit)? = null,
) {
    if (placeholder != null && show) {
        CompositionLocalProvider(LocalContentColor provides placeholderColor, placeholder)
    }
}

@Composable
@NonSkippableComposable
fun ProvideContentColor(color: Color, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalContentColor provides color, content)
}

// Internal functions from TextFieldColors

/**
 * Represents the container color for this text field.
 *
 * @param enabled whether the text field is enabled
 * @param isError whether the text field's current value is in error
 * @param focused whether the text field is in focus
 */
@Stable
private fun TextFieldColors.containerColor(enabled: Boolean, isError: Boolean, focused: Boolean): Color =
    when {
        !enabled -> disabledContainerColor
        isError -> errorContainerColor
        focused -> focusedContainerColor
        else -> unfocusedContainerColor
    }

/**
 * Represents the color used for the placeholder of this text field.
 *
 * @param enabled whether the text field is enabled
 * @param isError whether the text field's current value is in error
 * @param focused whether the text field is in focus
 */
@Stable
private fun TextFieldColors.placeholderColor(enabled: Boolean, isError: Boolean, focused: Boolean): Color =
    when {
        !enabled -> disabledPlaceholderColor
        isError -> errorPlaceholderColor
        focused -> focusedPlaceholderColor
        else -> unfocusedPlaceholderColor
    }

/**
 * Represents the color used for the input field of this text field.
 *
 * @param enabled whether the text field is enabled
 * @param isError whether the text field's current value is in error
 * @param focused whether the text field is in focus
 */
@Stable
private fun TextFieldColors.textColor(enabled: Boolean, isError: Boolean, focused: Boolean): Color =
    when {
        !enabled -> disabledTextColor
        isError -> errorTextColor
        focused -> focusedTextColor
        else -> unfocusedTextColor
    }

/**
 * Represents the color used for the cursor of this text field.
 *
 * @param isError whether the text field's current value is in error
 */
@Stable
private fun TextFieldColors.cursorColor(isError: Boolean): Color =
    if (isError) errorCursorColor else cursorColor

