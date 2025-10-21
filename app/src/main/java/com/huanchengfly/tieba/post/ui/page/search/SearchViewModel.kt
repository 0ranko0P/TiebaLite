package com.huanchengfly.tieba.post.ui.page.search

import android.util.Log
import androidx.collection.LruCache
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.ControlledRunner
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.repository.SearchRepository
import com.huanchengfly.tieba.post.ui.models.search.SearchSuggestion
import com.huanchengfly.tieba.post.ui.page.search.thread.SearchThreadSortType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class SearchUiState(
    val submittedKeyword: String = "",
    val sortType: Int = SearchThreadSortType.SORT_TYPE_NEWEST,
    val suggestion: SearchSuggestion? = null,
) : UiState {

    val isKeywordNotEmpty: Boolean = submittedKeyword.isNotEmpty()
}

private const val TAG = "SearchViewModel"

@Stable
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepo: SearchRepository
): ViewModel() {

    /**
     * In-Memory cache of recent search suggestion
     * */
    private val cache: LruCache<String, SearchSuggestion> = LruCache(10)

    private var searchSuggestionRunner = ControlledRunner<Unit>()

    private val emptySuggestion = SearchSuggestion(null, emptyList())

    /**
     * One-off [SearchUiEvent], but no guarantee to be received.
     * */
    private val _uiEvent: MutableSharedFlow<SearchUiEvent?> = MutableSharedFlow()
    val uiEvent: Flow<SearchUiEvent?>
        get() = _uiEvent

    private var _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val handler = CoroutineExceptionHandler { _, e ->
        Log.e(TAG, "onError: ", e)
        if (viewModelScope.isActive) {
            viewModelScope.launch { _uiEvent.emit(SearchUiEvent.Error(e)) }
        }
    }

    val searchHistories: StateFlow<List<String>> = searchRepo.getHistoryFlow()
        .catch { e ->
            handler.handleException(currentCoroutineContext(), e)
        }
        .stateIn(viewModelScope, started = SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onClearHistory() {
        viewModelScope.launch(handler) {
            runCatching {
                require(searchHistories.value.isNotEmpty()) { "Empty History" }
                searchRepo.clearHistory()
            }
            .onFailure { e ->
                _uiEvent.emit(SearchUiEvent.ClearHistoryFailed(e))
            }
            .onSuccess { _uiEvent.emit(SearchUiEvent.ClearHistorySucceed) }
        }
    }

    /**
     * Called on delete search history is clicked.
     * */
    fun onDeleteHistory(history: String) {
        viewModelScope.launch(handler) {
            runCatching {
                searchRepo.deleteHistory(history)
            }
            .onFailure { e -> _uiEvent.emit(SearchUiEvent.DeleteHistoryFailed(e)) }
        }
    }

    /**
     * Called when the input text in the search box has been changed.
     * */
    fun onKeywordInputChanged(keyword: String) {
        viewModelScope.launch(handler) {
            searchSuggestionRunner.cancelPreviousThenRun {
                val keywordSnapshot = _uiState.value.submittedKeyword
                when {
                    // on clear
                    keyword.isEmpty() || keyword.isBlank() || keyword == keywordSnapshot -> {
                        _uiState.update { it.copy(suggestion = null) }
                    }

                    // on cache hit
                    cache[keyword] != null -> _uiState.update { it.copy(suggestion = cache[keyword]!!) }

                    // fetch search suggestion from network now
                    else -> {
                        delay(200) // user might type real fast, wait 200ms here

                        runCatching {
                            searchRepo.searchSuggestions(keyword)
                        }
                        .onFailure { e ->
                            if (e !is CancellationException) {
                                Log.w(TAG, "onKeywordInputChanged: ${e.getErrorMessage()}")
                            }
                        }
                        .onSuccess { suggestion ->
                            if (isActive) {
                                cacheSuggestion(keyword, suggestion)
                                _uiState.update {
                                    // check new keyword submitted
                                    if (it.submittedKeyword == keywordSnapshot) it.copy(suggestion = suggestion) else it
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun onSubmitKeyword(keyword: String) {
        if (keyword == _uiState.value.submittedKeyword) return

        searchSuggestionRunner.cancelCurrent()
        viewModelScope.launch(handler) {
            val newState = _uiState.updateAndGet { it.copy(submittedKeyword = keyword, suggestion = null) }
            if (newState.isKeywordNotEmpty) {
                searchRepo.addHistory(keyword)
            }
        }
    }

    fun onSortTypeChanged(sortType: Int) {
        viewModelScope.launch(handler) {
            _uiState.update { it.copy(sortType = sortType) }
        }
    }

    private fun cacheSuggestion(keyword: String, suggestion: SearchSuggestion) {
        val isEmpty = suggestion.forum == null && suggestion.suggestions.isEmpty()
        // replace with empty obj if result is empty
        cache.put(keyword, if (isEmpty) emptySuggestion else suggestion)
    }
}
