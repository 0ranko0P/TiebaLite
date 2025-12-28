package com.huanchengfly.tieba.post.ui.page.forum

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Stable
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.huanchengfly.tieba.post.api.models.SignResultBean
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.BaseStateViewModel
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.TbLiteExceptionHandler
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.emitGlobalEventSuspend
import com.huanchengfly.tieba.post.arch.stateInViewModel
import com.huanchengfly.tieba.post.models.database.ForumHistory
import com.huanchengfly.tieba.post.repository.ForumRepository
import com.huanchengfly.tieba.post.repository.HistoryRepository
import com.huanchengfly.tieba.post.ui.models.forum.ForumData
import com.huanchengfly.tieba.post.ui.models.settings.ForumFAB
import com.huanchengfly.tieba.post.ui.models.settings.ForumSortType
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.TB_LITE_DOMAIN
import com.huanchengfly.tieba.post.ui.page.forum.threadlist.ForumThreadListUiEvent
import com.huanchengfly.tieba.post.ui.page.forum.threadlist.ForumType
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.set
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.requestPinShortcut
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@Stable
@HiltViewModel
class ForumViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    private val forumRepo: ForumRepository,
    private val historyRepo: HistoryRepository,
    savedStateHandle: SavedStateHandle
) : BaseStateViewModel<ForumUiState>() {

    private val param = savedStateHandle.toRoute<Destination.Forum>()
    private val forumName: String = param.forumName

    private var historyRecorded = false

    override val errorHandler = TbLiteExceptionHandler(TAG) { _, e, _ ->
        _uiState.update { it.copy(error = e) }
    }

    val sortType: StateFlow<Int> = forumRepo.getSortType(forumName)
        .stateInViewModel(initialValue = ForumSortType.BY_REPLY)

    override fun createInitialState(): ForumUiState = ForumUiState()

    private var forumSignInJob: Job? = null
    private var forumLikeJob: Job? = null

    init {
        requestLoadForm()
    }

    private fun requestLoadForm() = launchInVM {
        _uiState.set { copy(forum = null, error = null) }
        val forumData = forumRepo.loadForumInfo(forumName)
        ensureActive()
        _uiState.set { copy(forum = forumData) }
        recordHistory(forumData)
    }

    fun onGoodClassifyChanged(classifyId: Int) {
        _uiState.set { copy(goodClassifyId = classifyId) }
        launchInVM {
            sendUiEvent(ForumUiEvent.ScrollToTop(isGood = true))
            emitGlobalEventSuspend(ForumThreadListUiEvent.ClassifyChanged(classifyId))
        }
    }

    fun onSortTypeChanged(@ForumSortType sortType: Int) {
        launchInVM {
            forumRepo.saveSortType(forumName, sortType)
            sendUiEvent(ForumUiEvent.ScrollToTop(isGood = false))
            delay(200) // wait ScrollToTop animation
            emitGlobalEventSuspend(ForumThreadListUiEvent.SortTypeChanged(sortType))
        }
    }

    fun onRefreshClicked(isGood: Boolean) {
        launchInVM {
            sendUiEvent(ForumUiEvent.ScrollToTop(isGood))
            delay(200) // wait ScrollToTop animation
            emitGlobalEventSuspend(ForumThreadListUiEvent.Refresh)
        }
    }

    fun onFabClicked(@ForumFAB fab: Int, isGood: Boolean) {
        when (fab) {
            ForumFAB.POST -> sendUiEvent(CommonUiEvent.FeatureUnavailable)

            ForumFAB.REFRESH -> onRefreshClicked(isGood)

            ForumFAB.BACK_TO_TOP -> sendUiEvent(ForumUiEvent.ScrollToTop(isGood))

            ForumFAB.HIDE -> throw IllegalStateException("Incorrect Compose state")
        }
    }

    fun onSignIn() {
        if (forumSignInJob?.isActive == true || currentState.forum?.signed == true) return

        forumSignInJob = launchJobInVM {
            val currentForum = currentState.forum!!
            runCatching {
                forumRepo.forumSignIn(currentForum.id, forumName,  currentForum.tbs!!)
            }
            .onFailure { emitUiEvent(ForumUiEvent.SignIn.Failure(it.getErrorMessage())) }
            .onSuccess {
                emitUiEvent(ForumUiEvent.SignIn.Success(it.signBonusPoint!!, it.userSignRank!!))
                _uiState.update { u -> u.copy(forum = currentForum.updateSignIn(info = it)) }
            }
            forumSignInJob = null
        }
    }

    fun onLikeForum() {
        val currentForum = currentState.forum ?: return
        if (currentForum.liked || forumLikeJob?.isActive == true) return

        forumLikeJob = launchJobInVM {
            runCatching {
                forumRepo.likeForum(currentForum)
            }
            .onFailure { emitUiEvent(ForumUiEvent.Like.Failure(it.getErrorMessage())) }
            .onSuccess { newForum ->
                _uiState.update { it.copy(forum = newForum) }
                emitUiEvent(ForumUiEvent.Like.Success(newForum.members.toString()))
            }
            forumLikeJob = null
        }
    }

    fun onDislikeForum() {
        val currentForum = currentState.forum ?: return
        if (!currentForum.liked || forumLikeJob?.isActive == true) return

        forumLikeJob = launchJobInVM {
            runCatching {
                forumRepo.dislikeForum(currentForum)
            }
            .onFailure {
                emitUiEvent(ForumUiEvent.Dislike.Failure(it.getErrorMessage()))
            }
            .onSuccess {
                _uiState.update { it.copy(forum = it.forum!!.copy(liked = false)) }
                emitUiEvent(ForumUiEvent.Dislike.Success)
            }
            forumLikeJob = null
        }
    }

    fun sendToDesktop(label: String) = launchInVM {
        val forum = currentState.forum!!
        requestPinShortcut(
            context,
            "forum_${forum.id}",
            forum.avatar,
            label,
            Intent(Intent.ACTION_VIEW, "$TB_LITE_DOMAIN://forum/${forum.name}".toUri())
        )
        .onSuccess {
            emitUiEvent(ForumUiEvent.PinShortcut.Success)
        }
        .onFailure {
            emitUiEvent(ForumUiEvent.PinShortcut.Failure(it.getErrorMessage()))
        }
    }

    fun shareForum() = TiebaUtil.shareForum(context, forumName)

    private fun recordHistory(forum: ForumData) = with(forum) {
        if (!historyRecorded) {
            historyRepo.saveHistory(ForumHistory(id, name, avatar))
            historyRecorded = true
        }
    }

    companion object {
        private const val TAG = "ForumViewModel"

        private fun ForumData.updateSignIn(info: SignResultBean.UserInfo): ForumData {
            return copy(
                signed  = info.isSignIn == 1,
                signedDays = info.contSignNum ?: signedDays,
                signedRank = info.userSignRank ?: signedRank,
                levelName = info.levelName ?: levelName,
                score = score + (info.signBonusPoint ?: 0),
                scoreLevelUp = info.levelUpScore?.toIntOrNull() ?: scoreLevelUp
            )
        }
    }
}

data class ForumUiState(
    val forum: ForumData? = null,
    val goodClassifyId: Int? = null,
    val error: Throwable? = null
)

sealed interface ForumUiEvent : UiEvent {

    data class ScrollToTop(val type: ForumType) : ForumUiEvent {
        constructor(isGood: Boolean) : this(type = if (isGood) ForumType.Good else ForumType.Latest)
    }

    sealed interface SignIn : ForumUiEvent {
        data class Success(val signBonusPoint: Int, val userSignRank: Int) : SignIn

        data class Failure(val errorMsg: String) : SignIn
    }

    sealed interface Like : ForumUiEvent {
        data class Success(val memberSum: String) : Like

        data class Failure(val errorMsg: String) : Like
    }

    sealed interface Dislike : ForumUiEvent {
        object Success : Dislike

        class Failure(val errorMsg: String) : Dislike
    }

    sealed interface PinShortcut: ForumUiEvent {
        object Success : PinShortcut

        class Failure(val errorMsg: String) : PinShortcut
    }
}