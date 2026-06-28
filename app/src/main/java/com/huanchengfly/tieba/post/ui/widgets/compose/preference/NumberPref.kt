package com.huanchengfly.tieba.post.ui.widgets.compose.preference

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

fun SegmentedPrefsScope.numberPref(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    @StringRes title: Int,
    @StringRes unit: Int,
) {
    customPreference(key = title) { shapes ->
        SegmentedNumberPreference(
            value = value,
            onValueChange = onValueChange,
            range = range,
            title = stringResource(title),
            unit = stringResource(unit),
            shapes = shapes,
        )
    }
}

@Composable
fun SegmentedNumberPreference(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    title: String,
    unit: String,
    modifier: Modifier = Modifier,
    shapes: ListItemShapes = ListItemDefaults.shapes(),
) {
    val focusManager = LocalFocusManager.current
    var input by rememberSaveable(value) { mutableStateOf(value.toString()) }
    val isInputValid = input.toIntOrNull()?.let(range::contains) == true

    SegmentedPreference(
        modifier = modifier,
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                )

                OutlinedTextField(
                    value = input,
                    onValueChange = { newValue ->
                        if (newValue.isNotEmpty() && !newValue.all(Char::isDigit)) return@OutlinedTextField
                        input = newValue
                        newValue
                            .toIntOrNull()
                            ?.takeIf(range::contains)
                            ?.takeIf { it != value }
                            ?.let(onValueChange)
                    },
                    modifier =
                        Modifier
                            .width(112.dp)
                            .onFocusChanged { if (!it.isFocused && !isInputValid) input = value.toString() },
                    shape = RoundedCornerShape(percent = 56),
                    singleLine = true,
                    isError = !isInputValid,
                    label = { Text(text = "${range.first}-${range.last} $unit") },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onDone = { focusManager.clearFocus() },
                        ),
                )
            }
        },
        shapes = shapes,
        colors = SegmentedListItemColors,
    )
}
