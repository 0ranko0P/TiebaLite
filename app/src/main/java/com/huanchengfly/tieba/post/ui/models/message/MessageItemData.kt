package com.huanchengfly.tieba.post.ui.models.message

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import com.huanchengfly.tieba.post.api.models.MessageListBean.MessageInfoBean
import com.huanchengfly.tieba.post.ui.models.Author
import java.util.Objects

/**
 * UI Model of [MessageInfoBean]
 * */
@Immutable
/*data */class MessageItemData(
    val replyUser: ReplyUser,
    val threadId: Long,
    val postId: Long,
    val isBlocked: Boolean,
    val isFloor: Boolean,
    val title: AnnotatedString? = null,
    val content: AnnotatedString? = null,
    val time: Long,
    val quoteContent: AnnotatedString? = null,
    val quoteUser: Author? = null,
    val quotePid: Long? = null,
    val forumName: String? = null,
    val threadType: String? = null,
    val unread: String? = null
) {

    val lazyListItemKey: Int
        get() = Objects.hash(postId, replyUser.id, time, quotePid)
}