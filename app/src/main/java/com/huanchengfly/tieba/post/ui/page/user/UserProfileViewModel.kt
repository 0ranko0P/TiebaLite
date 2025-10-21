package com.huanchengfly.tieba.post.ui.page.user

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.api.retrofit.exception.NoConnectivityException
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.components.imageProcessor.ImageProcessor
import com.huanchengfly.tieba.post.components.imageProcessor.RenderEffectImageProcessor
import com.huanchengfly.tieba.post.components.imageProcessor.RenderScriptImageProcessor
import com.huanchengfly.tieba.post.models.database.BlockUser
import com.huanchengfly.tieba.post.repository.BlockRepository
import com.huanchengfly.tieba.post.repository.UserProfileRepository
import com.huanchengfly.tieba.post.ui.models.user.UserProfile
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.set
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

    class FollowFailed(val e: Throwable) : UserProfileUiEvent

    class UnfollowFailed(val e: Throwable) : UserProfileUiEvent
}

@Immutable
data class UserProfileUiState(
    val isRefreshing: Boolean = false,
    val isRequestingFollow: Boolean = false,
    val error: Throwable? = null,
    val profile: UserProfile? = null,
) : UiState

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    private val userProfileRepo: UserProfileRepository,
    private val blockRepo: BlockRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val uid: Long = savedStateHandle.toRoute<Destination.UserProfile>().uid

    val blockState: StateFlow<UserBlockState> = blockRepo.observeUser(uid)
        .map {
            when (it) {
                null -> UserBlockState.None
                true -> UserBlockState.Whitelisted
                false -> UserBlockState.Blacklisted
            }
        }
        .stateIn(viewModelScope, started = SharingStarted.WhileSubscribed(5_000), UserBlockState.None)

    private val handler = CoroutineExceptionHandler { _, e ->
        if (e !is NoConnectivityException) {
            Log.e(TAG, "onError: ", e)
        }
        _uiState.update {
            it.copy(isRefreshing = false, error = e)
        }
    }

    // Null when power saver in on
    val imageProcessor: ImageProcessor? by lazy {
        if ((context as App).powerManager.isPowerSaveMode) return@lazy null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            RenderEffectImageProcessor()
        } else {
            RenderScriptImageProcessor(context)
        }
    }

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    init {
        refreshInternal(cached = true)
    }

    private fun refreshInternal(cached: Boolean) = viewModelScope.launch {
        _uiState.set { UserProfileUiState(isRefreshing = true) }

        viewModelScope.launch(handler) {
            val profile = userProfileRepo.loadUserProfile(uid, cached)
            _uiState.update { UserProfileUiState(profile = profile) }
        }
    }

    fun onRefresh() {
        if (!_uiState.value.isRefreshing) refreshInternal(cached = false)
    }

    /**
     * 更新用户屏蔽
     *
     * @param newState 将该用户加入白名单, 黑名单或移除
     * */
    private fun updateBlockState(newState: UserBlockState) {
        val name = _uiState.value.profile?.name ?: throw NullPointerException()
        when (newState) {
            UserBlockState.Blacklisted -> blockRepo.upsertUser(BlockUser(uid, name, false))

            UserBlockState.Whitelisted -> blockRepo.upsertUser(BlockUser(uid, name, true))

            UserBlockState.None -> blockRepo.deleteUser(uid)
        }
    }

    fun onUserBlacklisted() = updateBlockState(UserBlockState.Blacklisted)

    fun onUserWhitelisted() = updateBlockState(UserBlockState.Whitelisted)

    private fun updateRequestingFollowState(following: Boolean, requesting: Boolean) {
        _uiState.update {
            val profile = it.profile
            val newProfile = if (profile?.following != following) { // adjust fans num
                profile?.copy(following = following, fans = profile.fans + if (following) 1 else -1)
            } else {
                profile
            }
            it.copy(isRequestingFollow = requesting, profile = newProfile)
        }
    }

    fun onFollowClicked() {
        val oldUiState = _uiState.value
        val profile = oldUiState.profile ?: return
        if (oldUiState.isRequestingFollow || profile.following) return

        updateRequestingFollowState(following = true, requesting = true)
        viewModelScope.launch(handler) {
            runCatching {
                userProfileRepo.requestFollowUser(profile)
            }
            .onFailure { e ->
                emitGlobalEvent(UserProfileUiEvent.FollowFailed(e))
                updateRequestingFollowState(following = false, requesting = false) // reset unfollowed
            }
           .onSuccess {
               val message = it.toastText.takeUnless { toast -> toast.isEmpty() }
               emitGlobalEvent(UserProfileUiEvent.FollowSuccess(message))
               updateRequestingFollowState(following = true, requesting = false)
           }
        }
    }

    fun onUnFollowClicked() {
        val oldUiState = _uiState.value
        val profile = oldUiState.profile ?: return
        if (oldUiState.isRequestingFollow || !profile.following) return

        updateRequestingFollowState(following = false, requesting = true)
        viewModelScope.launch(handler) {
            runCatching {
                userProfileRepo.requestUnfollowUser(profile)
            }
            .onFailure { e ->
                emitGlobalEvent(UserProfileUiEvent.UnfollowFailed(e))
                updateRequestingFollowState(following = true, requesting = false) // reset followed
            }
            .onSuccess {
                updateRequestingFollowState(following = false, requesting = false)
            }
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
