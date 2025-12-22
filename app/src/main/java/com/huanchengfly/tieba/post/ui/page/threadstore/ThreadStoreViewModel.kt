package com.huanchengfly.tieba.post.ui.page.threadstore

import androidx.compose.runtime.Immutable
import androidx.compose.ui.util.fastFilter
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.BaseStateViewModel
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.TbLiteExceptionHandler
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.repository.ThreadStoreRepository
import com.huanchengfly.tieba.post.ui.models.ThreadStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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

@HiltViewModel
class ThreadStoreViewModel @Inject constructor(
    private val threadStoreRepo: ThreadStoreRepository
) : BaseStateViewModel<ThreadStoreUiState>() {

    override val errorHandler = TbLiteExceptionHandler(TAG) { context, e, suppressed ->
        if (suppressed && !currentState.isEmpty) {
            _uiState.update { it.copy(isRefreshing = false, isLoadingMore = false, error = null) }
            sendUiEvent(CommonUiEvent.ToastError(e))
        } else {
            _uiState.update { it.copy(isRefreshing = false, isLoadingMore = false, error = e) }
        }
    }

    init {
        refreshInternal()
    }

    override fun createInitialState(): ThreadStoreUiState = ThreadStoreUiState()

    private fun refreshInternal(): Unit = launchInVM {
        _uiState.update { ThreadStoreUiState(isRefreshing = true) }
        val data = threadStoreRepo.load()
        _uiState.update { ThreadStoreUiState(data = data, hasMore = data.hasMore) }
    }

    fun onRefresh() {
        if (currentState.isRefreshing) return else refreshInternal()
    }

    fun onLoadMore() {
        val oldState = currentState
        if (oldState.isLoadingMore || !oldState.hasMore) {
            return
        } else {
            _uiState.update { oldState.copy(isLoadingMore = true) }
        }
        launchInVM {
            val nextPage = oldState.currentPage + 1
            val data = threadStoreRepo.load(page = nextPage)
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

    fun onDelete(thread: ThreadStore) {
        viewModelScope.launch {
            val oldThreads = currentState.data
            val newThreads = withContext(Dispatchers.Default) {
                oldThreads.fastFilter { it.id != thread.id }
            }
            _uiState.update { it.copy(data = newThreads) }

            threadStoreRepo.remove(thread)
                .onFailure { e ->
                    emitUiEvent(ThreadStoreUiEvent.Delete.Failure(e.getErrorMessage()))
                    // Revert changes now
                    _uiState.update { it.copy(data = oldThreads) }
                }
                .onSuccess { emitUiEvent(ThreadStoreUiEvent.Delete.Success) }
        }
    }

    companion object {

        private const val TAG = "ThreadStoreViewModel"

        private val List<ThreadStore>.hasMore: Boolean
            get() = this.size == ThreadStoreRepository.LOAD_LIMIT // Result reached limit
    }
}
