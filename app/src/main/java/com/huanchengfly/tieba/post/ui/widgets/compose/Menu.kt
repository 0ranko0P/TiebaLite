package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.core.ui.modifier.onNotNull
import com.huanchengfly.tieba.post.core.ui.util.DisplayUtil.toDpOffset
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.Options
import kotlinx.coroutines.flow.filterIsInstance

val DefaultMenuItemContentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding

class MenuScope(
    private val menuState: MenuState,
    private val menuItemContentPadding: PaddingValues = DefaultMenuItemContentPadding,
    private val onDismiss: (() -> Unit)? = null,
) {
    fun dismiss() {
        onDismiss?.invoke()
        menuState.dismiss()
    }

    @NonRestartableComposable
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
            colors = MenuDefaults.itemColors(),
            contentPadding = menuItemContentPadding,
        )

    /**
     * Simple Text [DropdownMenuItem] with leadingIcon, auto close the menu after onClick event triggered.
     *
     * @see [MenuScope.dismiss]
     * */
    @Composable
    fun TextIconMenuItem(
        modifier: Modifier = Modifier,
        text: String,
        icon: ImageVector,
        enabled: Boolean = true,
        onClick: () -> Unit
    ) = DropdownMenuItem(
        text = { Text(text = text) },
        onClick = {
            onClick()
            dismiss()
        },
        modifier = modifier,
        leadingIcon = {
            Icon(imageVector = icon, contentDescription = null)
        },
        enabled = enabled,
        contentPadding = menuItemContentPadding,
    )

    @Composable
    fun ListPickerMenuItem(
        text: String,
        modifier: Modifier = Modifier,
        picked: Boolean,
        pickedIndicator: @Composable (() -> Unit)? = null,
        onClick: () -> Unit
    ) =
        DropdownMenuItem(
            selected = picked,
            onClick = {
                if (!picked) {
                    onClick()
                }
                dismiss()
            },
            text = { Text(text = text) },
            shapes = MenuDefaults.itemShapes(),
            modifier = modifier,
            trailingIcon = pickedIndicator,
            contentPadding = menuItemContentPadding,
        )

    @Composable
    @NonRestartableComposable
    fun ListPickerMenuItem(
        @StringRes textRes: Int,
        modifier: Modifier = Modifier,
        picked: Boolean,
        pickedIndicator: @Composable (() -> Unit)? = null,
        onClick: () -> Unit
    ) = ListPickerMenuItem(
        text = stringResource(textRes),
        modifier = modifier,
        picked = picked,
        pickedIndicator = pickedIndicator,
        onClick = onClick,
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
                textRes = title,
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
    menuItemContentPadding: PaddingValues = DefaultMenuItemContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    indication: Indication? = LocalIndication.current,
    triggerShape: Shape? = null,
    onDismiss: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    LaunchedEffect(interactionSource) {
        interactionSource.interactions
            .filterIsInstance<PressInteraction.Press>()
            .collect {
                menuState.offset = it.pressPosition
            }
    }

    Box(
        modifier = Modifier
            .onNotNull(triggerShape) { clip(shape = it) }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = indication,
                enabled = enabled,
                onClick = menuState::show
            )
    ) {
        content()

        Box {
            DropdownMenuPopup(
                expanded = menuState.expanded,
                onDismissRequest = {
                    menuState.dismiss()
                    onDismiss?.invoke()
                },
                offset = menuState.offset.toDpOffset(),
                modifier = modifier,
            ) {
                DropdownMenuGroup(
                    shapes = MenuDefaults.groupShapes(),
                ) {
                    MenuScope(menuState, menuItemContentPadding, onDismiss).menuContent()
                }
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
    menuItemContentPadding: PaddingValues = DefaultMenuItemContentPadding,
    onClick: (() -> Unit)? = null,
    shape: Shape? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    indication: Indication? = LocalIndication.current,
    content: @Composable () -> Unit,
) {
    LaunchedEffect(interactionSource) {
        interactionSource.interactions
            .filterIsInstance<PressInteraction.Press>()
            .collect {
                menuState.offset = it.pressPosition
            }
    }

    Box(
        modifier = modifier
            .onNotNull(shape) { clip(shape = it) }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = indication,
                enabled = enabled,
                onLongClick = menuState::show,
                onClick = onClick ?: {}
            )
    ) {
        content()

        Box {
            DropdownMenuPopup(
                expanded = menuState.expanded,
                onDismissRequest = menuState::dismiss,
                offset = menuState.offset.toDpOffset(),
                modifier = modifier,
            ) {
                val menuScope = MenuScope(menuState, menuItemContentPadding)
                DropdownMenuGroup(
                    shapes = MenuDefaults.groupShapes(),
                    content = { menuScope.menuContent() }
                )
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

/** The State object of [DropdownMenu] */
@Stable
class MenuState internal constructor() {

    private var _expanded by mutableStateOf(false)
    /** Whether the menu is expanded */
    val expanded: Boolean
        get() = _expanded

    private var _offset by mutableStateOf(Offset.Zero)
    /** Current offset from the original position of the menu*/
    var offset: Offset
        get() = _offset
        set(value) {
            if (value != _offset) {
                _offset = value
            }
        }

    fun toggle() {
        _expanded = !expanded
    }

    fun show() {
        _expanded = true
    }

    fun dismiss() {
        _expanded = false
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
                    _expanded = it[0] as Boolean
                    offset = Offset(it[1] as Long)
                }
            }
        )
    }
}