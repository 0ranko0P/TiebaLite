package com.huanchengfly.tieba.post.ui.page.main.explore.personalized

import android.util.Log
import androidx.collection.ArraySet
import androidx.collection.MutableScatterSet
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.emitGlobalEventSuspend
import com.huanchengfly.tieba.post.repository.ExploreRepository
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.models.Like
import com.huanchengfly.tieba.post.ui.models.ThreadItem
import com.huanchengfly.tieba.post.ui.models.explore.Dislike
import com.huanchengfly.tieba.post.ui.page.main.explore.ExplorePageItem
import com.huanchengfly.tieba.post.ui.page.main.explore.concern.ConcernViewModel.Companion.updateLikeStatus
import com.huanchengfly.tieba.post.ui.page.main.explore.concern.ConcernViewModel.Companion.updateLikeStatusUiStateCommon
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.set
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections
import javax.inject.Inject

@Immutable
data class PersonalizedUiState(
    val isRefreshing: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: Throwable? = null,
    val currentPage: Int = 1,
    val data: List<ThreadItem> = emptyList(),
): UiState {

    val isEmpty: Boolean
        get() = data.isEmpty()
}

@Stable
@HiltViewModel
class PersonalizedViewModel @Inject constructor(
    private val exploreRepo: ExploreRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    companion object {
        private const val TAG = "PersonalizedViewModel"

        private suspend fun List<ThreadItem>.distinctById(blockedIds: Set<Long>): List<ThreadItem> {
            return withContext(Dispatchers.Default) {
                val set = MutableScatterSet<Long>(size)
                val result = mutableListOf<ThreadItem>()
                fastForEach {
                    // Check blocked and distinct
                    if (it.id !in blockedIds && set.add(it.id)) result += it
                }
                return@withContext result
            }
        }
    }

    private val handler = CoroutineExceptionHandler { _, e ->
        Log.e(TAG, "onError: ", e)
        _uiState.update { it.copy(isRefreshing = false, error = e) }
    }

    private val _uiState = MutableStateFlow(PersonalizedUiState())
    val uiState: StateFlow<PersonalizedUiState> = _uiState.asStateFlow()

    /**
     * One-off [UiEvent], but no guarantee to be received.
     * */
    private val _uiEvent: MutableSharedFlow<UiEvent?> = MutableSharedFlow()
    val uiEvent: Flow<UiEvent?>
        get() = _uiEvent

    private val blockedIds: MutableSet<Long> = Collections.synchronizedSet(ArraySet())

    val hideBlockedContent: StateFlow<Boolean> = settingsRepository.blockSettings.flow
        .map { it.hideBlocked }
        .stateIn(viewModelScope, started = SharingStarted.WhileSubscribed(5_000), false)

    init {
        refreshInternal(cached = true)
    }

    private fun refreshInternal(cached: Boolean) = viewModelScope.launch(handler) {
        var showTip = false
        _uiState.set {
            showTip = !this.isEmpty
            PersonalizedUiState(isRefreshing = true)
        }
        val data = exploreRepo.loadPersonalized(1, cached).distinctById(blockedIds)
        _uiState.set { copy(isRefreshing = false, data = data) }
        if (showTip) {
            _uiEvent.emit(PersonalizedUiEvent.RefreshSuccess(data.size))
        }
    }

    fun onRefresh() {
        if (!_uiState.value.isRefreshing) refreshInternal(cached = false)
    }

    fun onLoadMore() {
        val oldState = _uiState.value
        if (!oldState.isLoadingMore) _uiState.set { copy(isLoadingMore = true) } else return

        viewModelScope.launch(handler) {
            val page = oldState.currentPage + 1
            val data = exploreRepo.loadPersonalized(page, cached = true)
            val newData = (oldState.data + data).distinctById(blockedIds)
            _uiState.update { it.copy(isLoadingMore = false, currentPage = page, data = newData) }
        }
    }

    fun onThreadLikeClicked(thread: ThreadItem) = viewModelScope.launch(handler) {
        updateLikeStatusUiStateCommon(
            thread = thread,
            onRequestLikeThread = { exploreRepo.onLikeThread(it, ExplorePageItem.Personalized) },
            onEvent = ::emitGlobalEventSuspend
        ) { threadId, liked, loading ->
            _uiState.update { it.copy(data = it.data.updateLikeStatus(threadId, liked, loading)) }
        }
    }

    fun onThreadDislike(thread: ThreadItem, reasons: List<Dislike>) {
        if (!blockedIds.add(thread.id)) return

        viewModelScope.launch(handler) {
            _uiState.update { it.copy(data = it.data.distinctById(blockedIds)) }
            runCatching {
                exploreRepo.onDislikeThread(thread, reasons)
            }
            .onFailure { // ignore errors and keep data changes
                _uiEvent.emit(PersonalizedUiEvent.DislikeFailed(it))
            }
        }
    }

    /**
     * Called when navigate back from thread page with latest [Like] status
     *
     * @param threadId target thread ID
     * @param like like status of target thread
     * */
    fun onThreadResult(threadId: Long, like: Like) {
        viewModelScope.launch(handler) {
            // compare and update with latest like status
            val newData = _uiState.value.data.updateLikeStatus(threadId, like)
            if (newData != null) {
                _uiState.update { it.copy(data = newData) }
                exploreRepo.purgeCache(ExplorePageItem.Personalized)
            }
            // else: empty or no status changes
        }
    }
}

sealed interface PersonalizedUiEvent : UiEvent {
    class RefreshSuccess(val count: Int) : PersonalizedUiEvent

    class DislikeFailed(val e: Throwable): PersonalizedUiEvent
}