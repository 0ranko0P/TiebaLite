package com.huanchengfly.tieba.post.ui.models

import androidx.compose.runtime.Immutable
import com.huanchengfly.tieba.post.api.models.protos.OriginThreadInfo
import com.huanchengfly.tieba.post.arch.ImmutableHolder

/**
 * Forum ID, name and avatar``(nullable)``
 *
 * @see com.huanchengfly.tieba.post.api.models.protos.SimpleForum
 * */
typealias SimpleForum = Triple<Long, String, String?>

/**
 * Ui Model of [com.huanchengfly.tieba.post.api.models.protos.ThreadInfo].
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
) {

    /**
     * Is this thread collected.
     *
     * @see collectMarkPid
     * @see com.huanchengfly.tieba.post.api.models.protos.ThreadInfo.collectStatus
     * */
    val collected: Boolean
        get() = collectMarkPid != null

    fun copy(
        title: String = this.title,
        collectMarkPid: Long? = this.collectMarkPid,
        like: Like = this.like,
        originThreadInfo: ImmutableHolder<OriginThreadInfo>? = this.originThreadInfo,
        replyNum: Int = this.replyNum,
        simpleForum: SimpleForum = this.simpleForum,
    ) = ThreadInfoData(
        id = this.id,
        title = title,
        collectMarkPid = collectMarkPid,
        firstPostId = this.firstPostId,
        like = like,
        originThreadInfo = originThreadInfo,
        replyNum = replyNum,
        simpleForum = simpleForum
    )

    /**
     * Called when user clicked like button
     *
     * @return new [ThreadInfoData] with like status updated
     * */
    fun updateLikeStatus(liked: Boolean, loading: Boolean): ThreadInfoData = copy(
        like = like.updateLikeStatus(liked).setLoading(loading)
    )
}