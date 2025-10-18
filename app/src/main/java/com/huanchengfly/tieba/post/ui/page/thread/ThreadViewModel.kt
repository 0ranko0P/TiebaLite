package com.huanchengfly.tieba.post.ui.page.thread

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.toRoute
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.booleanToString
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.components.ClipBoardLinkDetector
import com.huanchengfly.tieba.post.models.ThreadHistoryInfoBean
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.repository.EmptyDataException
import com.huanchengfly.tieba.post.repository.HistoryRepository
import com.huanchengfly.tieba.post.repository.HistoryType
import com.huanchengfly.tieba.post.repository.PageData
import com.huanchengfly.tieba.post.repository.PbPageRepository
import com.huanchengfly.tieba.post.repository.PbPageUiResponse
import com.huanchengfly.tieba.post.repository.ThreadStoreRepository
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.toJson
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.PbContentRender.Companion.TAG_LZ
import com.huanchengfly.tieba.post.ui.models.PostData
import com.huanchengfly.tieba.post.ui.models.SubPostItemData
import com.huanchengfly.tieba.post.ui.models.ThreadInfoData
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.Destination.Companion.navTypeOf
import com.huanchengfly.tieba.post.ui.page.Destination.Reply
import com.huanchengfly.tieba.post.ui.page.Destination.SubPosts
import com.huanchengfly.tieba.post.ui.widgets.compose.buildChipInlineContent
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.set
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.TiebaUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlin.reflect.typeOf

