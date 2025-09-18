package com.huanchengfly.tieba.post.ui.models.settings

enum class DarkPreference {
    FOLLOW_SYSTEM, ALWAYS, DISABLED
}

/**
 * User UI Settings
 *
 * @param darkPreference 夜间模式偏好
 * @param reduceEffect 降低模糊效果
 * @param setupFinished 设置向导已完成
 * @param homeForumList 吧列表单列显示
 * */
data class UISettings(
    val darkPreference: DarkPreference = DarkPreference.FOLLOW_SYSTEM,
    val reduceEffect: Boolean = false,
    val setupFinished: Boolean = false,
    val homeForumList: Boolean = false
)