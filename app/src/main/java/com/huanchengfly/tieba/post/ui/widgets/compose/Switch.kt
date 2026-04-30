package com.huanchengfly.tieba.post.ui.widgets.compose

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.flow.filterIsInstance

/**
 *
 * Material 3 Switch with hardcoded icon thumb.
 *
 * For thumb customization, use official one instead.
 *
 * @param checked whether or not this switch is checked
 * @param onCheckedChange called when this switch is clicked. If `null`, then this switch will not
 *   be interactable, unless something else handles its input events and updates its state.
 * @param modifier the [Modifier] to be applied to this switch
 * @param enabled controls the enabled state of this switch. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param colors [SwitchColors] that will be used to resolve the colors used for this switch in
 *   different states. See [SwitchDefaults.colors].
 */
@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    val view = LocalView.current
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource, view) {
        interactionSource.interactions
            .filterIsInstance<PressInteraction.Release>()
            .collect {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
    }

    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        thumbContent = {
            if (checked) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            }
        },
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
    )
}