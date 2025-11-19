package com.huanchengfly.tieba.post.ui.models.settings

import androidx.annotation.IntDef
import androidx.compose.runtime.Immutable
import com.huanchengfly.tieba.post.utils.ImageUtil

@IntDef(ForumFAB.POST, ForumFAB.REFRESH, ForumFAB.BACK_TO_TOP, ForumFAB.HIDE)
@Retention(AnnotationRetention.SOURCE)
annotation class ForumFAB {
    companion object {
        const val POST = 1
        const val REFRESH = 2
        const val BACK_TO_TOP = 4
        const val HIDE = 8
    }
}

/**
 * 帖子排序方式
 * */
@IntDef(ForumSortType.BY_REPLY, ForumSortType.BY_SEND)
@Retention(AnnotationRetention.SOURCE)
annotation class ForumSortType {
    companion object {
        const val BY_REPLY = 0
        const val BY_SEND = 1
    }
}

/**
 * 图片上传水印
 * */
@IntDef(WaterType.NO, WaterType.USER_NAME, WaterType.FORUM_NAME)
@Retention(AnnotationRetention.SOURCE)
annotation class WaterType {
    companion object {
        const val NO = 0
        const val USER_NAME = 1
        const val FORUM_NAME = 2
    }
}

/**
 * User habit
 *
 * @param collectedDesc 收藏贴倒序浏览
 * @param favoriteDesc 收藏贴自动开启倒序浏览
 * @param favoriteSeeLz 从收藏进入的贴子将自动切换至只看楼主
 * @param forumSortType 吧页面默认排序方式
 * @param forumFAB 吧页面悬浮按钮功能
 * @param hideMedia 隐藏贴子列表的图片和视频
 * @param hideReply 隐藏回贴入口
 * @param hideReplyWarning 隐藏回贴风险提示
 * @param imageLoadType 图片加载设置
 * @param imageWatermarkType 图片上传水印设置
 * @param showBothName 同时显示用户名和昵称
 * @param showHistoryInHome 首页显示最近逛的吧
 * */
@Immutable
data class HabitSettings(
    val collectedDesc: Boolean = false,
    val favoriteDesc: Boolean = false,
    val favoriteSeeLz: Boolean = true,
    @ForumSortType val forumSortType: Int = ForumSortType.BY_REPLY,
    @ForumFAB val forumFAB: Int = ForumFAB.BACK_TO_TOP,
    val hideMedia: Boolean = false,
    val hideReply: Boolean = false,
    val hideReplyWarning: Boolean = false,
    val imageLoadType: Int = ImageUtil.SETTINGS_SMART_ORIGIN,
    @WaterType val imageWatermarkType: Int = WaterType.FORUM_NAME,
    val showBothName: Boolean = false,
    val showHistoryInHome: Boolean = true
)