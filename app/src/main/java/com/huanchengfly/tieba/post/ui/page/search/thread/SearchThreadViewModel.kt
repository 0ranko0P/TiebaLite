package com.huanchengfly.tieba.post.ui.page.search.thread

import androidx.annotation.IntDef
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastDistinctBy
import com.huanchengfly.tieba.post.arch.BaseStateViewModel
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.TbLiteExceptionHandler
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.repository.SearchRepository
import com.huanchengfly.tieba.post.ui.models.search.SearchThreadInfo
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.set
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.withContext
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
    @SearchThreadSortType val sortType: Int = SearchThreadSortType.NEWEST,
) : UiState {

    val isEmpty: Boolean
        get() = data.isEmpty()
}

private const val TAG = "SearchThreadViewModel"

@Stable
@HiltViewModel
class SearchThreadViewModel @Inject constructor(
    private val searchRepo: SearchRepository
) : BaseStateViewModel<SearchThreadUiState>() {

    override val errorHandler = TbLiteExceptionHandler(TAG) { _, e, suppressed ->
        if (suppressed && !currentState.isEmpty) {
            _uiState.update { it.copy(isRefreshing = false, isLoadingMore = false, error = null) }
            sendUiEvent(CommonUiEvent.ToastError(e))
        } else {
            _uiState.update { it.copy(isRefreshing = false, isLoadingMore = false, error = e) }
        }
    }

    override fun createInitialState(): SearchThreadUiState = SearchThreadUiState()

    private fun searchThreadInternal(keyword: String) {
        if (keyword.isEmpty() || keyword.isBlank()) {
            _uiState.set { SearchThreadUiState(isRefreshing = false, keyword = keyword, sortType = sortType) }
            return // on clear
        }

        val uiStateSnapshot = _uiState.updateAndGet { SearchThreadUiState(keyword = keyword, sortType = it.sortType) }
        launchInVM {
            val sortType = uiStateSnapshot.sortType
            val (hasMore, threads) = searchRepo.searchThread(keyword, page = 1, sortType)
            _uiState.update {
                it.copy(isRefreshing = false, hasMore = hasMore, data = threads)
            }
        }
    }

    fun onLoadMore() {
        if (currentState.isLoadingMore) return

        val uiStateSnapshot = _uiState.updateAndGet { it.copy(isLoadingMore = true) }
        launchInVM {
            val page = uiStateSnapshot.currentPage + 1
            val sortType = uiStateSnapshot.sortType
            val (hasMore, threads) = searchRepo.searchThread(uiStateSnapshot.keyword, page, sortType)
            // Old threads + new threads
            val newData = withContext(Dispatchers.Default) {
                (uiStateSnapshot.data + threads).fastDistinctBy { it.lazyListKey }
            }
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
        if (currentState.keyword != keyword) searchThreadInternal(keyword) else return
    }

    fun onRefresh() {
        val uiStateSnapshot = currentState
        if (uiStateSnapshot.isRefreshing) return else searchThreadInternal(uiStateSnapshot.keyword)
    }

    fun onSortTypeChanged(@SearchThreadSortType sortType: Int) {
        if (currentState.sortType != sortType) {
            val oldState = _uiState.updateAndGet { it.copy(sortType = sortType) }
            searchThreadInternal(oldState.keyword)
        }
    }
}

@IntDef(SearchThreadSortType.NEWEST, SearchThreadSortType.OLDEST, SearchThreadSortType.RELATIVE)
@Retention(AnnotationRetention.SOURCE)
annotation class SearchThreadSortType {
    companion object {
        const val NEWEST = 5
        const val OLDEST = 0
        const val RELATIVE = 2
    }
}