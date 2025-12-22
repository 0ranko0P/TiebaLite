package com.huanchengfly.tieba.post.ui.page.user.post

import androidx.compose.ui.util.fastDistinctBy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.arch.TbLiteExceptionHandler
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.repository.UserProfileRepository
import com.huanchengfly.tieba.post.ui.models.user.PostListItem
import com.huanchengfly.tieba.post.ui.page.user.post.UserPostViewModel.Companion.UserPostVmFactory
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.set
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UserPostUiState(
    val isRefreshing: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: Throwable? = null,

    val currentPage: Int = 1,
    val hasMore: Boolean = false,
    val data: List<PostListItem> = emptyList(),
) : UiState {

    val isEmpty: Boolean
        get() = data.isEmpty()
}

@HiltViewModel(assistedFactory = UserPostVmFactory::class)
class UserPostViewModel @AssistedInject constructor(
    @Assisted val uid: Long,
    private val userProfileRepo: UserProfileRepository,
) : ViewModel() {

    private val handler = TbLiteExceptionHandler(TAG) { _, e, _ ->
        _uiState.update { it.copy(isRefreshing = false, error = e) }
    }

    private val _uiState = MutableStateFlow(UserPostUiState(isRefreshing = true))
    val uiState: StateFlow<UserPostUiState> = _uiState.asStateFlow()

    init {
        refreshInternal(cached = true)
    }

    private fun refreshInternal(cached: Boolean) = viewModelScope.launch(handler) {
        _uiState.update { UserPostUiState(isRefreshing = true) }
        val data = userProfileRepo.loadUserPost(uid, page = 1, cached)
        _uiState.update {
            UserPostUiState(isRefreshing = false, data = data, currentPage = 1, hasMore = data.size >= 60)
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
            val data = userProfileRepo.loadUserPost(uid, page, cached = true)
            val newData = if (data.isNotEmpty()) {
                withContext(Dispatchers.Default) {
                    (oldState.data + data).fastDistinctBy { p -> p.lazyListKey }
                }
            } else {
                null
            }
            val hasMore = newData != null && newData.size > oldState.data.size

            _uiState.update {
                if (hasMore) {
                    it.copy(isLoadingMore = false, currentPage = page, data = newData, hasMore = true)
                } else {
                    it.copy(isLoadingMore = false, hasMore = false)
                }
            }
        }
    }

    companion object {
        private const val TAG = "UserPostViewModel"

        @AssistedFactory
        interface UserPostVmFactory {
            fun create(uid: Long): UserPostViewModel
        }
    }
}
