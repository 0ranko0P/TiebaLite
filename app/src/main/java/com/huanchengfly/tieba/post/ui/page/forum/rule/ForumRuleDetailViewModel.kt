package com.huanchengfly.tieba.post.ui.page.forum.rule

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForumRuleDetailViewModel @Inject constructor(savedStateHandle: SavedStateHandle) : ViewModel() {

    val forumId: Long = savedStateHandle.toRoute<Destination.ForumRuleDetail>().forumId

    private val handler = CoroutineExceptionHandler { _, e ->
        Log.e(TAG, "onError: ", e)
        uiState = ForumRuleDetailUiState.Error(e)
    }

    var uiState by mutableStateOf<ForumRuleDetailUiState?>(null)
        private set

    init {
        reload()
    }

    fun reload() {
        if (uiState == ForumRuleDetailUiState.Loading) return
        uiState = ForumRuleDetailUiState.Loading

        viewModelScope.launch(handler) {
            TiebaApi.getInstance()
                .forumRuleDetailFlow(forumId)
                .collect { response ->
                    uiState = if (response.data_ != null) {
                        ForumRuleDetailUiState.Success(
                            title = response.data_.title,
                            publishTime = response.data_.publish_time,
                            preface = response.data_.preface,
                            data = response.data_.rules.map { ForumRuleItemData(it) }.toImmutableList(),
                            author = response.data_.bazhu?.wrapImmutable()
                        )
                    } else {
                        ForumRuleDetailUiState.Error(
                            TiebaException(response.error?.error_msg ?: "Null response")
                        )
                    }
                }
        }
    }

    companion object {
        private const val TAG = "ForumRuleDetailViewMode"

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
    }
}

@Immutable
data class ForumRuleItemData(
    val title: String,
    val contentRenders: ImmutableList<PbContentRender>,
) {
    constructor(forumRule: ForumRule) : this(forumRule.title, forumRule.content.renders)
}