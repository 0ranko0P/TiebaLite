package com.huanchengfly.tieba.post.ui

import androidx.compose.runtime.Immutable

/**
 * 贴吧头像过渡动画唯一标识键
 * */
@Immutable
@JvmInline
value class ForumAvatarSharedBoundsKey(private val value: String) {

    /**
     * @param forumName 吧名
     * @param extraKey 额外标识键. 确保推荐页, 搜索页中多个贴子来自同一个吧时过渡动画的唯一性
     * */
    constructor(forumName: String, extraKey: Any?): this(
        if (extraKey != null) forumName + extraKey else forumName
    )
}

/**
 * 贴吧吧名过渡动画唯一标识键
 * */
@Immutable
@JvmInline
value class ForumTitleSharedBoundsKey(private val value: String) {

    /**
     * @param forumName 吧名
     * @param extraKey 额外标识键. 确保推荐页, 搜索页中多个贴子来自同一个吧时过渡动画的唯一性
     * */
    constructor(forumName: String, extraKey: Any?): this(
        if (extraKey != null) forumName + extraKey else forumName
    )
}

object SearchToolbarSharedBoundsKey