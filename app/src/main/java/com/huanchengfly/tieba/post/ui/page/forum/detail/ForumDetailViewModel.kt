package com.huanchengfly.tieba.post.ui.page.forum.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.repository.ForumRepository
import com.huanchengfly.tieba.post.ui.models.forum.ForumDetail
import com.huanchengfly.tieba.post.ui.page.Destination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForumDetailUiState(
    val isLoading: Boolean = false,
    val error: Throwable? = null,
    val detail: ForumDetail? = null
): UiState

@HiltViewModel
class ForumDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val forumRepo: ForumRepository
) : ViewModel() {

    val params = savedStateHandle.toRoute<Destination.ForumDetail>()

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
            forumRepo.loadForumDetail(forumName = params.forumName)
        }
        .onFailure { e -> _state.update { ForumDetailUiState(error = e) } }
        .onSuccess { detail ->
            _state.update { ForumDetailUiState(detail = detail) }
        }
    }
}