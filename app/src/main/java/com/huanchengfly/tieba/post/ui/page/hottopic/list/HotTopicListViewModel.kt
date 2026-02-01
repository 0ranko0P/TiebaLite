package com.huanchengfly.tieba.post.ui.page.hottopic.list

import androidx.compose.runtime.Stable
import com.huanchengfly.tieba.post.api.models.protos.topicList.NewTopicList
import com.huanchengfly.tieba.post.arch.BaseStateViewModel
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.TbLiteExceptionHandler
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.repository.HotTopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import javax.inject.Inject

private const val TAG = "HotTopicListViewModel"

@Stable
@HiltViewModel
class HotTopicListViewModel @Inject constructor(
    private val hotTopicRepo: HotTopicRepository,
) : BaseStateViewModel<HotTopicListUiState>() {

    override val errorHandler = TbLiteExceptionHandler(TAG) { _, e, suppressed ->
        // Allow user browse existing content on suppressed exceptions
        if (suppressed && currentState.topicList.isNotEmpty()) {
            _uiState.update { it.copy(isRefreshing = false, error = null) }
            sendUiEvent(CommonUiEvent.ToastError(e))
        } else {
            _uiState.update { it.copy(isRefreshing = false, error = e) }
        }
    }

    init {
        refreshInternal()
    }

    override fun createInitialState(): HotTopicListUiState = HotTopicListUiState()

    private fun refreshInternal(): Unit = launchInVM {
        _uiState.update { HotTopicListUiState(isRefreshing = true) }
        val data = hotTopicRepo.loadTopicList()
        _uiState.update { it.copy(isRefreshing = false, topicList = data) }
    }

    fun onRefresh() {
        if (!currentState.isRefreshing) refreshInternal()
    }
}

data class HotTopicListUiState(
    val isRefreshing: Boolean = true,
    val error: Throwable? = null,
    val topicList: List<NewTopicList> = emptyList()
) : UiState