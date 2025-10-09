package com.huanchengfly.tieba.post.ui.page.search.thread

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.repository.SearchRepository
import com.huanchengfly.tieba.post.ui.models.search.SearchThreadInfo
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.set
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class SearchThreadUiState(
    val isRefreshing: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: Throwable? = null,
    val currentPage: Int = 1,
    val hasMore: Boolean = false,
    val keyword: String = "",
    val data: List<SearchThreadInfo> = emptyList(),
    val sortType: Int = SearchThreadSortType.SORT_TYPE_NEWEST,
) : UiState {

    val isEmpty: Boolean
        get() = data.isEmpty()
}

private const val TAG = "SearchThreadViewModel"

@Stable
@HiltViewModel
class SearchThreadViewModel @Inject constructor(
    private val searchRepo: SearchRepository
) : ViewModel() {

    private var _uiState = MutableStateFlow(SearchThreadUiState())
    val uiState: StateFlow<SearchThreadUiState> = _uiState.asStateFlow()

    private val handler = CoroutineExceptionHandler { _, e ->
        Log.e(TAG, "onError: ", e)
        _uiState.update { it.copy(isRefreshing = false, isLoadingMore = false, error = e) }
    }

    private fun searchThreadInternal(keyword: String) {
        if (keyword.isEmpty() || keyword.isBlank()) {
            _uiState.set { SearchThreadUiState(isRefreshing = false, keyword = keyword, sortType = sortType) }
            return // on clear
        }

        val uiStateSnapshot = _uiState.updateAndGet { SearchThreadUiState(keyword = keyword, sortType = it.sortType) }
        viewModelScope.launch(handler) {
            val sortType = uiStateSnapshot.sortType
            val (hasMore, threads) = searchRepo.searchThread(keyword, page = 1, sortType)
            _uiState.update {
                it.copy(isRefreshing = false, hasMore = hasMore, data = threads)
            }
        }
    }

    fun onLoadMore() {
        if (_uiState.value.isLoadingMore) return

        val uiStateSnapshot = _uiState.updateAndGet { it.copy(isLoadingMore = true) }
        viewModelScope.launch(handler) {
            val page = uiStateSnapshot.currentPage + 1
            val sortType = uiStateSnapshot.sortType
            val (hasMore, threads) = searchRepo.searchThread(uiStateSnapshot.keyword, page, sortType)
            val newData = uiStateSnapshot.data + threads
            _uiState.update {
                if (it === uiStateSnapshot) {
                    it.copy(isLoadingMore = false, currentPage = page, hasMore = hasMore, data = newData)
                } else {
                    it // state changed during loading, skip update
                }
            }
        }
    }

    fun onKeywordChanged(keyword: String) {
        if (_uiState.value.keyword != keyword) searchThreadInternal(keyword) else return
    }

    fun onRefresh() {
        val uiStateSnapshot = _uiState.value
        if (uiStateSnapshot.isRefreshing) return else searchThreadInternal(uiStateSnapshot.keyword)
    }

    fun onSortTypeChanged(sortType: Int) {
        if (uiState.value.sortType != sortType) {
            val oldState = _uiState.updateAndGet { it.copy(sortType = sortType) }
            searchThreadInternal(oldState.keyword)
        }
    }
}

object SearchThreadSortType {
    const val SORT_TYPE_NEWEST = 5
    const val SORT_TYPE_OLDEST = 0
    const val SORT_TYPE_RELATIVE = 2
}