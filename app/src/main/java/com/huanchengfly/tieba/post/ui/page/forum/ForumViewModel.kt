package com.huanchengfly.tieba.post.ui.page.forum

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.CommonResponse
import com.huanchengfly.tieba.post.api.models.LikeForumResultBean
import com.huanchengfly.tieba.post.api.models.protos.frsPage.ForumInfo
import com.huanchengfly.tieba.post.api.models.protos.frsPage.FrsPageResponse
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorCode
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.BaseViewModel
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.PartialChange
import com.huanchengfly.tieba.post.arch.PartialChangeProducer
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiIntent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.emitGlobalEventSuspend
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.models.ForumHistoryExtra
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.repository.FrsPageRepository
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.forum.threadlist.ForumThreadListUiEvent
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils.Companion.ForumFabFunction
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils.Companion.ForumSortType
import com.huanchengfly.tieba.post.utils.HistoryUtil
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import com.huanchengfly.tieba.post.utils.requestPinShortcut
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@Stable
@HiltViewModel
class ForumViewModel @Inject constructor(savedStateHandle: SavedStateHandle) :
    BaseViewModel<ForumUiIntent, ForumPartialChange, ForumUiState, ForumUiEvent>() {

    val param = savedStateHandle.toRoute<Destination.Forum>()

    private var forumName: String = param.forumName

    @ForumFabFunction
    val fab: String = App.INSTANCE.appPreferences.forumFabFunction

    private val scope = CoroutineScope(Dispatchers.Main + CoroutineName(TAG))

    @get:ForumSortType
    var sortType by mutableIntStateOf(ForumSortType.BY_REPLY)
        private set

    init {
        requestLoadForm(App.INSTANCE)
    }

    override fun createInitialState(): ForumUiState = ForumUiState()

    override fun createPartialChangeProducer(): PartialChangeProducer<ForumUiIntent, ForumPartialChange, ForumUiState> =
        ForumPartialChangeProducer

    override fun dispatchEvent(partialChange: ForumPartialChange): UiEvent? {
        return when (partialChange) {
            is ForumPartialChange.SignIn.Success -> ForumUiEvent.SignIn.Success(
                partialChange.signBonusPoint,
                partialChange.userSignRank
            )

            is ForumPartialChange.SignIn.Failure -> ForumUiEvent.SignIn.Failure(
                partialChange.error.getErrorCode(),
                partialChange.error.getErrorMessage()
            )

            is ForumPartialChange.Like.Success -> ForumUiEvent.Like.Success(partialChange.data.info.memberSum)
            is ForumPartialChange.Like.Failure -> ForumUiEvent.Like.Failure(
                partialChange.error.getErrorCode(),
                partialChange.error.getErrorMessage()
            )

            is ForumPartialChange.Unlike.Success -> ForumUiEvent.Unlike.Success
            is ForumPartialChange.Unlike.Failure -> ForumUiEvent.Unlike.Failure(
                partialChange.error.getErrorCode(),
                partialChange.error.getErrorMessage()
            )

            else -> null
        }
    }

    private object ForumPartialChangeProducer :
        PartialChangeProducer<ForumUiIntent, ForumPartialChange, ForumUiState> {
        @OptIn(ExperimentalCoroutinesApi::class)
        override fun toPartialChangeFlow(intentFlow: Flow<ForumUiIntent>): Flow<ForumPartialChange> =
            merge(
                intentFlow.filterIsInstance<ForumUiIntent.Load>()
                    .flatMapConcat { it.produceLoadPartialChange() },
                intentFlow.filterIsInstance<ForumUiIntent.SignIn>()
                    .flatMapConcat { it.produceLoadPartialChange() },
                intentFlow.filterIsInstance<ForumUiIntent.Like>()
                    .flatMapConcat { it.produceLoadPartialChange() },
                intentFlow.filterIsInstance<ForumUiIntent.Unlike>()
                    .flatMapConcat { it.produceLoadPartialChange() },
                intentFlow.filterIsInstance<ForumUiIntent.ToggleShowHeader>()
                    .flatMapConcat { it.produceLoadPartialChange() },
            )

        private fun ForumUiIntent.Load.produceLoadPartialChange() =
            FrsPageRepository.frsPage(forumName, 1, 1, sortType, null, true)
                .map<FrsPageResponse, ForumPartialChange.Load> {
                    if (it.data_?.forum == null) {
                        throw NullPointerException(it.error?.error_msg ?: "未知错误")
                    }
                    ForumPartialChange.Load.Success(it.data_.forum, it.data_.anti?.tbs)
                }
                .onStart { emit(ForumPartialChange.Load.Start) }
                .catch { emit(ForumPartialChange.Load.Failure(it)) }

        private fun ForumUiIntent.SignIn.produceLoadPartialChange() =
            TiebaApi.getInstance().signFlow("$forumId", forumName, tbs)
                .map { signResultBean ->
                    if (signResultBean.userInfo?.signBonusPoint != null &&
                        signResultBean.userInfo.levelUpScore != null &&
                        signResultBean.userInfo.contSignNum != null &&
                        signResultBean.userInfo.userSignRank != null &&
                        signResultBean.userInfo.isSignIn != null &&
                        signResultBean.userInfo.levelName != null &&
                        signResultBean.userInfo.allLevelInfo.isNotEmpty()
                    ) {
                        val levelUpScore = signResultBean.userInfo.levelUpScore.toInt()
                        ForumPartialChange.SignIn.Success(
                            signResultBean.userInfo.signBonusPoint.toInt(),
                            levelUpScore,
                            signResultBean.userInfo.contSignNum.toInt(),
                            signResultBean.userInfo.userSignRank.toInt(),
                            signResultBean.userInfo.isSignIn.toInt(),
                            signResultBean.userInfo.allLevelInfo.last { it.score.toInt() < levelUpScore }.id.toInt(),
                            signResultBean.userInfo.levelName
                        )
                    } else ForumPartialChange.SignIn.Failure(NullPointerException("未知错误"))
                }
                .catch { emit(ForumPartialChange.SignIn.Failure(it)) }

        private fun ForumUiIntent.Like.produceLoadPartialChange() =
            TiebaApi.getInstance().likeForumFlow("$forumId", forumName, tbs)
                .map<LikeForumResultBean, ForumPartialChange.Like> {
                    ForumPartialChange.Like.Success(it)
                }
                .catch { emit(ForumPartialChange.Like.Failure(it)) }

        private fun ForumUiIntent.Unlike.produceLoadPartialChange() =
            TiebaApi.getInstance().unlikeForumFlow("$forumId", forumName, tbs)
                .map<CommonResponse, ForumPartialChange.Unlike> {
                    ForumPartialChange.Unlike.Success
                }
                .catch { emit(ForumPartialChange.Unlike.Failure(it)) }

        private fun ForumUiIntent.ToggleShowHeader.produceLoadPartialChange() =
            flowOf(ForumPartialChange.ToggleShowHeader(showHeader))
    }

    fun saveHistory(forum: ForumInfo) = scope.launch(Dispatchers.IO) {
        HistoryUtil.saveHistory(
            History(
                timestamp = System.currentTimeMillis(),
                avatar = forum.avatar,
                type = HistoryUtil.TYPE_FORUM,
                data = forum.name,
                extras = Json.encodeToString(ForumHistoryExtra(forum.id))
            ), false
        )
    }

    fun requestLoadForm(context: Context) = scope.launch {
        sortType = withContext(Dispatchers.IO) {
            getSortType(context, forumName).first()
        }
        send(ForumUiIntent.Load(forumName, sortType))
    }

    fun onSortTypeChanged(@ForumSortType sortType: Int, isGood: Boolean) = scope.launch {
        this@ForumViewModel.sortType = sortType
        emitGlobalEventSuspend(ForumThreadListUiEvent.Refresh(isGood, sortType))
        saveSortType(forumName, sortType)
    }

    fun onRefreshClicked(isGood: Boolean) {
        scope.launch {
            emitGlobalEventSuspend(ForumThreadListUiEvent.BackToTop(isGood))
            emitGlobalEventSuspend(ForumThreadListUiEvent.Refresh(isGood, sortType))
        }
    }

    fun onFabClicked(context: Context, isGood: Boolean) {
        when (fab) {
            ForumFabFunction.POST -> context.toastShort(R.string.toast_feature_unavailable)

            ForumFabFunction.REFRESH -> onRefreshClicked(isGood)

            ForumFabFunction.BACK_TO_TOP -> scope.launch {
                emitGlobalEventSuspend(ForumThreadListUiEvent.BackToTop(isGood))
            }

            ForumFabFunction.HIDE -> throw IllegalStateException("Incorrect Compose state")
        }
    }

    fun onSignIn(forum: ForumInfo, tbs: String) {
        if (forum.sign_in_info?.user_info?.is_sign_in != 1) {
            send(ForumUiIntent.SignIn(forum.id, forum.name, tbs))
        }
    }

    fun onFollow(forum: ForumInfo, tbs: String) {
        if (forum.is_like != 1) {
            send(ForumUiIntent.Like(forum.id, forum.name, tbs))
        }
    }

    fun sendToDesktop(context: Context, forum: ForumInfo) = scope.launch {
        val result = requestPinShortcut(
            context,
            "forum_${forum.id}",
            forum.avatar,
            context.getString(R.string.title_forum, forum.name),
            Intent(Intent.ACTION_VIEW).setData(Uri.parse("tblite://forum/${forum.name}"))
        )
        if (result.isSuccess) {
            context.toastShort(R.string.toast_send_to_desktop_success)
        } else {
            val message = result.exceptionOrNull()?.message ?: return@launch
            context.toastShort(message)
        }
    }

    fun shareForum(context: Context) {
        TiebaUtil.shareText(
            context,
            "https://tieba.baidu.com/f?kw=$forumName",
            context.getString(R.string.title_forum, forumName)
        )
    }

    companion object {
        private const val TAG = "ForumViewModel"

        /**
         * PreferenceDataStore where [sortTypeKey] saved
         * */
        private val DataStore by lazy {
            PreferenceDataStoreFactory.create {
                App.INSTANCE.preferencesDataStoreFile(name = "forum_preferences")
            }
        }

        private fun sortTypeKey(forumName: String) = intPreferencesKey("${forumName}_sort_type")

        /**
         * Sort preference per forum
         *
         * @see [ForumSortType]
         * */
        fun getSortType(context: Context, forumName: String): Flow<Int> {
            return DataStore.data
                .map { it[sortTypeKey(forumName)] ?: context.appPreferences.defaultSortType }
                .distinctUntilChanged()
        }

        private suspend fun saveSortType(forumName: String, @ForumSortType sortType: Int) {
            val context = App.INSTANCE
            // Default from app_preference
            val default = context.appPreferences.defaultSortType
            // Save to forum_preferences
            DataStore.edit {
                val key = sortTypeKey(forumName)
                if (sortType == default) {
                    it.remove(key) // Keep dataStore clean
                } else {
                    it[key] = sortType
                }
            }
        }
    }
}

