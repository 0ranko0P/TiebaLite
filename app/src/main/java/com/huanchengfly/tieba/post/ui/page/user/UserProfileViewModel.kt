package com.huanchengfly.tieba.post.ui.page.user

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.bumptech.glide.Glide
import com.huanchengfly.tieba.post.api.models.FollowBean
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.BaseStateViewModel
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.TbLiteExceptionHandler
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.stateInViewModel
import com.huanchengfly.tieba.post.components.imageProcessor.ImageProcessor
import com.huanchengfly.tieba.post.components.imageProcessor.RenderEffectImageProcessor
import com.huanchengfly.tieba.post.components.imageProcessor.RenderScriptImageProcessor
import com.huanchengfly.tieba.post.models.database.BlockUser
import com.huanchengfly.tieba.post.models.database.UserProfile
import com.huanchengfly.tieba.post.repository.BlockRepository
import com.huanchengfly.tieba.post.repository.UserProfileRepository
import com.huanchengfly.tieba.post.ui.models.user.PermissionList
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.utils.StringUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface UserBlockState {

    object Blacklisted: UserBlockState

    object Whitelisted: UserBlockState

    object None: UserBlockState
}

sealed interface UserProfileUiEvent : UiEvent {
    class FollowSuccess(val message: String?): UserProfileUiEvent

    class FollowFailed(val message: String) : UserProfileUiEvent

    class UnfollowFailed(val message: String) : UserProfileUiEvent

    class PermissionListException(val message: String): UserProfileUiEvent
}

@Immutable
data class UserProfileUiState(
    val isRefreshing: Boolean = true,
    val isRequestingFollow: Boolean = false,
    val isLoadingPermList: Boolean = true,
    val userProfile: UserProfile? = null,
    val permList: PermissionList? = null,
    val error: Throwable? = null,
) : UiState

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    @param:ApplicationContext val context: Context,
    private val userProfileRepo: UserProfileRepository,
    private val blockRepo: BlockRepository,
    savedStateHandle: SavedStateHandle
) : BaseStateViewModel<UserProfileUiState>() {

    private val params = savedStateHandle.toRoute<Destination.UserProfile>()
    val uid: Long = params.uid

    val blockState: StateFlow<UserBlockState> = blockRepo.observeUser(uid)
        .map {
            when (it) {
                null -> UserBlockState.None
                true -> UserBlockState.Whitelisted
                false -> UserBlockState.Blacklisted
            }
        }
        .stateInViewModel(initialValue = UserBlockState.None)

    override val uiState: StateFlow<UserProfileUiState> = combine(
        flow = _uiState,
        flow2 = userProfileRepo.observeUserProfile(uid)
    ) { state, profile ->
        // Wait transition animation
        if (profile != null && state.userProfile == null && !params.avatar.isNullOrEmpty()) {
            Glide.with(context)
                .load(StringUtil.getBigAvatarUrl(profile.portrait))
                .preload()
            delay(300)
        }
        state.copy(userProfile = profile)
    }
    .stateInViewModel(initialValue = createInitialState())

    override val currentState: UserProfileUiState
        get() = uiState.value

    override val errorHandler = TbLiteExceptionHandler(TAG) { _, e, suppressed ->
        if (suppressed && currentState.userProfile != null) {
            _uiState.update { it.copy(isRefreshing = false, isLoadingPermList = false, error = null) }
            sendUiEvent(CommonUiEvent.ToastError(e))
        } else {
            _uiState.update { it.copy(isRefreshing = false, isLoadingPermList = false, error = e) }
        }
    }

    val imageProcessor: ImageProcessor by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            RenderEffectImageProcessor()
        } else {
            RenderScriptImageProcessor(context)
        }
    }

    init {
        refreshInternal(forceRefresh = false)
    }

    override fun createInitialState(): UserProfileUiState = UserProfileUiState()

    private fun refreshInternal(forceRefresh: Boolean): Unit = launchInVM {
        _uiState.update { it.copy(isRefreshing = true, isLoadingPermList = true, error = null) }
        userProfileRepo.refreshUserProfile(uid, forceRefresh, params.recordHistory)
        val permList = getUserBlackInfoSafe()
        _uiState.update { it.copy(isRefreshing = false, isLoadingPermList = false, permList = permList, error = null) }
    }

    fun onRefresh() {
        if (!_uiState.value.isRefreshing) refreshInternal(forceRefresh = true)
    }

    /**
     * 更新用户屏蔽
     *
     * @param newState 将该用户加入白名单, 黑名单或移除
     * */
    private fun updateBlockState(newState: UserBlockState) {
        val name = currentState.userProfile?.run { nickname ?: name } ?: throw NullPointerException()
        when (newState) {
            blockState.value -> blockRepo.deleteUser(uid)

            UserBlockState.Blacklisted -> blockRepo.upsertUser(BlockUser(uid, name, false))

            UserBlockState.Whitelisted -> blockRepo.upsertUser(BlockUser(uid, name, true))

            else -> {}
        }
    }

    fun onUserBlacklisted() = updateBlockState(UserBlockState.Blacklisted)

    fun onUserWhitelisted() = updateBlockState(UserBlockState.Whitelisted)

    private suspend fun updateFollowStateInternal(follow: Boolean): FollowBean.Info? {
        val start = System.currentTimeMillis()
        val oldUiState = currentState
        val profile = oldUiState.userProfile!!
        if (oldUiState.isRequestingFollow || profile.following == follow) {
            throw IllegalStateException()
        }
        val rec = if (follow) {
            userProfileRepo.requestFollowUser(profile)
        } else {
            userProfileRepo.requestUnfollowUser(profile)
            null
        }
        if (System.currentTimeMillis() - start < 300) delay(400) // wait loading animation
        return rec
    }

    fun onFollowClicked() = launchInVM {
        runCatching {
            updateFollowStateInternal(follow = true)
        }
        .onFailure { e ->
            sendUiEvent(UserProfileUiEvent.FollowFailed(message = e.getErrorMessage()))
        }
        .onSuccess {
            val message = it!!.toastText.takeUnless { toast -> toast.isEmpty() }
            sendUiEvent(UserProfileUiEvent.FollowSuccess(message))
        }
        _uiState.update { it.copy(isRequestingFollow = false) }
    }

    fun onUnFollowClicked() = launchInVM {
        runCatching {
            updateFollowStateInternal(follow = false)
        }
        .onFailure { e ->
            sendUiEvent(UserProfileUiEvent.UnfollowFailed(message = e.getErrorMessage()))
        }
        _uiState.update { it.copy(isRequestingFollow = false) }
    }

    private suspend fun getUserBlackInfoSafe(): PermissionList? {
        return runCatching {
            userProfileRepo.getUserBlackInfo(uid)
        }
        .onFailure { e ->
            if (e !is TiebaNotLoggedInException) {
                sendUiEvent(UserProfileUiEvent.PermissionListException(e.getErrorMessage()))
            }
        }
        .getOrNull()
    }

    fun setUserBlack(permList: PermissionList) = viewModelScope.launch {
        if (currentState.permList == permList) return@launch // Double check

        _uiState.update { it.copy(isLoadingPermList = true) }
        runCatching {
            userProfileRepo.setUserBlack(uid, permList)
        }
        .onFailure { e ->
            sendUiEvent(UserProfileUiEvent.PermissionListException(e.getErrorMessage()))
            _uiState.update { it.copy(isLoadingPermList = false) }
        }
        .onSuccess {
            _uiState.update { it.copy(isLoadingPermList = false, permList = permList) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        imageProcessor?.cleanup()
    }

    companion object {
        private const val TAG = "UserProfileViewModel"
    }
}
