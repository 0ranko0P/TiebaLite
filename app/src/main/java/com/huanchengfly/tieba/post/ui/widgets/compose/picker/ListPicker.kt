package com.huanchengfly.tieba.post.ui.widgets.compose.picker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableMap

/**
 * A Map contains option and its StringRes description
 * */
typealias Options<Option> = ImmutableMap<Option, Int>

@Composable
fun <Option> ListSinglePicker(
    items: Options<Option>,
    selected: Option,
    onItemSelected: (item: Option, changed: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    selectedIndicator: @Composable () -> Unit = {
        Icon(Icons.Rounded.Check, contentDescription = null)
    },
    enabled: Boolean = true,
    itemIconSupplier: (@Composable (Option) -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(modifier = modifier) {
        items.forEach { (value, description) ->
            val isSelected = value == selected
            val descriptionText = stringResource(id = description)

            Surface(
                onClick = { onItemSelected(value, !isSelected)},
                modifier = Modifier.semantics(mergeDescendants = true) {
                    contentDescription = descriptionText
                    role = Role.DropdownList
                    this.selected = isSelected
                },
                enabled = enabled,
                color = if (isSelected) colorScheme.primaryContainer else Color.Transparent,
                contentColor = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (itemIconSupplier != null) {
                        itemIconSupplier(value)
                        Spacer(modifier = Modifier.width(16.dp))
                    }

                    Text(
                        text = descriptionText,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (isSelected) {
                        selectedIndicator()
                    }
                }
            }
        }
    }
}
