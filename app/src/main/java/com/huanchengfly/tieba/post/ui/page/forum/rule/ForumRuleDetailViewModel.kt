package com.huanchengfly.tieba.post.ui.page.forum.rule

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.repository.ForumRepository
import com.huanchengfly.tieba.post.ui.models.forum.ForumRule
import com.huanchengfly.tieba.post.utils.extension.set
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
class ForumRuleDetailUiState(
    val isLoading: Boolean = false,
    val error: Throwable? = null,
    val data: ForumRule? = null
): UiState

@HiltViewModel(assistedFactory = ForumRuleDetailViewModel.Companion.Factory::class)
class ForumRuleDetailViewModel @AssistedInject constructor(
    @Assisted val forumId: Long,
    private val forumRepo: ForumRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForumRuleDetailUiState(isLoading = true))
    val uiState: StateFlow<ForumRuleDetailUiState> = _uiState.asStateFlow()

    init {
        loadLatest()
    }

    fun reload() {
        if (!uiState.value.isLoading) {
            loadLatest()
        }
    }

    private fun loadLatest() {
        _uiState.set { ForumRuleDetailUiState(isLoading = true) }

        viewModelScope.launch {
            runCatching {
                forumRepo.loadForumRule(forumId)
            }
            .onFailure { e -> _uiState.update { ForumRuleDetailUiState(error = e) } }
            .onSuccess { rule ->
                _uiState.update { ForumRuleDetailUiState(data = rule) }
            }
        }
    }

    companion object {

        @AssistedFactory
        interface Factory {
            fun create(forumId: Long): ForumRuleDetailViewModel
        }
    }
}

