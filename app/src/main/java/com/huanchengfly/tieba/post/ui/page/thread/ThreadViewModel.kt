package com.huanchengfly.tieba.post.ui.page.thread

import android.content.Context
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
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.toRoute
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.Error
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.booleanToString
import com.huanchengfly.tieba.post.api.models.protos.Page
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorCode
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.BaseStateViewModel
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.TbLiteExceptionHandler
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.components.ClipBoardLinkDetector
import com.huanchengfly.tieba.post.models.database.ThreadHistory
import com.huanchengfly.tieba.post.repository.HistoryRepository
import com.huanchengfly.tieba.post.repository.PageData
import com.huanchengfly.tieba.post.repository.PbPageRepository
import com.huanchengfly.tieba.post.repository.PbPageUiResponse
import com.huanchengfly.tieba.post.repository.ThreadStoreRepository
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.common.PbContentRender.Companion.TAG_LZ
import com.huanchengfly.tieba.post.ui.models.PostData
import com.huanchengfly.tieba.post.ui.models.SubPostItemData
import com.huanchengfly.tieba.post.ui.models.ThreadInfoData
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.Destination.Companion.navTypeOf
import com.huanchengfly.tieba.post.ui.page.Destination.Reply
import com.huanchengfly.tieba.post.ui.page.Destination.SubPosts
import com.huanchengfly.tieba.post.ui.page.threadstore.ThreadStoreUiEvent
import com.huanchengfly.tieba.post.ui.widgets.compose.buildChipInlineContent
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.set
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
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
) : BaseStateViewModel<ThreadUiState>() {

    private val params = savedStateHandle.toRoute<Destination.Thread>(
        typeMap = mapOf(typeOf<ThreadFrom?>() to navTypeOf<ThreadFrom?>(isNullableAllowed = true))
    )

    private val threadId: Long = params.threadId
    private val postId: Long = params.postId
    private val historyTimeStamp = System.currentTimeMillis()

    private var from: String = params.from?.tag ?: ""

    val info: ThreadInfoData?
        get() = currentState.thread

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

    var hideReply by mutableStateOf(false)
        private set

    private val isRefreshing: Boolean
        get() = currentState.isRefreshing

    private val isLoadingMore: Boolean
        get() = currentState.isLoadingMore

    /**
     * Job of Add/Update/Remove thread collections, cancelable.
     *
     * @see updateCollections
     * @see removeFromCollections
     * */
    private var collectionsJob: Job? = null

    override val errorHandler = TbLiteExceptionHandler(TAG) { _, e, _ ->
        _uiState.update {
            it.copy(isRefreshing = false, isLoadingMore = false, isLoadingLatestReply = false, error = e)
        }
    }

    private val loadMoreHandler = CoroutineExceptionHandler { context, e ->
        if (e.getErrorCode() == Error.ERROR_POST_NOMORE || currentState.data.isNotEmpty()) {
            _uiState.update {
                it.copy(isRefreshing = false, isLoadingMore = false, isLoadingLatestReply = false, error = null)
            }
            sendUiEvent(CommonUiEvent.ToastError(e))
        } else {
            errorHandler.handleException(context = context, exception = e)
        }
    }

    private val firstPostId: Long
        get() = currentState.firstPost?.id ?: 0L

    private val forumId: Long?
        get() = params.forumId ?: currentState.forum?.first

    private val forumName: String?
        get() = currentState.forum?.second

    override fun createInitialState(): ThreadUiState {
        return ThreadUiState(seeLz = params.seeLz, sortType = params.sortType)
    }

    init {
        requestLoad(page = 0, postId = postId, scrollToReply = params.scrollToReply)
        viewModelScope.launch {
            hideReply = settingsRepository.habitSettings.snapshot().hideReply
        }
    }

    fun requestLoad(page: Int = 1, postId: Long, scrollToReply: Boolean = true) {
        if (isRefreshing) return // Check refreshing

        val oldState = _uiState.updateAndGet { it.copy(isRefreshing = true, error = null) }
        launchInVM {
            val sortType = oldState.sortType
            val fromType = from.takeIf { it == FROM_STORE }.orEmpty()
            val response = threadRepo
                .pbPage(threadId, page, postId, forumId, oldState.seeLz, sortType, from = fromType)
            val pageData = response.page.let {
                it.mapToUiModel(
                    previous = it.current_page,
                    nextPagePostId = response.nextPagePostId,
                    hasPrevious = if (sortType != ThreadSortType.BY_DESC) {
                        it.has_prev != 0 // Bug: Server returns wrong has_prev when FROM_STORE with seeLz enabled
                    } else {
                        // Check has previous manually if sort by DESC
                        it.total_page > 1 && it.current_page < it.total_page
                    }
                )
            }
            _uiState.update {
                it.updateStateFrom(response).copy(pageData = pageData)
            }
            if (scrollToReply) {
                sendUiEvent(ThreadUiEvent.LoadSuccess(page, postId))
            }
        }
    }

    fun requestLoadFirstPage() {
        if (isRefreshing) return // Check refreshing

        val oldState = _uiState.updateAndGet { it.copy(isRefreshing = true, error = null) }
        launchInVM {
            val sortType = oldState.sortType
            val isAscSorting = sortType == ThreadSortType.BY_ASC
            val response = threadRepo.pbPage(threadId, 0, 0, forumId, oldState.seeLz, sortType)
            val pageData = response.page.run {
                mapToUiModel(
                    previous = total_page,
                    current = if (isAscSorting) current_page else total_page,
                    nextPagePostId = if (isAscSorting) {
                        response.nextPagePostId
                    } else {
                        response.posts.lastOrNull()?.id ?: 0
                    }
                )
            }
            _uiState.update {
                it.updateStateFrom(response).copy(firstPost = it.firstPost, pageData = pageData)
            }
            // Scroll LazyList based on current sort type
            if (isAscSorting) {
                emitUiEvent(ThreadUiEvent.ScrollToFirstReply)
            } else {
                emitUiEvent(ThreadUiEvent.ScrollToLatestReply)
            }
        }
    }

    fun requestLoad(page: Int) {
        val state = currentState
        // Check target page is first page
        if ((page <= 1 && state.sortType == ThreadSortType.BY_ASC) ||
            (page == state.pageData.total && state.sortType == ThreadSortType.BY_DESC)
        ) {
            requestLoadFirstPage()
        } else {
            requestLoad(page, postId = 0)
        }
    }

    /**
     * Load previous page.
     *
     * @param offset offset of the first visible post. Will be used as backward scroll offset
     *   in [ThreadUiEvent.LoadPreviousSuccess]. This is a workaround for broken scroll
     *   position preservation caused by LoadPreviousButton.
     *
     * @see [PageData.hasPrevious]
     * */
    fun requestLoadPrevious(offset: Int) {
        if (isLoadingMore) return else _uiState.set { copy(isLoadingMore = true, error = null) }

        launchInVM(loadMoreHandler) {
            val state = currentState
            val sortType = state.sortType
            val page = state.pageData.previousPage(sortType)
            val postId = state.data.first().id
            val response = threadRepo
                .pbPage(threadId, page, postId, forumId, state.seeLz, sortType, back = true)
            val newData = concatNewPostList(old = state.data, new = response.posts, asc = false)
            val pageData = response.page.mapToUiModel(
                previous = response.page.current_page,
                current = state.pageData.current,
                hasMore = state.pageData.hasMore
            )

            _uiState.update {
                it.copy(isLoadingMore = false, thread = response.thread, data = newData, pageData = pageData)
            }
            // Scroll to previous floor
            val previousIndex = withContext(Dispatchers.Default) {
                newData.indexOfFirst { p -> p.floor == state.data[0].floor }
            }
            // Check no visible post(covered by BottomBar) || empty new data
            if (offset > 0 && previousIndex > 0) {
                emitUiEvent(ThreadUiEvent.LoadPreviousSuccess(previousIndex, -offset))
            }
        }
    }

    fun requestLoadMore() {
        if (isLoadingMore) return else _uiState.set { copy(isLoadingMore = true, error = null) }

        launchInVM(loadMoreHandler) {
            val state = currentState
            val sortType = state.sortType
            val nextPage = state.pageData.nextPage(sortType)
            val response = threadRepo
                .pbPage(threadId, nextPage, state.pageData.nextPagePostId, forumId, state.seeLz, sortType)
            val newData = concatNewPostList(old = state.data, new = response.posts)
            val pageData = response.page.mapToUiModel(
                previous = state.pageData.previous,
                nextPagePostId = response.nextPagePostId,
                hasPrevious = state.pageData.hasPrevious
            )

            _uiState.update {
                it.updateStateFrom(response).copy(data = newData, pageData = pageData)
            }
        }
    }

    /**
     * 加载当前贴子的最新回复
     */
    fun requestLoadLatestPosts() = launchInVM(loadMoreHandler) {
        if (isLoadingMore) return@launchInVM // Check loading status

        val state = _uiState.updateAndGet { it.copy(isLoadingMore = true, error = null) }
        val curLatestPostId = state.data.last().id
        val response = threadRepo.pbPage(
            threadId = threadId,
            page = 0,
            postId = curLatestPostId,
            forumId = forumId,
            seeLz = state.seeLz,
            sortType = state.sortType,
            lastPostId = curLatestPostId
        )
        val data = concatNewPostList(state.data, response.posts)
        val pageData = response.page.mapToUiModel(
            previous = state.pageData.previous,
            nextPagePostId = response.nextPagePostId,
            hasPrevious = state.pageData.hasPrevious
        )
        _uiState.update {
            it.copy(isLoadingMore = false, data = data, thread = response.thread, latestPosts = null, pageData = pageData)
        }
    }

    /**
     * 当前用户发送新的回复时，加载用户发送的回复
     */
    fun requestLoadMyLatestReply(newPostId: Long) {
        if (currentState.isLoadingLatestReply) return

        launchInVM(loadMoreHandler) {
            val state = _uiState.updateAndGet { it.copy(isLoadingLatestReply = true, error = null) }
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
                val continuous = isContinuous || response.page.current_page == state.pageData.current

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

            _uiState.update {
                it.copy(isLoadingLatestReply = false, error = null, tbs = response.tbs, data = newState.data, latestPosts = newState.latestPosts)
            }
            if (hasNewPost) {
                emitUiEvent(ThreadUiEvent.ScrollToLatestReply)
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
                    emitUiEvent(ThreadStoreUiEvent.Add.Failure(message = e.getErrorMessage()))
                }
                .onSuccess {
                    _uiState.update {
                        it.copy(thread = it.thread!!.copy(collectMarkPid = markedPost.id))
                    }
                    emitUiEvent(ThreadStoreUiEvent.Add.Success(markedPost.floor))
                }
        }
    }

    /**
     * 取消收藏这个帖子
     * */
    fun removeFromCollections() {
        if (collectionsJob?.isActive == true) {
            sendUiEvent(ThreadStoreUiEvent.Loading)
            return
        }

        collectionsJob = launchJobInVM {
            val state = _uiState.first()
            runCatching {
                require(state.thread!!.collected)
                storeRepo.remove(threadId, forumId = forumId, tbs = state.tbs)
            }
            .onFailure { e ->
                emitUiEvent(ThreadStoreUiEvent.Delete.Failure(message = e.getErrorMessage()))
            }
            .onSuccess {
                _uiState.update { it.copy(thread = it.thread!!.copy(collectMarkPid = null)) }
                emitUiEvent(ThreadStoreUiEvent.Delete.Success)
            }
        }
    }

    fun onPostLikeClicked(post: PostData) {
        if (currentState.user == null) {
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
                    _uiState.update { it.updateLikedPost(post.id, !liked, loading = true) }
                }
                .catch { e ->
                    sendUiEvent(ThreadLikeUiEvent.Failed(e))
                    _uiState.update { it.updateLikedPost(post.id, liked, loading = false) }
                }
                .collect {
                    if (System.currentTimeMillis() - start < 400) { // Wait for button animation
                        delay(250)
                    }
                    _uiState.update { it.updateLikedPost(post.id, !liked, loading = false) }
                }
        }
    }

    fun onThreadLikeClicked(): Unit = launchInVM {
        val stateSnapshot = currentState
        val oldThread = stateSnapshot.thread ?: throw NullPointerException()
        val like = oldThread.like

        // check user logged in & requesting like status update
        if (stateSnapshot.user == null) {
            emitUiEvent(ThreadLikeUiEvent.NotLoggedIn); return@launchInVM
        } else if (like.loading) {
            emitUiEvent(ThreadLikeUiEvent.Connecting); return@launchInVM
        }

        _uiState.update { it.copy(thread = oldThread.updateLikeStatus(liked = !like.liked, loading = true)) }
        runCatching {
            threadRepo.requestLikeThread(oldThread)
        }
        .onFailure { e ->
            sendUiEvent(ThreadLikeUiEvent.Failed(e))
            _uiState.update { it.copy(thread = oldThread) } // Reset to old thread
        }
        .onSuccess { _ ->
            _uiState.update { // Update like loading status
                it.copy(thread = oldThread.updateLikeStatus(liked = !like.liked, loading = false))
            }
        }
    }

    fun onDeleteConfirmed(): Job = launchJobInVM {
        val post = _deletePost.getAndUpdate { null } ?: throw NullPointerException()
        if (post.id == currentState.firstPost!!.id) {
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
    fun onDeleteThread() = _deletePost.update { currentState.firstPost }

    fun onDeleteCancelled() = _deletePost.update { null }

    private suspend fun requestDeletePost(post: PostData) {
        val state = _uiState.first()
        val delMyPost = post.author.id == state.user?.id
        runCatching {
            threadRepo.deletePost(post.id, state.thread!!, state.tbs, delMyPost)
        }
        .onFailure { e -> sendUiEvent(ThreadUiEvent.DeletePostFailed(message = e.getErrorMessage())) }
        .onSuccess {
            // Remove this post from data list
            _uiState.update { it.copy(data = it.data.fastFilter { p -> p.id != post.id }) }
            sendUiEvent(ThreadUiEvent.DeletePostSuccess)
        }
    }

    private suspend fun requestDeleteThread() {
        val state = currentState
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
        val newState = _uiState.updateAndGet { it.copy(seeLz = !it.seeLz) }
        val collectMarkPid = newState.thread?.collectMarkPid
        // Jump to collectMarkPid when seeLz switched off
        if (!newState.seeLz && collectMarkPid != null && collectMarkPid != newState.firstPost?.id) {
            requestLoad(0, postId = newState.thread.collectMarkPid)
        } else {
            requestLoadFirstPage()
        }
    }

    fun onSortChanged(@ThreadSortType sortType: Int) {
        _uiState.update { it.copy(sortType = sortType) }
        requestLoadFirstPage()
    }

    fun onSaveHistory(lastVisiblePost: PostData?) {
        val state = currentState
        val author = state.lz ?: return
        val title = state.thread?.title ?: return

        val history = ThreadHistory(
            id = threadId,
            avatar = author.avatarUrl,
            name = author.nameShow,
            forum = forumName,
            title = title,
            isSeeLz = state.seeLz,
            pid = lastVisiblePost?.takeIf { it.id > 0 && it.floor > 5 }?.id ?: 0, // 大于 5 楼
            timestamp = historyTimeStamp,
        )
        historyRepo.saveHistory(history)
    }

    fun onShareThread() = TiebaUtil.shareThread(context, info?.title?: "", threadId)

    fun onCopyThreadLink() {
        val seeLz = currentState.seeLz
        val link = "https://tieba.baidu.com/p/$threadId?see_lz=${seeLz.booleanToString()}"
        TiebaUtil.copyText(context = context, text = link)
        ClipBoardLinkDetector.onCopyTiebaLink(link)
    }

    fun onReportThread(navigator: NavController) = viewModelScope.launch {
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

    private fun ThreadUiState.updateStateFrom(response: PbPageUiResponse): ThreadUiState {
        if (response.user == null) {
            hideReply = true
        }

        return this.copy(
            isRefreshing = false,
            isLoadingMore = false,
            isLoadingLatestReply = false,
            error = null,
            user = response.user,
            data = response.posts,
            firstPost = this.firstPost ?: response.firstPost, // use old firstPost if possible
            tbs = response.tbs,
            thread = response.thread,
            latestPosts = null,
        )
    }

    private fun Page.mapToUiModel(
        current: Int = current_page,
        previous: Int = 0,
        nextPagePostId: Long = 0,
        // Note: Do not use has_more, check manually
        hasMore: Boolean = if (currentState.sortType != ThreadSortType.BY_DESC) {
            new_total_page > 1 && current < new_total_page
        } else {
            new_total_page > 1 && current > 1
        },
        // Note: Use has_prev only when load with post
        hasPrevious: Boolean = if (currentState.sortType != ThreadSortType.BY_DESC) {
            new_total_page > 1 && previous > 1 && previous < new_total_page
        } else {
            new_total_page > 1 && previous < new_total_page
        },
    ): PageData = PageData(
        current = current,
        previous = previous,
        total = new_total_page,
        postCount = total_count,
        nextPagePostId = nextPagePostId,
        hasMore = hasMore,
        hasPrevious = hasPrevious
    )

    companion object {

        private const val TAG = "ThreadViewModel"

        private fun PageData.nextPage(sortType: Int): Int {
            val page = if (sortType == ThreadSortType.BY_DESC) current - 1 else current + 1
            return page.coerceIn(1, total)
        }

        private fun PageData.previousPage(sortType: Int): Int {
            val page = if (sortType == ThreadSortType.BY_DESC) previous + 1 else previous - 1
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

    object ScrollToFirstReply : ThreadUiEvent

    object ScrollToLatestReply : ThreadUiEvent

    data class LoadPreviousSuccess(val previousIndex: Int, val offset: Int) : ThreadUiEvent

    data class LoadSuccess(val page: Int, val postId: Long) : ThreadUiEvent

    data class ToReplyDestination(val direction: Reply): ThreadUiEvent

    data class ToSubPostsDestination(val direction: SubPosts): ThreadUiEvent
}
