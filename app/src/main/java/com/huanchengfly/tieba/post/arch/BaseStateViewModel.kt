package com.huanchengfly.tieba.post.arch

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

@Suppress("PropertyName", "unused")
abstract class BaseStateViewModel<State>: ViewModel() {

    protected open val errorHandler: CoroutineExceptionHandler = CoroutineExceptionHandler { _, e ->
        Log.e(this::class.simpleName, "onError: ", e)
        sendUiEvent(event = CommonUiEvent.ToastError(e))
    }

    protected val _uiState: MutableStateFlow<State> by lazy {
        MutableStateFlow(createInitialState())
    }
    open val uiState: StateFlow<State> get() = _uiState

    protected val _uiEvent: MutableSharedFlow<UiEvent> by lazy { MutableSharedFlow() }
    val uiEvent: SharedFlow<UiEvent> get() = _uiEvent

    val currentState: State
        get() = _uiState.value

    protected abstract fun createInitialState(): State

    protected fun launchJobInVM(
        context: CoroutineContext = errorHandler,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return viewModelScope.launch(context, block = block)
    }

    protected fun launchInVM(
        context: CoroutineContext = errorHandler,
        block: suspend CoroutineScope.() -> Unit
    ) {
        viewModelScope.launch(context, block = block)
    }

    protected fun sendUiEvent(event: UiEvent) {
        if (viewModelScope.isActive) {
            viewModelScope.launch { _uiEvent.emit(event) }
        } else {
            Log.e(this::class.simpleName, "onSendUiEvent: VM destroyed! event=$event")
        }
    }

    protected suspend fun emitUiEvent(event: UiEvent) = _uiEvent.emit(event)
}