package com.huanchengfly.tieba.post.ui.models

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.util.fastDistinctBy
import androidx.compose.ui.util.fastMap
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.abstractText
import com.huanchengfly.tieba.post.api.models.protos.personalized.ThreadPersonalized
import com.huanchengfly.tieba.post.api.models.protos.updateAgreeStatus
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.ui.widgets.compose.buildThreadContent
import com.huanchengfly.tieba.post.utils.BlockManager.shouldBlock
import com.huanchengfly.tieba.post.utils.appPreferences
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Immutable
class ThreadItemData(
    val thread: ThreadInfoItem,
    val blocked: Boolean = thread.info.shouldBlock(),
    val personalized: ImmutableHolder<ThreadPersonalized>? = null,
    val hidden: Boolean = blocked && App.INSTANCE.appPreferences.hideBlockedContent,
) {

    constructor(info: ThreadInfo): this(ThreadInfoItem(info))

    val id: Long
        get() = thread.info.id

    val forumId: Long
        get() = thread.info.forumId

    val title: String
        get() = thread.info.title

    val threadId: Long
        get() = thread.info.threadId

    val isTop: Boolean
        get() = thread.info.isTop == 1
}

/**
 * Wrapper class to cache [content] AnnotatedString
 * */
@Immutable
class ThreadInfoItem(val info: ThreadInfo) {
    val hasAgree: Boolean = info.agree?.hasAgree == 1 // Like button
    val agreeNum: Long = info.agree?.agreeNum?: 0

    val content: AnnotatedString = with(info) {
        buildThreadContent(title, abstractText, tabName, isGood = this.isGood == 1)
    }
}

fun List<ThreadItemData>.updateAgreeStatus(threadId: Long): ImmutableList<ThreadItemData> {
    return fastMap { data ->
        val thread = data.thread.info
        if (thread.id == threadId) {
            val hasAgree = if (data.thread.hasAgree) 0 else 1
            ThreadItemData(thread.updateAgreeStatus(hasAgree = hasAgree))
        } else data
    }.toImmutableList()
}

fun List<ThreadItemData>.distinctById(): ImmutableList<ThreadItemData> {
    return fastDistinctBy { it.id }.toImmutableList()
}