package com.huanchengfly.tieba.post.ui.page.reply

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.text.Editable
import android.text.Spannable
import android.text.style.ImageSpan
import android.util.Log
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.util.fastForEach
import androidx.core.net.toUri
import androidx.core.text.getSpans
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.BaseViewModel
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.ControlledRunner
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.PartialChange
import com.huanchengfly.tieba.post.arch.PartialChangeProducer
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiIntent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.components.spans.EmoticonSpanV2
import com.huanchengfly.tieba.post.core.common.di.ApplicationScope
import com.huanchengfly.tieba.post.core.database.dao.DraftDao
import com.huanchengfly.tieba.post.core.database.model.Draft
import com.huanchengfly.tieba.post.core.network.exception.TiebaUnknownException
import com.huanchengfly.tieba.post.core.network.exception.getErrorCode
import com.huanchengfly.tieba.post.core.network.exception.getErrorMessage
import com.huanchengfly.tieba.post.core.network.model.AddThreadBean
import com.huanchengfly.tieba.post.core.network.model.UploadPictureResultBean
import com.huanchengfly.tieba.post.core.network.model.protos.addPost.AddPostResponse
import com.huanchengfly.tieba.post.repository.AddPostRepository
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.utils.Emoticon
import com.huanchengfly.tieba.post.utils.EmoticonManager
import com.huanchengfly.tieba.post.utils.EmoticonManager.getEmoticonIdByName
import com.huanchengfly.tieba.post.utils.EmoticonUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
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
    @ApplicationScope val coroutineScope: CoroutineScope,
    private val addPostRepository: AddPostRepository,
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
    }

    override fun createInitialState() = ReplyUiState()

    override fun createPartialChangeProducer(): PartialChangeProducer<ReplyUiIntent, ReplyPartialChange, ReplyUiState> {
        return ReplyPartialChangeProducer()
    }

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

    private inner class ReplyPartialChangeProducer: PartialChangeProducer<ReplyUiIntent, ReplyPartialChange, ReplyUiState> {

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
            )

        private fun ReplyUiIntent.Send.producePartialChange(): Flow<ReplyPartialChange.Send> {
            if (forumId != 0L && threadId == 0L ) {
                return addPostRepository
                    .addThread(
                        content,
                        forumId,
                        forumName,
                        title = title,
                        isHide = 1,
                        isTitle = if (title.isNullOrEmpty()) 1 else 0,
                    )
                    .map<AddThreadBean, ReplyPartialChange.Send> {
                        if (it.tid == null) throw TiebaUnknownException
                        viewModelScope.emitGlobalEvent(
                            GlobalEvent.AddThreadSuccess(
                                checkNotNull(it.tid?.toLong()),
                                checkNotNull(it.pid?.toLong()),
                                checkNotNull(it.errorMsg),
                            )
                        )
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
            return addPostRepository
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
                    val newPostId = checkNotNull(it.data_?.pid?.toLongOrNull())
                    viewModelScope.launch {
                        if (postId != null) {
                            emitGlobalEvent(
                                GlobalEvent.ReplySuccess(
                                    threadId,
                                    postId,
                                    postId,
                                    subPostId,
                                    newPostId
                                )
                            )
                        } else {
                            emitGlobalEvent(GlobalEvent.ReplySuccess(threadId, newPostId))
                        }
                    }
                    ReplyPartialChange.Send.Success(
                        threadId = it.data_!!.tid,
                        postId = it.data_!!.pid,
                        expInc = it.data_!!.exp?.inc.orEmpty()
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
                    val rec = addPostRepository.upload(
                        forumName = forumName,
                        images = imageUris.map { it.toUri() },
                        isOriginImage = isOriginImage
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
                replyUserId = replyUserId
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
                val spans = getEmoticonSpans(context, emoticonSize, source = input)
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
        coroutineScope.launch {
            draftDao.deleteByIds(threadId, postId ?: 0, subPostId ?: 0)
        }
    }

    override fun onCleared() {
        emoticonContentRunner.cancelCurrent()
        if (isTopicThread) return
        val draft = userDraft?.toString()?.trim() ?: return
        coroutineScope.launch {
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

        private suspend fun getEmoticonSpans(
            context: Context,
            size: Int,
            source: CharSequence?,
            emoticonType: Int = EmoticonUtil.EMOTICON_ALL_TYPE
        ): List<AnnotatedString.Range<ImageSpan>> = withContext(Dispatchers.Default) {
            if (source == null || source.length < 4) { // Minimum emotion text length
                return@withContext emptyList()
            }

            val spans = mutableListOf<AnnotatedString.Range<ImageSpan>>()
            try {
                val patternEmoticon = EmoticonUtil.getRegexPattern(emoticonType)
                val matcherEmoticon = patternEmoticon.matcher(source)
                while (matcherEmoticon.find()) {
                    val key = matcherEmoticon.group()
                    val start = matcherEmoticon.start()
                    val end = start + key.length
                    val group1 = matcherEmoticon.group(1) ?: ""
                    val id = getEmoticonIdByName(group1) ?: continue
                    val rec = runCatching { EmoticonManager.getEmoticonBitmap(id, size) }
                    val bitmap = rec.getOrNull() ?: continue
                    val emoticonDrawable = BitmapDrawable(context.resources, bitmap)
                    val span = EmoticonSpanV2(emoticonDrawable, size)
                    spans.add(AnnotatedString.Range(item = span, start = start, end = end))
                    ensureActive()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            return@withContext spans
        }
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
        val title: String? = null,
        val postId: Long? = null,
        val subPostId: Long? = null,
        val replyUserId: Long? = null,
    ) : ReplyUiIntent

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