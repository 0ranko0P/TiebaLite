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
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.toRoute
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.booleanToString
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.pbPage.PbPageResponse
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaException
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorCode
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.firstOrThrow
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.components.ClipBoardLinkDetector
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.getBoolean
import com.huanchengfly.tieba.post.models.ThreadHistoryInfoBean
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.repository.EmptyDataException
import com.huanchengfly.tieba.post.repository.HistoryRepository
import com.huanchengfly.tieba.post.repository.HistoryType
import com.huanchengfly.tieba.post.repository.PbPageRepository
import com.huanchengfly.tieba.post.toJson
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.PbContentRender.Companion.TAG_LZ
import com.huanchengfly.tieba.post.ui.models.PostData
import com.huanchengfly.tieba.post.ui.models.SubPostItemData
import com.huanchengfly.tieba.post.ui.models.ThreadInfoData
import com.huanchengfly.tieba.post.ui.models.ThreadUiState
import com.huanchengfly.tieba.post.ui.models.UserData
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.Destination.Companion.navTypeOf
import com.huanchengfly.tieba.post.ui.page.Destination.Reply
import com.huanchengfly.tieba.post.ui.page.Destination.SubPosts
import com.huanchengfly.tieba.post.ui.widgets.compose.buildChipInlineContent
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.set
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils.Companion.KEY_REPLY_HIDE
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.TiebaUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlin.math.max
import kotlin.reflect.typeOf

