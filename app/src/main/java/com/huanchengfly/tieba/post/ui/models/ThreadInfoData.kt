package com.huanchengfly.tieba.post.ui.models

import com.huanchengfly.tieba.post.api.models.protos.OriginThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.hasAgree


/**
 * Represents [ThreadInfo] in UI
 *
 * @param collected         True when [ThreadInfo.collectStatus] is 1
 * @param isShareThread     True when [ThreadInfo.is_share_thread] is 1
 * */
data class ThreadInfoData(
    val title: String,
    val collectMarkPid: Long,
    val collected: Boolean,
    val firstPostId: Long,
    val hasAgree: Boolean,
    val agreeNum: Int,
    val diffAgreeNum: Long,
    val isShareThread: Boolean,
    val originThreadInfo: OriginThreadInfo?,
    val replyNum: Int,
) {
    constructor(info: ThreadInfo): this(
        title = info.title,
        collectMarkPid = info.collectMarkPid.toLongOrNull()?: 0L,
        collected = info.collectStatus == 1,
        firstPostId = info.firstPostId,
        hasAgree = info.hasAgree == 1,
        agreeNum = info.agreeNum,
        diffAgreeNum = info.agree?.diffAgreeNum?: info.agreeNum.toLong(),
        isShareThread = info.is_share_thread == 1,
        originThreadInfo = info.origin_thread_info,
        replyNum = info.replyNum
    )

    fun updateCollectStatus(collected: Boolean = true, markPostId: Long = 0): ThreadInfoData {
        return this.copy(collected = collected, collectMarkPid = markPostId)
    }

    fun updateAgreeStatus(hasAgree: Boolean): ThreadInfoData {
        return if (hasAgree != this.hasAgree) {
            val offset = if (hasAgree) 1 else -1
            copy(
                hasAgree = hasAgree,
                agreeNum = agreeNum + offset,
                diffAgreeNum = diffAgreeNum + offset
            )
        } else this
    }
}