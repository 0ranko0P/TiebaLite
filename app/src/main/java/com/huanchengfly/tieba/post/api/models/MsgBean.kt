package com.huanchengfly.tieba.post.api.models

import com.google.gson.annotations.SerializedName
import com.huanchengfly.tieba.post.models.BaseBean
import com.huanchengfly.tieba.post.models.ErrorBean

class MsgBean : ErrorBean() {
    val message: MessageBean? = null

    data class MessageBean(
        @SerializedName("replyme")
        val replyMe: Int = 0,
        @SerializedName("atme")
        val atMe: Int = 0,
        val fans: Int = 0,
    ) : BaseBean()
}