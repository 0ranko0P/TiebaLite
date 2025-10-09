package com.huanchengfly.tieba.post.ui.page.search

import android.content.Context
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.UiEvent

sealed interface SearchUiEvent : UiEvent {

    object ClearHistorySucceed : SearchUiEvent

    class ClearHistoryFailed(val e: Throwable) : SearchUiEvent

    class DeleteHistoryFailed(val e: Throwable) : SearchUiEvent

    class Error(val e: Throwable): SearchUiEvent

    fun toMessage(context: Context): String = when(this) {
        ClearHistorySucceed -> context.getString(R.string.toast_clear_success)

        is ClearHistoryFailed -> context.getString(R.string.toast_clear_failure, e.getErrorMessage())

        is DeleteHistoryFailed -> context.getString(R.string.toast_delete_failure, e.getErrorMessage())

        is Error -> context.getString(R.string.title_unknown_error)
    }
}
