package com.huanchengfly.tieba.post.ui.page.reply

import android.content.Context
import android.net.Uri
import android.text.Editable
import android.text.SpannableString
import android.text.TextWatcher
import android.util.Log
import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.huanchengfly.tieba.post.App
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
import com.huanchengfly.tieba.post.models.database.dao.DraftDao
import com.huanchengfly.tieba.post.repository.AddPostRepository
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.utils.Emoticon
import com.huanchengfly.tieba.post.utils.EmoticonManager
import com.huanchengfly.tieba.post.utils.FileUtil
import com.huanchengfly.tieba.post.utils.StringUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@HiltViewModel
class ReplyViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    private val draftDao: DraftDao,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<ReplyUiIntent, ReplyPartialChange, ReplyUiState, ReplyUiEvent>(), TextWatcher {

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

    var emoticons: List<Emoticon> = emptyList()
        private set

    private val _text = MutableStateFlow(SpannableString(""))
    val text: StateFlow<SpannableString> = _text.asStateFlow()

    /**
     * Draft to save, will be cleared after a successful reply.
     *
     * @see onTextChanged
     * @see deleteDraft
     * */
    private var userDraft: CharSequence? = null

    private val emoticonContentRunner = ControlledRunner<Unit>()

    private var emoticonSize = -1

    init {
        // Restore draft from DataBase
        viewModelScope.launch {
            val draft = draftDao.getByIds(threadId, postId ?: 0, subPostId ?: 0).firstOrNull()
            if (!draft.isNullOrEmpty()) {
                onTextChanged(draft, 0, 0, draft.length)
            }
        }
        emoticons = EmoticonManager.getAllEmoticon()
        super.initialized = true
    }

    override fun createInitialState() = ReplyUiState()

    override fun createPartialChangeProducer(): PartialChangeProducer<ReplyUiIntent, ReplyPartialChange, ReplyUiState> =
        ReplyPartialChangeProducer

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

    private object ReplyPartialChangeProducer :
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
                intentFlow.filterIsInstance<ReplyUiIntent.SwitchReplyType>()
                    .flatMapConcat { it.producePartialChange() },
            )

        private fun ReplyUiIntent.Send.producePartialChange(): Flow<ReplyPartialChange.Send> {
            if (forumId != 0L && threadId == 0L ) {
                return AddPostRepository
                    .addThread(
                        content,
                        forumId,
                        forumName,
                        title = "",//这三个后面再做
                        isHide = 1,
                        isTitle = 1,
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
                    replyUserId = replyUserId
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
            ImageUploader(forumName)
                .uploadImages(
                    imageUris.map {
                        FileUtil.getRealPathFromUri(
                            App.INSTANCE,
                            Uri.parse(it)
                        )
                    },
                    isOriginImage
                )
                .map<List<UploadPictureResultBean>, ReplyPartialChange.UploadImages> {
                    ReplyPartialChange.UploadImages.Success(it)
                }
                .onStart { emit(ReplyPartialChange.UploadImages.Start) }
                .catch {
                    it.printStackTrace()
                    emit(
                        ReplyPartialChange.UploadImages.Failure(
                            it.getErrorCode(),
                            it.getErrorMessage()
                        )
                    )
                }

        private fun ReplyUiIntent.SwitchReplyType.producePartialChange() =
            flowOf(ReplyPartialChange.SwitchReplyType(replyType))

        private fun ReplyUiIntent.AddImage.producePartialChange() =
            flowOf(ReplyPartialChange.AddImage(imageUris))

        private fun ReplyUiIntent.RemoveImage.producePartialChange() =
            flowOf(ReplyPartialChange.RemoveImage(imageIndex))

        private fun ReplyUiIntent.ToggleIsOriginImage.producePartialChange() =
            flowOf(ReplyPartialChange.ToggleIsOriginImage(isOriginImage))
    }

    fun onSendReply(curTbs: String) {
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
                postId = postId,
                subPostId = subPostId,
                replyUserId = replyUserId
            )
        )
    }

    fun onSendReplyWithImage(resultList: List<UploadPictureResultBean>, curTbs: String) {
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
                postId = postId,
                subPostId = subPostId,
                replyUserId = replyUserId,
            )
        )
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        val input = s?.toString()?: ""
        userDraft = input
        viewModelScope.launch {
            emoticonContentRunner.cancelPreviousThenRun {
                val newText = StringUtil.getEmoticonContent(context, emoticonSize, source = input)
                _text.update { newText }
            }
        }
    }

    fun setEmoticonSize(size: Int) {
        emoticonSize = size
    }

    fun deleteDraft() {
        userDraft = null
        AppBackgroundScope.launch {
            draftDao.deleteByIds(threadId, postId ?: 0, subPostId ?: 0)
        }
    }

    override fun onCleared() {
        super.onCleared()
        emoticonContentRunner.cancelCurrent()
        val draft = userDraft?.toString()?.trim()
        if (!draft.isNullOrEmpty() && draft.isNotBlank()) {
            AppBackgroundScope.launch {
                runCatching {
                    draftDao.upsert(Draft(threadId, postId ?: 0, subPostId ?: 0, draft))
                }
                .onFailure { e ->
                    Log.e(TAG, "onCleared: Save draft failed: ${e.message}, content: $draft")
                }
            }
        }
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { /*** NO-OP ***/ }

    override fun afterTextChanged(s: Editable?) { /*** NO-OP ***/ }

    companion object {
        private const val TAG = "ReplyViewModel"

        const val MAX_SELECTABLE_IMAGE = 9
    }
}

sealed interface ReplyUiIntent : UiIntent {
    data class UploadImages(
        val forumName: String,
        val imageUris: List<String>,
        val isOriginImage: Boolean
    ) : ReplyUiIntent

    data class Send(
        val content: String,
        val forumId: Long,
        val forumName: String,
        val threadId: Long,
        val tbs: String,
        val postId: Long? = null,
        val subPostId: Long? = null,
        val replyUserId: Long? = null,
    ) : ReplyUiIntent

    data class SwitchReplyType(val replyType: ReplyType) : ReplyUiIntent

    data class AddImage(val imageUris: List<String>) : ReplyUiIntent

    data class RemoveImage(val imageIndex: Int) : ReplyUiIntent

    data class ToggleIsOriginImage(val isOriginImage: Boolean) : ReplyUiIntent
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

    data class SwitchReplyType(val replyType: ReplyType) : ReplyPartialChange {
        override fun reduce(oldState: ReplyUiState): ReplyUiState =
            oldState.copy(replyType = replyType)
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
}

data class ReplyUiState(
    val isSending: Boolean = false,
    val replySuccess: Boolean = false,
    val replyType: ReplyType = ReplyType.NONE,
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