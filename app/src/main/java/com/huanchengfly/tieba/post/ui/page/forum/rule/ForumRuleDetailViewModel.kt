package com.huanchengfly.tieba.post.ui.page.forum.rule

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.protos.BawuRoleInfoPub
import com.huanchengfly.tieba.post.api.models.protos.ForumRule
import com.huanchengfly.tieba.post.api.models.protos.renders
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaException
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.common.PbContentRender
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.set
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ForumRuleDetailUiState: UiState {
    data object Loading: ForumRuleDetailUiState

    data class Error(val throwable: Throwable): ForumRuleDetailUiState

    data class Success(
        val title: String,
        val publishTime: String,
        val preface: String,
        val data: ImmutableList<ForumRuleItemData>,
        val author: ImmutableHolder<BawuRoleInfoPub>?
    ): ForumRuleDetailUiState
}

@HiltViewModel
class ForumRuleDetailViewModel @Inject constructor(savedStateHandle: SavedStateHandle) : ViewModel() {

    val forumId: Long = savedStateHandle.toRoute<Destination.ForumRuleDetail>().forumId

    private val _uiState: MutableStateFlow<ForumRuleDetailUiState> = MutableStateFlow(
        value = ForumRuleDetailUiState.Loading
    )
    val uiState: StateFlow<ForumRuleDetailUiState> = _uiState.asStateFlow()

    init {
        loadLatest()
    }

    fun reload() {
        if (uiState.value != ForumRuleDetailUiState.Loading) {
            loadLatest()
        }
    }

    private fun loadLatest() {
        _uiState.set { ForumRuleDetailUiState.Loading }

        viewModelScope.launch {
            val newState = TiebaApi.getInstance()
                .forumRuleDetailFlow(forumId)
                .catch { e ->
                    _uiState.update { ForumRuleDetailUiState.Error(e) }
                }
                .map { response ->
                    if (response.data_ != null) {
                        ForumRuleDetailUiState.Success(
                            title = response.data_.title,
                            publishTime = response.data_.publish_time,
                            preface = response.data_.preface,
                            data = response.data_.rules.map { ForumRuleItemData(it) }.toImmutableList(),
                            author = response.data_.bazhu?.wrapImmutable()
                        )
                    } else {
                        val err = TiebaException(response.error?.error_msg ?: "Null response")
                        ForumRuleDetailUiState.Error(throwable = err)
                    }
                }
                .firstOrNull()

            if (newState != null) {
                _uiState.update { newState }
            }
        }
    }
}

@Immutable
data class ForumRuleItemData(
    val title: String?,
    val contentRenders: ImmutableList<PbContentRender>,
) {
    constructor(forumRule: ForumRule) : this(
        title = forumRule.title.takeUnless { it.isEmpty() },
        contentRenders = forumRule.content.renders
    )
}
