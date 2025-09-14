package com.huanchengfly.tieba.post.ui.page.user

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.retrofit.exception.NoConnectivityException
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaException
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.firstOrThrow
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.components.imageProcessor.ImageProcessor
import com.huanchengfly.tieba.post.components.imageProcessor.RenderEffectImageProcessor
import com.huanchengfly.tieba.post.components.imageProcessor.RenderScriptImageProcessor
import com.huanchengfly.tieba.post.models.database.Block
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.set
import com.huanchengfly.tieba.post.utils.BlockManager
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val uid: Long = savedStateHandle.toRoute<Destination.UserProfile>().uid

    private val handler = CoroutineExceptionHandler { _, e ->
        if (e !is NoConnectivityException) {
            Log.e(TAG, "onError: ", e)
        }
        _uiState.update {
            it.copy(isRefreshing = false, disableButton = false, error = e.wrapImmutable())
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
        refresh()
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return
        _uiState.set { copy(isRefreshing = true, error = null) }

        viewModelScope.launch(handler) {
            val newState = TiebaApi.getInstance()
                .userProfileFlow(uid)
                .map {
                    val user = it.data_?.user ?: throw TiebaException("Null user data: ${it.error}")
                    UserProfileUiState(
                        block = BlockManager.findUserById(uid),
                        profile = parseUserProfile(user)
                    )
                }
                .firstOrThrow()

            _uiState.update { newState }
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
                Block(category, Block.TYPE_USER, username = uiState.value.profile?.name, uid = uid)
            )
        }
        _uiState.update { it.copy(block = newBlock) }
    }

    fun onBlackListClicked() = updateBlockCategory(Block.CATEGORY_BLACK_LIST)

    fun onWhiteListClicked() = updateBlockCategory(Block.CATEGORY_WHITE_LIST)

    private fun setupFollowState(following: Boolean) {
        val state = _uiState.value
        val profile = state.profile

        // Adjust fans num if needed
        var fansChanges = 0
        if (profile?.following != following) fansChanges = if (following) 1 else -1
        val newProfile = profile?.copy(following = following, fans = profile.fans + fansChanges)

        _uiState.update { it.copy(disableButton = !state.disableButton, profile = newProfile) }
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
                    context.toastShort(R.string.toast_like_failed, it.getErrorMessage())
                }
                .firstOrNull() ?: return@launch

            setupFollowState(following = true)
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
                    context.toastShort(R.string.toast_unlike_failed, it.getErrorMessage())
                }
                .firstOrNull() ?: return@launch

            setupFollowState(following = false)
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
                name = user.nameShow.trim(),
                userName = user.name.takeUnless { it == user.nameShow || it.length <= 1 }?.trim()?.let { "($it)" },
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
                threadNum = user.thread_num,
                postNum = user.post_num,
                forumNum = user.my_like_num,
                followNum = user.concern_num.getShortNumString(),
                fans = user.fans_num,
                agreeNum = user.total_agree_num.getShortNumString(),
                bazuDesc = user.bazhu_grade?.desc?.takeUnless { it.isEmpty() },
                newGod = user.new_god_data?.takeUnless { it.status <= 0 }?.field_name,
                privateForum = user.privSets?.like != 1,
                isOfficial = user.is_guanfang == 1
            )
    }
}

/**
 * Data class that represents [User] in UserProfilePage
 *
 * @param following [User.has_concerned] to Boolean
 * @param followNum formatted [User.concern_num]
 * @param agreeNum formatted [User.total_agree_num]
 * @param privateForum 隐藏关注的吧
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
    val threadNum: Int,
    val postNum: Int,
    val forumNum: Int,
    val followNum: String,
    val fans: Int,
    val agreeNum: String,
    val bazuDesc: String?,
    val newGod: String?,
    val privateForum: Boolean,
    val isOfficial: Boolean
)

data class UserProfileUiState(
    val isRefreshing: Boolean = false,
    val error: ImmutableHolder<Throwable>? = null,

    val block: Block? = null,
    val disableButton: Boolean = false,
    val profile: UserProfile? = null,
) : UiState