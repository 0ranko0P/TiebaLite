package com.huanchengfly.tieba.post.ui.page.thread

import android.content.Context
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorCode
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.UiEvent

sealed interface ThreadLikeUiEvent : UiEvent {

    object Connecting: ThreadLikeUiEvent

    // object Succeed: ThreadLikeUiEvent

    object NotLoggedIn: ThreadLikeUiEvent

    class Failed(val e: Throwable) : ThreadLikeUiEvent

    fun toMessage(context: Context): String = when (this) {
        is Connecting -> context.getString(R.string.toast_connecting)

        is NotLoggedIn -> context.getString(R.string.title_not_logged_in)

        is Failed -> context.getString(R.string.snackbar_agree_fail, e.getErrorCode(), e.getErrorMessage())
    }
}