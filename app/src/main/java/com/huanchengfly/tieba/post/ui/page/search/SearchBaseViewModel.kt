package com.huanchengfly.tieba.post.ui.page.search

import androidx.compose.runtime.Immutable
import com.huanchengfly.tieba.post.arch.BaseStateViewModel
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.TbLiteExceptionHandler
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.repository.SearchResult
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.set
import kotlinx.coroutines.flow.update

@Immutable
open class BaseSearchUiState<T>(
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
    ) = BaseSearchUiState(keyword, exactMatch, fuzzyMatch, isRefreshing, error)
}

abstract class SearchBaseViewModel<T>: BaseStateViewModel<BaseSearchUiState<T>>() {

    override val errorHandler = TbLiteExceptionHandler(this::class.java.simpleName) { _, e, suppressed ->
        if (suppressed && !currentState.isEmpty) {
            _uiState.update { it.copy(isRefreshing = false, error = null) }
            sendUiEvent(CommonUiEvent.ToastError(e))
        } else {
            _uiState.update { it.copy(isRefreshing = false, error = e) }
        }
    }

    override fun createInitialState(): BaseSearchUiState<T> = BaseSearchUiState()

    protected abstract suspend fun search(keyword: String): SearchResult<T>

    private fun searchForumInternal(keyword: String) {
        if (keyword.isNotEmpty()) {
            _uiState.set { BaseSearchUiState(keyword, isRefreshing = true) }
        } else {
            _uiState.set { BaseSearchUiState(keyword, isRefreshing = false) }
            return  // on clear
        }
        launchInVM {
            val (exactMatch, fuzzyMatch) = search(keyword)
            _uiState.update {
                it.copy(exactMatch = exactMatch, fuzzyMatch = fuzzyMatch, isRefreshing = false)
            }
        }
    }

    fun onKeywordChanged(keyword: String) {
        if (currentState.keyword != keyword) searchForumInternal(keyword) else return
    }

    fun onRefresh() {
        val uiStateSnapshot = currentState
        if (uiStateSnapshot.isRefreshing) return else searchForumInternal(uiStateSnapshot.keyword)
    }
}