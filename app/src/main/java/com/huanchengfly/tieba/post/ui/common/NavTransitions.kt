package com.huanchengfly.tieba.post.ui.common

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Immutable
import com.huanchengfly.tieba.post.core.common.ktx.unsafeLazy

@Immutable
class NavTransitions(
    val enterTransition: EnterTransition,
    val exitTransition: ExitTransition,
    val popEnterTransition: EnterTransition = enterTransition,
    val popExitTransition: ExitTransition = exitTransition,
) {

    val transitionSpec: ContentTransform
        get() = enterTransition togetherWith exitTransition

    val popTransitionSpec: ContentTransform
        get() = popEnterTransition togetherWith popExitTransition

    companion object {
        private val DefaultAnimationSpec: FiniteAnimationSpec<Any> = tween(
            durationMillis = AnimationConstants.DefaultDurationMillis,
            easing = FastOutSlowInEasing
        )

        @Suppress("UNCHECKED_CAST")
        fun <T> defaultAnimationSpec(): FiniteAnimationSpec<T> {
            return DefaultAnimationSpec as FiniteAnimationSpec<T>
        }

        val FadeTransitions: NavTransitions by unsafeLazy {
            NavTransitions(
                enterTransition = fadeIn(animationSpec = defaultAnimationSpec()),
                exitTransition = fadeOut(animationSpec = defaultAnimationSpec()),
            )
        }

        val DefaultTransitions: NavTransitions by unsafeLazy {
            NavTransitions(
                enterTransition = scaleIn(
                    animationSpec = defaultAnimationSpec(),
                    initialScale = 0.9f
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 300)
                ),
                exitTransition = scaleOut(
                    animationSpec = defaultAnimationSpec(),
                    targetScale = 1.1f
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 200)
                ),
                popEnterTransition = scaleIn(
                    animationSpec = defaultAnimationSpec(),
                    initialScale = 1.1f
                )  + fadeIn(
                    animationSpec = tween(durationMillis = 300)
                ),
                popExitTransition = scaleOut(
                    animationSpec = defaultAnimationSpec(),
                    targetScale = 0.9f
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 200)
                )
            )
        }

        val SlideTransitions: NavTransitions by unsafeLazy {
            NavTransitions(
                enterTransition = slideInHorizontally(
                    animationSpec = defaultAnimationSpec(),
                    initialOffsetX = { it }
                ),
                exitTransition = slideOutHorizontally(
                    animationSpec = defaultAnimationSpec(),
                    targetOffsetX = { -it / 8 }
                ) + scaleOut(
                    animationSpec = defaultAnimationSpec(),
                    targetScale = 0.9f
                ),
                popEnterTransition = slideInHorizontally(
                    animationSpec = defaultAnimationSpec(),
                    initialOffsetX = { -it / 8 }
                ) + scaleIn(
                    animationSpec = defaultAnimationSpec(),
                    initialScale = 0.9f
                ),
                popExitTransition = slideOutHorizontally(
                    animationSpec = defaultAnimationSpec(),
                    targetOffsetX = { it }
                ) + fadeOut(
                    animationSpec = tween(delayMillis = 100)
                )
            )
        }
    }
}