package com.huanchengfly.tieba.post.ui.models.settings

import androidx.compose.runtime.Immutable
import com.huanchengfly.tieba.post.utils.LauncherIcons

enum class DarkPreference {
    FOLLOW_SYSTEM, ALWAYS, DISABLED
}

/**
 * User UI Settings
 *
 * @param appIcon 应用图标
 * @param appIconThemed 应用图标使用动态取色
 * @param darkAmoled 纯黑背景颜色
 * @param darkPreference 夜间模式偏好
 * @param darkenImage 夜间模式压暗缩略图
 * @param liftBottomBar 略微抬起贴子页底栏
 * @param reduceEffect 降低模糊效果
 * @param setupFinished 设置向导已完成
 * @param homeForumList 吧列表单列显示
 * */
@Immutable
data class UISettings(
    val appIcon: LauncherIcons = LauncherIcons.NEW_ICON,
    val appIconThemed: Boolean = false,
    val darkAmoled: Boolean = false,
    val darkPreference: DarkPreference = DarkPreference.FOLLOW_SYSTEM,
    val darkenImage: Boolean = true,
    val liftBottomBar: Boolean = true,
    val reduceEffect: Boolean = false,
    val setupFinished: Boolean = false,
    val homeForumList: Boolean = false,
)