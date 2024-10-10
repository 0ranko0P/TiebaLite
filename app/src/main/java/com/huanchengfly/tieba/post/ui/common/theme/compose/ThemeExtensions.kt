package com.huanchengfly.tieba.post.ui.common.theme.compose

import androidx.compose.ui.graphics.Color
import com.huanchengfly.tieba.post.utils.ThemeUtil

val ExtendedColors.pullRefreshIndicator: Color
    get() = if (ThemeUtil.isTranslucentTheme(this)) {
        windowBackground
    } else {
        indicator
    }

val ExtendedColors.loadMoreIndicator: Color
    get() = if (ThemeUtil.isTranslucentTheme(this)) {
        windowBackground
    } else {
        indicator
    }

val ExtendedColors.threadBottomBar: Color
    get() = if (ThemeUtil.isTranslucentTheme(this)) {
        windowBackground
    } else {
        bottomBar
    }

val ExtendedColors.menuBackground: Color
    get() = if (ThemeUtil.isTranslucentTheme(this)) {
        windowBackground
    } else {
        card
    }

val ExtendedColors.invertChipBackground: Color
    get() = if (this.isNightMode) primary.copy(alpha = 0.3f) else primary

val ExtendedColors.invertChipContent: Color
    get() = if (this.isNightMode) primary else onPrimary