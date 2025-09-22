package com.huanchengfly.tieba.post.ui.models.settings

import androidx.annotation.IntDef


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
 * User habit
 *
 * @param favoriteDesc 收藏贴自动开启倒序浏览
 * @param favoriteSeeLz 从收藏进入的贴子将自动切换至只看楼主
 * @param forumSortType 吧页面默认排序方式, default: [ForumSortType.BY_REPLY]
 * @param forumFAB 吧页面悬浮按钮功能, default: [ForumFAB.BACK_TO_TOP]
 * @param showBothName 同时显示用户名和昵称, default: false
 * @param showHistoryInHome 首页显示最近逛的吧, default: true
 * */
class HabitSettings(
    val favoriteDesc: Boolean,
    val favoriteSeeLz: Boolean,
    @ForumSortType val forumSortType: Int,
    @ForumFAB val forumFAB: Int,
    val showBothName: Boolean,
    val showHistoryInHome: Boolean
)