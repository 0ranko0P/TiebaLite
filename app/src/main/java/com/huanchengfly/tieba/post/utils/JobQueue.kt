package com.huanchengfly.tieba.post.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class JobQueue {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val queue = Channel<Job>(Channel.UNLIMITED)

    init {
        scope.launch(Dispatchers.Default) {
            for (job in queue) job.join()
        }
    }

    fun submit(
        context: CoroutineContext = EmptyCoroutineContext,
        block: suspend CoroutineScope.() -> Unit
    ) {
        synchronized(this) {
            val job = scope.launch(context, CoroutineStart.LAZY, block)
            queue.trySend(job)
        }
    }

    fun cancel() {
        queue.cancel()
        scope.cancel()
    }
}