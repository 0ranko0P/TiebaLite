package com.huanchengfly.tieba.post.ui.page.search

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.repository.SearchResult
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
open class UiState<T>(
    val keyword: String = "",
    val exactMatch: T? = null,
    val fuzzyMatch: List<T> = emptyList(),
    val isRefreshing: Boolean = true,
    val error: Throwable? = null,
) : UiState {

    val isEmpty: Boolean
        get() = exactMatch == null && fuzzyMatch.isEmpty()

    fun copy(
        keyword: String = this.keyword,
        exactMatch: T? = this.exactMatch,
        fuzzyMatch: List<T> = this.fuzzyMatch,
        isRefreshing: Boolean = this.isRefreshing,
        error: Throwable? = this.error,
    ) = UiState(keyword, exactMatch, fuzzyMatch, isRefreshing, error)
}

abstract class SearchBaseViewModel<T>: ViewModel() {

    protected open var _uiState = MutableStateFlow(UiState<T>())
    val uiState = _uiState.asStateFlow()

    protected abstract suspend fun search(keyword: String): SearchResult<T>

    private fun searchForumInternal(keyword: String) {
        if (keyword.isNotEmpty()) {
            _uiState.set { UiState(keyword, isRefreshing = true) }
        } else {
            _uiState.set { UiState(keyword, isRefreshing = false) }
            return  // on clear
        }

        viewModelScope.launch {
            runCatching {
                search(keyword)
            }
            .onFailure { e ->
                _uiState.update { it.copy(isRefreshing = false, error = e) }
            }
            .onSuccess { (exactMatch, fuzzyMatch) ->
                _uiState.update {
                    it.copy(exactMatch = exactMatch, fuzzyMatch = fuzzyMatch, isRefreshing = false)
                }
            }
        }
    }

    fun onKeywordChanged(keyword: String) {
        if (_uiState.value.keyword != keyword) searchForumInternal(keyword) else return
    }

    fun onRefresh() {
        val uiStateSnapshot = _uiState.value
        if (uiStateSnapshot.isRefreshing) return else searchForumInternal(uiStateSnapshot.keyword)
    }
}