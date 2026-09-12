package com.huanchengfly.tieba.post.ui.models

import com.huanchengfly.tieba.post.core.network.model.protos.PollOption

/**
 * UI Model of [com.huanchengfly.tieba.post.core.network.model.protos.PollInfo]
 * */
data class ThreadPollInfo(
    val title: String?,
    val totalPoll: Long,
    val options: List<PollOption>,
    val endTime: Long,
    val isMulti: Boolean = false,
    val isLoading: Boolean = false,
    val polledValue: List<Int>? = null,
) {

    val isPolled: Boolean
        get() = !polledValue.isNullOrEmpty()

    val isTimeExpired: Boolean
        get() = endTime in 1..System.currentTimeMillis()
}