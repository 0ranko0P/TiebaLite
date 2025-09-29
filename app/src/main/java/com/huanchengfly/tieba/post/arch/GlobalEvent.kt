package com.huanchengfly.tieba.post.arch

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
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