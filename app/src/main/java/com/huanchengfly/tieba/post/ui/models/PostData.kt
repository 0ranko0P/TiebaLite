package com.huanchengfly.tieba.post.ui.models

import android.content.Context
import androidx.compose.runtime.Immutable
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.contentRenders
import com.huanchengfly.tieba.post.api.models.protos.plainText
import com.huanchengfly.tieba.post.ui.common.PbContentRender
import com.huanchengfly.tieba.post.utils.BlockManager.shouldBlock
import com.huanchengfly.tieba.post.utils.DateTimeUtils.getRelativeTimeString
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * Represents [Post] in UI
 *
 * @param id            Post ID
 * @param subPosts      Loaded replies
 * @param subPostNumber Total number of replies
 * */
@Immutable
data class PostData(
    val id: Long,
    val author: UserData,
    val floor: Int,
    val title: String,
    val isNTitle: Boolean,
    val time: Long,
    val hasAgree: Int,
    val agreeNum: Long,
    val diffAgreeNum: Long,
    val blocked: Boolean,
    val plainText: String,
    val contentRenders: ImmutableList<PbContentRender>,
    val subPosts: ImmutableList<SubPostItemData>,
    val subPostNumber: Int
) {
    fun updateAgreeStatus(hasAgree: Int): PostData {
        return if (hasAgree != this.hasAgree) {
            if (hasAgree == 1) {
                copy(agreeNum = agreeNum + 1, diffAgreeNum = diffAgreeNum + 1, hasAgree = 1)
            } else {
                copy(agreeNum = agreeNum - 1, diffAgreeNum = diffAgreeNum - 1, hasAgree = 0)
            }
        } else this
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

    companion object {
        fun from(post: Post): PostData {
            val plainText = post.content.plainText
            val lzId = post.origin_thread_info?.author?.id

            val author = if (post.author != null) {
                UserData(post.author, post.author_id == lzId)
            } else {
                UserData.Empty
            }

            return PostData(
                id = post.id,
                author = author,
                floor = post.floor,
                title = post.title,
                isNTitle = post.is_ntitle == 1,
                time = post.time.toLong(),
                hasAgree = post.agree?.hasAgree ?: 0,
                agreeNum = post.agree?.agreeNum ?: 0L,
                diffAgreeNum = post.agree?.diffAgreeNum ?: 0L,
                blocked = shouldBlock(post.author_id, plainText),
                plainText = plainText,
                contentRenders = post.contentRenders,
                getSubPosts(post),
                post.sub_post_number
            )
        }

        private const val DESC_SEPARATOR = " · "

        private fun getSubPosts(post: Post): ImmutableList<SubPostItemData> = post.run {
            sub_post_list?.sub_post_list?.map {
                SubPostItemData(subPost = it, lzId = origin_thread_info?.author?.id?: 0L)
            }?.toImmutableList() ?: persistentListOf()
        }
    }
}