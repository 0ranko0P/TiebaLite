package com.huanchengfly.tieba.post.ui.models

import android.content.Context
import androidx.compose.runtime.Immutable
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.ui.common.PbContentRender
import com.huanchengfly.tieba.post.utils.DateTimeUtils.getRelativeTimeString

/**
 * Represents [Post] in UI
 *
 * This class caches PbContentRenders to avoid compiling regex patterns or build highlighted
 * content during the compose.
 *
 * @param id [Post.id]
 * @param author remapped [Post.author]
 * @param floor post floor
 * @param title post title, **null** when title is empty or [Post.is_ntitle]
 * @param time fixed [Post.time]
 * @param like remapped [Post.agree]
 * @param blocked whether [author] blocked or [Post.content] contains blocked keyword
 * @param plainText string text of [Post.content]
 * @param contentRenders Composable [PbContentRender] built from [Post.content]
 * @param subPosts      replies from [Post.sub_post_list]
 * @param subPostNumber total number of replies
 * */
@Immutable
/*data */class PostData(
    val id: Long,
    val author: UserData,
    val floor: Int,
    val title: String?,
    val time: Long,
    val like: Like,
    val blocked: Boolean,
    val plainText: String,
    val contentRenders: List<PbContentRender>,
    val subPosts: List<SubPostItemData>?,
    val subPostNumber: Int
) {

    /**
     * Called when user clicked like button
     *
     * @return new [PostData] with updated like status
     * */
    fun updateLikesCount(liked: Boolean, loading: Boolean): PostData {
        return copy(like = like.updateLikeStatus(liked).setLoading(loading))
    }

    /**
     * Returns formatted string (e.g 一分钟前 · 第 2 楼 · 来自中国)
     *
     * @see getRelativeTimeString
     * @see [User.ip_address]
     * */
    fun getDescText(context: Context): String {
        val texts = listOfNotNull(
            if (time != 0L) getRelativeTimeString(context, time) else null,

            if (floor > 1) context.getString(R.string.tip_post_floor, floor) else null,

            if (author.ip.isNotEmpty()) context.getString(R.string.text_ip_location, author.ip) else null
        )
        return if (texts.isEmpty()) "" else texts.joinToString(DESC_SEPARATOR)
    }

    fun copy(
        author: UserData = this.author,
        title: String? = this.title,
        time: Long = this.time,
        like: Like = this.like,
        blocked: Boolean = this.blocked,
        plainText: String = this.plainText,
        contentRenders: List<PbContentRender> = this.contentRenders,
        subPosts: List<SubPostItemData>? = this.subPosts,
        subPostNumber: Int = this.subPostNumber
    ) = PostData(
        id = this.id,
        author = author,
        floor = this.floor,
        title = title,
        time = time,
        like = like,
        blocked = blocked,
        plainText = plainText,
        contentRenders = contentRenders,
        subPosts = subPosts,
        subPostNumber = subPostNumber
    )
}

private const val DESC_SEPARATOR = " · "