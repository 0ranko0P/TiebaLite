package com.huanchengfly.tieba.post.ui.page.main.explore.hot

import android.util.Log
import android.util.SparseArray
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.core.util.forEach
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.emitGlobalEventSuspend
import com.huanchengfly.tieba.post.repository.ExploreRepository
import com.huanchengfly.tieba.post.repository.ExploreRepository.Companion.HOT_THREAD_TAB_ALL
import com.huanchengfly.tieba.post.ui.models.Like
import com.huanchengfly.tieba.post.ui.models.ThreadItem
import com.huanchengfly.tieba.post.ui.models.explore.HotTab
import com.huanchengfly.tieba.post.ui.models.explore.RecommendTopic
import com.huanchengfly.tieba.post.ui.page.main.explore.ExplorePageItem
import com.huanchengfly.tieba.post.ui.page.main.explore.concern.ConcernViewModel.Companion.updateLikeStatus
import com.huanchengfly.tieba.post.ui.page.main.explore.concern.ConcernViewModel.Companion.updateLikeStatusUiStateCommon
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.set
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import javax.inject.Inject

private const val TAG = "HotViewModel"

@Immutable
data class HotUiState(
    val isRefreshing: Boolean = false,
    val selectedTab: HotTab,
    val topics: List<RecommendTopic> = emptyList(),
    val tabs: List<HotTab> = emptyList(),
    val threads: List<ThreadItem>? = null, // Loading
    val error: Throwable? = null,
) : UiState {

    fun isTabSelected(tab: HotTab): Boolean = selectedTab.tabCode == tab.tabCode
}

@Stable
@HiltViewModel
class HotViewModel @Inject constructor(private val exploreRepo: ExploreRepository) : ViewModel() {

    private val defaultTab = HotTab(name = "", tabCode = HOT_THREAD_TAB_ALL, isLoading = false)

    private val _uiState = MutableStateFlow(HotUiState(isRefreshing = true, selectedTab = defaultTab))
    val uiState: StateFlow<HotUiState> = _uiState.asStateFlow()

    private val memCache = SparseArray<WeakReference<List<ThreadItem>>>()
    private val memCacheMutex = Mutex()

    private val handler = CoroutineExceptionHandler { _, e ->
        Log.e(TAG, "onError: ", e)
        _uiState.update { it.copy(isRefreshing = false, error = e) }
    }

    init {
        refreshInternal(cached = true)
    }

    // Save or update In-Memory cache
    private suspend fun updateCache(tab: HotTab, threads: List<ThreadItem>) = memCacheMutex.withLock {
        memCache.set(tab.tabCode.hashCode(), WeakReference(threads))
    }

    // Get from In-Memory cache
    private suspend fun getCached(tab: HotTab): List<ThreadItem>? = memCacheMutex.withLock {
        memCache[tab.tabCode.hashCode()]?.get()
    }

    private suspend fun clearCached() = memCacheMutex.withLock {
        memCache.forEach { k, v -> v.clear() }
        memCache.clear()
    }

    private fun refreshInternal(cached: Boolean) = viewModelScope.launch(handler) {
        _uiState.update { it.copy(isRefreshing = true, selectedTab = defaultTab, error = null) }
        if (!cached) {
            memCache.clear() // force-refresh, clear in-memory cache
        }
        val data = exploreRepo.loadHotTopic(cached)
        updateCache(defaultTab, data.threads)
        // defaultTab + tabs
        val tabs = listOf(defaultTab, *data.tabs.toTypedArray())
        defaultTab.isLoading = false
        _uiState.update {
            it.copy(isRefreshing = false, topics = data.topics, tabs = tabs, threads = data.threads)
        }
    }

    fun onRefresh() {
        if (!_uiState.value.isRefreshing) refreshInternal(cached = false)
    }

    fun onTabSelected(tab: HotTab) {
        // Check/update tab selected, loading state
        if (!_uiState.value.isTabSelected(tab)) _uiState.set { copy(selectedTab = tab, threads = null) } else return
        if (!tab.isLoading) tab.isLoading = true else return

        viewModelScope.launch(handler) {
            var topics: List<RecommendTopic>? = null
            var threads: List<ThreadItem>? = getCached(tab) // get from memory cache
            try {
                if (threads == null) {
                    val data = exploreRepo.loadHotThreads(tab.tabCode, cached = true)
                    threads = data.threads
                    topics = data.topics
                    updateCache(tab, threads)
                }
            } finally {
                withContext(Dispatchers.Main.immediate) { tab.isLoading = false }
            }

            _uiState.update {
                // Update threads if tab not switched
                threads = if (it.isTabSelected(tab)) threads else it.threads
                it.copy(topics = topics ?: it.topics, threads = threads)
            }
        }
    }

    fun onThreadLikeClicked(thread: ThreadItem) {
        viewModelScope.launch(handler) {
            val stateSnapshot = _uiState.value
            val selectedTab = stateSnapshot.selectedTab
            val success = updateLikeStatusUiStateCommon(
                thread = thread,
                onRequestLikeThread = { exploreRepo.onLikeThread(it, ExplorePageItem.Hot) },
                onEvent = ::emitGlobalEventSuspend
            ) { threadId, liked, loading ->
                _uiState.update {
                    // update if tab not switched
                    if (it.isTabSelected(selectedTab) && it.threads != null) {
                        it.copy(threads = it.threads.updateLikeStatus(threadId, liked, loading))
                    } else {
                        it // tab switched, do not update UI state
                    }
                }
            }

            if (success) { // update in-memery cache too
                val cached = getCached(selectedTab)
                if (cached != null) {
                    updateCache(selectedTab, cached.updateLikeStatus(thread.id, !thread.liked, loading = false))
                }
            }
        }
    }

    /**
     * Called when navigating back from thread page with the latest [Like] status
     *
     * @param threadId target thread ID
     * @param like like status of target thread
     * */
    fun onThreadResult(threadId: Long, like: Like) {
        viewModelScope.launch(handler) {
            val stateSnapshot = _uiState.value
            // compare and update with latest like status
            val newThreads = stateSnapshot.threads?.updateLikeStatus(threadId, like)
            if (newThreads != null) {
                _uiState.update { it.copy(threads = newThreads) }
                // update in-memory cache too
                updateCache(stateSnapshot.selectedTab, newThreads)
                // purge local cache
                exploreRepo.purgeCache(ExplorePageItem.Hot)
            }
            // else: empty or no status changes
        }
    }

    override fun onCleared() {
        runBlocking(handler) { clearCached() }
        super.onCleared()
    }
}
