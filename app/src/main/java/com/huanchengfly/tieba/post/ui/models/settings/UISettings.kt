package com.huanchengfly.tieba.post.ui.models.settings

import androidx.compose.runtime.Immutable
import com.huanchengfly.tieba.post.utils.LauncherIcons

enum class DarkPreference {
    FOLLOW_SYSTEM, ALWAYS, DISABLED
}

/**
 * 底部导航栏标签显示模式
 *
 * @since 4.0.0-beta.4.3
 * */
enum class NavigationLabel {
    ALWAYS, SELECTED, NONE
}

/**
 * User UI Settings
 *
 * @param appIcon 应用图标
 * @param appIconThemed 应用图标使用动态取色
 * @param bottomNavFloating 主页底部导航栏悬浮模式
 * @param bottomNavLabel 主页底部导航栏标签显示模式
 * @param darkAmoled 纯黑背景颜色
 * @param darkPreference 夜间模式偏好
 * @param darkenImage 夜间模式压暗缩略图
 * @param hideExplore 隐藏主页「动态」入口
 * @param reduceEffect 降低模糊效果
 * @param setupFinished 设置向导已完成
 * @param homeForumList 吧列表单列显示
 * */
@Immutable
data class UISettings(
    val appIcon: LauncherIcons = LauncherIcons.NEW_ICON,
    val appIconThemed: Boolean = false,
    val bottomNavFloating: Boolean = false,
    val bottomNavLabel: NavigationLabel = NavigationLabel.ALWAYS,
    val darkAmoled: Boolean = false,
    val darkPreference: DarkPreference = DarkPreference.FOLLOW_SYSTEM,
    val darkenImage: Boolean = true,
    val hideExplore: Boolean = false,
    val reduceEffect: Boolean = false,
    val setupFinished: Boolean = false,
    val homeForumList: Boolean = false,
)