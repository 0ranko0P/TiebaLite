package com.huanchengfly.tieba.post.arch

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.huanchengfly.tieba.post.ui.models.Like
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

sealed interface GlobalEvent : UiEvent {

    class ScrollToTop(val tag: Any) : GlobalEvent

    data class ReplySuccess(
        val threadId: Long,
        val newPostId: Long,
        val postId: Long? = null,
        val subPostId: Long? = null,
        val newSubPostId: Long? = null,
    ) : GlobalEvent

    data class ThreadLike(
        val threadId: Long,
        val like: Like,
        val forumId: Long = -1,
    ): GlobalEvent

    /**
     * 全局事件: 帖子收藏/更新
     *
     * @param threadId 帖子 ID
     * @param markedPostId 收藏到的楼 ID
     * */
    data class ThreadStore(val threadId: Long, val markedPostId: Long): GlobalEvent

    /**
     * 全局事件: 帖子取消收藏
     *
     * @param threadId 帖子 ID
     * */
    data class ThreadStoreDelete(val threadId: Long): GlobalEvent

    data class AddThreadSuccess(
        val newThreadId: Long,
        val newPostId: Long,
        val msg: String?,
    ) : GlobalEvent
}

private val globalEventSharedFlow: MutableSharedFlow<UiEvent> =
    MutableSharedFlow(0, 2, BufferOverflow.DROP_OLDEST)

val GlobalEventFlow = globalEventSharedFlow.asSharedFlow()

fun CoroutineScope.emitGlobalEvent(event: UiEvent) {
    launch {
        globalEventSharedFlow.emit(event)
    }
}

suspend fun emitGlobalEventSuspend(event: UiEvent) {
    globalEventSharedFlow.emit(event)
}

inline fun <reified Event : UiEvent> CoroutineScope.onGlobalEvent(
    noinline filter: (Event) -> Boolean = { true },
    noinline listener: suspend (Event) -> Unit,
): Job {
    return launch {
        GlobalEventFlow
            .filterIsInstance<Event>()
            .filter {
                filter(it)
            }
            .cancellable()
            .collect {
                Log.i("GlobalEvent", "onGlobalEvent: $it")
                listener(it)
            }
    }
}

@Composable
inline fun <reified Event : UiEvent> onGlobalEvent(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    noinline filter: (Event) -> Boolean = { true },
    noinline listener: suspend (Event) -> Unit,
) {
    DisposableEffect(filter, listener) {
        val job = coroutineScope.onGlobalEvent(filter, listener)
        onDispose {
            job.cancel()
        }
    }
}