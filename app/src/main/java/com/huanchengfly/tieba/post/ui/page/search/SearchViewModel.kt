package com.huanchengfly.tieba.post.ui.page.search

import android.util.Log
import androidx.collection.LruCache
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.BaseStateViewModel
import com.huanchengfly.tieba.post.arch.ControlledRunner
import com.huanchengfly.tieba.post.arch.TbLiteExceptionHandler
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.stateInViewModel
import com.huanchengfly.tieba.post.repository.SearchRepository
import com.huanchengfly.tieba.post.ui.models.search.SearchSuggestion
import com.huanchengfly.tieba.post.ui.page.search.thread.SearchThreadSortType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.isActive
import javax.inject.Inject

@Immutable
data class SearchUiState(
    val submittedKeyword: String = "",
    @SearchThreadSortType val sortType: Int = SearchThreadSortType.NEWEST,
    val suggestion: SearchSuggestion? = null,
) : UiState {

    val isKeywordNotEmpty: Boolean = submittedKeyword.isNotEmpty()
}

private const val TAG = "SearchViewModel"

@Stable
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepo: SearchRepository
): BaseStateViewModel<SearchUiState>() {

    /**
     * In-Memory cache of recent search suggestion
     * */
    private val cache: LruCache<String, SearchSuggestion> = LruCache(10)

    private var searchSuggestionRunner = ControlledRunner<Unit>()

    private val emptySuggestion = SearchSuggestion(null, emptyList())

    override val errorHandler = TbLiteExceptionHandler(TAG) { _, e, _ ->
        if (viewModelScope.isActive) {
            sendUiEvent(SearchUiEvent.Error(e))
        }
    }

    val searchHistories: StateFlow<List<String>> = searchRepo.getHistoryFlow()
        .catch { e -> errorHandler.handleException(currentCoroutineContext(), e) }
        .stateInViewModel(initialValue = emptyList())

    override fun createInitialState(): SearchUiState = SearchUiState()

    fun onClearHistory(): Unit = launchInVM {
        runCatching {
            require(searchHistories.value.isNotEmpty()) { "Empty History" }
            searchRepo.clearHistory()
        }
        .onFailure { e ->
            emitUiEvent(SearchUiEvent.ClearHistoryFailed(e))
        }
        .onSuccess { emitUiEvent(SearchUiEvent.ClearHistorySucceed) }
    }

    /**
     * Called on delete search history is clicked.
     * */
    fun onDeleteHistory(history: String): Unit = launchInVM {
        runCatching {
            searchRepo.deleteHistory(history)
        }
        .onFailure { e -> emitUiEvent(SearchUiEvent.DeleteHistoryFailed(e)) }
    }

    /**
     * Called when the input text in the search box has been changed.
     * */
    fun onKeywordInputChanged(keyword: String) {
        launchInVM {
            searchSuggestionRunner.cancelPreviousThenRun {
                val keywordSnapshot = currentState.submittedKeyword
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
        if (keyword == currentState.submittedKeyword) return

        searchSuggestionRunner.cancelCurrent()
        launchInVM {
            val newState = _uiState.updateAndGet { it.copy(submittedKeyword = keyword, suggestion = null) }
            if (newState.isKeywordNotEmpty) {
                searchRepo.addHistory(keyword)
            }
        }
    }

    fun onSortTypeChanged(@SearchThreadSortType sortType: Int): Unit = launchInVM {
        _uiState.update { it.copy(sortType = sortType) }
    }

    private fun cacheSuggestion(keyword: String, suggestion: SearchSuggestion) {
        val isEmpty = suggestion.forum == null && suggestion.suggestions.isEmpty()
        // replace with empty obj if result is empty
        cache.put(keyword, if (isEmpty) emptySuggestion else suggestion)
    }

    override fun onCleared() {
        super.onCleared()
        searchSuggestionRunner.cancelCurrent()
        cache.evictAll()
    }
}
