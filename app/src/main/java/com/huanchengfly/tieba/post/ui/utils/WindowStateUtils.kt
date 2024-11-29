package com.huanchengfly.tieba.post.ui.utils

import android.graphics.Rect
import androidx.window.layout.FoldingFeature
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.WindowWidthSizeClass
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Information about the posture of the device
 */
sealed interface DevicePosture {
    data object NormalPosture : DevicePosture

    data class BookPosture(
        val hingePosition: Rect
    ) : DevicePosture

    data class Separating(
        val hingePosition: Rect,
        var orientation: FoldingFeature.Orientation
    ) : DevicePosture
}

@OptIn(ExperimentalContracts::class)
fun isBookPosture(foldFeature: FoldingFeature?): Boolean {
    contract { returns(true) implies (foldFeature != null) }
    return foldFeature?.state == FoldingFeature.State.HALF_OPENED &&
            foldFeature.orientation == FoldingFeature.Orientation.VERTICAL
}

@OptIn(ExperimentalContracts::class)
fun isSeparating(foldFeature: FoldingFeature?): Boolean {
    contract { returns(true) implies (foldFeature != null) }
    return foldFeature?.state == FoldingFeature.State.FLAT && foldFeature.isSeparating
}

/**
 * Different type of navigation supported by app depending on device size and state.
 */
enum class MainNavigationType {
    BOTTOM_NAVIGATION, NAVIGATION_RAIL, PERMANENT_NAVIGATION_DRAWER
}

/**
 * Different position of navigation content inside Navigation Rail, Navigation Drawer depending on device size and state.
 */
enum class MainNavigationContentPosition {
    TOP, CENTER
}

fun WindowWidthSizeClass.getNavType(posture: DevicePosture): MainNavigationType = when (this) {

    WindowWidthSizeClass.Compact -> MainNavigationType.BOTTOM_NAVIGATION

    WindowWidthSizeClass.Medium -> MainNavigationType.NAVIGATION_RAIL

    WindowWidthSizeClass.Expanded -> {
        if (posture is DevicePosture.BookPosture) {
            MainNavigationType.NAVIGATION_RAIL
        } else {
            MainNavigationType.PERMANENT_NAVIGATION_DRAWER
        }
    }

    else -> MainNavigationType.BOTTOM_NAVIGATION
}
