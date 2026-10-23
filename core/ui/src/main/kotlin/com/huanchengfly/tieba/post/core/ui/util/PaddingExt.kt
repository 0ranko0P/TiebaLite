package com.huanchengfly.tieba.post.core.ui.util

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.takeOrElse

@Stable
fun PaddingValues.copy(
    start: Dp = Dp.Unspecified,
    top: Dp = Dp.Unspecified,
    end: Dp = Dp.Unspecified,
    bottom: Dp = Dp.Unspecified,
): PaddingValues = object : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
        (if (layoutDirection == LayoutDirection.Ltr) start else end).takeOrElse {
            this@copy.calculateLeftPadding(layoutDirection)
        }

    override fun calculateTopPadding(): Dp = top

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
        (if (layoutDirection == LayoutDirection.Ltr) end else start).takeOrElse {
            this@copy.calculateRightPadding(layoutDirection)
        }

    override fun calculateBottomPadding(): Dp = bottom
}
