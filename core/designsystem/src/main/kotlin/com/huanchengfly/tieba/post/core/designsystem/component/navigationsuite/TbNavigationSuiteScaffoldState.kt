package com.huanchengfly.tieba.post.core.designsystem.component.navigationsuite

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldState
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.huanchengfly.tieba.post.core.designsystem.component.navigationsuite.TbNavigationSuiteScaffoldState.Companion.Saver

class TbNavigationSuiteScaffoldState(
    var initialValue: NavigationSuiteScaffoldValue
): NavigationSuiteScaffoldState {

    var layoutType by mutableStateOf(TbNavigationSuiteType.None)
        private set

    internal val internalState = Animatable(
        initialValue = if (initialValue.isVisible) Visible else Hidden,
        typeConverter = Float.VectorConverter
    )

    override val isAnimating: Boolean
        get() = internalState.isRunning

    private val _currentVal = derivedStateOf {
        if (internalState.value == Visible) {
            NavigationSuiteScaffoldValue.Visible
        } else {
            NavigationSuiteScaffoldValue.Hidden
        }
    }

    override val targetValue: NavigationSuiteScaffoldValue
        get() = if (internalState.targetValue == Visible) NavigationSuiteScaffoldValue.Visible else NavigationSuiteScaffoldValue.Hidden

    override val currentValue: NavigationSuiteScaffoldValue
        get() = _currentVal.value

    override suspend fun hide() {
        internalState.snapTo(Hidden)
    }

    override suspend fun show() {
        internalState.animateTo(targetValue = Visible, animationSpec = AnimationSpec)
    }

    override suspend fun toggle() {
        internalState.animateTo(
            targetValue = if (targetValue.isVisible) Hidden else Visible,
            animationSpec = AnimationSpec,
        )
    }

    suspend fun setState(state: NavigationSuiteScaffoldValue) {
        if (state.isVisible) show() else snapTo(state)
    }

    override suspend fun snapTo(targetValue: NavigationSuiteScaffoldValue) {
        val target = if (targetValue.isVisible) Visible else Hidden
        internalState.snapTo(target)
    }

    companion object {
        private val AnimationSpec: SpringSpec<Float> = spring(dampingRatio = 0.9f, stiffness = 700f)

        private const val Hidden = 0f
        private const val Visible = 1f

        val NavigationSuiteScaffoldValue.isVisible
            get() = this === NavigationSuiteScaffoldValue.Visible

        /** The default [Saver] implementation for [NavigationSuiteScaffoldState]. */
        private fun Saver() =
            Saver<TbNavigationSuiteScaffoldState, NavigationSuiteScaffoldValue>(
                save = { it.targetValue },
                restore = { TbNavigationSuiteScaffoldState(it) },
            )

        /** Create and [remember] a [TbNavigationSuiteScaffoldState] */
        @Composable
        fun rememberNavigationSuiteScaffoldState(
            initialValue: NavigationSuiteScaffoldValue = NavigationSuiteScaffoldValue.Visible,
            floating: Boolean = false,
            noLabel: Boolean = false,
            windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfoV2(),
        ): TbNavigationSuiteScaffoldState {
            val state = rememberSaveable(saver = Saver()) {
                TbNavigationSuiteScaffoldState(initialValue = initialValue)
            }

            LaunchedEffect(windowAdaptiveInfo, floating, noLabel) {
                state.layoutType = TbNavigationSuiteType.fromNavigationSuiteType(
                    type = calculateNavigationType(windowAdaptiveInfo),
                    floating = floating,
                    noLabel = noLabel
                )
            }
            return state
        }
    }
}
