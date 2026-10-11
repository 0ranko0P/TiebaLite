package com.huanchengfly.tieba.post.core.ui.util

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState

inline val AnimatedVisibilityScope.isPostExitAniRunning: Boolean
    get() = transition.isRunning && transition.targetState == EnterExitState.PostExit