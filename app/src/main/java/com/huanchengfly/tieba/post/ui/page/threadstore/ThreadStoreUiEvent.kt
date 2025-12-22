package com.huanchengfly.tieba.post.ui.page.threadstore

import android.content.Context
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.UiEvent

sealed interface ThreadStoreUiEvent : UiEvent {

    object Loading : ThreadStoreUiEvent

    sealed interface Add : ThreadStoreUiEvent {

        data class Success(val floor: Int) : Add

        data class Failure(val message: String) : Add
    }

    sealed interface Delete : ThreadStoreUiEvent {

        object Success : Delete

        data class Failure(val message: String) : Delete
    }

    fun toMessage(context: Context): String = when (this) {
        is Add.Failure -> context.getString(R.string.message_update_collect_mark_failed, message)

        is Add.Success -> context.getString(R.string.message_add_favorite_success, floor)

        is Delete.Failure -> context.getString(R.string.delete_store_failure, message)

        Delete.Success -> context.getString(R.string.delete_store_success)

        Loading -> context.getString(R.string.toast_connecting)
    }
}
