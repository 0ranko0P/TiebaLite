package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.block
import com.huanchengfly.tieba.post.ui.common.theme.compose.onNotNull
import com.huanchengfly.tieba.post.ui.widgets.compose.picker.Options
import kotlinx.coroutines.flow.filterIsInstance

class MenuScope(
    private val menuState: MenuState,
    private val onDismiss: (() -> Unit)? = null,
) {
    fun dismiss() {
        onDismiss?.invoke()
        menuState.expanded = false
    }

    @Composable
    fun TextMenuItem(modifier: Modifier = Modifier, @StringRes text: Int, onClick: () -> Unit) =
        TextMenuItem(modifier, stringResource(id = text), onClick)

    /**
     * Simple Text [DropdownMenuItem], auto close the menu after onClick event triggered.
     *
     * @see [MenuScope.dismiss]
     * */
    @Composable
    fun TextMenuItem(modifier: Modifier = Modifier, text: String, onClick: () -> Unit) =
        DropdownMenuItem(
            text = { Text(text = text) },
            onClick = {
                onClick()
                dismiss()
            },
            modifier = modifier,
        )

    @Composable
    fun ListPickerMenuItem(
        @StringRes text: Int,
        modifier: Modifier = Modifier,
        picked: Boolean,
        pickedIndicator: @Composable (() -> Unit)? = null,
        onClick: () -> Unit
    ) =
        DropdownMenuItem(
            text = {
                Text(text = stringResource(text))
            },
            onClick = {
                if (!picked) {
                    onClick()
                }
                dismiss()
            },
            modifier = modifier,
            trailingIcon = pickedIndicator,
            colors = if (picked) {
                val primary = MaterialTheme.colorScheme.primary
                MenuDefaults.itemColors(textColor = primary, trailingIconColor = primary)
            } else {
                MenuDefaults.itemColors()
            }
        )

    @Composable
    fun <Option> ListPickerMenuItems(
        items: Options<Option>,
        picked: Option,
        onItemPicked: (item: Option) -> Unit,
        pickedIndicator: @Composable () -> Unit = {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = stringResource(id = R.string.desc_checked),
            )
        }
    ) {
        items.forEach { (option, title) ->
            ListPickerMenuItem(
                text = title,
                picked = option == picked,
                onClick = {
                    onItemPicked(option)
                },
                pickedIndicator = pickedIndicator.takeIf { option == picked }
            )
        }
    }
}

@Composable
fun ClickMenu(
    menuContent: @Composable MenuScope.() -> Unit,
    modifier: Modifier = Modifier,
    menuState: MenuState = rememberMenuState(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    indication: Indication? = LocalIndication.current,
    triggerShape: Shape? = null,
    onDismiss: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val menuScope = MenuScope(menuState, onDismiss)
    LaunchedEffect(interactionSource) {
        interactionSource.interactions
            .filterIsInstance<PressInteraction.Press>()
            .collect {
                menuState.offset = it.pressPosition.round()
            }
    }

    Box(
        modifier = Modifier
            .block {
                triggerShape?.let { clip(it) }
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = indication,
                onClick = {
                    menuState.expanded = true
                }
            )
    ) {
        content()

        Box(
            modifier = Modifier.offset { menuState.offset }
        ) {
            DropdownMenu(
                expanded = menuState.expanded,
                onDismissRequest = menuScope::dismiss,
                modifier = modifier,
            ) {
                menuScope.menuContent()
            }
        }
    }
}

@Composable
fun LongClickMenu(
    menuContent: @Composable MenuScope.() -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    menuState: MenuState = rememberMenuState(),
    onClick: (() -> Unit)? = null,
    shape: Shape? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    indication: Indication? = LocalIndication.current,
    content: @Composable () -> Unit,
) {
    val menuScope = MenuScope(menuState)
    LaunchedEffect(interactionSource) {
        interactionSource.interactions
            .filterIsInstance<PressInteraction.Press>()
            .collect {
                menuState.offset = it.pressPosition.round()
            }
    }

    Box(
        modifier = modifier
            .onNotNull(shape) { clip(shape = it) }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = indication,
                enabled = enabled,
                onLongClick = {
                    menuState.expanded = true
                }
            ) {
                onClick?.invoke()
            }
    ) {
        content()
        Box(
            modifier = Modifier.offset { menuState.offset }
        ) {
            DropdownMenu(
                expanded = menuState.expanded,
                onDismissRequest = menuState::dismiss,
            ) {
                menuScope.menuContent()
            }
        }
    }
}

@Composable
fun rememberMenuState(): MenuState {
    return rememberSaveable(
        saver = MenuState.Saver,
        init = { MenuState() }
    )
}

@Stable
class MenuState internal constructor() {
    private var _expanded by mutableStateOf(false)

    fun toggle() {
        expanded = !expanded
    }

    fun show() {
        expanded = true
    }

    fun dismiss() {
        expanded = false
    }

    var expanded: Boolean
        get() = _expanded
        set(value) {
            if (value != _expanded) {
                _expanded = value
            }
        }

    private var _offset by mutableStateOf(IntOffset.Zero)

    var offset: IntOffset
        get() = _offset
        set(value) {
            if (value != _offset) {
                _offset = value
            }
        }

    companion object {
        val Saver: Saver<MenuState, *> = listSaver(
            save = {
                listOf<Any>(
                    it.expanded,
                    it.offset.packedValue
                )
            },
            restore = {
                MenuState().apply {
                    expanded = it[0] as Boolean
                    offset = IntOffset(it[1] as Long)
                }
            }
        )
    }
}