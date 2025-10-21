package com.huanchengfly.tieba.post.ui.page.forum.searchpost

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.repository.SearchRepository
import com.huanchengfly.tieba.post.ui.models.search.SearchThreadInfo
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.search.SearchUiEvent
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.set
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UiState for the ForumSearchPostPage
 * */
data class ForumSearchPostUiState(
    val isRefreshing: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: Throwable? = null,
    val currentPage: Int = 1,
    val hasMore: Boolean = false,
    val keyword: String = "",
    val data: List<SearchThreadInfo> = emptyList(),
    val sortType: Int = ForumSearchPostSortType.NEWEST,
    val filterType: Int = ForumSearchPostFilterType.ALL,
) : UiState {

    val isKeywordNotEmpty: Boolean = keyword.isNotEmpty() && keyword.isNotBlank()
}

@HiltViewModel
class ForumSearchPostViewModel @Inject constructor(
    private val searchRepo: SearchRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val params = savedStateHandle.toRoute<Destination.ForumSearchPost>()

    val forumName: String = params.forumName
    val forumId: Long = params.forumId

    /**
     * One-off [UiEvent], but no guarantee to be received.
     * */
    private val _uiEvent: MutableSharedFlow<SearchUiEvent?> = MutableSharedFlow()
    val uiEvent: Flow<SearchUiEvent?>
        get() = _uiEvent

    private var _uiState = MutableStateFlow(ForumSearchPostUiState())
    val uiState: StateFlow<ForumSearchPostUiState> = _uiState.asStateFlow()

    private val handler = CoroutineExceptionHandler { _, e ->
        Log.e(TAG, "onError: ", e)
        _uiState.update { it.copy(isRefreshing = false, isLoadingMore = false, error = e) }
    }

    val searchHistories: StateFlow<List<String>> = searchRepo.getPostHistoryFlow(forumId)
        .catch { e ->
            handler.handleException(currentCoroutineContext(), e)
        }
        .stateIn(viewModelScope, started = SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onClearHistory() {
        viewModelScope.launch(handler) {
            runCatching {
                require(searchHistories.value.isNotEmpty()) { "Empty History" }
                searchRepo.clearPostHistory(forumId)
            }
            .onFailure { _uiEvent.emit(SearchUiEvent.ClearHistoryFailed(it)) }
            .onSuccess { _uiEvent.emit(SearchUiEvent.ClearHistorySucceed) }
        }
    }

    /**
     * Called when delete search history is clicked
     *
     * @param history search history
     * */
    fun onDeleteHistory(history: String) {
        viewModelScope.launch(handler) {
            runCatching {
                searchRepo.deletePostHistory(forumId, history)
            }
            .onFailure { e -> _uiEvent.emit(SearchUiEvent.DeleteHistoryFailed(e)) }
        }
    }

    private fun searchPostInternal(keyword: String, sort: Int? = null, filter: Int? = null) {
        if (keyword.isEmpty() || keyword.isBlank()) {
            _uiState.set {
                ForumSearchPostUiState(isRefreshing = false, keyword = keyword, sortType = sortType, filterType = filterType)
            }
            return // on clear
        }

        val uiStateSnapshot = _uiState.updateAndGet {
            ForumSearchPostUiState(keyword = keyword, sortType = sort ?: it.sortType, filterType = filter ?: it.filterType)
        }

        viewModelScope.launch(handler) {
            val sortType = uiStateSnapshot.sortType
            val filterType = uiStateSnapshot.filterType
            val (hasMore, posts) = searchRepo.searchPost(keyword, forumName, forumId, sortType, filterType, page = 1)
            _uiState.update { it.copy(isRefreshing = false, hasMore = hasMore, data = posts) }
        }
    }

    fun onLoadMore() {
        if (_uiState.value.isLoadingMore) return

        val uiStateSnapshot = _uiState.updateAndGet { it.copy(isLoadingMore = true) }
        viewModelScope.launch(handler) {
            val page = uiStateSnapshot.currentPage + 1
            val (hasMore, threads) = searchRepo.searchPost(
                keyword = uiStateSnapshot.keyword,
                forumName = forumName,
                forumId = forumId,
                sortType = uiStateSnapshot.sortType,
                filterType = uiStateSnapshot.filterType,
                page = page
            )
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

    fun onRefresh() {
        val uiStateSnapshot = _uiState.value
        if (!uiStateSnapshot.isRefreshing) searchPostInternal(uiStateSnapshot.keyword) else return
    }

    fun onSubmitKeyword(keyword: String) {
        if (keyword != _uiState.value.keyword) {
            viewModelScope.launch(handler) {
                searchRepo.addPostHistory(forumId, keyword)
            }
            searchPostInternal(keyword)
        }
    }

    fun onFilterTypeChanged(filterType: Int) {
        val uiStateSnapshot = uiState.value
        if (uiStateSnapshot.filterType != filterType) {
            searchPostInternal(keyword = uiStateSnapshot.keyword, filter = filterType)
        }
    }

    fun onSortTypeChanged(sortType: Int) {
        val uiStateSnapshot = uiState.value
        if (uiStateSnapshot.sortType != sortType) {
            searchPostInternal(keyword = uiStateSnapshot.keyword, sort = sortType)
        }
    }
}

private const val TAG = "ForumSearchPostViewMode"

object ForumSearchPostSortType {
    const val NEWEST = 1
    const val RELATIVE = 2
}

object ForumSearchPostFilterType {
    const val ONLY_THREAD = 1
    const val ALL = 2
}