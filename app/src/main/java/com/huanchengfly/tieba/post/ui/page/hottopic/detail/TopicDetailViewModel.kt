package com.huanchengfly.tieba.post.ui.page.hottopic.detail

import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.huanchengfly.tieba.post.api.models.TopicInfoBean
import com.huanchengfly.tieba.post.arch.BaseStateViewModel
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.TbLiteExceptionHandler
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiIntent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.emitGlobalEventSuspend
import com.huanchengfly.tieba.post.arch.stateInViewModel
import com.huanchengfly.tieba.post.repository.HotTopicRepository
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.models.ThreadItem
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.main.explore.concern.ConcernViewModel.Companion.updateLikeStatus
import com.huanchengfly.tieba.post.ui.page.main.explore.concern.ConcernViewModel.Companion.updateLikeStatusUiStateCommon
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.set
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Stable
@HiltViewModel
class TopicDetailViewModel @Inject constructor(
    private val hotTopicRepo: HotTopicRepository,
    settingsRepo: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : BaseStateViewModel<TopicDetailUiState>() {

    private val param = savedStateHandle.toRoute<Destination.HotTopicDetail>()
    val topicId: Long = param.topicId
    val topicName: String = param.topicName

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
    }

    override fun createInitialState(): TopicDetailUiState = TopicDetailUiState()

    private fun refreshInternal() {
        _uiState.set { createInitialState() }
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
        if (!oldState.isLoadingMore) _uiState.set { copy(isLoadingMore = true) } else return

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

    companion object {

        private const val TAG = "TopicDetailViewModel"

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
