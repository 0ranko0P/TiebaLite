package com.huanchengfly.tieba.post.ui.page.user.fans

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Immutable
import com.huanchengfly.tieba.post.arch.BaseStateViewModel
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.stateInViewModel
import com.huanchengfly.tieba.post.core.network.exception.getErrorMessage
import com.huanchengfly.tieba.post.core.network.model.FanListBean.FanUserBean
import com.huanchengfly.tieba.post.repository.UserProfileRepository
import com.huanchengfly.tieba.post.ui.models.user.ConcernType
import com.huanchengfly.tieba.post.ui.models.user.FanUser
import com.huanchengfly.tieba.post.ui.page.user.fans.UserFansViewModel.Companion.UserFansVmFactory
import com.huanchengfly.tieba.post.ui.page.user.followlist.FollowListViewModel.Companion.FollowListFilter
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.extension.set
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
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

sealed interface UserFansUiEvent : UiEvent {
    data class FollowFailed(val message: String) : UserFansUiEvent

    data class UnfollowFailed(val message: String) : UserFansUiEvent
}

@Immutable
data class UserFansUiState(
    val filter: FollowListFilter = FollowListFilter.All,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: Throwable? = null,
    val currentPage: Int = 1,
    val fansCount: Int = 0,
    val hasMore: Boolean = false,
    val tipsText: String? = null,
    val users: List<FanUser> = emptyList(),
) : UiState

@HiltViewModel(assistedFactory = UserFansVmFactory::class)
class UserFansViewModel @AssistedInject constructor(
    @param:ApplicationContext val context: Context,
    @Assisted val uid: Long,
    private val userProfileRepo: UserProfileRepository,
) : BaseStateViewModel<UserFansUiState>() {

    override val errorHandler = CoroutineExceptionHandler { _, e ->
        Log.e(TAG, "onError: ", e)
        _uiState.update { it.copy(isRefreshing = false, isLoadingMore = false, error = e) }
    }

    val filteredUsers: StateFlow<List<FanUser>> = _uiState
        .distinctUntilChangedBy { Objects.hash(it.filter, it.users) }
        .map {
            when (it.filter) {
                FollowListFilter.All -> it.users
                FollowListFilter.Mutual -> it.users.filter { u -> u.concernType == ConcernType.MUTUAL }
            }
        }
        .flowOn(Dispatchers.Default)
        .stateInViewModel(initialValue = emptyList())

    override fun createInitialState(): UserFansUiState = UserFansUiState(isRefreshing = true)

    init {
        refreshInternal()
    }

    private fun refreshInternal() {
        _uiState.set { createInitialState() }
        launchInVM {
            val result = userProfileRepo.loadUserFanList(uid, page = 1)
            val users = result.userList.mapToUiModel()
            _uiState.set {
                UserFansUiState(
                    currentPage = result.page.currentPage,
                    fansCount = result.page.totalCount,
                    hasMore = result.page.hasMore == 1,
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
                userProfileRepo.loadUserFanList(uid, page = oldState.currentPage + 1)
            }
            .onFailure { e ->
                _uiState.update { it.copy(isRefreshing = false, isLoadingMore = false, error = null) }
                sendUiEvent(CommonUiEvent.ToastError(e))
            }
            .onSuccess { result ->
                val list = result.userList.mapToUiModel()
                val users = withContext(Dispatchers.Default) {
                    (oldState.users + list).distinctBy { it.uid }
                }
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        currentPage = oldState.currentPage + 1,
                        fansCount = result.page.totalCount,
                        hasMore = result.page.hasMore == 1,
                        users = users
                    )
                }
            }
        }
    }

    fun onFilterChanged(filter: FollowListFilter) = _uiState.update { it.copy(filter = filter) }

    private suspend fun <T> updateConcertTypeInternal(
        user: FanUser,
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

    fun onFollowClicked(user: FanUser) = launchInVM {
        val newConcernType = if (user.concernType == ConcernType.FANS) ConcernType.MUTUAL else ConcernType.FOLLOWING
        updateConcertTypeInternal(user, newConcernType) {
            userProfileRepo.requestFollowUser(uid = user.uid, portrait = user.portrait)
        }
        .onFailure { e ->
            sendUiEvent(UserFansUiEvent.FollowFailed(message = e.getErrorMessage()))
        }
        .onSuccess {
            _uiState.update { it.copy(fansCount = it.fansCount + 1) }
        }
    }

    fun onUnfollowClicked(user: FanUser) = launchInVM {
        val newConcernType = if (user.concernType == ConcernType.MUTUAL) ConcernType.FANS else ConcernType.NONE
        updateConcertTypeInternal(user, newConcernType) {
            userProfileRepo.requestUnfollowUser(uid = user.uid, portrait = user.portrait)
        }
        .onFailure { e ->
            sendUiEvent(UserFansUiEvent.UnfollowFailed(message = e.getErrorMessage()))
        }
        .onSuccess {
            _uiState.update { it.copy(fansCount = it.fansCount - 1) }
        }
    }

    companion object {
        private const val TAG = "UserFansViewModel"

        @AssistedFactory
        interface UserFansVmFactory {
            fun create(uid: Long): UserFansViewModel
        }

        private suspend fun List<FanUserBean>.mapToUiModel(): List<FanUser> {
            if (isEmpty()) return emptyList()

            return withContext(Dispatchers.Default) {
                map {
                    FanUser(
                        uid = it.id,
                        avatar = StringUtil.getAvatarUrl(it.portrait),
                        displayName = StringUtil.getUserNameString(
                            showBoth = true,
                            username = it.name.orEmpty(),
                            nickname = it.nameShow
                        ),
                        // 为了使列表更加紧凑, 关注时间与简介只显示其中一个
                        intro = it.intro?.takeUnless { _ -> it.followTime > 0 },
                        portrait = it.portrait!!,
                        followTime = it.followTime,
                        concernType = it.hasConcerned,
                    )
                }
            }
        }

        /**
         * Update [ConcernType] of target [FanUser] in this list
         * */
        private suspend fun List<FanUser>.updateConcernType(
            uid: Long,
            @ConcernType type: Int
        ): List<FanUser> = withContext(Dispatchers.Default) {
            map {
                if (it.uid != uid) it else it.copy(concernType = type)
            }
        }
    }
}
