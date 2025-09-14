package com.huanchengfly.tieba.post.ui.page.forum.rule

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.repository.ForumRepository
import com.huanchengfly.tieba.post.ui.models.forum.ForumRule
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.set
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
class ForumRuleDetailUiState(
    val isLoading: Boolean = false,
    val error: Throwable? = null,
    val data: ForumRule? = null
): UiState

@HiltViewModel
class ForumRuleDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val forumRepo: ForumRepository
) : ViewModel() {

    val forumId: Long = savedStateHandle.toRoute<Destination.ForumRuleDetail>().forumId

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
}

