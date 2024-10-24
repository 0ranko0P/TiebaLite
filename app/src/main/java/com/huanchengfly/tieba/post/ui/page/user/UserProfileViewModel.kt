package com.huanchengfly.tieba.post.ui.page.user

import android.os.Build
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaException
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.components.imageProcessor.ImageProcessor
import com.huanchengfly.tieba.post.components.imageProcessor.RenderEffectImageProcessor
import com.huanchengfly.tieba.post.components.imageProcessor.RenderScriptImageProcessor
import com.huanchengfly.tieba.post.models.database.Block
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.utils.BlockManager
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor(savedStateHandle: SavedStateHandle) : ViewModel() {

    val uid: Long = savedStateHandle.toRoute<Destination.UserProfile>().uid

    private val handler = CoroutineExceptionHandler { _, e ->
        Log.e(TAG, "onError: ", e)
        _uiState.value = _uiState.value.copy(
            isRefreshing = false, disableButton = false, error = e.wrapImmutable()
        )
    }

    // Null when power saver in on
    val imageProcessor: ImageProcessor? by lazy {
        if (App.INSTANCE.batterySaver.isPowerSaveMode) return@lazy null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            RenderEffectImageProcessor()
        } else {
            RenderScriptImageProcessor(App.INSTANCE)
        }
    }

    private val _uiState: MutableState<UserProfileUiState> = mutableStateOf(UserProfileUiState())
    val uiState: State<UserProfileUiState>
        get() = _uiState

    val isRefreshing: Boolean
        get() = _uiState.value.isRefreshing

    init {
        refresh()
    }

    fun refresh() {
        if (isRefreshing) return
        _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)

        viewModelScope.launch(handler) {
            TiebaApi.getInstance()
                .userProfileFlow(uid)
                .collect {
                    val user = it.data_?.user ?: throw TiebaException("Null user data: ${it.error}")
                    val block = BlockManager.findUserById(uid)
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        disableButton = false,
                        error = null,
                        block = block,
                        profile = parseUserProfile(user)
                    )
                }
        }
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
                Block(category, Block.TYPE_USER, username = uiState.value.profile?.userName, uid = uid)
            )
        }
        _uiState.value = _uiState.value.copy(block = newBlock)
    }

    fun onBlackListClicked() = updateBlockCategory(Block.CATEGORY_BLACK_LIST)

    fun onWhiteListClicked() = updateBlockCategory(Block.CATEGORY_WHITE_LIST)

    private fun setupFollowState(following: Boolean) {
        val state = _uiState.value
        val profile = state.profile

        // Adjust fans num if needed
        var fansChanges = 0
        if (profile?.following != following) fansChanges = if (following) 1 else -1

        _uiState.value = state.copy(
            disableButton = !state.disableButton,
            profile = profile?.copy(following = following, fans = profile.fans + fansChanges)
        )
    }

    fun onFollowClicked(tbs: String) {
        if (_uiState.value.disableButton) return
        setupFollowState(following = true)
        viewModelScope.launch(handler) {
            val portrait = _uiState.value.profile?.portrait ?: throw NullPointerException("Uninitialized user")
            TiebaApi.getInstance()
                .followFlow(portrait, tbs)
                .catch {
                    setupFollowState(following = false)
                    App.INSTANCE.toastShort(R.string.toast_like_failed, it.getErrorMessage())
                }
                .collect {
                    setupFollowState(following = true)
                }
        }
    }

    fun onUnFollowClicked(tbs: String) {
        if (_uiState.value.disableButton) return
        setupFollowState(following = false)
        viewModelScope.launch(handler) {
            val portrait = _uiState.value.profile?.portrait ?: throw NullPointerException("Uninitialized user")
            TiebaApi.getInstance()
                .unfollowFlow(portrait, tbs)
                .catch {
                    setupFollowState(following = true)
                    App.INSTANCE.toastShort(R.string.toast_unlike_failed, it.getErrorMessage())
                }.collect {
                    setupFollowState(following = false)
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        imageProcessor?.cleanup()
    }

    companion object {
        private const val TAG = "UserProfileViewModel"

        fun parseUserProfile(user: User): UserProfile =
            UserProfile(
                uid = user.id,
                portrait = user.portrait,
                name = user.nameShow,
                userName = user.name.takeUnless { it == user.nameShow || it.length <= 1 },
                tiebaUid = user.tieba_uid,
                intro = user.intro.takeUnless { it.isEmpty() },
                sex = when (user.sex) {
                    1 -> "♂"
                    2 -> "♀"
                    else -> "?"
                },
                tbAge = user.tb_age,
                address = user.ip_address.takeUnless { it.isEmpty() },
                following = user.has_concerned != 0,
                threadNum = user.thread_num.getShortNumString(),
                postNum = user.post_num.getShortNumString(),
                forumNum = user.my_like_num.toString(),
                followNum = user.concern_num.getShortNumString(),
                fans = user.fans_num,
                agreeNum = user.total_agree_num.getShortNumString(),
                bazuDesc = user.bazhu_grade?.desc,
                newGod = user.new_god_data?.takeUnless { it.status <= 0 }?.field_name
            )
    }
}

/**
 * Data class that represents [User] in UserProfilePage
 *
 * @param following [User.has_concerned] to Boolean
 * @param threadNum formatted [User.thread_num]
 * @param postNum formatted [User.post_num]
 * @param forumNum formatted [User.my_like_num]
 * @param followNum formatted [User.concern_num]
 * @param agreeNum formatted [User.total_agree_num]
 * */
data class UserProfile(
    val uid: Long,
    val portrait: String,
    val name: String,
    val userName: String?,
    val tiebaUid: String,
    val intro: String?,
    val sex: String,
    val tbAge: String,
    val address: String?,
    val following: Boolean,
    val threadNum: String,
    val postNum: String,
    val forumNum: String,
    val followNum: String,
    val fans: Int,
    val agreeNum: String,
    val bazuDesc: String?,
    val newGod: String?
)

data class UserProfileUiState(
    val isRefreshing: Boolean = false,
    val error: ImmutableHolder<Throwable>? = null,

    val block: Block? = null,
    val disableButton: Boolean = false,
    val profile: UserProfile? = null,
) : UiState