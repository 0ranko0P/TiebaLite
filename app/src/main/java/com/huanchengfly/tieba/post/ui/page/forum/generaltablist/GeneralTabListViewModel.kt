package com.huanchengfly.tieba.post.ui.page.forum.generaltablist

import android.util.Log
import androidx.compose.runtime.Stable
import com.huanchengfly.tieba.post.arch.BaseStateViewModel
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.TbLiteExceptionHandler
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.emitGlobalEventSuspend
import com.huanchengfly.tieba.post.arch.stateInViewModel
import com.huanchengfly.tieba.post.core.network.model.protos.FrsTabInfo
import com.huanchengfly.tieba.post.repository.ExploreRepository.Companion.distinctById
import com.huanchengfly.tieba.post.repository.ForumRepository
import com.huanchengfly.tieba.post.repository.PbPageRepository
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.models.Like
import com.huanchengfly.tieba.post.ui.models.ThreadItem
import com.huanchengfly.tieba.post.ui.models.settings.ForumSortType
import com.huanchengfly.tieba.post.ui.page.forum.generaltablist.GeneralTabListViewModel.Companion.GeneralTabListVMFactory
import com.huanchengfly.tieba.post.ui.page.main.explore.concern.ConcernViewModel.Companion.updateLikeStatus
import com.huanchengfly.tieba.post.ui.page.main.explore.concern.ConcernViewModel.Companion.updateLikeStatusUiStateCommon
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

@Stable
@HiltViewModel(assistedFactory = GeneralTabListVMFactory::class)
class GeneralTabListViewModel @AssistedInject constructor(
    @Assisted val forumName: String,
    @Assisted val forumId: Long,
    @Assisted val tabInfo: FrsTabInfo,
    @Assisted val initialSortType: Int,
    private val forumRepo: ForumRepository,
    private val threadRepo: PbPageRepository,
    settingsRepo: SettingsRepository,
) : BaseStateViewModel<GeneralTabListUiState>() {

    val hideBlocked: StateFlow<Boolean> = settingsRepo.blockSettings
        .map { it.hideBlocked }
        .stateInViewModel(initialValue = true)

    override val errorHandler = TbLiteExceptionHandler(TAG) { _, e, suppressed ->
        // Allow user browse existing content on suppressed exceptions
        if (suppressed && currentState.threads.isNotEmpty()) {
            _uiState.update { it.copy(isRefreshing = false, isLoadingMore = false, error = null) }
            sendUiEvent(CommonUiEvent.ToastError(e))
        } else {
            _uiState.update { it.copy(isRefreshing = false, isLoadingMore = false, error = e) }
        }
    }

    override fun createInitialState() = GeneralTabListUiState(sortType = initialSortType)

    init {
        Log.d(TAG, "onInit: $forumName tabID: ${tabInfo.tabId}, initialSort: $initialSortType")
        refreshInternal(sortType = initialSortType)
    }

    private fun refreshInternal(@ForumSortType sortType: Int, forceNew: Boolean = false) {
        _uiState.update { GeneralTabListUiState(isRefreshing = true, sortType = sortType) }
        launchInVM {
            val data = forumRepo.generalTabList(
                forumId = forumId,
                forumName = forumName,
                tabInfo = tabInfo,
                pn = 1,
                sortType = sortType,
                lastThreadId = 0,
                forceNew = forceNew,
            )
            val threads = data.threads.distinctById()
            _uiState.update {
                GeneralTabListUiState(
                    threads = threads,
                    currentPage = 1,
                    hasMore = data.hasMore,
                    lastThreadId = threads.lastOrNull()?.id ?: 0,
                )
            }
        }
    }

    fun loadMore() {
        val oldState = currentState
        if (oldState.isLoadingMore) return else _uiState.update { it.copy(isLoadingMore = true) }

        launchInVM {
            val data = forumRepo.generalTabList(
                forumId = forumId,
                forumName = forumName,
                tabInfo = tabInfo,
                pn = oldState.currentPage + 1,
                sortType = oldState.sortType,
                lastThreadId = oldState.lastThreadId,
                forceNew = true,
            )
            val threads = (oldState.threads + data.threads).distinctById()

            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    isLoadingMore = false,
                    threads = threads,
                    hasMore = data.hasMore && data.threads.isNotEmpty(),
                    currentPage = oldState.currentPage + 1,
                    lastThreadId = data.threads.lastOrNull()?.id ?: oldState.lastThreadId,
                )
            }
        }
    }

    fun onRefresh() {
        if (!currentState.isRefreshing) {
            refreshInternal(currentState.sortType, forceNew = true)
        }
    }

    fun onSortTypeChanged(@ForumSortType sortType: Int) {
        if (!currentState.isRefreshing) {
            refreshInternal(sortType = sortType, forceNew = false/* Load cached result */)
        }
    }

    fun onThreadLikeClicked(thread: ThreadItem) = launchInVM {
        updateLikeStatusUiStateCommon(
            thread = thread,
            onRequestLikeThread = threadRepo::requestLikeThread,
            onEvent = ::emitGlobalEventSuspend
        ) { threadId, liked, loading ->
            _uiState.update { it.copy(threads = it.threads.updateLikeStatus(threadId, liked, loading)) }
        }
    }

    fun onThreadResult(threadId: Long, like: Like): Unit = launchInVM {
        val newThreads = currentState.threads.updateLikeStatus(threadId, like)
        if (newThreads != null) {
            _uiState.update { it.copy(threads = newThreads) }
        }
    }

    companion object {
        private const val TAG = "GeneralTabListViewModel"

        @AssistedFactory
        interface GeneralTabListVMFactory {
            fun create(forumName: String, forumId: Long, tabInfo: FrsTabInfo, initialSortType: Int): GeneralTabListViewModel
        }
    }
}

data class GeneralTabListUiState(
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val threads: List<ThreadItem> = emptyList(),
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val lastThreadId: Long = 0,
    @ForumSortType val sortType: Int = ForumSortType.BY_REPLY,
    val error: Throwable? = null
) : UiState

sealed interface GeneralTabListUiEvent : UiEvent {

    data class SortTypeChanged(
        val tabId: Int,
        @ForumSortType val sortType: Int
    ): GeneralTabListUiEvent

    data class Refresh(val tabId: Int) : GeneralTabListUiEvent
}
