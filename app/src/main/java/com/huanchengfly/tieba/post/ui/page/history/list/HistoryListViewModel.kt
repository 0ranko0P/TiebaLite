package com.huanchengfly.tieba.post.ui.page.history.list

import androidx.compose.runtime.Immutable
import androidx.compose.ui.util.fastFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.repository.HistoryRepository
import com.huanchengfly.tieba.post.repository.HistoryType
import com.huanchengfly.tieba.post.ui.page.history.list.HistoryListViewModel.Companion.HistoryVmFactory
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.set
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat

@HiltViewModel(assistedFactory = HistoryVmFactory::class)
class HistoryListViewModel @AssistedInject constructor(
    @Assisted @HistoryType val type: Int,
    private val historyRepo: HistoryRepository,
) : ViewModel() {

    private val dateFormatter = DateFormat.getDateInstance()

    /**
     * One-off [UiEvent], but no guarantee to be received.
     * */
    private val _uiEvent: MutableSharedFlow<UiEvent?> = MutableSharedFlow()
    val uiEvent: Flow<UiEvent?>
        get() = _uiEvent

    private val _uiState = MutableStateFlow(HistoryListUiState(isRefreshing = true))
    val uiState: StateFlow<HistoryListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { refreshInternal() }
    }

    private suspend fun refreshInternal() {
        _uiState.update { HistoryListUiState(isRefreshing = true) }
        historyRepo.getHistory(type)
            .onSuccess { histories ->
                val newState = HistoryListUiState().updateWith(histories, 0)
                _uiState.update { newState }
            }
            .onFailure {
                _uiEvent.emit(HistoryListUiEvent.Failure(it.getErrorMessage()))
                historyRepo.deleteAll()
                _uiState.update { HistoryListUiState() }
            }
    }

    fun onRefresh() {
        if (_uiState.value.isRefreshing) return

        viewModelScope.launch {
            refreshInternal()
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.hasMore && !state.isLoadingMore) return else _uiState.set { copy(isLoadingMore = true) }

        viewModelScope.launch {
            val page = state.currentPage + 1
            historyRepo.getHistory(type, page)
                .onSuccess { histories ->
                    val newState = state.updateWith(histories, page)
                    _uiState.update { newState }
                }
                .onFailure { e ->
                    _uiEvent.emit(HistoryListUiEvent.Failure(e.getErrorMessage()))
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
        }
    }

    fun delete(history: History) {
        val oldState = _uiState.value
        if (oldState.isLoadingMore) return

        viewModelScope.launch {
            if (historyRepo.delete(history)) {
                val newState = withContext(Dispatchers.Default) {
                    oldState.copy(
                        todayHistoryData = oldState.todayHistoryData.fastFilter { it.id != history.id },
                        beforeHistoryData = oldState.beforeHistoryData.fastFilter { it.id != history.id }
                    )
                }
                _uiState.update { newState }
            } else {
                _uiEvent.emit(HistoryListUiEvent.Failure("Unknown Database error"))
            }
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            if (historyRepo.deleteAll()) {
                _uiState.update { HistoryListUiState(hasMore = false) }
            } else {
                _uiEvent.emit(HistoryListUiEvent.Failure("Unknown Database error"))
            }
        }
    }

    private suspend fun HistoryListUiState.updateWith(
        histories: List<History>,
        page: Int = currentPage
    ): HistoryListUiState = withContext(Dispatchers.Default) {
        val today = histories.filter { DateTimeUtils.isToday(dateFormatter, it.timestamp) }
        val before = histories.filterNot { DateTimeUtils.isToday(dateFormatter, it.timestamp) }
        copy(
            isRefreshing = false,
            isLoadingMore = false,
            todayHistoryData = todayHistoryData + today,
            beforeHistoryData = beforeHistoryData + before,
            currentPage = page,
            hasMore = histories.size == HistoryRepository.PAGE_SIZE
        )
    }

    companion object {

        @AssistedFactory
        interface HistoryVmFactory {
            fun create(@HistoryType type: Int): HistoryListViewModel
        }
    }
}

@Immutable
data class HistoryListUiState(
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val currentPage: Int = 0,
    val todayHistoryData: List<History> = emptyList(),
    val beforeHistoryData: List<History> = emptyList(),
) : UiState

sealed interface HistoryListUiEvent : UiEvent {

    data class Failure(val errorMsg: String) : HistoryListUiEvent

    object DeleteAll : HistoryListUiEvent
}