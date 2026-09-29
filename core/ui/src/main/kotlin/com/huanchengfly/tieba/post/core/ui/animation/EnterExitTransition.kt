package com.huanchengfly.tieba.post.core.ui.animation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

fun defaultVerticalEnterTransition(topToBottom: Boolean = true): EnterTransition {
    return fadeIn(
        animationSpec = tween(durationMillis = AnimationConstants.DefaultDurationMillis / 2),
        initialAlpha = 0f
    ) + slideInVertically(
        initialOffsetY = { if (topToBottom) -it else it }
    )
}

fun defaultVerticalExitTransition(topToBottom: Boolean = true): ExitTransition {
    return fadeOut(
        animationSpec = tween(durationMillis = AnimationConstants.DefaultDurationMillis / 2),
        targetAlpha = 0f
    ) + slideOutVertically(
        targetOffsetY = { if (topToBottom) -it else it }
    )
}

