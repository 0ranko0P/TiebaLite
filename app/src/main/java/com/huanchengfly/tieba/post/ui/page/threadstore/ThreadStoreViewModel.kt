package com.huanchengfly.tieba.post.ui.page.threadstore

import androidx.compose.runtime.Immutable
import androidx.compose.ui.util.fastFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.repository.ThreadStoreRepository
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.models.ThreadStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Immutable
data class ThreadStoreUiState(
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val currentPage: Int = 1,
    val data: List<ThreadStore> = emptyList(),
    val error: Throwable? = null
) : UiState {

    val isEmpty: Boolean
        get() = data.isEmpty()
}

private val List<ThreadStore>.hasMore: Boolean
    get() = this.size == ThreadStoreRepository.LOAD_LIMIT // Result reached limit

@HiltViewModel
class ThreadStoreViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    private val threadStoreRepo: ThreadStoreRepository
) : ViewModel() {

    val habitSettingsFlow = settingsRepository.habitSettings.flow

    private val _uiState: MutableStateFlow<ThreadStoreUiState> = MutableStateFlow(ThreadStoreUiState())
    val uiState: StateFlow<ThreadStoreUiState> = _uiState.asStateFlow()

    /**
     * One-off [UiEvent], but no guarantee to be received.
     * */
    private val _uiEvent: MutableSharedFlow<UiEvent?> = MutableSharedFlow()
    val uiEvent: Flow<UiEvent?>
        get() = _uiEvent

    init {
        refreshInternal()
    }

    private fun refreshInternal() = viewModelScope.launch {
        _uiState.update { ThreadStoreUiState(isRefreshing = true) }
        threadStoreRepo.load()
            .onFailure { e -> _uiState.update { it.copy(isLoadingMore = false, error = e) } }
            .onSuccess { data ->
                _uiState.update { ThreadStoreUiState(data = data, hasMore = data.hasMore) }
            }
    }

    fun onRefresh() {
        if (uiState.value.isRefreshing) return else refreshInternal()
    }

    fun onLoadMore() {
        if (uiState.value.isLoadingMore || !uiState.value.hasMore) {
            return
        } else {
            _uiState.update { it.copy(isLoadingMore = true) }
        }

        viewModelScope.launch {
            val oldState = _uiState.first()
            val nextPage = oldState.currentPage + 1
            threadStoreRepo.load(page = nextPage)
                .onFailure { e -> _uiState.update { it.copy(isLoadingMore = false, error = e) } }
                .onSuccess { data ->
                    if (data.isEmpty()) {
                        _uiState.update { it.copy(isLoadingMore = false, hasMore = false) }
                    } else {
                        val newData = withContext(Dispatchers.Default) { oldState.data + data }
                        _uiState.update {
                            ThreadStoreUiState(currentPage = nextPage, data = newData, hasMore = data.hasMore)
                        }
                    }
                }
        }
    }

    fun onDelete(thread: ThreadStore) {
        viewModelScope.launch {
            val oldThreads = _uiState.first().data
            val newThreads = withContext(Dispatchers.Default) {
                oldThreads.fastFilter { it.id != thread.id }
            }
            _uiState.update { it.copy(data = newThreads) }

            threadStoreRepo.remove(thread)
                .onFailure { e ->
                    _uiEvent.tryEmit(ThreadStoreUiEvent.Delete.Failure(e))
                    // Revert changes now
                    _uiState.update { it.copy(data = oldThreads) }
                }
                .onSuccess { _uiEvent.tryEmit(ThreadStoreUiEvent.Delete.Success) }
        }
    }
}

sealed interface ThreadStoreUiEvent : UiEvent {
    sealed interface Delete : ThreadStoreUiEvent {

        object Success : Delete

        data class Failure(val error: Throwable) : Delete
    }
}