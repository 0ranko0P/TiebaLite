@file:Suppress("NOTHING_TO_INLINE")

package com.huanchengfly.tieba.post.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * [RowScope.FadedVisibility] composable animates the appearance and disappearance of its content
 * when the [AnimatedVisibility] is in a [Row]. The default animations are tailored specific to the
 * [Row] layout. See [androidx.compose.animation.AnimatedVisibility].
 *
 * @param visible defines whether the content should be visible
 * @param modifier modifier for the [Layout] created to contain the [content]
 * @param label A label to differentiate from other animations in Android Studio animation preview.
 * @param content Content to appear or disappear based on the value of [visible]
 */
@Composable
inline fun RowScope.FadedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    label: String = "FadedVisibility",
    noinline content: @Composable() AnimatedVisibilityScope.() -> Unit,
) =
    AnimatedVisibility(visible, modifier, fadeIn(), fadeOut(), label, content)

@Composable
inline fun ColumnScope.FadedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    label: String = "FadedVisibility",
    noinline content: @Composable AnimatedVisibilityScope.() -> Unit,
) =
    AnimatedVisibility(visible, modifier, fadeIn(), fadeOut(), label, content)
