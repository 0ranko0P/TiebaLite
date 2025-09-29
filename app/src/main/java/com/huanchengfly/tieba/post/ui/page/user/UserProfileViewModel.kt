package com.huanchengfly.tieba.post.ui.page.user

import android.content.Context
import android.os.Build
import android.util.Log
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
import com.huanchengfly.tieba.post.models.database.Block
import com.huanchengfly.tieba.post.repository.UserProfileRepository
import com.huanchengfly.tieba.post.ui.models.user.UserProfile
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.set
import com.huanchengfly.tieba.post.utils.BlockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface UserProfileUiEvent : UiEvent {
    class FollowSuccess(val message: String?): UserProfileUiEvent

    class FollowFailed(val e: Throwable) : UserProfileUiEvent

    class UnfollowFailed(val e: Throwable) : UserProfileUiEvent
}

data class UserProfileUiState(
    val isRefreshing: Boolean = false,
    val isRequestingFollow: Boolean = false,
    val error: Throwable? = null,
    val block: Block? = null,
    val profile: UserProfile? = null,
) : UiState

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    private val userProfileRepo: UserProfileRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val uid: Long = savedStateHandle.toRoute<Destination.UserProfile>().uid

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
            val block = BlockManager.findUserById(uid)
            _uiState.update { UserProfileUiState(block = block, profile = profile) }
        }
    }

    fun onRefresh() {
        if (!_uiState.value.isRefreshing) refreshInternal(cached = false)
    }

    private fun updateBlockCategory(category: Int) = viewModelScope.launch(handler) {
        val block = _uiState.value.block
        // Remove if Blacklisted or Whitelisted
        val newBlock = if (block?.category == category) {
            BlockManager.removeBlock(block.id)
            null
        } else if (block != null) {
            BlockManager.saveOrUpdateBlock(block.clone(category = category))
        } else {
            BlockManager.saveOrUpdateBlock(
                Block(category, Block.TYPE_USER, username = uiState.value.profile?.name, uid = uid)
            )
        }
        _uiState.update { it.copy(block = newBlock) }
    }

    fun onBlackListClicked() = updateBlockCategory(Block.CATEGORY_BLACK_LIST)

    fun onWhiteListClicked() = updateBlockCategory(Block.CATEGORY_WHITE_LIST)

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
