package com.huanchengfly.tieba.post.ui.page.forum.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.repository.ForumRepository
import com.huanchengfly.tieba.post.ui.models.forum.ForumDetail
import com.huanchengfly.tieba.post.ui.page.forum.detail.ForumDetailViewModel.Companion.ForumDetailVmFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ForumDetailUiState(
    val isLoading: Boolean = false,
    val error: Throwable? = null,
    val detail: ForumDetail? = null
): UiState

@HiltViewModel(assistedFactory = ForumDetailVmFactory::class)
class ForumDetailViewModel @AssistedInject constructor(
    @Assisted val forumName: String,
    private val forumRepo: ForumRepository
) : ViewModel() {

    private val _state: MutableStateFlow<ForumDetailUiState> = MutableStateFlow(ForumDetailUiState())
    val state: StateFlow<ForumDetailUiState> = _state.asStateFlow()

    init {
        loadDetails()
    }

    fun reload() {
        if (!_state.value.isLoading) {
            loadDetails()
        }
    }

    private fun loadDetails() = viewModelScope.launch {
        _state.update { ForumDetailUiState(isLoading = true) }
        runCatching {
            forumRepo.loadForumDetail(forumName)
        }
        .onFailure { e -> _state.update { ForumDetailUiState(error = e) } }
        .onSuccess { detail ->
            _state.update { ForumDetailUiState(detail = detail) }
        }
    }

    companion object {

        @AssistedFactory
        interface ForumDetailVmFactory {
            fun create(forumName: String): ForumDetailViewModel
        }
    }
}