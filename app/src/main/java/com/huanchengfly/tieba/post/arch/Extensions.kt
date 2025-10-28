package com.huanchengfly.tieba.post.arch

import android.util.Log
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.App.Companion.AppBackgroundScope
import com.huanchengfly.tieba.post.api.retrofit.exception.NoConnectivityException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlin.reflect.KProperty1

fun <T> Flow<T>.collectIn(
    lifecycleOwner: LifecycleOwner,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    action: suspend (T) -> Unit
): Job = lifecycleOwner.lifecycleScope.launch {
    flowWithLifecycle(lifecycleOwner.lifecycle, minActiveState).collect(action)
}

@Composable
inline fun <reified T : UiState, A> Flow<T>.collectPartialAsState(
    prop1: KProperty1<T, A>,
    initial: A,
): State<A> {
    return produceState(
        initialValue = initial,
        key1 = this,
        key2 = prop1,
        key3 = initial
    ) {
        this@collectPartialAsState
            .map {
                prop1.get(it)
            }
            .distinctUntilChanged()
            .collect {
                value = it
            }
    }
}

fun <T> Flow<T>.shareInBackground(
    started: SharingStarted = SharingStarted.WhileSubscribed(5_000),
    replay: Int = 1
): SharedFlow<T> = shareIn(AppBackgroundScope, started, replay)

@Throws(NoConnectivityException::class)
suspend inline fun <T> Flow<T>.firstOrThrow(): T = firstOrNull() ?: throw NoConnectivityException()

@Composable
inline fun <reified Event : UiEvent> Flow<UiEvent>.onEvent(
    noinline listener: suspend (Event) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    DisposableEffect(key1 = listener, key2 = this) {
        with(coroutineScope) {
            val job = launch {
                this@onEvent
                    .filterIsInstance<Event>()
                    .cancellable()
                    .flowOn(Dispatchers.IO)
                    .collect {
                        launch {
                            listener(it)
                        }
                    }
            }

            onDispose { job.cancel() }
        }
    }
}

@OptIn(InternalComposeApi::class)
@Composable
inline fun <reified Event : UiEvent> BaseViewModel<*, *, *, *>.onEvent(
    noinline listener: suspend (Event) -> Unit
) {
    val applyContext = currentComposer.applyCoroutineContext
    val coroutineScope = remember(applyContext) { CoroutineScope(applyContext) }
    DisposableEffect(key1 = listener, key2 = this) {
        val job = coroutineScope.launch {
            uiEventFlow
                .filterIsInstance<Event>()
                .cancellable()
                .flowOn(Dispatchers.IO)
                .collect {
                    coroutineScope.launch {
                        listener(it)
                    }
                }
        }

        onDispose { job.cancel() }
    }
}

@Composable
inline fun <reified VM : BaseViewModel<*, *, *, *>> pageViewModel(
    key: String? = null,
): VM {
    return hiltViewModel<VM>(key = key).apply {
        val context = LocalContext.current
        if (context is BaseComposeActivity) {
            val coroutineScope = rememberCoroutineScope()

            DisposableEffect(key1 = this) {
                with(coroutineScope) {
                    val job =
                        uiEventFlow
                            .filterIsInstance<CommonUiEvent>()
                            .cancellable()
                            .flowOn(Dispatchers.IO)
                            .collectIn(context) {
                                context.handleCommonEvent(it)
                            }

                    onDispose {
                        Log.i(this@apply::class.simpleName, "onDispose")
                        job.cancel()
                    }
                }
            }
        }
    }
}

@Composable
inline fun <INTENT : UiIntent, reified VM : BaseViewModel<INTENT, *, *, *>> pageViewModel(
    initialIntent: List<INTENT> = emptyList(),
    key: String? = null,
): VM {
    return pageViewModel<VM>(key = key).apply {
        if (initialIntent.isNotEmpty()) {
            LaunchedEffect(key1 = initialized) {
                if (!initialized) {
                    initialized = true
                    initialIntent.asFlow()
                        .onEach(this@apply::send)
                        .flowOn(Dispatchers.IO)
                        .launchIn(viewModelScope)
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
val TopAppBarScrollBehavior.isOverlapping: Boolean
    get() = state.overlappedFraction > 0.01f

@Suppress("NOTHING_TO_INLINE")
@OptIn(ExperimentalMaterial3Api::class)
inline fun List<TopAppBarScrollBehavior>.isOverlapping(pagerState: PagerState): Boolean {
    return this.getOrNull(pagerState.currentPage)?.isOverlapping ?: false
}

/**
 * Replacement of [PagerState.isScrollInProgress]
 * */
val PagerState.isScrolling: Boolean
    get() = currentPageOffsetFraction != 0f

val PagerState.isFirstPage
    get() = currentPage == 0

val PagerState.isLastPage
    get() = currentPage == pageCount - 1

fun <T> unsafeLazy(initializer: () -> T): Lazy<T> = lazy(LazyThreadSafetyMode.NONE, initializer)