@Stable
@HiltViewModel
class ThreadViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    private val historyRepo: HistoryRepository,
    private val storeRepo: ThreadStoreRepository,
    private val threadRepo: PbPageRepository,
    settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val params = savedStateHandle.toRoute<Destination.Thread>(
        typeMap = mapOf(typeOf<ThreadFrom?>() to navTypeOf<ThreadFrom?>(isNullableAllowed = true))
    )

    private val threadId: Long = params.threadId
    private val postId: Long = params.postId
    private val historyTimeStamp = System.currentTimeMillis()

    private var from: String = params.from?.tag ?: ""

    private var history: History? = null

    private var _threadUiState = MutableStateFlow(
        ThreadUiState(seeLz = params.seeLz, sortType = params.sortType)
    )
    val threadUiState: StateFlow<ThreadUiState> = _threadUiState.asStateFlow()

    val info: ThreadInfoData?
        get() = _threadUiState.value.thread

    /**
     * Post or Thread(FirstPost) marked for deletion.
     *
     * @see onDeletePost
     * @see onDeleteThread
     * */
    private val _deletePost: MutableStateFlow<PostData?> = MutableStateFlow(null)
    val deletePost: StateFlow<PostData?> = _deletePost.asStateFlow()

    var isImmersiveMode by mutableStateOf(false)
        private set

    var hideReply = false

    private val isRefreshing: Boolean
        get() = _threadUiState.value.isRefreshing

    private val isLoadingMore: Boolean
        get() = _threadUiState.value.isLoadingMore

    /**
     * Job of Add/Update/Remove thread collections, cancelable.
     *
     * @see updateCollections
     * @see removeFromCollections
     * */
    private var collectionsJob: Job? = null

    private val handler = CoroutineExceptionHandler { _, e ->
        Log.e(TAG, "onError: ", e)
        _threadUiState.update {
            it.copy(isRefreshing = false, isLoadingMore = false, isLoadingLatestReply = false, error = e)
        }
    }

    private val firstPostId: Long
        get() = threadUiState.value.firstPost?.id ?: 0L

    private val forumId: Long?
        get() = params.forumId ?: _threadUiState.value.forum?.first

    private val forumName: String?
        get() = threadUiState.value.forum?.second

    /**
     * One-off [UiEvent], but no guarantee to be received.
     * */
    private val _uiEvent: MutableSharedFlow<UiEvent?> = MutableSharedFlow()
    val uiEvent: Flow<UiEvent?>
        get() = _uiEvent

    init {
        requestLoad(page = 0, postId)
        viewModelScope.launch {
            val habitSettings = settingsRepository.habitSettings.flow
            hideReply = habitSettings.first().hideReply
        }
    }

    fun requestLoad(page: Int = 1, postId: Long) {
        if (isRefreshing) return // Check refreshing

        val oldState = _threadUiState.updateAndGet { it.copy(isRefreshing = true, error = null) }
        viewModelScope.launch(handler) {
            val sortType = oldState.sortType
            val fromType = from.takeIf { it == FROM_STORE }.orEmpty()
            val response = threadRepo
                .pbPage(threadId, page, postId, forumId, oldState.seeLz, sortType, from = fromType)
            _threadUiState.update { it.updateStateFrom(response) }
            sendUiEvent(ThreadUiEvent.LoadSuccess(page = response.page.current))
        }
    }

    fun requestLoadFirstPage() {
        if (isRefreshing) return // Check refreshing

        val oldState = _threadUiState.updateAndGet { it.copy(isRefreshing = true, error = null) }
        viewModelScope.launch(handler) {
            val sortType = oldState.sortType
            val response = threadRepo.pbPage(threadId, 0, 0, forumId, oldState.seeLz, sortType)
            val page = if (sortType != ThreadSortType.BY_DESC) {
                response.page
            } else {
                response.page.copy(current = response.page.total, nextPagePostId = response.posts.lastOrNull()?.id ?: 0)
            }

            _threadUiState.update {
                it.updateStateFrom(response).copy(firstPost = it.firstPost, page = page)
            }
        }
    }

    fun requestLoadPrevious() {
        if (isLoadingMore) return else _threadUiState.set { copy(isLoadingMore = true, error = null) }

        viewModelScope.launch(handler) {
            val state = _threadUiState.value
            val sortType = state.sortType
            val page = state.page.previousPage(sortType)
            val postId = state.data.first().id
            val response = threadRepo
                .pbPage(threadId, page, postId, forumId, state.seeLz, sortType, back = true)
            val newData = concatNewPostList(old = state.data, new = response.posts, asc = false)

            _threadUiState.update {
                it.copy(isLoadingMore = false, thread = response.thread, data = newData, page = response.page)
            }
        }
    }

    fun requestLoadMore() {
        if (isLoadingMore) return else _threadUiState.set { copy(isLoadingMore = true, error = null) }

        viewModelScope.launch(handler) {
            val state = _threadUiState.value
            val sortType = state.sortType
            val nextPage = state.page.nextPage(sortType)
            val response = threadRepo
                .pbPage(threadId, nextPage, state.page.nextPagePostId, forumId, state.seeLz, sortType)
            val newData = concatNewPostList(old = state.data, new = response.posts)

            _threadUiState.update {
                it.updateStateFrom(response).run {
                    copy(data = newData, page = page.copy(hasPrevious = state.page.hasPrevious))
                }
            }
        }
    }

    /**
     * 加载当前贴子的最新回复
     */
    fun requestLoadLatestPosts() = viewModelScope.launch(handler) {
        if (isLoadingMore) return@launch // Check loading status

        val state = _threadUiState.updateAndGet { it.copy(isLoadingMore = true, error = null) }
        val curLatestPostId = state.data.last().id
        runCatching {
            threadRepo.pbPage(
                threadId = threadId,
                page = 0,
                postId = curLatestPostId,
                forumId = forumId,
                seeLz = state.seeLz,
                sortType = state.sortType,
                lastPostId = curLatestPostId
            )
        }
        .onFailure { e ->
            if (e is EmptyDataException) {
                sendMsg(R.string.no_more)
                _threadUiState.update { s -> s.copy(isLoadingMore = false, error = null) }
            } else {
                throw e
            }
        }
        .onSuccess { response ->
            val data = concatNewPostList(state.data, response.posts)
            _threadUiState.update {
                it.copy(
                    isLoadingMore = false,
                    data = data,
                    thread = response.thread,
                    latestPosts = null,
                    page = response.page
                )
            }
        }
    }

    /**
     * 当前用户发送新的回复时，加载用户发送的回复
     */
    fun requestLoadMyLatestReply(newPostId: Long) {
        if (_threadUiState.value.isLoadingLatestReply) return

        viewModelScope.launch(handler) {
            val state = _threadUiState.updateAndGet { it.copy(isLoadingLatestReply = true, error = null) }
            val isDesc = state.sortType == ThreadSortType.BY_DESC
            val curLatestPostFloor = if (isDesc) {
                state.data.firstOrNull()?.floor ?: 1 // DESC -> first
            } else {
                state.data.lastOrNull()?.floor ?: 1  // ASC  -> last
            }

            val response = threadRepo.pbPage(threadId, page = 0, postId = newPostId, forumId = forumId)
            val hasNewPost: Boolean
            val newState = withContext(Dispatchers.Default) {
                val postData = response.posts
                val oldPostData = state.data
                val oldPostIds = oldPostData.mapTo(HashSet()) { it.id }
                hasNewPost = postData.any { !oldPostIds.contains(it.id) }
                val firstLatestPost = postData.first()
                val isContinuous = firstLatestPost.floor == curLatestPostFloor + 1
                val continuous = isContinuous || response.page.current == state.page.current

                val replacePostIndexes = oldPostData.mapIndexedNotNull { index, old ->
                    val replaceItemIndex = postData.indexOfFirst { it.id == old.id }
                    if (replaceItemIndex != -1) index to replaceItemIndex else null
                }
                val newPost = oldPostData.mapIndexed { index, oldItem ->
                    val replaceIndex = replacePostIndexes.firstOrNull { it.first == index }
                    if (replaceIndex != null) postData[replaceIndex.second] else oldItem
                }
                val addPosts = postData.filter { old ->
                    !newPost.any { new -> new.id == old.id }
                }
                ensureActive()

                when {
                    hasNewPost && continuous -> state.copy(
                        data = if (isDesc) addPosts.reversed() + newPost else newPost + addPosts,
                        latestPosts = null
                    )

                    hasNewPost -> state.copy(data = newPost, latestPosts = postData)

                    !hasNewPost -> state.copy(data = newPost, latestPosts = null)

                    else -> state
                }
            }

            _threadUiState.update {
                it.copy(isLoadingLatestReply = false, error = null, tbs = response.tbs, data = newState.data, latestPosts = newState.latestPosts)
            }
            if (hasNewPost) {
                sendUiEvent(ThreadUiEvent.ScrollToLatestReply)
            }
        }
    }

    /**
     * 收藏/更新这个帖子到 [markedPost] 楼
     * */
    fun updateCollections(markedPost: PostData) {
        collectionsJob?.let { if (it.isActive) it.cancel() }
        // Launch in different CoroutineScope
        collectionsJob = MainScope().launch {
            storeRepo.add(threadId, postId = markedPost.id)
                .onFailure { e ->
                    sendMsg(R.string.message_update_collect_mark_failed, e.getErrorMessage())
                }
                .onSuccess {
                    _threadUiState.update {
                        it.copy(thread = it.thread!!.updateCollectStatus(collected = true, markPostId = markedPost.id))
                    }
                    sendMsg(R.string.message_add_favorite_success, markedPost.floor)
                }
        }
    }

    /**
     * 取消收藏这个帖子
     * */
    fun removeFromCollections() {
        if (collectionsJob?.isActive == true) {
            sendMsg(R.string.toast_connecting)
            return
        }

        collectionsJob = viewModelScope.launch(handler) {
            val state = _threadUiState.first()
            runCatching {
                if (state.thread?.collected == false) throw IllegalStateException()
                storeRepo.remove(threadId, forumId = forumId, tbs = state.tbs)
            }
            .onFailure { e ->
                sendMsg(R.string.delete_store_failure, e.getErrorMessage())
            }
            .onSuccess {
                _threadUiState.update {
                    it.copy(thread = it.thread!!.updateCollectStatus(collected = false, markPostId = 0))
                }
                sendMsg(R.string.delete_store_success)
            }
        }
    }

    fun onPostLikeClicked(post: PostData) {
        if (threadUiState.value.user == null) {
            sendUiEvent(ThreadLikeUiEvent.NotLoggedIn); return
        } else if (post.like.loading) {
            sendUiEvent(ThreadLikeUiEvent.Connecting); return
        }

        viewModelScope.launch {
            val start = System.currentTimeMillis()
            val liked = post.like.liked
            val opType = if (liked) 1 else 0 // 操作 0 = 点赞, 1 = 取消点赞

            TiebaApi.getInstance()
                .opAgreeFlow(threadId.toString(), post.id.toString(), opType, objType = 1)
                .onStart {
                    _threadUiState.update { it.updateLikedPost(post.id, !liked, loading = true) }
                }
                .catch { e ->
                    sendUiEvent(ThreadLikeUiEvent.Failed(e))
                    _threadUiState.update { it.updateLikedPost(post.id, liked, loading = false) }
                }
                .collect {
                    if (System.currentTimeMillis() - start < 400) { // Wait for button animation
                        delay(250)
                    }
                    _threadUiState.update { it.updateLikedPost(post.id, !liked, loading = false) }
                }
        }
    }

    fun onThreadLikeClicked() = viewModelScope.launch(handler) {
        val stateSnapshot = _threadUiState.value
        val oldThread = stateSnapshot.thread ?: throw NullPointerException()
        val like = oldThread.like

        // check user logged in & requesting like status update
        if (stateSnapshot.user == null) {
            sendUiEvent(ThreadLikeUiEvent.NotLoggedIn); return@launch
        } else if (like.loading) {
            sendUiEvent(ThreadLikeUiEvent.Connecting); return@launch
        }

        _threadUiState.update { it.copy(thread = oldThread.updateLikeStatus(liked = !like.liked, loading = true)) }
        runCatching {
            threadRepo.requestLikeThread(oldThread)
        }
        .onFailure { e ->
            sendUiEvent(ThreadLikeUiEvent.Failed(e))
            _threadUiState.update { it.copy(thread = oldThread) } // Reset to old thread
        }
        .onSuccess { _ ->
            _threadUiState.update { // Update like loading status
                it.copy(thread = oldThread.updateLikeStatus(liked = !like.liked, loading = false))
            }
        }
    }

    fun onDeleteConfirmed(): Job = viewModelScope.launch(handler) {
        val post = _deletePost.getAndUpdate { null } ?: throw NullPointerException()
        if (post.id == threadUiState.value.firstPost!!.id) {
            requestDeleteThread()
        } else {
            requestDeletePost(post)
        }
    }

    /**
     * Mark my post for deletion
     *
     * @see onDeleteConfirmed
     * */
    fun onDeletePost(post: PostData) = _deletePost.update { post }

    /**
     * Mark my thread for deletion
     *
     * @see onDeleteConfirmed
     * */
    fun onDeleteThread() = _deletePost.update { threadUiState.value.firstPost }

    fun onDeleteCancelled() = _deletePost.update { null }

    private suspend fun requestDeletePost(post: PostData) {
        val state = _threadUiState.first()
        val delMyPost = post.author.id == state.user?.id
        runCatching {
            threadRepo.deletePost(post.id, state.thread!!, state.tbs, delMyPost)
        }
        .onFailure { e -> sendUiEvent(ThreadUiEvent.DeletePostFailed(message = e.getErrorMessage())) }
        .onSuccess {
            // Remove this post from data list
            _threadUiState.update { it.copy(data = it.data.fastFilter { p -> p.id != post.id }) }
            sendUiEvent(ThreadUiEvent.DeletePostSuccess)
        }
    }

    private suspend fun requestDeleteThread() {
        val state = _threadUiState.value
        val delMyThread = state.lz!!.id == state.user?.id
        runCatching {
            threadRepo.deleteThread(state.thread!!, state.tbs, delMyThread)
        }
        .onFailure { e -> sendUiEvent(ThreadUiEvent.DeletePostFailed(message = e.getErrorMessage())) }
        .onSuccess {
            sendUiEvent(CommonUiEvent.NavigateUp)
        }
    }

    fun onSeeLzChanged() {
        _threadUiState.update { it.copy(seeLz = !it.seeLz) }
        requestLoadFirstPage()
    }

    fun onSortChanged(sortType: Int) {
        _threadUiState.update { it.copy(sortType = sortType) }
        requestLoadFirstPage()
    }

    fun onLastPostVisibilityChanged(pid: Long, floor: Int?) = viewModelScope.launch {
        val state = threadUiState.value
        val author = state.lz ?: return@launch
        val title = state.thread?.title ?: return@launch

        history = withContext(Dispatchers.IO) {
            val bean = ThreadHistoryInfoBean(state.seeLz, pid, forumName, floor = floor?.toString())
            History(
                title = title,
                data = threadId.toString(),
                type = HistoryType.THREAD,
                timestamp = historyTimeStamp,
                extras = bean.toJson(),
                avatar =  StringUtil.getAvatarUrl(author.portrait),
                username = author.nameShow
            )
        }
    }

    fun onShareThread() = TiebaUtil.shareThread(context, info?.title?: "", threadId)

    fun onCopyThreadLink() {
        val seeLz = _threadUiState.value.seeLz
        val link = "https://tieba.baidu.com/p/$threadId?see_lz=${seeLz.booleanToString()}"
        TiebaUtil.copyText(context = context, text = link)
        ClipBoardLinkDetector.onCopyTiebaLink(link)
    }

    fun onReportThread(context: Context, navigator: NavController) = viewModelScope.launch {
        TiebaUtil.reportPost(context, navigator, firstPostId.toString())
    }

    fun onImmersiveModeChanged() {
        isImmersiveMode = !isImmersiveMode
    }

    fun onReplyThread() = sendUiEvent(
        event = ThreadUiEvent.ToReplyDestination(
            Reply(forumId = forumId ?: 0, forumName = forumName ?: "", threadId = threadId)
        )
    )

    fun onReplyPost(post: PostData) = sendUiEvent(
        event = ThreadUiEvent.ToReplyDestination(
            Reply(
                forumId = forumId ?: 0,
                forumName = forumName.orEmpty(),
                threadId = threadId,
                postId = post.id,
                replyUserId = post.author.id,
                replyUserName = post.author.nameShow.takeIf { name -> name.isNotEmpty() } ?: post.author.name,
                replyUserPortrait = post.author.portrait
            )
        )
    )

    fun onReplyClicked(post: PostData) {
        if (post.id == firstPostId) {
            onReplyThread()
        } else {
            onReplyPost(post)
        }
    }

    fun onReplySubPost(post: PostData, subPost: SubPostItemData) = sendUiEvent(
        ThreadUiEvent.ToReplyDestination(
            Reply(
                forumId = forumId ?: 0,
                forumName = forumName.orEmpty(),
                threadId = threadId,
                postId = post.id,
                subPostId = subPost.id,
                replyUserId = subPost.author.id,
                replyUserName = subPost.author.nameShow.takeIf { name -> name.isNotEmpty() }
                    ?: subPost.author.name,
                replyUserPortrait = subPost.author.portrait,
            )
        )
    )

    fun onOpenSubPost(post: PostData, subPostId: Long) {
        val forumId = forumId ?: return
        sendUiEvent(
            ThreadUiEvent.ToSubPostsDestination(SubPosts(threadId, forumId, post.id, subPostId))
        )
    }

    override fun onCleared() {
        super.onCleared()
        history?.let { historyRepo.save(it) }
    }

    private fun sendMsg(@StringRes int: Int, vararg formatArgs: Any) {
        sendMsg(context.getString(int, *formatArgs))
    }

    private fun sendMsg(@StringRes int: Int) = sendMsg(context.getString(int))

    private fun sendMsg(msg: String) {
        if (viewModelScope.isActive) {
            sendUiEvent(CommonUiEvent.Toast(msg))
        } else {
            context.toastShort(msg)
        }
    }

    private fun sendUiEvent(event: UiEvent) {
        viewModelScope.launch { _uiEvent.emit(event) }
    }

    private suspend fun ThreadUiState.updateStateFrom(response: PbPageUiResponse): ThreadUiState {
        withContext(Dispatchers.Main) {
            if (response.user == null) {
                hideReply = true
            }
        }

        return this.copy(
            isRefreshing = false,
            isLoadingMore = false,
            isLoadingLatestReply = false,
            error = null,
            user = response.user,
            data = response.posts,
            firstPost = response.firstPost ?: this.firstPost,
            tbs = response.tbs,
            thread = response.thread,
            latestPosts = null,
            page = response.page
        )
    }

    companion object {

        private const val TAG = "ThreadViewModel"

        private fun PageData.nextPage(sortType: Int): Int {
            val page = if (sortType == ThreadSortType.BY_DESC) current - 1 else current + 1
            return page.coerceIn(1, total)
        }

        private fun PageData.previousPage(sortType: Int): Int {
            val page = if (sortType == ThreadSortType.BY_DESC) current + 1 else current - 1
            return page.coerceIn(1, total)
        }

        @Volatile
        private var LzInlineContentMap: WeakReference<Map<String, InlineTextContent>?> = WeakReference(null)

        val cachedLzInlineContent: Map<String, InlineTextContent>
            @Composable get() = LzInlineContentMap.get() ?: synchronized(this) {
                LzInlineContentMap.get() ?: persistentMapOf(
                    TAG_LZ to buildChipInlineContent(
                        text = stringResource(id = R.string.tip_lz),
                        textStyle = MaterialTheme.typography.labelMedium
                    )
                ).apply { LzInlineContentMap = WeakReference(this) }
            }

        private fun ThreadUiState.updateLikedPost(postId: Long, liked: Boolean, loading: Boolean) = copy(
            data = this.data.fastMap { post ->
                if (post.id == postId) post.updateLikesCount(liked, loading) else post
            }
        )

        private suspend fun concatNewPostList(
            old: List<PostData>,
            new: List<PostData>,
            asc: Boolean = true
        ): List<PostData> = withContext(Dispatchers.Default) {
            val postIds = old.mapTo(HashSet()) { it.id }
            new.filterNot { postIds.contains(it.id) } // filter out old post
                .let { new ->
                    if (asc) old + new else new + old
                }
        }
    }
}

sealed interface ThreadUiEvent : UiEvent {
    class DeletePostFailed(val message: String) : ThreadUiEvent

    object DeletePostSuccess : ThreadUiEvent

    data object ScrollToFirstReply : ThreadUiEvent

    data object ScrollToLatestReply : ThreadUiEvent

    data class LoadSuccess(val page: Int) : ThreadUiEvent

    data class ToReplyDestination(val direction: Reply): ThreadUiEvent

    data class ToSubPostsDestination(val direction: SubPosts): ThreadUiEvent
}

object ThreadSortType {
    const val BY_ASC = 0
    const val BY_DESC = 1
    const val BY_HOT = 2
    const val DEFAULT = BY_ASC
}