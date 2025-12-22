package com.huanchengfly.tieba.post.ui.page.main.explore.personalized

import androidx.collection.ArraySet
import androidx.collection.MutableScatterSet
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastForEach
import com.huanchengfly.tieba.post.arch.BaseStateViewModel
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.TbLiteExceptionHandler
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.emitGlobalEventSuspend
import com.huanchengfly.tieba.post.arch.stateInViewModel
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
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
) : BaseStateViewModel<PersonalizedUiState>() {

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

    override val errorHandler = TbLiteExceptionHandler(TAG) { _, e, suppressed ->
        // Allow user browse existing content on suppressed exceptions
        if (suppressed && !currentState.isEmpty) {
            _uiState.update { it.copy(isRefreshing = false, isLoadingMore = false, error = null) }
            sendUiEvent(CommonUiEvent.ToastError(e))
        } else {
            _uiState.update { it.copy(isRefreshing = false, isLoadingMore = false, error = e) }
        }
    }

    private val blockedIds: MutableSet<Long> = Collections.synchronizedSet(ArraySet())

    val hideBlockedContent: StateFlow<Boolean> = settingsRepository.blockSettings
        .map { it.hideBlocked }
        .stateInViewModel(initialValue = false)

    init {
        refreshInternal(cached = true)
    }

    override fun createInitialState(): PersonalizedUiState = PersonalizedUiState()

    private fun refreshInternal(cached: Boolean): Unit = launchInVM {
        var showTip = false
        _uiState.set {
            showTip = !this.isEmpty
            PersonalizedUiState(isRefreshing = true)
        }
        val data = exploreRepo.loadPersonalized(1, cached).distinctById(blockedIds)
        _uiState.set { copy(isRefreshing = false, data = data) }
        if (showTip) {
            sendUiEvent(PersonalizedUiEvent.RefreshSuccess(data.size))
        }
    }

    fun onRefresh() {
        if (!currentState.isRefreshing) refreshInternal(cached = false)
    }

    fun onLoadMore() {
        val oldState = currentState
        if (!oldState.isLoadingMore) _uiState.set { copy(isLoadingMore = true) } else return

        launchInVM {
            val page = oldState.currentPage + 1
            val data = exploreRepo.loadPersonalized(page, cached = true)
            val newData = (oldState.data + data).distinctById(blockedIds)
            _uiState.update { it.copy(isLoadingMore = false, currentPage = page, data = newData) }
        }
    }

    fun onThreadLikeClicked(thread: ThreadItem): Unit = launchInVM {
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

        launchInVM {
            _uiState.update { it.copy(data = it.data.distinctById(blockedIds)) }
            runCatching {
                exploreRepo.onDislikeThread(thread, reasons)
            }
            .onFailure { // ignore errors and keep data changes
                sendUiEvent(PersonalizedUiEvent.DislikeFailed(it))
            }
        }
    }

    /**
     * Called when navigating back from thread page with the latest [Like] status
     *
     * @param threadId target thread ID
     * @param like latest thread like status
     * */
    fun onThreadResult(threadId: Long, like: Like): Unit = launchInVM {
        // compare and update with latest like status
        val newData = currentState.data.updateLikeStatus(threadId, like)
        if (newData != null) {
            _uiState.update { it.copy(data = newData) }
            exploreRepo.purgeCache(ExplorePageItem.Personalized)
        }
        // else -> empty or no status changes
    }
}

sealed interface PersonalizedUiEvent : UiEvent {
    class RefreshSuccess(val count: Int) : PersonalizedUiEvent

    class DislikeFailed(val e: Throwable): PersonalizedUiEvent
}