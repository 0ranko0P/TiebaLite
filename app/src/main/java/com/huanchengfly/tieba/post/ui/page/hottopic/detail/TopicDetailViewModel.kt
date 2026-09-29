package com.huanchengfly.tieba.post.ui.page.hottopic.detail

import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.arch.BaseStateViewModel
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.TbLiteExceptionHandler
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.arch.stateInViewModel
import com.huanchengfly.tieba.post.core.data.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.core.network.model.TopicInfoBean
import com.huanchengfly.tieba.post.repository.HotTopicRepository
import com.huanchengfly.tieba.post.ui.models.Like
import com.huanchengfly.tieba.post.ui.models.ThreadItem
import com.huanchengfly.tieba.post.ui.page.hottopic.detail.TopicDetailViewModel.Companion.TopicDetailVmFactory
import com.huanchengfly.tieba.post.ui.page.main.explore.concern.ConcernViewModel.Companion.updateLikeStatus
import com.huanchengfly.tieba.post.ui.page.main.explore.concern.ConcernViewModel.Companion.updateLikeStatusUiStateCommon
import com.huanchengfly.tieba.post.utils.extension.set
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

@Stable
@HiltViewModel(assistedFactory = TopicDetailVmFactory::class)
class TopicDetailViewModel @AssistedInject constructor(
    @Assisted val topicId: Long,
    @Assisted val topicName: String,
    private val hotTopicRepo: HotTopicRepository,
    settingsRepo: SettingsRepository,
) : BaseStateViewModel<TopicDetailUiState>() {

    override val errorHandler = TbLiteExceptionHandler(TAG) { _, e, suppressed ->
        // Allow user browse existing content on suppressed exceptions
        if (suppressed && !currentState.isEmpty) {
            _uiState.update { it.copy(isRefreshing = false, isLoadingMore = false, error = null) }
            sendUiEvent(CommonUiEvent.ToastError(e))
        } else {
            _uiState.update { it.copy(isRefreshing = false, isLoadingMore = false, error = e) }
        }
    }

    val hideBlockedContent: StateFlow<Boolean> = settingsRepo.blockSettings
        .map { it.hideBlocked }
        .stateInViewModel(initialValue = false)

    init {
        refreshInternal()
        observeThreadLike()
    }

    override fun createInitialState(): TopicDetailUiState = TopicDetailUiState()

    private fun refreshInternal() {
        _uiState.set {
            // Allow user browse existing contents
            if (isEmpty) TopicDetailUiState() else TopicDetailUiState(hasMore = false, topicInfo = topicInfo, threads = threads)
        }
        launchInVM {
            val data = hotTopicRepo.loadTopicDetail(topicId, topicName, lastId = null)
            _uiState.set {
                TopicDetailUiState(
                    isRefreshing = false,
                    hasMore = data.hasMore,
                    currentPage = 1,
                    topicInfo = data.topicInfo,
                    threads = data.threads
                )
            }
        }
    }

    fun onRefresh() {
        if (!currentState.isRefreshing) refreshInternal()
    }

    fun onLoadMore() {
        val oldState = currentState
        if (oldState.isLoadingMore || oldState.isRefreshing) return

        _uiState.set { copy(isLoadingMore = true) }
        launchInVM {
            val page = oldState.currentPage + 1
            val lastId = oldState.threads.last().feedId
            val data = hotTopicRepo.loadTopicDetailMore(topicId, topicName, page, lastId = lastId)
            val threads = withContext(Dispatchers.Default) {
                (oldState.threads + data.threads).distinctBy { it.feedId }
            }
            _uiState.update {
                oldState.copy(
                    isLoadingMore = false,
                    currentPage = page,
                    hasMore = data.hasMore,
                    topicInfo = data.topicInfo,
                    threads = threads,
                )
            }
        }
    }

    fun onThreadLikeClicked(thread: ThreadItem): Unit = launchInVM {
        updateLikeStatusUiStateCommon(
            thread = thread,
            onRequestLikeThread = { hotTopicRepo.onLikeThread(thread) },
            onEvent = ::sendUiEvent
        ) { threadId, liked, loading ->
            _uiState.update { it.copy(threads = it.threads.updateLikeStatus(threadId, liked, loading)) }
        }
    }

    /**
     * Observe [Like] status changes from thread page
     * */
    private fun observeThreadLike() = viewModelScope.onGlobalEvent<GlobalEvent.ThreadLike> { event ->
        val threadId = event.threadId
        val like: Like = event.like
        val stateSnapshot = currentState
        // compare and update with latest like status
        val newThreads = stateSnapshot.threads.updateLikeStatus(threadId, like)
        if (newThreads != null) {
            _uiState.update { stateSnapshot.copy(threads = newThreads) }
        }
        // else: empty or no status changes
    }

    companion object {
        private const val TAG = "TopicDetailViewModel"

        @AssistedFactory
        interface TopicDetailVmFactory {
            fun create(topicId: Long, topicName: String): TopicDetailViewModel
        }

        val ThreadItem.feedId: Long
            get() = id
    }
}

sealed interface TopicDetailUiEvent : UiEvent {
    object RefreshSuccess : TopicDetailUiEvent
}

data class TopicDetailUiState(
    val isRefreshing: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: Throwable? = null,
    val hasMore: Boolean = true,
    val currentPage: Int = 1,
    val topicInfo: TopicInfoBean? = null,
    val threads: List<ThreadItem> = emptyList(),
) : UiState {

    val isEmpty: Boolean
        get() = topicInfo == null || threads.isEmpty()
}
