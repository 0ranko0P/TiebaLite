package com.huanchengfly.tieba.post.ui.models.message

import androidx.compose.runtime.Immutable
import com.huanchengfly.tieba.post.api.models.MessageListBean.ReplyerInfoBean

/**
 * UI Model of [ReplyerInfoBean]
 * */
@Immutable
class ReplyUser(
    val id: Long,
    val nameShow: String,
    val avatarUrl: String?,
    val isFans: Boolean
)