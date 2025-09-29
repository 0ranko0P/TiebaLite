package com.huanchengfly.tieba.post.ui.page.forum.threadlist

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.api.retrofit.exception.NoConnectivityException
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.emitGlobalEventSuspend
import com.huanchengfly.tieba.post.repository.ExploreRepository.Companion.distinctById
import com.huanchengfly.tieba.post.repository.ForumRepository
import com.huanchengfly.tieba.post.repository.PbPageRepository
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.models.ThreadItem
import com.huanchengfly.tieba.post.ui.models.settings.ForumSortType
import com.huanchengfly.tieba.post.ui.page.forum.threadlist.ForumThreadListViewModel.Companion.ForumVMFactory
import com.huanchengfly.tieba.post.ui.page.main.explore.concern.ConcernViewModel.Companion.updateLikeStatus
import com.huanchengfly.tieba.post.ui.page.main.explore.concern.ConcernViewModel.Companion.updateLikeStatusUiStateCommon
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlin.math.min

@Stable
@HiltViewModel(assistedFactory = ForumVMFactory::class)
class ForumThreadListViewModel @AssistedInject constructor(
    @Assisted val forumName: String,
    @Assisted val forumId: Long,
    @Assisted val type: ForumType,
    @ApplicationContext val context: Context,
    private val forumRepo: ForumRepository,
    private val threadRepo: PbPageRepository,
    settingsRepo: SettingsRepository,
) : ViewModel() {

    private val handler = CoroutineExceptionHandler { _, e ->
        if (e !is NoConnectivityException) {
            Log.e(TAG, "onError", e)
        }
        _uiState.update { it.copy(isRefreshing = false, isLoadingMore = false, error = e) }
    }

    private val sortTypeFlow: Flow<Int>? =
        if (type == ForumType.Latest) forumRepo.getSortType(forumName) else null

    private val _uiState: MutableStateFlow<ForumThreadListUiState> = MutableStateFlow(
        ForumThreadListUiState(isRefreshing = true)
    )
    val uiState: StateFlow<ForumThreadListUiState> = _uiState.asStateFlow()

    val hideBlocked: StateFlow<Boolean> = settingsRepo.blockSettings.flow
        .map { it.hideBlocked }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    init {
        viewModelScope.launch(handler) {
            loadInternal(sortType = null, classifyId = null)
        }
    }

    private suspend fun loadInternal(sortType: Int?, classifyId: Int?, forceNew: Boolean = false) {
        _uiState.update { it.copy(isRefreshing = true) }
        val data = if (type == ForumType.Latest) {
            val sort = sortType ?: sortTypeFlow!!.first()
            forumRepo.loadPage(forumName, page = 1, sortType = sort, forceNew)
        } else {
            forumRepo.loadGoodPage(forumName, page = 1, classifyId, forceNew)
        }
        _uiState.update {
            ForumThreadListUiState(
                goodClassifyId = it.goodClassifyId,
                threads = data.threads,
                threadIds = data.threadIds,
                currentPage = 1,
                hasMore = data.hasMore
            )
        }
    }

    fun onClassifyIdChanged(classifyId: Int) {
        val state = _uiState.updateAndGet { it.copy(goodClassifyId = classifyId) }
        if (state.isRefreshing) return
        viewModelScope.launch(handler) {
            // Load cached result if id classifyId is 0
            loadInternal(sortType = null, classifyId, forceNew = classifyId != 0)
        }
    }

    fun onSortTypeChanged(@ForumSortType sortType: Int?) {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch(handler) {
            // Load cached result
            loadInternal(sortType = sortType, classifyId = null, forceNew = false)
        }
    }

    fun onRefresh() {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch(handler) {
            if (type == ForumType.Latest) {
                loadInternal(sortType = sortTypeFlow!!.first(), classifyId = null, forceNew = true)
            } else {
                val currentClassifyId = _uiState.value.goodClassifyId ?: 0
                loadInternal(sortType = null, classifyId = currentClassifyId, forceNew = true)
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore) return else _uiState.update { it.copy(isLoadingMore = true) }

        viewModelScope.launch(handler) {
            val sortType = if (type == ForumType.Latest) sortTypeFlow!!.first() else 0
            if (state.threadIds.isNotEmpty()) {
                val size = min(state.threadIds.size, 30)
                val threadIds = state.threadIds.subList(0, size)
                val newList = forumRepo.threadList(forumId, forumName, state.currentPage, sortType, threadIds)
                val threadList = (state.threads + newList).distinctById()

                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        isLoadingMore = false,
                        threads = threadList,
                        threadIds = state.threadIds.drop(size),
                        hasMore = threadList.isNotEmpty()
                    )
                }
            } else {
                val page = state.currentPage + 1
                val data = if (type == ForumType.Latest) {
                    forumRepo.loadMorePage(forumName, page, sortType)
                } else {
                    forumRepo.loadMoreGood(forumName, page, state.goodClassifyId)
                }
                val threadList = (state.threads + data.threads).distinctById()
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        isLoadingMore = false,
                        threads = threadList,
                        threadIds = data.threadIds,
                        currentPage = page,
                        hasMore = data.hasMore
                    )
                }
            }
        }
    }

    fun onThreadLikeClicked(thread: ThreadItem) = viewModelScope.launch(handler) {
        updateLikeStatusUiStateCommon(
            thread = thread,
            onRequestLikeThread = threadRepo::requestLikeThread,
            onEvent = ::emitGlobalEventSuspend
        ) { threadId, liked, loading ->
            _uiState.update { it.copy(threads = it.threads.updateLikeStatus(threadId, liked, loading)) }
        }
    }

    companion object {
        private const val TAG = "ForumThreadListViewMode"

        @AssistedFactory
        interface ForumVMFactory {
            fun create(forumName: String, forumId: Long, type: ForumType): ForumThreadListViewModel
        }
    }
}

enum class ForumType {
    Latest, Good
}

data class ForumThreadListUiState(
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val goodClassifyId: Int? = null,
    val threads: List<ThreadItem> = emptyList(),
    val threadIds: List<Long> = emptyList(),
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val error: Throwable? = null
) : UiState

sealed interface ForumThreadListUiEvent : UiEvent {

    data class SortTypeChanged(val sortType: Int): ForumThreadListUiEvent

    data class ClassifyChanged(val goodClassifyId: Int) : ForumThreadListUiEvent

    object Refresh : ForumThreadListUiEvent

    data class BackToTop(val isGood: Boolean) : ForumThreadListUiEvent
}