package com.huanchengfly.tieba.post.ui.page.history

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.huanchengfly.tieba.post.di.DefaultDispatcher
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.repository.HistoryRepository
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    @DefaultDispatcher val dispatcher: CoroutineDispatcher,
    private val historyRepo: HistoryRepository
) : ViewModel() {

    val forumHistory: Flow<PagingData<HistoryUiModel>> = historyRepo.getForumHistory()
        .mapUiModel()
        .flowOn(dispatcher)
        .cachedIn(viewModelScope)

    val threadHistory: Flow<PagingData<HistoryUiModel>> = historyRepo.getThreadHistory()
        .mapUiModel()
        .flowOn(dispatcher)
        .cachedIn(viewModelScope)

    val userHistory: Flow<PagingData<HistoryUiModel>> = historyRepo.getUserHistory()
        .mapUiModel()
        .flowOn(dispatcher)
        .cachedIn(viewModelScope)

    private val _updating: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val updating: StateFlow<Boolean> = _updating.asStateFlow()

    private inline fun update(crossinline block: suspend CoroutineScope.() -> Unit) {
        if (!_updating.value) {
            viewModelScope.launch(dispatcher) {
                _updating.update { true }
                block()
                _updating.update { false }
            }
        }
    }

    fun onDelete(history: History) = update {
        historyRepo.deleteHistory(history)
    }

    fun onDelete(historyList: List<History>) = update {
        historyRepo.deleteHistory(historyList)
    }

    /**
     * Called when delete all button clicked
     * */
    fun onDeleteAll() = update {
        historyRepo.deleteAll()
    }

    /**
     * Map history entity to UI Model with formatted time separators.
     * */
    private fun <T: History> Flow<PagingData<T>>.mapUiModel(): Flow<PagingData<HistoryUiModel>> = map { pagingData ->
        pagingData
            .map { HistoryUiModel.Item(history = it) }
            .insertSeparators { before, after ->
                val afterTime = after?.history?.timestamp
                val beforeTime = before?.history?.timestamp ?: 0
                if (afterTime != null && !DateTimeUtils.isSameDay(afterTime, beforeTime)) {
                    HistoryUiModel.DateHeader(
                        DateTimeUtils.getRelativeDayString(context, timestamp = afterTime)
                    )
                } else {
                    null
                }
            }
    }
}

sealed interface HistoryUiModel {

    data class Item(val history: History): HistoryUiModel

    data class DateHeader(val date: String): HistoryUiModel
}