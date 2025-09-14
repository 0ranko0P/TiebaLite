package com.huanchengfly.tieba.post.ui.models

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.getContentText
import com.huanchengfly.tieba.post.api.models.protos.plainText
import com.huanchengfly.tieba.post.api.models.protos.plainTexts
import com.huanchengfly.tieba.post.api.models.protos.renders
import com.huanchengfly.tieba.post.ui.common.PbContentRender
import com.huanchengfly.tieba.post.utils.BlockManager.shouldBlock

/**
 * Represents [SubPostList] in UI
 *
 * This class caches PbContentRenders to avoid compiling regex patterns or build highlighted
 * content during the compose.
 *
 * @param author remapped author of this reply from [SubPostList.author]
 * @param id [SubPostList.id]
 * @param blocked whether [author] or [plainText] is blocked
 * @param time [SubPostList.time]
 * @param like remapped [SubPostList.agree]
 * @param plainText string text of [SubPostList.content]
 * */
@Immutable
/* data */class SubPostItemData private constructor(
    val author: UserData,
    val id: Long,
    val blocked: Boolean,
    val time: Long,
    val like: Like,
    val plainText: String,
) {

    val authorId: Long
        get() = author.id

    val isLz: Boolean
        get() = author.isLz

    // Content for ThreadPage
    var content: AnnotatedString? = null
        private set

    // Content for SubPostsPage
    var pbContent: List<PbContentRender>? = null
        private set

    constructor(subPost: SubPostList, lzId: Long, fromSubPost: Boolean): this(
        author = UserData(subPost.author!!, isLz = lzId == subPost.author.id),
        id = subPost.id,
        blocked = shouldBlock(subPost.author.id, *subPost.content.plainTexts.toTypedArray()),
        time = subPost.time.toLong(),
        like = subPost.agree?.let { Like(agree = it) } ?: LikeZero,
        plainText = subPost.content.plainText,
    ) {
        if (fromSubPost) {
            pbContent = subPost.content.renders
        } else {
            content = subPost.getContentText(isLz)
        }
    }

    /**
     * Called when user clicked like button
     *
     * @return new item with updated like status
     * */
    fun updateLikesCount(liked: Boolean, loading: Boolean): SubPostItemData = copy(
        like = like.updateLikeStatus(liked).setLoading(loading)
    )

    fun copy(
        blocked: Boolean = this.blocked,
        time: Long = this.time,
        like: Like = this.like,
        plainText: String = this.plainText
    ) = SubPostItemData(
        author = this.author,
        id = this.id,
        blocked = blocked,
        time = time,
        like = like,
        plainText = plainText
    ).also {
        it.content = this.content
        it.pbContent = this.pbContent
    }
}