sealed interface ForumUiIntent : UiIntent {
    data class Load(
        val forumName: String,
        val sortType: Int = -1
    ) : ForumUiIntent

    data class SignIn(
        val forumId: Long,
        val forumName: String,
        val tbs: String
    ) : ForumUiIntent

    data class Like(
        val forumId: Long,
        val forumName: String,
        val tbs: String
    ) : ForumUiIntent

    data class Unlike(
        val forumId: Long,
        val forumName: String,
        val tbs: String
    ) : ForumUiIntent

    data class ToggleShowHeader(
        val showHeader: Boolean
    ) : ForumUiIntent
}

sealed interface ForumPartialChange : PartialChange<ForumUiState> {
    sealed class Load : ForumPartialChange {
        override fun reduce(oldState: ForumUiState): ForumUiState = when (this) {
            Start -> oldState.copy(isLoading = true)
            is Success -> oldState.copy(
                isLoading = true,
                isError = false,
                forum = forum.wrapImmutable(),
                tbs = tbs
            )

            is Failure -> oldState.copy(isLoading = false, isError = true)
        }

        object Start : Load()

        data class Success(
            val forum: ForumInfo,
            val tbs: String?
        ) : Load()

        data class Failure(
            val error: Throwable
        ) : Load()
    }

    sealed class SignIn : ForumPartialChange {
        override fun reduce(oldState: ForumUiState): ForumUiState = when (this) {
            is Failure -> oldState
            is Success -> oldState.copy(
                forum = oldState.forum?.getImmutable {
                    copy(
                        user_level = level,
                        level_name = levelName,
                        cur_score = oldState.forum.get { cur_score } + signBonusPoint,
                        levelup_score = levelUpScore,
                        sign_in_info = oldState.forum.get { sign_in_info }?.copy(
                            user_info = oldState.forum.get { sign_in_info }?.user_info?.copy(
                                is_sign_in = isSignIn,
                                user_sign_rank = userSignRank,
                                cont_sign_num = contSignNum
                            )
                        )
                    )
                }
            )
        }

