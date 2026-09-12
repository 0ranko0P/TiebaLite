package com.huanchengfly.tieba.post.ui.models

import androidx.compose.runtime.Immutable
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.core.network.model.protos.OriginThreadInfo

/**
 * Forum ID, name and avatar``(nullable)``
 *
 * @see com.huanchengfly.tieba.post.core.network.model.protos.SimpleForum
 * */
typealias SimpleForum = Triple<Long, String, String?>

/**
 * UI Model of [com.huanchengfly.tieba.post.core.network.model.protos.ThreadInfo].
 *
 * Mini version of [ThreadItem] for thread page.
 * */
@Immutable
/*data */class ThreadInfoData(
    val id: Long,
    val title: String,
    val collectMarkPid: Long?,
    val firstPostId: Long,
    val like: Like,
    val originThreadInfo: ImmutableHolder<OriginThreadInfo>?,
    val replyNum: Int,
    val simpleForum: SimpleForum,
    val pollInfo: ThreadPollInfo?,
) {

    /**
     * Is this thread collected.
     *
     * @see collectMarkPid
     * @see com.huanchengfly.tieba.post.core.network.model.protos.ThreadInfo.collectStatus
     * */
    val collected: Boolean
        get() = collectMarkPid != null

    fun copy(
        title: String = this.title,
        collectMarkPid: Long? = this.collectMarkPid,
        firstPostId: Long = this.firstPostId,
        like: Like = this.like,
        originThreadInfo: ImmutableHolder<OriginThreadInfo>? = this.originThreadInfo,
        replyNum: Int = this.replyNum,
        simpleForum: SimpleForum = this.simpleForum,
        pollInfo: ThreadPollInfo? = this.pollInfo,
    ) = ThreadInfoData(
        id = this.id,
        title = title,
        collectMarkPid = collectMarkPid,
        firstPostId = firstPostId,
        like = like,
        originThreadInfo = originThreadInfo,
        replyNum = replyNum,
        simpleForum = simpleForum,
        pollInfo = pollInfo,
    )

    /**
     * Called when user clicked like button
     *
     * @return new [ThreadInfoData] with like status updated
     * */
    fun updateLikeStatus(liked: Boolean, loading: Boolean): ThreadInfoData = copy(
        like = like.updateLikeStatus(liked).setLoading(loading)
    )

    fun updatePollStatus(loading: Boolean): ThreadInfoData = copy(
        pollInfo = pollInfo?.copy(isLoading = loading)
    )
}