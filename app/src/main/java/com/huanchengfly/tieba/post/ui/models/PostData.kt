package com.huanchengfly.tieba.post.ui.models

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.ui.util.fastMapNotNull
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.contentRenders
import com.huanchengfly.tieba.post.api.models.protos.plainText
import com.huanchengfly.tieba.post.ui.common.PbContentRender
import com.huanchengfly.tieba.post.utils.BlockManager.shouldBlock
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.DateTimeUtils.getRelativeTimeString
import kotlinx.collections.immutable.ImmutableList

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
    val contentRenders: ImmutableList<PbContentRender>,
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
        contentRenders: ImmutableList<PbContentRender> = this.contentRenders,
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

    companion object {
        fun from(
            post: Post,
            lzId: Long,
            hideBlocked: Boolean = false,
        ): PostData {
            val plainText = post.content.plainText
            val like = if (post.agree == null) LikeZero else Like(post.agree)
            val authorId = post.author?.id ?: post.author_id
            val author = post.author?.let { UserData(post.author, authorId == lzId) }
            val subPostsList = post.getSubPostUiModels(lzId, hideBlocked)

            return PostData(
                id = post.id,
                author = author ?: UserData.Empty,
                floor = post.floor,
                title = post.title.takeUnless {
                    post.is_ntitle == 1 || post.title.isEmpty() || post.title.isBlank()
                },
                time = DateTimeUtils.fixTimestamp(post.time.toLong()),
                like = like,
                blocked = shouldBlock(post.author_id, plainText),
                plainText = plainText,
                contentRenders = post.contentRenders,
                subPosts = subPostsList,
                subPostNumber = post.sub_post_number
            )
        }

        private const val DESC_SEPARATOR = " · "

        /**
         * @return list of [SubPostItemData] and size of blocked items
         * */
        private fun Post.getSubPostUiModels(lzId: Long, hideBlocked: Boolean): List<SubPostItemData>? {
            return sub_post_list?.sub_post_list
                ?.fastMapNotNull {
                    val item = SubPostItemData(subPost = it, lzId = lzId, fromSubPost = false)
                    // remove if blocked and hide
                    if (item.blocked && hideBlocked) null else item
                }
                ?.takeUnless { it.isEmpty() }
        }
    }
}