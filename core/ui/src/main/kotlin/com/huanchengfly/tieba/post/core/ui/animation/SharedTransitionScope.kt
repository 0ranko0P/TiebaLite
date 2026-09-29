package com.huanchengfly.tieba.post.core.ui.animation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.PlaceholderSize
import androidx.compose.animation.SharedTransitionScope.ResizeMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation3.ui.LocalNavAnimatedContentScope

val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

@Composable
fun Modifier.localSharedElements(
    key: Any,
    boundsTransform: BoundsTransform = DefaultBoundsTransform,
    placeHolderSize: PlaceholderSize = PlaceholderSize.ContentSize,
    renderInOverlayDuringTransition: Boolean = true,
    zIndexInOverlay: Float = 0f,
    clipInOverlayDuringTransition: SharedTransitionScope.OverlayClip = ParentClip
): Modifier = composed {
    val sharedTransitionScope = LocalSharedTransitionScope.current ?: return@composed Modifier
    with(sharedTransitionScope) {
        Modifier.sharedElement(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = LocalNavAnimatedContentScope.current,
            boundsTransform = boundsTransform,
            placeholderSize = placeHolderSize,
            renderInOverlayDuringTransition = renderInOverlayDuringTransition,
            zIndexInOverlay = zIndexInOverlay,
            clipInOverlayDuringTransition = clipInOverlayDuringTransition
        )
    }
}

fun Modifier.localSharedBounds(
    key: Any,
    enter: EnterTransition = fadeIn(),
    exit: ExitTransition = fadeOut(),
    boundsTransform: BoundsTransform = DefaultBoundsTransform,
    resizeMode: ResizeMode = ResizeMode.scaleToBounds(ContentScale.FillWidth, Alignment.Center),
    placeHolderSize: PlaceholderSize = PlaceholderSize.ContentSize,
    renderInOverlayDuringTransition: Boolean = true,
    zIndexInOverlay: Float = 0f,
    clipInOverlayDuringTransition: SharedTransitionScope.OverlayClip = ParentClip
): Modifier = composed {
    val sharedTransitionScope = LocalSharedTransitionScope.current ?: return@composed Modifier
    with(sharedTransitionScope) {
        Modifier.sharedBounds(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = LocalNavAnimatedContentScope.current,
            enter = enter,
            exit = exit,
            boundsTransform = boundsTransform,
            resizeMode = resizeMode,
            placeholderSize = placeHolderSize,
            renderInOverlayDuringTransition = renderInOverlayDuringTransition,
            zIndexInOverlay = zIndexInOverlay,
            clipInOverlayDuringTransition = clipInOverlayDuringTransition
        )
    }
}

context(sharedTransitionScope: SharedTransitionScope)
fun Modifier.animateEnterExit(
    animatedVisibilityScope: AnimatedVisibilityScope?,
    zIndexInOverlay: Float = 1f,
    enter: EnterTransition = defaultVerticalEnterTransition(),
    exit: ExitTransition = defaultVerticalExitTransition(),
): Modifier {
    return with(sharedTransitionScope) {
        with(animatedVisibilityScope ?: return this@animateEnterExit) {
            this@animateEnterExit
                .renderInSharedTransitionScopeOverlay(zIndexInOverlay)
                .animateEnterExit(enter, exit)
        }
    }
}

private val ParentClip: SharedTransitionScope.OverlayClip =
    object : SharedTransitionScope.OverlayClip {
        override fun getClipPath(
            sharedContentState: SharedTransitionScope.SharedContentState,
            bounds: Rect,
            layoutDirection: LayoutDirection,
            density: Density
        ): Path? {
            return sharedContentState.parentSharedContentState?.clipPathInOverlay
        }
    }

val DefaultBoundsTransform = BoundsTransform { _, _ ->
    spring(
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = Rect.VisibilityThreshold
    )
}

val DefaultTextBoundsTransform = BoundsTransform { _, _ ->
    spring(
        stiffness = Spring.StiffnessLow,
        visibilityThreshold = Rect.VisibilityThreshold
    )
}