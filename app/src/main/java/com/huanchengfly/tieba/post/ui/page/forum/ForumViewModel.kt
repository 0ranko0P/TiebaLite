package com.huanchengfly.tieba.post.ui.page.forum

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Stable
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.SignResultBean
import com.huanchengfly.tieba.post.api.retrofit.exception.NoConnectivityException
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.emitGlobalEventSuspend
import com.huanchengfly.tieba.post.models.ForumHistoryExtra
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.repository.ForumRepository
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.models.forum.ForumData
import com.huanchengfly.tieba.post.ui.models.settings.ForumFAB
import com.huanchengfly.tieba.post.ui.models.settings.ForumSortType
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.forum.threadlist.ForumThreadListUiEvent
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.set
import com.huanchengfly.tieba.post.utils.HistoryUtil
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.requestPinShortcut
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@Stable
@HiltViewModel
class ForumViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    private val forumRepo: ForumRepository,
    settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val handler = CoroutineExceptionHandler { _, e ->
        if (e !is NoConnectivityException) {
            Log.e(TAG, "onError: ", e)
        }
        _uiState.update { it.copy(error = e) }
    }

    private val param = savedStateHandle.toRoute<Destination.Forum>()

    private val forumName: String = param.forumName

    val fab = settingsRepository.habitSettings.flow
        .map { it.forumFAB }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ForumFAB.BACK_TO_TOP)

    val sortType = forumRepo.getSortType(forumName)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ForumSortType.BY_REPLY)

    /**
     * One-off [UiEvent], but no guarantee to be received.
     * */
    private val _uiEvent: MutableSharedFlow<UiEvent> = MutableSharedFlow()
    val uiEvent: Flow<UiEvent> = _uiEvent.asSharedFlow()

    private val _uiState: MutableStateFlow<ForumUiState> = MutableStateFlow(ForumUiState())
    val uiState: StateFlow<ForumUiState> = _uiState.asStateFlow()

    private var singleJob: Job? = null

    init {
        requestLoadForm()
    }

    private fun saveHistory(forum: ForumData) {
        HistoryUtil.saveHistory(
            history = History(
                timestamp = System.currentTimeMillis(),
                avatar = forum.avatar,
                type = HistoryUtil.TYPE_FORUM,
                data = forum.name,
                extras = Json.encodeToString(ForumHistoryExtra(forum.id))
            )
        )
    }

    private fun requestLoadForm() = viewModelScope.launch(handler) {
        _uiState.set { copy(forum = null, error = null) }
        val forumData = forumRepo.loadForumInfo(forumName)
        _uiState.set { copy(forum = forumData) }
        saveHistory(forumData)
    }

    fun onGoodClassifyChanged(classifyId: Int) {
        _uiState.set { copy(goodClassifyId = classifyId) }
        viewModelScope.launch(handler) {
            emitGlobalEventSuspend(ForumThreadListUiEvent.BackToTop(isGood = true))
            delay(200) // wait ScrollToTop animation
            emitGlobalEventSuspend(ForumThreadListUiEvent.ClassifyChanged(classifyId))
        }
    }

    fun onSortTypeChanged(@ForumSortType sortType: Int) {
        viewModelScope.launch {
            forumRepo.saveSortType(forumName, sortType)
            emitGlobalEventSuspend(ForumThreadListUiEvent.BackToTop(isGood = false))
            delay(200) // wait ScrollToTop animation
            emitGlobalEventSuspend(ForumThreadListUiEvent.SortTypeChanged(sortType))
        }
    }

    fun onRefreshClicked(isGood: Boolean) {
        viewModelScope.launch {
            emitGlobalEventSuspend(ForumThreadListUiEvent.BackToTop(isGood))
            delay(200) // wait ScrollToTop animation
            emitGlobalEventSuspend(ForumThreadListUiEvent.Refresh)
        }
    }

    fun onFabClicked(isGood: Boolean) {
        when (fab.value) {
            ForumFAB.POST -> sendMsg(context.getString(R.string.toast_feature_unavailable))

            ForumFAB.REFRESH -> onRefreshClicked(isGood)

            ForumFAB.BACK_TO_TOP -> viewModelScope.launch {
                emitGlobalEventSuspend(ForumThreadListUiEvent.BackToTop(isGood))
            }

            ForumFAB.HIDE -> throw IllegalStateException("Incorrect Compose state")
        }
    }

    fun onSignIn() {
        if (singleJob?.isActive == true || _uiState.value.forum?.signed == true) return

        singleJob = viewModelScope.launch(handler) {
            val currentForum = _uiState.first().forum!!
            runCatching {
                forumRepo.forumSignIn(currentForum.id, forumName,  currentForum.tbs!!)
            }
            .onFailure { sendUiEvent(ForumUiEvent.SignIn.Failure(it.getErrorMessage())) }
            .onSuccess {
                sendUiEvent(ForumUiEvent.SignIn.Success(it.signBonusPoint!!, it.userSignRank!!))
                _uiState.update { u -> u.copy(forum = currentForum.updateSignIn(info = it)) }
            }
        }
    }

    fun onLikeForum() {
        val currentForum = _uiState.value.forum
        if (currentForum == null || currentForum.liked) return

        singleJob = viewModelScope.launch(Dispatchers.IO + handler) {
            val rec = TiebaApi.getInstance()
                .likeForumFlow(forumId = currentForum.id.toString(), forumName, tbs = currentForum.tbs!!)
                .catch {
                    sendUiEvent(ForumUiEvent.Like.Failure(it.getErrorMessage()))
                }
                .firstOrNull() ?: return@launch

            val info = rec.info
            val newForum = currentForum.copy(
                liked = true,
                level = info.levelId.toInt(),
                levelName = info.levelName,
                score = info.curScore.toInt(),
                scoreLevelUp = info.levelUpScore.toInt(),
                members = info.memberSum.toInt()
            )
            _uiState.update { it.copy(forum = newForum) }
            sendUiEvent(ForumUiEvent.Like.Success(info.memberSum))
        }
    }

    fun onDislikeForum() {
        val currentForum = _uiState.value.forum
        if (currentForum == null || !currentForum.liked) return

        singleJob = viewModelScope.launch(Dispatchers.IO + handler) {
            TiebaApi.getInstance()
                .unlikeForumFlow(forumId = currentForum.id.toString(), forumName, currentForum.tbs!!)
                .catch {
                    sendUiEvent(ForumUiEvent.Dislike.Failure(it.getErrorMessage()))
                }
                .firstOrNull() ?: return@launch
            _uiState.update { it.copy(forum = it.forum!!.copy(liked = false)) }
            sendUiEvent(ForumUiEvent.Dislike.Success)
        }
    }

    private fun sendUiEvent(event: UiEvent) = viewModelScope.launch { _uiEvent.emit(event) }

    private fun sendMsg(msg: String) {
        if (viewModelScope.isActive) {
            sendUiEvent(CommonUiEvent.Toast(message = msg))
        } else {
            context.toastShort(text = msg)
        }
    }

    fun sendToDesktop() = viewModelScope.launch {
        val forum = _uiState.value.forum ?: return@launch
        requestPinShortcut(
            context,
            "forum_${forum.id}",
            forum.avatar,
            context.getString(R.string.title_forum, forum.name),
            Intent(Intent.ACTION_VIEW, "tblite://forum/${forum.name}".toUri())
        )
        .onSuccess {
            sendMsg(context.getString(R.string.toast_send_to_desktop_success))
        }
        .onFailure {
            sendMsg(it.getErrorMessage())
        }
    }

    fun shareForum() = TiebaUtil.shareForum(context, forumName)

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
}