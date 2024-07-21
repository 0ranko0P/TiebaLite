package com.huanchengfly.tieba.post.ui.models

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.getContentText
import com.huanchengfly.tieba.post.api.models.protos.plainText
import com.huanchengfly.tieba.post.utils.BlockManager.shouldBlock

/**
 * Represents [SubPostList] in UI
 * */
@Immutable
data class SubPostItemData(
    val id: Long,
    val author: UserData,
    val content: AnnotatedString,
    val plainText: String,
    val blocked: Boolean,
    val isLz: Boolean,
    val authorId: Long,
) {
    constructor(subPost: SubPostList, lzId: Long): this(
        id = subPost.id,
        author = UserData(subPost.author!!, lzId == subPost.author_id),
        content = subPost.getContentText(lzId),
        plainText = subPost.content.plainText,
        blocked = subPost.shouldBlock(),
        isLz = lzId == subPost.author_id,
        authorId = subPost.author_id
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SubPostItemData

        if (id != other.id) return false
        if (blocked != other.blocked) return false
        if (isLz != other.isLz) return false
        if (authorId != other.authorId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + blocked.hashCode()
        result = 31 * result + isLz.hashCode()
        result = 31 * result + authorId.hashCode()
        return result
    }
}