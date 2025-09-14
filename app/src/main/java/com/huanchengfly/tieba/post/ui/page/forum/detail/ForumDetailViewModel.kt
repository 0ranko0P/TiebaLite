package com.huanchengfly.tieba.post.ui.page.forum.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.CommonResponse
import com.huanchengfly.tieba.post.api.models.protos.plainText
import com.huanchengfly.tieba.post.api.retrofit.exception.NoConnectivityException
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaApiException
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.Destination.Companion.parcelableListType
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.set
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.reflect.typeOf

sealed class ForumDetailUiState {
    object Loading: ForumDetailUiState()

    class Success(
        val forumId: Long,
        val intro: String,
        val name: String,
        val slogan: String,
        val memberCount: Int,
        val threadCount: Int,
        val managers: List<ManagerData>
    ): ForumDetailUiState()

    class Error(val error: Throwable) : ForumDetailUiState()
}

@HiltViewModel
class ForumDetailViewModel @Inject constructor(savedStateHandle: SavedStateHandle) : ViewModel() {
    val params = savedStateHandle.toRoute<Destination.ForumDetail>(
        typeMap = mapOf(typeOf<ArrayList<ManagerData>>() to parcelableListType<ManagerData>())
    )

    val forumId = params.forumId

    private val _state: MutableStateFlow<ForumDetailUiState> = MutableStateFlow(
        value = ForumDetailUiState.Loading
    )
    val state: StateFlow<ForumDetailUiState> = _state.asStateFlow()

    init {
        loadDetails(forumId)
    }

    fun reload() {
        if (state != ForumDetailUiState.Loading) {
            loadDetails(forumId)
        }
    }

    private fun loadDetails(forumId: Long) = viewModelScope.launch {
        _state.set { ForumDetailUiState.Loading }
        val newState = TiebaApi.getInstance()
            .getForumDetailFlow(forumId)
            .catch {
                _state.set { ForumDetailUiState.Error(it) }
            }
            .map {
                val forumInfo = it.data_?.forum_info
                if (forumInfo == null) {
                    throw TiebaApiException(CommonResponse(0, it.error?.error_msg.orEmpty()))
                }

                ForumDetailUiState.Success(
                    forumId = forumId,
                    intro = forumInfo.content.plainText,
                    name = params.forumName,
                    slogan = forumInfo.slogan,
                    memberCount = forumInfo.member_count,
                    threadCount = params.threadCount,
                    managers = params.managers
                )
            }
            .firstOrNull() ?: ForumDetailUiState.Error(NoConnectivityException())

        _state.update { newState }
    }
}