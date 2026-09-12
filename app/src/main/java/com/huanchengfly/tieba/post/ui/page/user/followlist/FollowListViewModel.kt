package com.huanchengfly.tieba.post.ui.page.user.followlist

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.huanchengfly.tieba.post.arch.BaseStateViewModel
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.stateInViewModel
import com.huanchengfly.tieba.post.core.network.exception.getErrorMessage
import com.huanchengfly.tieba.post.core.network.model.FollowListBean.FollowUserBean
import com.huanchengfly.tieba.post.repository.UserProfileRepository
import com.huanchengfly.tieba.post.ui.models.user.ConcernType
import com.huanchengfly.tieba.post.ui.models.user.FollowUser
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.user.followlist.FollowListViewModel.Companion.FollowListFilter
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.extension.set
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.util.Objects
import javax.inject.Inject

sealed interface FollowListUiEvent : UiEvent {
    data class FollowFailed(val message: String) : FollowListUiEvent

    data class UnfollowFailed(val message: String) : FollowListUiEvent
}

@Immutable
data class FollowListUiState(
    val filter: FollowListFilter = FollowListFilter.All,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: Throwable? = null,
    val currentPage: Int = 1,
    val hasMore: Boolean = false,
    val totalFollowNum: Int = 0,
    val tipsText: String? = null,
    val users: List<FollowUser> = emptyList(),
) : UiState

@HiltViewModel
class FollowListViewModel @Inject constructor(
    @param:ApplicationContext val context: Context,
    private val userProfileRepo: UserProfileRepository,
    savedStateHandle: SavedStateHandle
) : BaseStateViewModel<FollowListUiState>() {

    override val errorHandler = CoroutineExceptionHandler { _, e ->
        Log.e(TAG, "onError: ", e)
        _uiState.update { it.copy(isRefreshing = false, isLoadingMore = false, error = e) }
    }

    private val param = savedStateHandle.toRoute<Destination.UserFollowList>()
    val uid: Long = param.uid

    val filteredUsers: StateFlow<List<FollowUser>> = _uiState
        .distinctUntilChangedBy { Objects.hash(it.filter, it.users) }
        .map {
            when (it.filter) {
                FollowListFilter.All -> it.users
                FollowListFilter.Mutual -> it.users.filter { u -> u.concernType == ConcernType.MUTUAL }
            }
        }
        .flowOn(Dispatchers.Default)
        .stateInViewModel(initialValue = emptyList())

    override fun createInitialState(): FollowListUiState = FollowListUiState(isRefreshing = true)

    init {
        refreshInternal()
    }

    private fun refreshInternal() {
        _uiState.set { createInitialState() }
        launchInVM {
            val result = userProfileRepo.loadUserFollowList(uid, page = 1)
            val users = result.followList.mapToUiModel()
            _uiState.set {
                FollowListUiState(
                    currentPage = 1,
                    hasMore = result.hasMore == 1,
                    totalFollowNum = result.totalFollowNum,
                    tipsText = result.tipsText,
                    users = users,
                )
            }
        }
    }

    fun onRefresh() {
        if (!currentState.isRefreshing) refreshInternal()
    }

    fun onLoadMore() {
        val oldState = currentState
        if (oldState.isLoadingMore || oldState.isRefreshing) return

        _uiState.update { it.copy(isLoadingMore = true) }
        launchInVM {
            runCatching {
                userProfileRepo.loadUserFollowList(uid, page = oldState.currentPage + 1)
            }
            .onFailure { e ->
                _uiState.update { it.copy(isRefreshing = false, isLoadingMore = false, error = null) }
                sendUiEvent(CommonUiEvent.ToastError(e))
            }
            .onSuccess { result ->
                val followList = result.followList.mapToUiModel()
                val users = withContext(Dispatchers.Default) {
                    (oldState.users + followList).distinctBy { it.uid }
                }
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        currentPage = result.pageNum,
                        hasMore = result.hasMore == 1,
                        users = users
                    )
                }
            }
        }
    }

    fun onFilterChanged(filter: FollowListFilter) = _uiState.update { it.copy(filter = filter) }

    private suspend fun <T> updateConcertTypeInternal(
        user: FollowUser,
        newConcernType: Int,
        block: suspend () -> T,
    ): Result<T> {
        val start = System.currentTimeMillis()
        val oldConcernType = user.concernType
        _uiState.update {
            it.copy(users = it.users.updateConcernType(uid = user.uid, type = ConcernType.UPDATING))
        }
        return runCatching { block() }
            .onFailure { e ->
                Log.w(TAG, "onUpdateConcertTypeInternal", e)
                _uiState.update {
                    it.copy(users = it.users.updateConcernType(uid = user.uid, type = oldConcernType))
                }
            }
            .onSuccess {
                _uiState.update {
                    it.copy(users = it.users.updateConcernType(uid = user.uid, type = newConcernType))
                }
                val cost = System.currentTimeMillis() - start
                Log.i(TAG, "onUpdateConcertTypeInternal: Type $oldConcernType -> $newConcernType, cost ${cost}ms")
            }
    }

    fun onFollowClicked(user: FollowUser) = launchInVM {
        val newConcernType = if (user.concernType == ConcernType.FANS) ConcernType.MUTUAL else ConcernType.FOLLOWING
        updateConcertTypeInternal(user, newConcernType) {
            userProfileRepo.requestFollowUser(uid = user.uid, portrait = user.portrait)
        }
        .onFailure { e ->
            sendUiEvent(FollowListUiEvent.FollowFailed(message = e.getErrorMessage()))
        }
        .onSuccess {
            _uiState.update { it.copy(totalFollowNum = it.totalFollowNum + 1) }
        }
    }

    fun onUnfollowClicked(user: FollowUser) = launchInVM {
        val newConcernType = if (user.concernType == ConcernType.MUTUAL) ConcernType.FANS else ConcernType.NONE
        updateConcertTypeInternal(user, newConcernType) {
            userProfileRepo.requestUnfollowUser(uid = user.uid, portrait = user.portrait)
        }
        .onFailure { e ->
            sendUiEvent(FollowListUiEvent.UnfollowFailed(message = e.getErrorMessage()))
        }
        .onSuccess {
            _uiState.update { it.copy(totalFollowNum = it.totalFollowNum - 1) }
        }
    }

    companion object {
        private const val TAG = "FollowListViewModel"

        enum class FollowListFilter { All, Mutual }

        private suspend fun List<FollowUserBean>.mapToUiModel(): List<FollowUser> = if (isNotEmpty()) {
            withContext(Dispatchers.Default) {
                map {
                    FollowUser(
                        uid = it.id,
                        avatar = StringUtil.getAvatarUrl(it.portrait),
                        displayName = StringUtil.getUserNameString(
                            showBoth = true,
                            username = it.name.orEmpty(),
                            nickname = it.nameShow
                        ),
                        portrait = it.portrait!!,
                        intro = it.intro?.takeUnless { intro -> intro.isEmpty() || intro.isBlank() },
                        concernType = it.hasConcerned,
                    )
                }
            }
        } else {
            emptyList()
        }

        /**
         * Update [ConcernType] of target [FollowUser] in this list
         * */
        private suspend fun List<FollowUser>.updateConcernType(
            uid: Long,
            @ConcernType type: Int
        ): List<FollowUser> = withContext(Dispatchers.Default) {
            map {
                if (it.uid != uid) it else it.copy(concernType = type)
            }
        }
    }
}
