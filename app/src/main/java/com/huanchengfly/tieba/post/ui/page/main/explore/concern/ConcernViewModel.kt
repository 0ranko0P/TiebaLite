package com.huanchengfly.tieba.post.ui.page.main.explore.concern

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.emitGlobalEventSuspend
import com.huanchengfly.tieba.post.repository.ExploreRepository
import com.huanchengfly.tieba.post.repository.ExploreRepository.Companion.distinctById
import com.huanchengfly.tieba.post.ui.models.Like
import com.huanchengfly.tieba.post.ui.models.ThreadItem
import com.huanchengfly.tieba.post.ui.page.main.explore.ExplorePageItem
import com.huanchengfly.tieba.post.ui.page.thread.ThreadLikeUiEvent
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.set
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Immutable
data class ConcernUiState(
    val isRefreshing: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val lastRequestUnix: Long = 0,
    val nextPageTag: String = "",
    val data: List<ThreadItem> = emptyList(),
    val error: Throwable? = null
): UiState {

    val isEmpty: Boolean
        get() = data.isEmpty()
}

@Stable
@HiltViewModel
class ConcernViewModel @Inject constructor(
    private val exploreRepo: ExploreRepository
) : ViewModel() {

    private val handler = CoroutineExceptionHandler { _, e ->
        Log.e(TAG, "onError: ", e)
        _uiState.update { it.copy(isRefreshing = false, error = e) }
    }

    private val _uiState = MutableStateFlow(ConcernUiState(isRefreshing = true))
    val uiState: StateFlow<ConcernUiState> = _uiState.asStateFlow()

    init {
        refreshInternal(cached = true)
    }

    private fun refreshInternal(cached: Boolean) = viewModelScope.launch(handler) {
        var lastRequestUnix: Long? = null
        _uiState.set {
            lastRequestUnix = this.lastRequestUnix.takeUnless { it == 0L }
            ConcernUiState(isRefreshing = true)
        }
        val rec = exploreRepo.refreshUserLike(lastRequestUnix, cached)
        val data = rec.threads.distinctById()
        _uiState.set {
            copy(isRefreshing = false, hasMore = rec.hasMore, lastRequestUnix = rec.requestUnix, nextPageTag = rec.pageTag, data = data)
        }
    }

    fun onRefresh() {
        if (!_uiState.value.isRefreshing) refreshInternal(cached = false)
    }

    fun onLoadMore() {
        val oldState = _uiState.value
        if (!oldState.isLoadingMore) _uiState.set { copy(isLoadingMore = true) } else return

        viewModelScope.launch(handler) {
            val rec = exploreRepo.loadUserLike(oldState.nextPageTag, oldState.lastRequestUnix)
            val data = (oldState.data + rec.threads).distinctById()
            _uiState.update {
                it.copy(isLoadingMore = false, hasMore = rec.hasMore, lastRequestUnix = rec.requestUnix, nextPageTag = rec.pageTag, data = data)
            }
        }
    }

    /**
     * Called when user clicked like button on target [ThreadItem]
     * */
    fun onThreadLikeClicked(thread: ThreadItem) = viewModelScope.launch(handler) {
        updateLikeStatusUiStateCommon(
            thread = thread,
            onRequestLikeThread = { exploreRepo.onLikeThread(it, ExplorePageItem.Concern) },
            onEvent = ::emitGlobalEventSuspend
        ) { threadId, liked, loading ->
            _uiState.update { it.copy(data = it.data.updateLikeStatus(threadId, liked, loading)) }
        }
    }

    /**
     * Called when navigate back from thread page with latest [Like] status
     *
     * @param threadId target thread ID
     * @param like latest like status of target thread
     * */
    fun onThreadResult(threadId: Long, like: Like) {
        viewModelScope.launch(handler) {
            // compare and update with latest like status
            val newData = _uiState.value.data.updateLikeStatus(threadId, like)
            if (newData != null) {
                _uiState.update { it.copy(data = newData) }
                exploreRepo.purgeCache(ExplorePageItem.Concern)
            }
            // else: empty or no status changes
        }
    }

    companion object {
        private const val TAG = "ConcernViewModel"

        /**
         * Update Like status of target [ThreadItem] in this list
         *
         * @param threadId id of target [ThreadItem]
         * @param liked new like status
         * @param loading is requesting like status update to server
         *
         * @return new thread list with like status updated
         * */
        suspend fun List<ThreadItem>.updateLikeStatus(
            threadId: Long,
            liked: Boolean,
            loading: Boolean
        ): List<ThreadItem> = withContext(Dispatchers.Default) {
            fastMap {
                if (it.id != threadId) return@fastMap it

                it.copy(like = it.like.updateLikeStatus(liked).setLoading(loading))
            }
        }

        /**
         * Update Like status of target [ThreadItem] in this list
         *
         * @param threadId id of target [ThreadItem]
         * @param like new like status
         *
         * @return new thread list with like status updated or **null** if no status changes
         * */
        suspend fun List<ThreadItem>.updateLikeStatus(threadId: Long, like: Like) = if (this.isNotEmpty()) {
            withContext(Dispatchers.Default) {
                var changed = false
                fastMap {
                    if (it.id == threadId) {
                        // no status changes, return null directly
                        if (it.liked == like.liked && it.like.count == like.count) return@withContext null
                        changed = true
                        it.copy(like = like)
                    } else {
                        it
                    }
                }.takeIf { changed } // else target thread not found
            }
        } else {
            null
        }

        suspend fun updateLikeStatusUiStateCommon(
            thread: ThreadItem,
            onRequestLikeThread: suspend (ThreadItem) -> Unit,
            onEvent: suspend (ThreadLikeUiEvent) -> Unit,
            onUpdateThreadList: suspend (threadId: Long, liked: Boolean, loading: Boolean) -> Unit
        ): Boolean = withContext(Dispatchers.Main) {
            if (thread.like.loading) {
                onEvent(ThreadLikeUiEvent.Connecting)
                return@withContext false
            }
            val threadId = thread.id
            val liked = !thread.liked

            // set loading to true
            onUpdateThreadList(threadId, liked, true)
            // request like status update to server
            runCatching {
                onRequestLikeThread(thread)
            }
            .onFailure {
                if (it is TiebaNotLoggedInException) {
                    onEvent(ThreadLikeUiEvent.NotLoggedIn)
                } else {
                    onEvent(ThreadLikeUiEvent.Failed(e = it))
                }
                // revert like status changes on this ThreadItem, set loading to false
                onUpdateThreadList(threadId, !liked, false)
            }
            .onSuccess {
                // set loading to false
                onUpdateThreadList(threadId, liked, false)
            }
            .isSuccess
        }
    }
}