        data class Success(
            val signBonusPoint: Int,
            val levelUpScore: Int,
            val contSignNum: Int,
            val userSignRank: Int,
            val isSignIn: Int,
            val level: Int,
            val levelName: String
        ) : SignIn()

        data class Failure(
            val error: Throwable
        ) : SignIn()
    }

    sealed class Like : ForumPartialChange {
        override fun reduce(oldState: ForumUiState): ForumUiState = when (this) {
            is Failure -> oldState
            is Success -> oldState.copy(
                forum = oldState.forum?.getImmutable {
                    copy(
                        is_like = 1,
                        cur_score = data.info.curScore.toInt(),
                        levelup_score = data.info.levelUpScore.toInt(),
                        user_level = data.info.levelId.toInt(),
                        level_name = data.info.levelName,
                        member_num = data.info.memberSum.toInt()
                    )
                }
            )
        }

        data class Success(val data: LikeForumResultBean) : Like()

        data class Failure(val error: Throwable) : Like()
    }

    sealed class Unlike : ForumPartialChange {
        override fun reduce(oldState: ForumUiState): ForumUiState = when (this) {
            is Failure -> oldState
            is Success -> oldState.copy(
                forum = oldState.forum?.getImmutable { copy(is_like = 0) }
            )
        }

        object Success : Unlike()

        data class Failure(val error: Throwable) : Unlike()
    }

    data class ToggleShowHeader(val showHeader: Boolean) : ForumPartialChange {
        override fun reduce(oldState: ForumUiState): ForumUiState =
            oldState.copy(showForumHeader = showHeader)
    }
}

data class ForumUiState(
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val forum: ImmutableHolder<ForumInfo>? = null,
    val tbs: String? = null,
    val showForumHeader: Boolean = true
) : UiState

sealed interface ForumUiEvent : UiEvent {
    sealed interface SignIn : ForumUiEvent {
        data class Success(
            val signBonusPoint: Int,
            val userSignRank: Int,
        ) : SignIn

        data class Failure(
            val errorCode: Int,
            val errorMsg: String,
        ) : SignIn
    }

    sealed interface Like : ForumUiEvent {
        data class Success(
            val memberSum: String
        ) : Like

        data class Failure(
            val errorCode: Int,
            val errorMsg: String,
        ) : Like
    }

    sealed interface Unlike : ForumUiEvent {
        object Success : Like

        data class Failure(
            val errorCode: Int,
            val errorMsg: String,
        ) : Like
    }
}