package com.huanchengfly.tieba.post.ui.models.user

import androidx.compose.runtime.Immutable
import com.huanchengfly.tieba.post.api.models.protos.PostInfoList
import com.huanchengfly.tieba.post.ui.models.Author
import java.util.Objects

@Immutable
class PostContent(
    val postId: Long,
    val text: String,
    val timeDesc: String,
    val isSubPost: Boolean,
)

/**
 * UI Model of [PostInfoList]
 * */
@Immutable
class PostListItem(
    val author: Author,
    val contents: List<PostContent>,
    val title: String,
    val forumId: Long,
    val threadId: Long,
    val deleted: Boolean
) {

    val lazyListKey: Int
        get() = Objects.hash(threadId, contents.firstOrNull()?.postId ?: -1, contents.size)
}