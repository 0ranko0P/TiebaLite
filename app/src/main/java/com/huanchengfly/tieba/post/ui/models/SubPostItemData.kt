package com.huanchengfly.tieba.post.ui.models

import androidx.annotation.WorkerThread
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.getContentText
import com.huanchengfly.tieba.post.api.models.protos.plainText
import com.huanchengfly.tieba.post.api.models.protos.plainTexts
import com.huanchengfly.tieba.post.api.models.protos.renders
import com.huanchengfly.tieba.post.api.models.protos.updateAgreeStatus
import com.huanchengfly.tieba.post.ui.common.PbContentRender
import com.huanchengfly.tieba.post.utils.BlockManager.shouldBlock

/**
 * Represents [SubPostList] in UI
 * */
@Immutable
class SubPostItemData private constructor(
    val author: UserData,
    val subPost: SubPostList,
) {
    val id: Long
        get() = subPost.id

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

    val blocked: Boolean = shouldBlock(authorId, *subPost.content.plainTexts.toTypedArray())

    val time: Long
        get() = subPost.time.toLong()

    val plainText: String
        get() = subPost.content.plainText

    val hasAgree: Boolean
        get() = subPost.agree?.hasAgree == 1

    val agreeNum: Long
        get() = subPost.agree?.agreeNum ?: 0

    val diffAgreeNum: Long
        get() = subPost.agree?.diffAgreeNum ?: 0

    constructor(subPost: SubPostList, lzId: Long): this(
        author = UserData(subPost.author!!, lzId == subPost.author_id),
        subPost = subPost
    )

    @WorkerThread
    fun buildContent(fromSubPost: Boolean): SubPostItemData {
        if (fromSubPost) {
            pbContent = subPost.content.renders
        } else {
            content = subPost.getContentText(isLz)
        }
        return this
    }

    fun updateAgreeStatus(hasAgree: Boolean): SubPostItemData {
        return SubPostItemData(author, subPost.updateAgreeStatus(if (hasAgree) 1 else 0)).also {
            it.content = content
            it.pbContent = pbContent
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SubPostItemData

        if (id != other.id) return false
        if (blocked != other.blocked) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + blocked.hashCode()
        result = 31 * result + isLz.hashCode()
        return result
    }
}