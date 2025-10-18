package com.huanchengfly.tieba.post.ui.models

import androidx.compose.runtime.Immutable
import com.huanchengfly.tieba.post.api.models.protos.OriginThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo

// ForumID, name, avatar
typealias SimpleForum = Triple<Long, String, String?>

/**
 * Represents [ThreadInfo] in UI
 *
 * @param collected         True when [ThreadInfo.collectStatus] not 0
 * @param isShareThread     True when [ThreadInfo.is_share_thread] is 1
 * */
@Immutable
data class ThreadInfoData(
    val id: Long,
    val title: String,
    val collectMarkPid: Long,
    val collected: Boolean,
    val firstPostId: Long,
    val like: Like,
    val isShareThread: Boolean,
    val originThreadInfo: OriginThreadInfo?,
    val replyNum: Int,
    val simpleForum: SimpleForum,
) {
    constructor(info: ThreadInfo): this(
        id = info.threadId.takeUnless { it <= 0 } ?: info.id,
        title = info.title,
        collectMarkPid = info.collectMarkPid.toLongOrNull()?: 0L,
        collected = info.collectStatus != 0,
        firstPostId = info.firstPostId,
        like = info.agree?.let { Like(it) } ?: LikeZero,
        isShareThread = info.is_share_thread == 1,
        originThreadInfo = info.origin_thread_info?.takeIf { info.is_share_thread == 1 },
        replyNum = info.replyNum,
        simpleForum = info.forumInfo!!.let { SimpleForum(it.id, it.name, it.avatar) }
    )

    fun updateCollectStatus(collected: Boolean = true, markPostId: Long = 0): ThreadInfoData {
        return this.copy(collected = collected, collectMarkPid = markPostId)
    }

    /**
     * Called when user clicked like button
     *
     * @return new [ThreadInfoData] with like status updated
     * */
    fun updateLikeStatus(liked: Boolean, loading: Boolean): ThreadInfoData = copy(
        like = like.updateLikeStatus(liked).setLoading(loading)
    )
}