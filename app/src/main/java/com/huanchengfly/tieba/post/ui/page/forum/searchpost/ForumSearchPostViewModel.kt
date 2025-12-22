package com.huanchengfly.tieba.post.ui.page.forum.searchpost

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.huanchengfly.tieba.post.arch.BaseStateViewModel
import com.huanchengfly.tieba.post.arch.TbLiteExceptionHandler
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.stateInViewModel
import com.huanchengfly.tieba.post.repository.SearchRepository
import com.huanchengfly.tieba.post.ui.models.search.SearchThreadInfo
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.search.SearchUiEvent
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.set
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
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
) : BaseStateViewModel<ForumSearchPostUiState>() {

    private val params = savedStateHandle.toRoute<Destination.ForumSearchPost>()

    val forumName: String = params.forumName
    val forumId: Long = params.forumId

    override val errorHandler = TbLiteExceptionHandler(TAG) { _, e, _ ->
        _uiState.update { it.copy(isRefreshing = false, isLoadingMore = false, error = e) }
    }

    val searchHistories: StateFlow<List<String>> = searchRepo.getPostHistoryFlow(forumId)
        .catch { e ->
            errorHandler.handleException(currentCoroutineContext(), e)
        }
        .stateInViewModel(initialValue = emptyList())

    override fun createInitialState(): ForumSearchPostUiState = ForumSearchPostUiState()

    fun onClearHistory(): Unit = launchInVM {
        runCatching {
            require(searchHistories.value.isNotEmpty()) { "Empty History" }
            searchRepo.clearPostHistory(forumId)
        }
        .onFailure { e ->
            emitUiEvent(SearchUiEvent.ClearHistoryFailed(e))
        }
        .onSuccess { emitUiEvent(SearchUiEvent.ClearHistorySucceed) }
    }

    /**
     * Called when delete search history is clicked
     *
     * @param history search history
     * */
    fun onDeleteHistory(history: String): Unit = launchInVM {
        runCatching {
            searchRepo.deletePostHistory(forumId, history)
        }
        .onFailure { e -> emitUiEvent(SearchUiEvent.DeleteHistoryFailed(e)) }
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

        launchInVM {
            val sortType = uiStateSnapshot.sortType
            val filterType = uiStateSnapshot.filterType
            val (hasMore, posts) = searchRepo.searchPost(keyword, forumName, forumId, sortType, filterType, page = 1)
            _uiState.update { it.copy(isRefreshing = false, hasMore = hasMore, data = posts) }
        }
    }

    fun onLoadMore() {
        if (currentState.isLoadingMore) return

        val uiStateSnapshot = _uiState.updateAndGet { it.copy(isLoadingMore = true) }
        launchInVM {
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
        val uiStateSnapshot = currentState
        if (!uiStateSnapshot.isRefreshing) searchPostInternal(uiStateSnapshot.keyword) else return
    }

    fun onSubmitKeyword(keyword: String) {
        if (keyword != currentState.keyword) {
            launchInVM {
                searchRepo.addPostHistory(forumId, keyword)
            }
            searchPostInternal(keyword)
        }
    }

    fun onFilterTypeChanged(filterType: Int) {
        val uiStateSnapshot = currentState
        if (uiStateSnapshot.filterType != filterType) {
            searchPostInternal(keyword = uiStateSnapshot.keyword, filter = filterType)
        }
    }

    fun onSortTypeChanged(sortType: Int) {
        val uiStateSnapshot = currentState
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