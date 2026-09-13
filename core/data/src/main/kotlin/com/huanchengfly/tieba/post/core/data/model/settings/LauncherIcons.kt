package com.huanchengfly.tieba.post.core.data.model.settings

enum class LauncherIcons {
    NEW_ICON, NEW_ICON_THEMED, NEW_ICON_INVERT, OLD_ICON;

    fun supportThemedIcon(): Boolean = this == NEW_ICON
}