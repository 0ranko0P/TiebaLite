package com.huanchengfly.tieba.post.ui.widgets.compose.picker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import kotlinx.collections.immutable.ImmutableMap

/**
 * A Map contains option and its StringRes description
 * */
typealias Options<Option> = ImmutableMap<Option, Int>

@Composable
private fun selectedButtonColor(selected: Boolean): ButtonColors {
    return if (selected) {
        ButtonDefaults.textButtonColors(
            backgroundColor = ExtendedTheme.colors.accent.copy(0.1f),
            contentColor = ExtendedTheme.colors.accent
        )
    } else {
        ButtonDefaults.textButtonColors(
            backgroundColor = Color.Transparent,
            contentColor = ExtendedTheme.colors.text
        )
    }
}

@Composable
fun <Option> ListSinglePicker(
    items: Options<Option>,
    selected: Option,
    onItemSelected: (item: Option, changed: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    selectedIndicator: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = stringResource(id = R.string.desc_checked)
        )
    },
    enabled: Boolean = true,
    itemIconSupplier: ((Option) -> @Composable () -> Unit)? = null
) {
    Column(modifier = modifier) {
        items.forEach {
            val value = it.key
            val description = stringResource(it.value)
            val isSelected = value == selected

            Button(
                onClick = { onItemSelected(value, !isSelected)},
                enabled = enabled,
                elevation = null,
                shape = RectangleShape,
                colors = selectedButtonColor(isSelected),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
            ) {
                itemIconSupplier?.invoke(value)?.let { icon ->
                    icon()
                    Spacer(Modifier.size(16.dp))
                }

                Text(
                    text = description,
                    modifier = Modifier.weight(1f),
                    color = LocalContentColor.current.copy(LocalContentAlpha.current),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                if (isSelected) {
                    Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                        selectedIndicator()
                    }
                }
            }
        }
    }
}