@Stable
@HiltViewModel
class ThreadViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    private val historyRepo: HistoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val params = savedStateHandle.toRoute<Destination.Thread>(
        typeMap = mapOf(typeOf<ThreadFrom?>() to navTypeOf<ThreadFrom?>(isNullableAllowed = true))
    )

    val threadId: Long = params.threadId
    val postId: Long = params.postId

    private var _seeLz: Boolean by mutableStateOf(params.seeLz)
    /**
     * 只看楼主模式
     * */
    val seeLz: Boolean get() = _seeLz

    private var from: String = params.from?.tag ?: ""

    private var history: History? = null

    private var _threadUiState: MutableStateFlow<ThreadUiState> = MutableStateFlow(
        value = ThreadUiState(sortType = params.sortType)
    )
    val threadUiState: StateFlow<ThreadUiState> = _threadUiState.asStateFlow()

    private var _info: ThreadInfoData? by mutableStateOf(null)
    val info: ThreadInfoData? get() = _info

    /**
     * Post that have been marked for deletion, or delete thread if post id
     * is [firstPostId]
     *
     * @see onDeletePost
     * @see onDeleteThread
     * @see onDeleteConfirmed
     * */
    private var _deletePost: PostData? by mutableStateOf(null)
    val deletePost: PostData? get() = _deletePost

    var isImmersiveMode by mutableStateOf(false)
        private set

    /**
     * @see KEY_REPLY_HIDE
     * */
    var hideReply = false

    private val isRefreshing: Boolean
        get() = _threadUiState.value.isRefreshing

    private val isLoadingMore: Boolean
        get() = _threadUiState.value.isLoadingMore

    /**
     * Job of Add/Update/Remove favorite, cancelable.
     *
     * @see requestAddFavorite
     * @see requestRemoveFavorite
     * */
    private var favoriteJob: Job? = null

    private val handler = CoroutineExceptionHandler { _, e ->
        Log.e(TAG, "onError: ", e)
        _threadUiState.update {
            it.copy(isRefreshing = false, isLoadingMore = false, isLoadingLatestReply = false, error = e)
        }
    }

    val error: Throwable?
        get() = _threadUiState.value.error

    var curForumId: Long? = params.forumId

    val firstPostId: Long
        get() = _info?.firstPostId.takeIf { it != 0L } ?: threadUiState.value.firstPost?.id ?: 0L

    /**
     * One-off [UiEvent], but no guarantee to be received.
     * */
    private val _uiEvent: MutableStateFlow<UiEvent?> = MutableStateFlow(null)
    val uiEvent: Flow<UiEvent?> = _uiEvent.asSharedFlow()

    init {
        requestLoad(page = 0, postId)
        hideReply = context.dataStore.getBoolean(KEY_REPLY_HIDE, false)
    }

    fun requestLoad(page: Int = 1, postId: Long) {
        // Check refreshing
        if (isRefreshing) return else _threadUiState.set { copy(isRefreshing = true, error = null) }

        viewModelScope.launch(handler) {
            val sortType = _threadUiState.value.sortType
            val fromType = from.takeIf { it == FROM_STORE }.orEmpty()

            val response = PbPageRepository
                .pbPage(threadId, page, postId, curForumId, seeLz, sortType, from = fromType)
                .firstOrThrow()

            updateStateFrom(response)
            curForumId = _threadUiState.value.forum?.item?.id ?: curForumId
            sendUiEvent(ThreadUiEvent.LoadSuccess(response.data_!!.page!!.current_page))
        }
    }

    fun requestLoadFirstPage(seeLz: Boolean = _seeLz, sortType: Int = threadUiState.value.sortType) {
        // Check refreshing
        if (isRefreshing) return else _threadUiState.set { copy(isRefreshing = true, error = null) }

        _seeLz = seeLz
        viewModelScope.launch(handler) {
            val response = PbPageRepository
                .pbPage(threadId, 0, 0, curForumId, seeLz, sortType)
                .firstOrThrow()

            val firstPost = threadUiState.first().firstPost
            updateStateFrom(response)
            _threadUiState.update { it.copy(firstPost = firstPost, sortType = sortType) }
        }
    }

    fun requestLoadPrevious() {
        // Check refreshing
        if (isRefreshing) return else _threadUiState.set { copy(isRefreshing = true, error = null) }

        viewModelScope.launch(handler) {
            val state = _threadUiState.first()
            val page = max(state.currentPageMax - 1, 1)
            val postId = state.data.first().id

            val response = PbPageRepository
                .pbPage(threadId, page, postId, curForumId, seeLz, state.sortType, back = true)
                .firstOrThrow()

            val pageData = response.data_?.page ?: throw TiebaException("Null page")
            val author = response.data_.thread?.author ?: throw TiebaException("Null author")
            if (response.data_.forum == null) throw TiebaException("Null forum")
            if (response.data_.anti == null) throw TiebaException("Null anti")

            withContext(Dispatchers.Main) {
                _info = ThreadInfoData(response.data_.thread)
            }
            val newData = concatNewPostList(old = state.data, new = response.data_.post_list)

            _threadUiState.update {
                it.copy(
                    isRefreshing = false,
                    lz = UserData(author, true),
                    data = newData,
                    currentPageMin = pageData.current_page,
                    totalPage = pageData.new_total_page,
                    hasPrevious = pageData.has_prev != 0,
                )
            }
        }
    }

    fun requestLoadMore() {
        // Check loadingMore
        if (isRefreshing) return else _threadUiState.set { copy(isLoadingMore = true, error = null) }

        viewModelScope.launch(handler) {
            val state = _threadUiState.first()
            val sortType = state.sortType
            val page = state.run {
                if (sortType == ThreadSortType.BY_DESC) totalPage - currentPageMax else currentPageMax + 1
            }

            val response = PbPageRepository
                .pbPage(threadId, page, state.nextPagePostId, curForumId, seeLz, sortType)
                .firstOrThrow()

            val pageData = response.data_?.page ?: throw TiebaException("Null page")
            val author = response.data_.thread?.author ?: throw TiebaException("Null author")
            val forum = response.data_.forum?: throw TiebaException("Null forum")
            withContext(Dispatchers.Main) { _info = ThreadInfoData(response.data_.thread) }

            val newData = concatNewPostList(state.data, response.data_.post_list)
            val nextPagePostId = withContext(Dispatchers.Default) {
                val postIds = newData.mapTo(HashSet()) { it.id }
                response.data_.thread.getNextPagePostId(postIds, sortType)
            }
            _threadUiState.update {
                it.copy(
                    isLoadingMore = false,
                    lz = UserData(author, true),
                    data = newData,
                    forum = wrapImmutable(forum),
                    currentPageMax = pageData.current_page,
                    totalPage = pageData.new_total_page,
                    hasMore = pageData.has_more != 0,
                    nextPagePostId = nextPagePostId,
                    latestPosts = persistentListOf()
                )
            }
        }
    }

    /**
     * 加载当前贴子的最新回复
     */
    fun requestLoadLatestPosts() = viewModelScope.launch(handler) {
        // Check loadingMore
        if (isLoadingMore) return@launch else _threadUiState.update { it.copy(isLoadingMore = true, error = null) }

        val state = _threadUiState.first()
        val curLatestPostId = state.data.last().id
        val sortType = state.sortType

        PbPageRepository.pbPage(
            threadId = threadId,
            page = 0,
            postId = curLatestPostId,
            forumId = curForumId,
            seeLz = seeLz,
            sortType = sortType,
            lastPostId = curLatestPostId
        ).catch {
            if (it is EmptyDataException) {
                sendMsg(R.string.no_more)
                _threadUiState.update { s -> s.copy(isLoadingMore = false, error = null) }
            } else {
                throw it
            }
        }.collect { response ->
            checkNotNull(response.data_)
            checkNotNull(response.data_.thread)
            val author = response.data_.thread.author?: throw TiebaException("Null Author")
            val page = response.data_.page ?: throw TiebaException("Null page")

            val newThreadUiState = withContext(Dispatchers.Default) {
                val postList = response.data_.post_list.filterNot { it.isUnwanted }
                if (postList.isEmpty()) {
                    return@withContext null
                }

                val postIds = postList.mapTo(HashSet()) { it.id }
                val newData = concatNewPostList(state.data, response.data_.post_list)

                ensureActive()
                _info = ThreadInfoData(response.data_.thread)
                state.copy(
                    isLoadingMore = false,
                    lz = UserData(author, true),
                    data = newData,
                    currentPageMax = page.current_page,
                    totalPage = page.new_total_page,
                    hasMore = page.has_more != 0,
                    nextPagePostId = response.data_.thread.getNextPagePostId(postIds, sortType),
                    latestPosts = persistentListOf(),
                )
            }

            if (newThreadUiState == null) {
                sendMsg(R.string.no_more)
                _threadUiState.update { it.copy(isLoadingMore = false) }
            } else {
                _threadUiState.update { newThreadUiState }
            }
        }
    }

    /**
     * 当前用户发送新的回复时，加载用户发送的回复
     */
    fun requestLoadMyLatestReply(newPostId: Long) {
        if (_threadUiState.value.isLoadingLatestReply) {
            return
        } else {
            _threadUiState.update { it.copy(isLoadingLatestReply = true, error = null) }
        }

        viewModelScope.launch(handler) {
            val state = _threadUiState.first()
            val isDesc = state.sortType == ThreadSortType.BY_DESC
            val curLatestPostFloor = if (isDesc) {
                state.data.firstOrNull()?.floor ?: 1 // DESC -> first
            } else {
                state.data.lastOrNull()?.floor ?: 1  // ASC  -> last
            }

            val response = PbPageRepository
                .pbPage(threadId, page = 0, postId = newPostId, forumId = curForumId)
                .firstOrThrow()

            val page = response.data_?.page?.current_page ?: throw TiebaException("Null page")
            val anti = response.data_.anti ?: throw TiebaException("Null ant")
            val hasNewPost: Boolean

            val newState = withContext(Dispatchers.Default) {
                val postData = response.data_.post_list.map { PostData.from(it) }
                val oldPostData = state.data
                val oldPostIds = oldPostData.mapTo(HashSet()) { it.id }
                hasNewPost = response.data_.post_list.any { !oldPostIds.contains(it.id) }
                val firstLatestPost = response.data_.post_list.first()

                val isContinuous = firstLatestPost.floor == curLatestPostFloor + 1
                val continuous = isContinuous || page == state.currentPageMax

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
                        latestPosts = emptyList()
                    )

                    hasNewPost -> state.copy(data = newPost, latestPosts = postData)

                    !hasNewPost -> state.copy(data = newPost, latestPosts = emptyList())

                    else -> state
                }
            }

            _threadUiState.update {
                it.copy(isLoadingLatestReply = false, error = null, anti = anti.wrapImmutable(), data = newState.data, latestPosts = newState.latestPosts)
            }
            if (hasNewPost) {
                sendUiEvent(ThreadUiEvent.ScrollToLatestReply)
            }
        }
    }

    /**
     * Add or update bookmark thread
     * */
    fun requestAddFavorite(markedPost: PostData) {
        favoriteJob?.let { if (it.isActive) it.cancel() }
        favoriteJob = MainScope().launch {
            TiebaApi.getInstance()
                .addStoreFlow(threadId, markedPost.id)
                .catch { sendMsg(R.string.message_update_collect_mark_failed, it.getErrorMessage()) }
                .collect { response ->
                    if (response.errorCode == 0) {  // update local status
                        _info = info!!.updateCollectStatus(collected = true, markedPost.id)
                        sendMsg(R.string.message_add_favorite_success, markedPost.floor)
                    } else {
                        sendMsg("${response.errorMsg}, Code ${response.errorCode}")
                    }
                }
        }
    }

    fun requestRemoveFavorite() {
        val state = _threadUiState.value
        val forumId = state.forum?.get { id } ?: curForumId
        if (forumId == null) {
            sendMsg(R.string.delete_store_failure, "Null ForumId"); return
        }
        favoriteJob?.let { if (it.isActive) it.cancel() }
        favoriteJob = viewModelScope.launch(handler) {
            TiebaApi.getInstance().removeStoreFlow(threadId, forumId, tbs = state.anti?.get { tbs })
                .catch {
                    sendMsg(R.string.delete_store_failure, it.getErrorMessage())
                }
                .collect { response ->
                    if (response.errorCode == 0) { // clear local status
                        _info = info!!.updateCollectStatus(collected = false, markPostId = 0)
                        sendMsg(R.string.delete_store_success)
                    } else {
                        sendMsg(R.string.delete_store_failure, response.errorMsg)
                    }
                }
        }
    }

    fun onPostLikeClicked(post: PostData) {
        if (post.like.loading) {
            sendMsg(R.string.toast_agree_loading); return
        } else if (threadUiState.value.user == null) {
            sendMsg(R.string.title_not_logged_in); return
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
                .catch {
                    _threadUiState.update { s -> s.updateLikedPost(post.id, liked, loading = false) }
                    sendMsg(R.string.snackbar_agree_fail, it.getErrorCode(), it.getErrorMessage())
                }
                .collect {
                    if (System.currentTimeMillis() - start < 400) { // Wait for button animation
                        delay(250)
                    }
                    _threadUiState.update { it.updateLikedPost(post.id, !liked, loading = false) }
                }
        }
    }

    fun onThreadLikeClicked() {
        when {
            firstPostId == 0L -> return
            threadUiState.value.user == null -> {
                sendMsg(R.string.title_not_logged_in); return
            }
            info!!.like.loading -> {
                sendMsg(R.string.toast_agree_loading); return
            }
        }

        viewModelScope.launch {
            val liked = info!!.like.liked
            TiebaApi.getInstance().opAgreeFlow(
                threadId = threadId.toString(),
                postId = firstPostId.toString(),
                opType = if (liked) 1 else 0, // 操作 0 = 点赞, 1 = 取消点赞
                objType = 3
            )
            .onStart {
                _info = info!!.updateLikeStatus(liked = !liked, loading = true)
            }
            .catch {
                sendMsg(context.getString(R.string.error_tip) + it.getErrorMessage())
                _info = info!!.updateLikeStatus(liked = liked, loading = false)
            }
            .collect {
                _info = info!!.updateLikeStatus(liked = !liked, loading = false)
            }
        }
    }

    fun onDeleteConfirmed() {
        val forumId = curForumId
        val post = _deletePost
        if (forumId == null || post == null) { // Won't happen
            sendMsg(R.string.message_unknown_error); return
        }
        if (post == threadUiState.value.firstPost) {
            requestDeleteThread(forumId)
        } else {
            requestDeletePost(forumId, post)
        }
        _deletePost = null
    }

    private fun requestDeletePost(forumId: Long, post: PostData) = viewModelScope.launch {
        val state = _threadUiState.first()
        val forumName = state.forum?.item?.name.orEmpty()
        val tbs = state.anti?.item?.tbs
        val isSelfPost = post.author.id == state.user?.id
        TiebaApi.getInstance()
            .delPostFlow(forumId, forumName, threadId, post.id, tbs, false, isSelfPost)
            .catch {
                sendMsg(R.string.toast_delete_failure, it.getErrorMessage())
            }
            .collect { // Remove this post from data list
                val oldData = state.data.toMutableList()
                val deleted = oldData.removeIf { it.id == post.id }
                if (deleted) {
                    _threadUiState.update { it.copy(data = oldData.toList()) }
                }
                sendMsg(R.string.toast_delete_success)
            }
    }

    private fun requestDeleteThread(forumId: Long) = viewModelScope.launch {
        val state = _threadUiState.first()
        val forumName = state.forum?.item?.name.orEmpty()
        val tbs = state.anti?.item?.tbs
        val isSelfThread = state.lz?.id == state.user?.id
        TiebaApi.getInstance()
            .delThreadFlow(forumId, forumName, threadId, tbs, isSelfThread, false)
            .catch {
                sendMsg(R.string.toast_delete_failure, it.getErrorMessage())
            }
            .collect {
                sendUiEvent(CommonUiEvent.NavigateUp)
            }
    }

    fun onLastPostVisibilityChanged(pid: Long, floor: Int?) = viewModelScope.launch {
        val author = _threadUiState.first().lz ?: return@launch
        val title = info?.title ?: return@launch
        history = withContext(Dispatchers.IO) {
            val bean = ThreadHistoryInfoBean(
                isSeeLz = seeLz,
                pid = pid.toString(),
                forumName = _threadUiState.first().forum?.get { name },
                floor = floor?.toString()
            )
            History(
                title = title,
                data = threadId.toString(),
                type = HistoryType.THREAD,
                extras = bean.toJson(),
                avatar =  StringUtil.getAvatarUrl(author.portrait),
                username = author.nameShow
            )
        }
    }

    fun onShareThread() = TiebaUtil.shareThread(context, info?.title?: "", threadId)

    fun onCopyThreadLink() {
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
        ThreadUiEvent.ToReplyDestination(
            Reply(
                forumId = curForumId ?: 0,
                forumName = threadUiState.value.forum?.get { name } ?: "",
                threadId = threadId,
            )
        )
    )

    fun onReplyPost(post: PostData) = sendUiEvent(
        ThreadUiEvent.ToReplyDestination(
            Reply(
                forumId = curForumId ?: 0,
                forumName = threadUiState.value.forum?.get { name } ?: "",
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
                forumId = curForumId ?: 0,
                forumName = threadUiState.value.forum?.get { name } ?: "",
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
        val forumId = curForumId?: return
        sendUiEvent(
            ThreadUiEvent.ToSubPostsDestination(SubPosts(threadId, forumId, post.id, subPostId))
        )
    }

    fun onDeletePost(post: PostData) {
        require(post.author.id == _threadUiState.value.user?.id)
        _deletePost = post
    }

    fun onDeleteThread() {
        _deletePost = threadUiState.value.firstPost
    }

    override fun onCleared() {
        super.onCleared()
        history?.let {
            MainScope().launch { historyRepo.save(it) }
        }
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

    private fun sendUiEvent(event: UiEvent) = _uiEvent.update { event }

    fun onUiEventReceived() = _uiEvent.update { null }

    @Throws(TiebaException::class)
    private fun updateStateFrom(response: PbPageResponse) {
        val pageData = response.data_?.page ?: throw TiebaException("Null page")
        val author = response.data_.thread?.author ?: throw TiebaException("Null author")
        val forum = response.data_.forum?: throw TiebaException("Null forum")
        val anti = response.data_.anti?: throw TiebaException("Null anti")
        val thread = response.data_.thread
        val postList = response.data_.post_list
        val postIds = postList.mapTo(HashSet()) { it.id }
        val firstPost = response.data_.first_floor_post?.let { PostData.from(it, lzId = author.id) }
        val nonFirstPosts = postList.filterNot { it.floor <= 1 } // 0楼: 伪装的广告, 1楼: 楼主

        val user = response.data_.user?.run {
            if (is_login == 1) UserData(user = this, isLz = id == author.id) else null
        }
        if (user == null) {
            hideReply = true
        }

        _info = ThreadInfoData(thread)
        _threadUiState.update {
            it.copy(
                isRefreshing = false,
                error = null,
                lz = UserData(author, true),
                user = user,
                data = nonFirstPosts.map { post -> PostData.from(post) }.toImmutableList(),
                firstPost = firstPost ?: it.firstPost,
                forum = wrapImmutable(forum),
                anti = wrapImmutable(anti),
                currentPageMin = pageData.current_page,
                currentPageMax = pageData.current_page,
                totalPage = pageData.new_total_page,
                hasMore = pageData.has_more != 0,
                nextPagePostId = thread.getNextPagePostId(postIds, sortType = it.sortType),
                hasPrevious = pageData.has_prev != 0,
                latestPosts = persistentListOf()
            )
        }
    }

    companion object {

        private const val TAG = "ThreadViewModel"

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

        // 0楼: 伪装的广告, 1楼: 楼主
        private val Post.isUnwanted
            get() = floor <= 1

        private suspend fun concatNewPostList(old: List<PostData>, new: List<Post>): List<PostData> = withContext(Dispatchers.Default) {
            val postIds = old.mapTo(HashSet()) { it.id }
            new.filterNot { it.isUnwanted || postIds.contains(it.id) } // filter out old post
                .map { PostData.from(it) } // map new posts
                .let { old + it }
        }

        private fun ThreadInfo.getNextPagePostId(postIds: Set<Long>, sortType: Int): Long {
            val fetchedPostIds = pids
                .split(",")
                .filterNot { it.isBlank() }
                .map { it.toLong() }
            if (sortType == ThreadSortType.BY_DESC) {
                return fetchedPostIds.firstOrNull() ?: 0
            }
            val nextPostIds = fetchedPostIds.filterNot { pid -> postIds.contains(pid) }
            return if (nextPostIds.isNotEmpty()) nextPostIds.last() else 0
        }
    }
}

sealed interface ThreadUiEvent : UiEvent {
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