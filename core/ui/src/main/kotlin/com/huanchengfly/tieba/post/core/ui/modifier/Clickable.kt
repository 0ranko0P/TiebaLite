package com.huanchengfly.tieba.post.core.ui.modifier

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

/**
 * Configure component to receive clicks via input or accessibility "click" event.
 *
 * Add this modifier to the element to make it clickable within its bounds and never show any
 * indication.
 *
 * @param enabled Controls the enabled state. When `false`, [onClick], and this modifier will appear
 *   disabled for accessibility services
 * @param role the type of user interface element. Accessibility services might use this to describe
 *   the element or do customizations
 * @param onClick will be called when user clicks on the element
 */
fun Modifier.clickableNoIndication(
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onClick: () -> Unit
): Modifier = this then Modifier.clickable(
    interactionSource = null,
    indication = null,
    enabled = enabled,
    onClickLabel = null,
    role = role,
    onClick = onClick
)
