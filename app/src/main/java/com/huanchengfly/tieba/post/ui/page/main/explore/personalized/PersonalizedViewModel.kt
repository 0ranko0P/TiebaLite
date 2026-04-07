package com.huanchengfly.tieba.post.ui.page.main.explore.personalized

import androidx.collection.ArraySet
import androidx.collection.MutableScatterSet
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import com.huanchengfly.tieba.post.arch.BaseStateViewModel
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.TbLiteExceptionHandler
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.emitGlobalEventSuspend
import com.huanchengfly.tieba.post.arch.stateInViewModel
import com.huanchengfly.tieba.post.models.database.BlockForum
import com.huanchengfly.tieba.post.models.database.BlockUser
import com.huanchengfly.tieba.post.repository.BlockRepository
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
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
    private val blockRepo: BlockRepository,
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

    private var loadMoreJob: Job? = null

    init {
        refreshInternal(cached = true)
    }

    override fun createInitialState(): PersonalizedUiState = PersonalizedUiState()

    private fun refreshInternal(cached: Boolean): Unit = launchInVM {
        var showTip = false
        loadMoreJob?.cancel()
        _uiState.update {
            showTip = !it.isEmpty
            // Allow user browse existing content and disable loadMore
            it.copy(isRefreshing = true, isLoadingMore = true, error = null)
        }
        val data = exploreRepo.loadPersonalized(1, cached).distinctById(blockedIds)
        _uiState.set { PersonalizedUiState(isRefreshing = false, data = data) }
        if (showTip) {
            sendUiEvent(PersonalizedUiEvent.RefreshSuccess(data.size))
        }
    }

    fun onRefresh() {
        if (!currentState.isRefreshing) refreshInternal(cached = false)
    }

    fun onLoadMore() {
        val oldState = currentState
        if (oldState.isLoadingMore || oldState.isRefreshing) return

        _uiState.update { it.copy(isLoadingMore = true) }
        loadMoreJob?.cancel()
        loadMoreJob = launchJobInVM {
            val page = oldState.currentPage + 1
            val data = exploreRepo.loadPersonalized(page, cached = true)
            val newData = (oldState.data + data).distinctById(blockedIds)
            ensureActive()
            _uiState.update { it.copy(isLoadingMore = false, currentPage = page, data = newData) }
        }
    }

    fun onThreadLikeClicked(thread: ThreadItem): Unit = launchInVM {
        updateLikeStatusUiStateCommon(
            thread = thread,
            onRequestLikeThread = { exploreRepo.onLikeThread(it, ExplorePageItem.Personalized) },
            onEvent = ::emitGlobalEventSuspend
        ) { threadId, liked, loading ->
            val newData = currentState.data.updateLikeStatus(threadId, liked, loading)
            _uiState.update { it.copy(data = newData) }
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
            // Update local block rule (blacklist)
            var updated = false
            for (reason in reasons) {
                when (reason.id) {
                    Dislike.TYPE_ID_USER -> {
                        updated = true
                        blockRepo.upsertUser(
                            thread.author.run { BlockUser(uid = id, name, whitelisted = false) }
                        )
                    }

                    Dislike.TYPE_ID_FORUM -> {
                        updated = true
                        blockRepo.upsertForum(BlockForum(name = thread.simpleForum.second))
                    }
                }
            }
            if (updated) {
                val newData = withContext(Dispatchers.Default) {
                    currentState.data.fastMap { thread ->
                        val forumName = thread.simpleForum.second
                        val content = thread.content?.text.orEmpty()
                        val blocked = blockRepo.isBlocked(forumName, uid = thread.author.id, content)
                        if (blocked xor thread.blocked) thread.copy(blocked = blocked) else thread
                    }
                }
                _uiState.update { it.copy(data = newData) }
                sendUiEvent(PersonalizedUiEvent.BlockRuleUpdated)
            }
        }
    }

    /**
     * Called when navigating back from thread page.
     *
     * @param threadId target thread ID
     * @param like latest thread like status
     * */
    fun onThreadResult(threadId: Long, like: Like): Unit = launchInVM {
        val newData = currentState.data.updateLikeStatus(threadId, like)
        if (newData != null) {
            _uiState.update { it.copy(data = newData) }
            exploreRepo.updateCachedThreadLike(threadId, like, from = ExplorePageItem.Personalized)
        }
        // else -> empty or no status changes
    }
}

sealed interface PersonalizedUiEvent : UiEvent {
    class RefreshSuccess(val count: Int) : PersonalizedUiEvent

    object BlockRuleUpdated: PersonalizedUiEvent

    class DislikeFailed(val e: Throwable): PersonalizedUiEvent
}