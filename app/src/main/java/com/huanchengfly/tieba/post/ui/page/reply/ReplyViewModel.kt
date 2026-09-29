package com.huanchengfly.tieba.post.ui.page.reply

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.text.Editable
import android.text.Spannable
import android.text.style.ImageSpan
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.util.fastForEach
import androidx.core.text.getSpans
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.arch.BaseStateViewModel
import com.huanchengfly.tieba.post.arch.ControlledRunner
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.components.spans.EmoticonSpanV2
import com.huanchengfly.tieba.post.core.common.di.ApplicationScope
import com.huanchengfly.tieba.post.core.data.repository.AddPostRepository
import com.huanchengfly.tieba.post.core.database.dao.DraftDao
import com.huanchengfly.tieba.post.core.database.model.Draft
import com.huanchengfly.tieba.post.core.network.exception.getErrorCode
import com.huanchengfly.tieba.post.core.network.exception.getErrorMessage
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.reply.ReplyViewModel.Companion.ReplyVmFactory
import com.huanchengfly.tieba.post.utils.Emoticon
import com.huanchengfly.tieba.post.utils.EmoticonManager
import com.huanchengfly.tieba.post.utils.EmoticonManager.getEmoticonIdByName
import com.huanchengfly.tieba.post.utils.EmoticonUtil
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Stable
@HiltViewModel(assistedFactory = ReplyVmFactory::class)
class ReplyViewModel @AssistedInject constructor(
    @ApplicationContext val context: Context,
    @ApplicationScope val coroutineScope: CoroutineScope,
    @Assisted private val params: Destination.Reply,
    private val addPostRepository: AddPostRepository,
    private val draftDao: DraftDao,
) : BaseStateViewModel<ReplyUiState>() {

    private val forumId: Long
        get() = params.forumId

    val threadId = params.threadId
    val postId = params.postId
    private val subPostId = params.subPostId
    private val replyUserId = params.replyUserId

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
        Log.i(TAG, "onInit: emoticons: ${emoticons.size}, params: $params")
    }

    override fun createInitialState() = ReplyUiState()

    fun onAddImage(imageUris: List<Uri>) {
        // On device that don't support limited photo picker
        // Double check image uris size
        var images = currentState.selectedImageList + imageUris
        if (images.size > MAX_SELECTABLE_IMAGE) {
            images = images.subList(0, MAX_SELECTABLE_IMAGE)
        }
        _uiState.update { it.copy(selectedImageList = images) }
    }

    fun onRemoveImage(imageIndex: Int) {
        _uiState.update {
            it.copy(selectedImageList = it.selectedImageList - it.selectedImageList[imageIndex])
        }
    }

    fun onIsOriginImageChanged(isOriginImage: Boolean) {
        _uiState.update { it.copy(isOriginImage = isOriginImage) }
    }

    private suspend fun sendReplyInternal(content: String, title: String? = null) {
        _uiState.update { it.copy(isSending = true) }
        runCatching {
            if (forumId != 0L && threadId == 0L ) {
                val rec = addPostRepository.addThread(content, forumId, forumName = params.forumName, title)
                viewModelScope.emitGlobalEvent(
                    GlobalEvent.AddThreadSuccess(newThreadId = rec.tid, newPostId = rec.pid, msg = rec.message)
                )
                sendUiEvent(ReplyUiEvent.ReplySuccess())
            } else {
                val rec = addPostRepository.addPost(
                    content = content,
                    forumId = forumId,
                    forumName = params.forumName,
                    threadId = threadId,
                    postId = postId,
                    subPostId = subPostId,
                    replyUserId = replyUserId
                )
                val newPostId = checkNotNull(rec.pid.toLongOrNull()) { "Invalid PID: ${rec.pid}" }
                viewModelScope.emitGlobalEvent(
                    if (postId != null) {
                        GlobalEvent.ReplySuccess(threadId, newPostId = postId, postId, subPostId, newPostId)
                    } else {
                        GlobalEvent.ReplySuccess(threadId, newPostId)
                    }
                )
                sendUiEvent(ReplyUiEvent.ReplySuccess(expInc = rec.exp?.inc.orEmpty()))
            }
        }
        .onFailure { e ->
            Log.e(TAG, "onSendReplyInternal", e)
            _uiState.update { it.copy(isSending = false, replySuccess = false) }
            sendUiEvent(ReplyUiEvent.ReplyFailure(e.getErrorCode(), e.getErrorMessage()))
        }
        .onSuccess {
            _uiState.update { it.copy(isSending = false, replySuccess = true) }
        }
    }

    fun onSendReply(threadTitle: String) {
        val text = userDraft ?: return
        val replyContent = if (subPostId == null || subPostId == 0L) {
            text
        } else {
            "回复 #(reply, ${params.replyUserPortrait}, ${params.replyUserName}) :${text}"
        }
        launchInVM {
            sendReplyInternal(content = replyContent.toString(), title = threadTitle.takeIf { isTopicThread })
        }
    }

    fun onSendReplyWithImage(threadTitle: String?) {
        val oldState = currentState
        if (oldState.isUploading) return else _uiState.update { it.copy(isUploading = true) }

        launchInVM {
            val start = System.currentTimeMillis()
            val replyContent = userDraft?.toString() ?: ""
            val images = oldState.selectedImageList
            runCatching {
                addPostRepository.upload(forumName = params.forumName, images, isOriginImage = oldState.isOriginImage)
            }
            .onFailure { e ->
                Log.e(TAG, "onSendReplyWithImage: Upload image failed:", e)
                _uiState.update { it.copy(isUploading = false) }
                sendUiEvent(ReplyUiEvent.UploadFailure(e.getErrorMessage()))
            }
            .onSuccess { rec ->
                _uiState.update { it.copy(isUploading = false) }
                val imageContent = rec.joinToString("\n") { image ->
                    "#(pic,${image.picId ?: 0},${image.picInfo?.originPic?.width ?: 0},${image.picInfo?.originPic?.height ?: 0})"
                }
                sendReplyInternal(
                    content = "${replyContent}\n$imageContent",
                    title = threadTitle.takeIf { isTopicThread },
                )
                val cost = System.currentTimeMillis() - start
                Log.i(TAG, "onSendReplyWithImage: ${rec.size} images uploaded, cost ${cost}ms")
            }
        }
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

        @AssistedFactory
        interface ReplyVmFactory {
            fun create(params: Destination.Reply): ReplyViewModel
        }

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

@Immutable
data class ReplyUiState(
    val isSending: Boolean = false,
    val replySuccess: Boolean = false,
    val isUploading: Boolean = false,
    val isOriginImage: Boolean = false,
    val selectedImageList: List<Uri> = emptyList(),
) : UiState {

    fun isReplying(): Boolean = isUploading || isSending
}

sealed interface ReplyUiEvent : UiEvent {
    data class UploadFailure(val message: String) : ReplyUiEvent

    data class ReplySuccess(val expInc: String = "") : ReplyUiEvent

    data class ReplyFailure(val code: Int, val message: String): ReplyUiEvent
}