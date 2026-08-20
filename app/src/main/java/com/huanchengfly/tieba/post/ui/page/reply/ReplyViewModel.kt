package com.huanchengfly.tieba.post.ui.page.reply

import android.content.Context
import android.text.Editable
import android.text.Spannable
import android.text.style.ImageSpan
import android.util.Log
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastForEach
import androidx.core.net.toUri
import androidx.core.text.getSpans
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.huanchengfly.tieba.post.App.Companion.AppBackgroundScope
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.AddThreadBean
import com.huanchengfly.tieba.post.api.models.UploadPictureResultBean
import com.huanchengfly.tieba.post.api.models.protos.addPost.AddPostResponse
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaUnknownException
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorCode
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.BaseViewModel
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.ControlledRunner
import com.huanchengfly.tieba.post.arch.PartialChange
import com.huanchengfly.tieba.post.arch.PartialChangeProducer
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiIntent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.components.ImageUploader
import com.huanchengfly.tieba.post.models.database.Draft
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.models.database.dao.DraftDao
import com.huanchengfly.tieba.post.repository.AddPostRepository
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.models.settings.HabitSettings
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.Emoticon
import com.huanchengfly.tieba.post.utils.EmoticonManager
import com.huanchengfly.tieba.post.utils.StringUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Stable
@HiltViewModel
class ReplyViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    private val settingsRepo: SettingsRepository,
    private val draftDao: DraftDao,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<ReplyUiIntent, ReplyPartialChange, ReplyUiState, ReplyUiEvent>() {

    private val params = savedStateHandle.toRoute<Destination.Reply>()

    val forumId = params.forumId
    val forumName = params.forumName
    val threadId = params.threadId
    val postId = params.postId
    val subPostId = params.subPostId
    val replyUserId = params.replyUserId
    val replyUserName = params.replyUserName
    val replyUserPortrait = params.replyUserPortrait
    val tbs = params.tbs

    //threadId为0时切换为发主题帖
    val replyType = if (forumId != 0L && threadId == 0L) ReplyType.TOPIC_THREAD else ReplyType.NONE

    val isTopicThread = replyType == ReplyType.TOPIC_THREAD

    /**
     * 当前弹窗的发送账号，null 表示使用全局主账号
     * */
    var sendAsAccount by mutableStateOf<Account?>(null)
        private set

    fun selectSendAccount(account: Account?) {
        if (account?.uid == sendAsAccount?.uid) return
        if (uiState.value.uploadImageResultList.isNotEmpty()) {
            send(ReplyUiIntent.ClearUploadResults)
        }
        sendAsAccount = account
        if (account != null) {
            settingsRepo.replySendAsUid.set(account.uid)
        }
    }

    var emoticons: List<Emoticon> = emptyList()
        private set

    /**
     * Draft to save.
     *
     * @see getDraft
     * @see deleteDraft
     * */
    private var userDraft: CharSequence? = null

    private val emoticonContentRunner = ControlledRunner<Unit>()

    private var emoticonSize = -1

    init {
        emoticons = EmoticonManager.getAllEmoticon()
        super.initialized = true

        viewModelScope.launch {
            val savedUid = settingsRepo.replySendAsUid.snapshot()
            if (savedUid != -1L) {
                val account = AccountUtil.getInstance().allAccounts.first().find { it.uid == savedUid }
                if (account != null) {
                    sendAsAccount = account
                }
            }
        }
    }

    override fun createInitialState() = ReplyUiState()

    override fun createPartialChangeProducer(): PartialChangeProducer<ReplyUiIntent, ReplyPartialChange, ReplyUiState> =
        ReplyPartialChangeProducer(context, settingsRepo.habitSettings)

    override fun dispatchEvent(partialChange: ReplyPartialChange): UiEvent? = when (partialChange) {
        is ReplyPartialChange.Send.Success -> ReplyUiEvent.ReplySuccess(
            partialChange.threadId,
            partialChange.postId,
            partialChange.expInc
        )

        is ReplyPartialChange.UploadImages.Success -> ReplyUiEvent.UploadSuccess(partialChange.resultList)

        is ReplyPartialChange.Send.Failure -> CommonUiEvent.Toast(
            context.getString(
                R.string.toast_reply_failed,
                partialChange.errorCode,
                partialChange.errorMessage
            )
        )

        is ReplyPartialChange.UploadImages.Failure -> {
            CommonUiEvent.Toast(
                context.getString(R.string.toast_upload_image_failed, partialChange.errorMessage)
            )
        }

        else -> null
    }

    private class ReplyPartialChangeProducer(
        private val context: Context,
        private val habitSettings: Settings<HabitSettings>
    ) :
        PartialChangeProducer<ReplyUiIntent, ReplyPartialChange, ReplyUiState> {
        @OptIn(ExperimentalCoroutinesApi::class)
        override fun toPartialChangeFlow(intentFlow: Flow<ReplyUiIntent>): Flow<ReplyPartialChange> =
            merge(
                intentFlow.filterIsInstance<ReplyUiIntent.UploadImages>()
                    .flatMapConcat { it.producePartialChange() },
                intentFlow.filterIsInstance<ReplyUiIntent.Send>()
                    .flatMapConcat { it.producePartialChange() },
                intentFlow.filterIsInstance<ReplyUiIntent.AddImage>()
                    .flatMapConcat { it.producePartialChange() },
                intentFlow.filterIsInstance<ReplyUiIntent.RemoveImage>()
                    .flatMapConcat { it.producePartialChange() },
                intentFlow.filterIsInstance<ReplyUiIntent.ToggleIsOriginImage>()
                    .flatMapConcat { it.producePartialChange() },
                intentFlow.filterIsInstance<ReplyUiIntent.ClearUploadResults>()
                    .flatMapConcat { it.producePartialChange() },
            )

        private fun ReplyUiIntent.ClearUploadResults.producePartialChange() =
            flowOf(ReplyPartialChange.ClearUploadResults)

        private fun ReplyUiIntent.Send.producePartialChange(): Flow<ReplyPartialChange.Send> {
            if (forumId != 0L && threadId == 0L ) {
                return AddPostRepository
                    .addThread(
                        content,
                        forumId,
                        forumName,
                        title = title,
                        isHide = 1,
                        isTitle = if (title.isNullOrEmpty()) 1 else 0,
                        account = account,
                    )
                    .map<AddThreadBean, ReplyPartialChange.Send> {
                        if (it.tid == null) throw TiebaUnknownException
                        ReplyPartialChange.Send.Success(
                            threadId = it.tid!!,
                            postId = it.pid.orEmpty(),
                            expInc = ""
                        )
                    }
                    .onStart { emit(ReplyPartialChange.Send.Start) }
                    .catch {
                        Log.i("ReplyViewModel", "failure: ${it.message}")
                        it.printStackTrace()
                        emit(
                            ReplyPartialChange.Send.Failure(
                                it.getErrorCode(),
                                it.getErrorMessage()
                            )
                        )
                    }
            }
            return AddPostRepository
                .addPost(
                    content,
                    forumId,
                    forumName,
                    threadId,
                    tbs,
                    postId = postId,
                    subPostId = subPostId,
                    replyUserId = replyUserId,
                    account = account,
                )
                .map<AddPostResponse, ReplyPartialChange.Send> {
                    if (it.data_ == null) throw TiebaUnknownException
                    ReplyPartialChange.Send.Success(
                        threadId = it.data_.tid,
                        postId = it.data_.pid,
                        expInc = it.data_.exp?.inc.orEmpty()
                    )
                }
                .onStart { emit(ReplyPartialChange.Send.Start) }
                .catch {
                    Log.w(TAG, "failure", it)
                    emit(ReplyPartialChange.Send.Failure(it.getErrorCode(), it.getErrorMessage()))
                }
        }

        private fun ReplyUiIntent.UploadImages.producePartialChange() =
                flow<ReplyPartialChange.UploadImages> {
                    val rec = ImageUploader(forumName).upload(
                        context = context,
                        images = imageUris.map { it.toUri() },
                        watermarkType = habitSettings.snapshot().imageWatermarkType,
                        isOriginImage = isOriginImage,
                        account = account,
                    )
                    emit(ReplyPartialChange.UploadImages.Success(rec))
                }
                .catch {
                    Log.e(TAG, "producePartialChange: onUploadImages", it)
                    emit(
                        ReplyPartialChange.UploadImages.Failure(
                            it.getErrorCode(),
                            it.getErrorMessage()
                        )
                    )
                }
                .onStart { emit(ReplyPartialChange.UploadImages.Start) }

        private fun ReplyUiIntent.AddImage.producePartialChange() =
            flowOf(ReplyPartialChange.AddImage(imageUris))

        private fun ReplyUiIntent.RemoveImage.producePartialChange() =
            flowOf(ReplyPartialChange.RemoveImage(imageIndex))

        private fun ReplyUiIntent.ToggleIsOriginImage.producePartialChange() =
            flowOf(ReplyPartialChange.ToggleIsOriginImage(isOriginImage))
    }

    fun onSendReply(threadTitle: String, curTbs: String) {
        val text = userDraft ?: return
        val replyContent = if (subPostId == null || subPostId == 0L) {
            text
        } else {
            "回复 #(reply, ${replyUserPortrait}, ${replyUserName}) :${text}"
        }
        send(
            ReplyUiIntent.Send(
                content = replyContent.toString(),
                forumId = forumId,
                forumName = forumName,
                threadId = threadId,
                tbs = curTbs,
                title = threadTitle.takeIf { isTopicThread },
                postId = postId,
                subPostId = subPostId,
                replyUserId = replyUserId,
                account = sendAsAccount,
            )
        )
    }

    fun onSendReplyWithImage(resultList: List<UploadPictureResultBean>, threadTitle: String?, curTbs: String) {
        val imageContent = resultList.joinToString("\n") { image ->
            "#(pic,${image.picId ?: 0},${image.picInfo?.originPic?.width ?: 0},${image.picInfo?.originPic?.height ?: 0})"
        }

        send(
            ReplyUiIntent.Send(
                content = "${userDraft}\n$imageContent",
                forumId = forumId,
                forumName = forumName,
                threadId = threadId,
                tbs = curTbs,
                title = threadTitle.takeIf { isTopicThread },
                postId = postId,
                subPostId = subPostId,
                replyUserId = replyUserId,
                account = sendAsAccount,
            )
        )
    }

    fun setEmoticonSpans(s: Editable?) {
        val input = s?.toString() ?: ""
        userDraft = input

        if (s.isNullOrEmpty()) {
            emoticonContentRunner.cancelCurrent()
            return
        }

        viewModelScope.launch {
            emoticonContentRunner.cancelPreviousThenRun {
                val spans = StringUtil.getEmoticonSpans(context, emoticonSize, source = input)
                withContext(Dispatchers.Main) {
                    s.getSpans<ImageSpan>().forEach { s.removeSpan(it) }
                    ensureActive()
                    spans.fastForEach {
                        s.setSpan(it.item, it.start, it.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
            }
        }
    }

    fun setEmoticonSize(size: Int) {
        emoticonSize = size
    }

    suspend fun getDraft(): CharSequence? {
        return userDraft ?: draftDao.getByIds(threadId, postId ?: 0, subPostId ?: 0).firstOrNull()
    }

    fun deleteDraft() {
        userDraft = null
        AppBackgroundScope.launch {
            draftDao.deleteByIds(threadId, postId ?: 0, subPostId ?: 0)
        }
    }

    override fun onCleared() {
        emoticonContentRunner.cancelCurrent()
        if (isTopicThread) return
        val draft = userDraft?.toString()?.trim() ?: return
        AppBackgroundScope.launch {
            runCatching {
                if (draft.isNotEmpty() && draft.isNotBlank()) {
                    draftDao.upsert(Draft(threadId, postId ?: 0, subPostId ?: 0, draft))
                } else {
                    // User clear the content manually
                    draftDao.deleteByIds(threadId, postId ?: 0, subPostId ?: 0)
                }
            }
            .onFailure { e ->
                Log.e(TAG, "onCleared: Update draft failed: ${e.message}, len: ${draft.length}")
            }
        }
    }

    companion object {
        private const val TAG = "ReplyViewModel"

        const val MAX_SELECTABLE_IMAGE = 9
    }
}

sealed interface ReplyUiIntent : UiIntent {
    data class UploadImages(
        val forumName: String,
        val imageUris: List<String>,
        val isOriginImage: Boolean,
        val account: Account? = null,
    ) : ReplyUiIntent

    data class Send(
        val content: String,
        val forumId: Long,
        val forumName: String,
        val threadId: Long,
        val tbs: String,
        val title: String? = null,
        val postId: Long? = null,
        val subPostId: Long? = null,
        val replyUserId: Long? = null,
        val account: Account? = null,
    ) : ReplyUiIntent

    data class AddImage(val imageUris: List<String>) : ReplyUiIntent

    data class RemoveImage(val imageIndex: Int) : ReplyUiIntent

    data class ToggleIsOriginImage(val isOriginImage: Boolean) : ReplyUiIntent

    data object ClearUploadResults : ReplyUiIntent
}

sealed interface ReplyPartialChange : PartialChange<ReplyUiState> {
    sealed class UploadImages : ReplyPartialChange {
        override fun reduce(oldState: ReplyUiState): ReplyUiState = when (this) {
            is Start -> oldState.copy(isUploading = true)
            is Success -> oldState.copy(
                isUploading = false,
                uploadImageResultList = resultList.toImmutableList()
            )

            is Failure -> oldState.copy(isUploading = false)
        }

        object Start : UploadImages()

        data class Success(val resultList: List<UploadPictureResultBean>) : UploadImages()

        data class Failure(
            val errorCode: Int,
            val errorMessage: String
        ) : UploadImages()
    }

    sealed class Send : ReplyPartialChange {
        override fun reduce(oldState: ReplyUiState): ReplyUiState {
            return when (this) {
                is Start -> oldState.copy(isSending = true)
                is Success -> oldState.copy(isSending = false, replySuccess = true)
                is Failure -> oldState.copy(isSending = false, replySuccess = false)
            }
        }

        object Start : Send()

        data class Success(
            val threadId: String,
            val postId: String,
            val expInc: String
        ) : Send()

        data class Failure(
            val errorCode: Int,
            val errorMessage: String
        ) : Send()
    }

    data class AddImage(val imageUris: List<String>) : ReplyPartialChange {
        override fun reduce(oldState: ReplyUiState): ReplyUiState {
            // On device that don't support limited photo picker
            // Double check image uris size
            var images = oldState.selectedImageList + imageUris
            if (images.size > ReplyViewModel.MAX_SELECTABLE_IMAGE) {
               images = images.subList(0, ReplyViewModel.MAX_SELECTABLE_IMAGE)
            }
            return oldState.copy(selectedImageList = images.toImmutableList())
        }
    }

    data class RemoveImage(val imageIndex: Int) : ReplyPartialChange {
        override fun reduce(oldState: ReplyUiState): ReplyUiState =
            oldState.copy(selectedImageList = (oldState.selectedImageList - oldState.selectedImageList[imageIndex]).toImmutableList())
    }

    data class ToggleIsOriginImage(val isOriginImage: Boolean) : ReplyPartialChange {
        override fun reduce(oldState: ReplyUiState): ReplyUiState =
            oldState.copy(isOriginImage = isOriginImage)
    }

    data object ClearUploadResults : ReplyPartialChange {
        override fun reduce(oldState: ReplyUiState): ReplyUiState =
            oldState.copy(uploadImageResultList = persistentListOf())
    }
}

data class ReplyUiState(
    val isSending: Boolean = false,
    val replySuccess: Boolean = false,
    val isUploading: Boolean = false,
    val isOriginImage: Boolean = false,
    val selectedImageList: ImmutableList<String> = persistentListOf(),
    val uploadImageResultList: ImmutableList<UploadPictureResultBean> = persistentListOf(),
) : UiState

sealed interface ReplyUiEvent : UiEvent {
    data class UploadSuccess(val resultList: List<UploadPictureResultBean>) : ReplyUiEvent

    data class ReplySuccess(
        val threadId: String,
        val postId: String,
        val expInc: String
    ) : ReplyUiEvent
}
