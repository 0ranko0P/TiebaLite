package com.huanchengfly.tieba.post.ui.models

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.ui.common.PbContentRender

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
 * @param abstractContent formatted content without media, for ThreadPage
 * @param content full content with media, for SubPostsPage
 * */
@Immutable
/*data */class SubPostItemData(
    val author: UserData,
    val id: Long,
    val blocked: Boolean,
    val time: Long,
    val like: Like,
    val plainText: String,
    val abstractContent: AnnotatedString? = null,
    val content: List<PbContentRender>? = null
) {

    val authorId: Long
        get() = author.id

    val isLz: Boolean
        get() = author.isLz

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
        plainText: String = this.plainText,
    ) = SubPostItemData(
        author = this.author,
        id = this.id,
        blocked = blocked,
        time = time,
        like = like,
        plainText = plainText,
        abstractContent = this.abstractContent,
        content = this.content
    )
}