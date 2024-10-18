package com.huanchengfly.tieba.post.ui.page.thread

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.toRoute
import com.huanchengfly.tieba.post.App
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
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.getBoolean
import com.huanchengfly.tieba.post.models.ThreadHistoryInfoBean
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.removeAt
import com.huanchengfly.tieba.post.repository.EmptyDataException
import com.huanchengfly.tieba.post.repository.PbPageRepository
import com.huanchengfly.tieba.post.toJson
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
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
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils.Companion.KEY_REPLY_HIDE
import com.huanchengfly.tieba.post.utils.HistoryUtil
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.TiebaUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlin.math.max
import kotlin.reflect.typeOf

@Stable
@HiltViewModel
class ThreadViewModel @Inject constructor(savedStateHandle: SavedStateHandle) : ViewModel() {

    val params = savedStateHandle.toRoute<Destination.Thread>(
        typeMap = mapOf(typeOf<ThreadStoreExtra?>() to navTypeOf<ThreadStoreExtra?>(isNullableAllowed = true))
    )

    val threadId: Long = params.threadId
    val postId: Long = params.postId

    private var _seeLz: Boolean by mutableStateOf(params.seeLz)
    /**
     * 只看楼主模式
     * */
    val seeLz: Boolean get() = _seeLz

    private var from: String = params.from

    private var history: History? = null

    private var _threadUiState: ThreadUiState by mutableStateOf(ThreadUiState())
    val threadUiState: ThreadUiState get() = _threadUiState

    private var _info: ThreadInfoData? by mutableStateOf(null)
    val info: ThreadInfoData? get() = _info

    val data: List<PostData> get() = _threadUiState.data

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

    /**
     * 楼主 Data
     * */
    val lz: UserData? get() = _threadUiState.lz

    var isImmersiveMode by mutableStateOf(false)
        private set

    /**
     * @see KEY_REPLY_HIDE
     * */
    var hideReply = false

    val isRefreshing: Boolean get() = _threadUiState.isRefreshing
    val isLoadingMore: Boolean get() = _threadUiState.isLoadingMore
    val isLoadingLatestReply: Boolean get() = _threadUiState.isLoadingLatestReply

    /**
     * Job of Add/Update/Remove favorite, cancelable.
     *
     * @see requestAddFavorite
     * @see requestRemoveFavorite
     * */
    private var favoriteJob: Job? = null

    private val handler = CoroutineExceptionHandler { _, e ->
        Log.e(TAG, "onError: ", e)
        _threadUiState = _threadUiState.copy(
            isRefreshing = false, isLoadingMore = false, isLoadingLatestReply = false, error = e.wrapImmutable()
        )
    }

    val error: Throwable? get() = _threadUiState.error?.item

    var curForumId: Long? = params.forumId

    val firstPostId: Long
        get() = _info?.firstPostId.takeIf { it != 0L } ?: threadUiState.firstPost?.id ?: 0L

    /**
     * One-off [UiEvent], but guaranteed to be received.
     *
     * @see onUiEventReceived
     * */
    private val _uiEvent: MutableState<UiEvent?> = mutableStateOf(null)
    val uiEvent: State<UiEvent?> = _uiEvent

    init {
        _threadUiState = _threadUiState.copy(sortType = params.sortType)
        requestLoad(page = 0, postId)
        viewModelScope.launch {
            hideReply = App.INSTANCE.dataStore.getBoolean(KEY_REPLY_HIDE, false)
        }
    }

    fun requestLoad(page: Int = 1, postId: Long) {
        if (isRefreshing) return
        _threadUiState = _threadUiState.copy(isRefreshing = true, error = null)
        viewModelScope.launch(handler) {
            val sortType = _threadUiState.sortType
            val fromType = from.takeIf { it == ThreadPageFrom.FROM_STORE }.orEmpty()
            PbPageRepository
                .pbPage(threadId, page, postId, curForumId, seeLz, sortType, from = fromType)
                .collect { response ->
                    updateStateFrom(response)
                    curForumId = _threadUiState.forum?.item?.id ?: curForumId
                    sendUiEvent(ThreadUiEvent.LoadSuccess(response.data_!!.page!!.current_page))
                }
        }
    }

    fun requestLoadFirstPage(seeLz: Boolean = _seeLz, sortType: Int = threadUiState.sortType) {
        if (isRefreshing) return
        _threadUiState = _threadUiState.copy(isRefreshing = true, error = null)

        _seeLz = seeLz
        viewModelScope.launch(handler) {
            PbPageRepository.pbPage(threadId, 0, 0, curForumId, seeLz, sortType)
                .collect { response ->
                    val firstPost = threadUiState.firstPost
                    updateStateFrom(response)
                    _threadUiState = _threadUiState.copy(firstPost = firstPost, sortType = sortType)
                }
        }
    }

    fun requestLoadPrevious() {
        if (isRefreshing) return
        _threadUiState = _threadUiState.copy(isRefreshing = true, error = null)

        val page = max(threadUiState.currentPageMax - 1, 1)
        val postId = threadUiState.data.first().id
        val sortType = threadUiState.sortType
        viewModelScope.launch(handler) {
            PbPageRepository
                .pbPage(threadId, page, postId, curForumId, seeLz, sortType, back = true)
                .collect { response ->
                    val pageData = response.data_?.page ?: throw TiebaException("Null page")
                    val author = response.data_.thread?.author ?: throw TiebaException("Null author")
                    if (response.data_.forum == null) throw TiebaException("Null forum")
                    if (response.data_.anti == null) throw TiebaException("Null anti")
                    // Filter out old post
                    val uniqueData = filterUnique(threadUiState.data, response.data_.post_list)

                    _info = ThreadInfoData(response.data_.thread)
                    _threadUiState = threadUiState.copy(
                        isRefreshing = false,
                        lz = UserData(author, true),
                        data = (uniqueData + threadUiState.data).toImmutableList(),
                        currentPageMin = pageData.current_page,
                        totalPage = pageData.new_total_page,
                        hasPrevious = pageData.has_prev != 0,
                    )
                }
        }
    }

    fun requestLoadMore() = viewModelScope.launch(handler) {
        if (isLoadingMore) return@launch
        _threadUiState = _threadUiState.copy(isLoadingMore = true, error = null)

        val nextPagePostId = threadUiState.nextPagePostId
        val sortType = threadUiState.sortType

        val page = threadUiState.run {
            if (sortType == ThreadSortType.BY_DESC) totalPage - currentPageMax else currentPageMax + 1
        }
        PbPageRepository
            .pbPage(threadId, page, nextPagePostId, curForumId, seeLz, sortType)
            .collect { response ->
                val pageData = response.data_?.page ?: throw TiebaException("Null page")
                val author = response.data_.thread?.author ?: throw TiebaException("Null author")
                val forum = response.data_.forum?: throw TiebaException("Null forum")

                _info = ThreadInfoData(response.data_.thread)
                _threadUiState = withContext(Dispatchers.IO) {
                    val oldData = threadUiState.data
                    val uniqueData = filterUnique(oldData, response.data_.post_list)
                    val postIds = (oldData + uniqueData).mapTo(HashSet()) { it.id }
                    threadUiState.copy(
                        isLoadingMore = false,
                        lz = UserData(author, true),
                        data = (oldData + uniqueData).toImmutableList(),
                        forum = wrapImmutable(forum),
                        currentPageMax = pageData.current_page,
                        totalPage = pageData.new_total_page,
                        hasMore = pageData.has_more != 0,
                        nextPagePostId = response.data_.thread.getNextPagePostId(postIds, sortType),
                        latestPosts = persistentListOf()
                    )
                }
            }
    }

    /**
     * 加载当前贴子的最新回复
     */
    fun requestLoadLatestPosts() = viewModelScope.launch(handler) {
        if (isLoadingMore) return@launch
        _threadUiState = _threadUiState.copy(isLoadingMore = true, error = null)

        val curLatestPostId = threadUiState.data.last().id
        val sortType = threadUiState.sortType
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
                _threadUiState = _threadUiState.copy(isLoadingMore = false)
            } else {
                handler.handleException(currentCoroutineContext(), it)
            }
        }.collect { response ->
            checkNotNull(response.data_)
            checkNotNull(response.data_.thread)
            val author = response.data_.thread.author?: throw TiebaException("Null Author")
            val page = response.data_.page ?: throw TiebaException("Null page")

            val newThreadUiState = withContext(Dispatchers.IO) {
                val postList = response.data_.post_list.filterNot { it.floor == 1 }
                if (postList.isEmpty()) {
                    return@withContext null
                } else {
                    val oldData = threadUiState.data
                    val oldPostIds = oldData.mapTo(HashSet()) { it.id }
                    val uniqueData = postList
                        .filterNot { item -> oldPostIds.contains(item.id) }
                        .map { post -> PostData.from(post) }

                    ensureActive()
                    _info = ThreadInfoData(response.data_.thread)
                    return@withContext threadUiState.copy(
                        isLoadingMore = false,
                        lz = UserData(author, true),
                        data = (oldData + uniqueData).toImmutableList(),
                        currentPageMax = page.current_page,
                        totalPage = page.new_total_page,
                        hasMore = page.has_more != 0,
                        nextPagePostId = response.data_.thread.getNextPagePostId(
                            postList.mapTo(HashSet()) { it.id },
                            sortType
                        ),
                        latestPosts = persistentListOf(),
                    )
                }
            }
            if (newThreadUiState == null) {
                sendMsg(R.string.no_more)
                _threadUiState = _threadUiState.copy(isLoadingMore = false)
            } else {
                _threadUiState = newThreadUiState
            }
        }
    }

    /**
     * 当前用户发送新的回复时，加载用户发送的回复
     */
    fun requestLoadMyLatestReply(newPostId: Long) = viewModelScope.launch(handler) {
        if (isLoadingLatestReply) return@launch
        _threadUiState = _threadUiState.copy(isLoadingLatestReply = true, error = null)

        val isDesc = threadUiState.sortType == ThreadSortType.BY_DESC
        val curLatestPostFloor = if (isDesc) {
            threadUiState.data.firstOrNull()?.floor ?: 1 // DESC -> first
        } else {
            threadUiState.data.lastOrNull()?.floor ?: 1  // ASC  -> last
        }

        PbPageRepository.pbPage(threadId, page = 0, postId = newPostId, forumId = curForumId)
            .collect { response ->
                val page = response.data_?.page?.current_page ?: throw TiebaException("Null page")
                val anti = response.data_.anti ?: throw TiebaException("Null ant")
                val oldPostIds = threadUiState.data.mapTo(HashSet()) { it.id }
                val hasNewPost = response.data_.post_list.any { !oldPostIds.contains(it.id) }

                _threadUiState = withContext(Dispatchers.IO) {
                    val firstLatestPost = response.data_.post_list.first()
                    val isContinuous = firstLatestPost.floor == curLatestPostFloor + 1
                    val continuous = isContinuous || page == threadUiState.currentPageMax

                    val postData = response.data_.post_list.map { PostData.from(it) }
                    val oldPostData = threadUiState.data

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
                    val oldState = threadUiState.copy(isLoadingLatestReply = false, error = null, anti = anti.wrapImmutable())
                    return@withContext when {
                        hasNewPost && continuous -> {
                            val sortedData = if (isDesc) addPosts.reversed() + newPost else newPost + addPosts
                            oldState.copy(
                                data = sortedData.toImmutableList(),
                                latestPosts = persistentListOf()
                            )
                        }

                        hasNewPost -> oldState.copy(
                            data = newPost.toImmutableList(),
                            latestPosts = postData.toImmutableList()
                        )

                        !hasNewPost -> oldState.copy(
                            data = newPost.toImmutableList(),
                            latestPosts = persistentListOf()
                        )

                        else -> oldState
                    }
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
        val forumId = threadUiState.forum?.get { id } ?: curForumId
        if (forumId == null) {
            sendMsg(R.string.delete_store_failure, "Null ForumId"); return
        }
        favoriteJob?.let { if (it.isActive) it.cancel() }
        favoriteJob = viewModelScope.launch(handler) {
            TiebaApi.getInstance().removeStoreFlow(threadId, forumId, threadUiState.anti?.get { tbs })
                .catch {
                    sendMsg(R.string.delete_store_failure, it.getErrorMessage())
                }
                .collect { response ->
                    if (response.errorCode == 0) { // clear local status
                        _info = info!!.updateCollectStatus(collected = false, markPostId = 0)
                        sendMsg(R.string.message_remove_favorite_success)
                    } else {
                        sendMsg(R.string.delete_store_failure, response.errorMsg)
                    }
                }
        }
    }

    fun onAgreePost(post: PostData) = viewModelScope.launch {
        if (threadUiState.user == null) {
            sendMsg(R.string.title_empty_login); return@launch
        }

        val start = System.currentTimeMillis()
        val agree = (post.hasAgree == 1).not()

        TiebaApi.getInstance()
            .opAgreeFlow(threadId.toString(), post.id.toString(), if (agree) 0 else 1, objType = 1)
            .onStart {
                _threadUiState = threadUiState.updateAgreePost(post.id, if (agree) 1 else 0)
            }
            .catch {
                val end = System.currentTimeMillis()
                if (end - start < 400) { // Wait for button animation
                    delay(400 - (end - start))
                }
                _threadUiState = threadUiState.updateAgreePost(post.id, if (agree) 0 else 1)
                sendMsg(R.string.snackbar_agree_fail, it.getErrorCode(), it.getErrorMessage())
            }
            .collect { /*** no-op ***/ }
    }

    fun onAgreeThreadClicked() {
        if (firstPostId == 0L) return
        if (threadUiState.user == null) {
            sendMsg(R.string.title_empty_login); return
        }
        val newAgreeStatus = info!!.hasAgree.not()
        viewModelScope.launch {
            val start = System.currentTimeMillis()
            TiebaApi.getInstance().opAgreeFlow(
                threadId = threadId.toString(),
                postId = firstPostId.toString(),
                opType = if (newAgreeStatus) 0 else 1,
                objType = 3
            ).onStart {
                _info = info!!.updateAgreeStatus(hasAgree = newAgreeStatus)
            }.catch {
                val end = System.currentTimeMillis()
                if (end - start < 400) { // Wait for button animation
                    delay(400 - (end - start))
                }
                sendMsg(App.INSTANCE.getString(R.string.error_tip) + it.getErrorMessage())
                _info = info!!.updateAgreeStatus(hasAgree = !newAgreeStatus)
            }.collect { /*** no-op ***/ }
        }
    }

    fun onDeleteConfirmed() {
        val forumId = curForumId
        val post = _deletePost
        if (forumId == null || post == null) { // Won't happen
            sendMsg(R.string.message_unknown_error); return
        }
        if (post == threadUiState.firstPost) {
            requestDeleteThread(forumId)
        } else {
            requestDeletePost(forumId, post)
        }
        _deletePost = null
    }

    private fun requestDeletePost(forumId: Long, post: PostData) = viewModelScope.launch {
        val forumName = threadUiState.forum?.item?.name.orEmpty()
        val tbs = threadUiState.anti?.item?.tbs
        val isSelfPost = post.author.id == threadUiState.user?.id
        TiebaApi.getInstance()
            .delPostFlow(forumId, forumName, threadId, post.id, tbs, false, isSelfPost)
            .catch {
                sendMsg(R.string.toast_delete_failure, it.getErrorMessage())
            }
            .collect { // Remove this post from data list
                val oldData = threadUiState.data
                val deletedPostIndex = oldData.indexOfFirst { it.id == post.id }
                if (deletedPostIndex != -1) {
                    _threadUiState = threadUiState.copy(data = oldData.removeAt(deletedPostIndex))
                }
                sendMsg(R.string.toast_delete_success)
            }
    }

    private fun requestDeleteThread(forumId: Long) = viewModelScope.launch {
        val forumName = threadUiState.forum?.item?.name.orEmpty()
        val tbs = threadUiState.anti?.item?.tbs
        val isSelfThread = lz?.id == threadUiState.user?.id
        TiebaApi.getInstance()
            .delThreadFlow(forumId, forumName, threadId, tbs, isSelfThread, false)
            .catch {
                sendMsg(R.string.toast_delete_failure, it.getErrorMessage())
            }
            .collect {
                sendUiEvent(CommonUiEvent.NavigateUp)
            }
    }

    fun onLastPostVisibilityChanged(pid: Long, floor: Int?) = viewModelScope.launch(Dispatchers.Main) {
        val author = lz ?: return@launch
        val title = info?.title ?: return@launch
        history = withContext(Dispatchers.IO) {
            val bean = ThreadHistoryInfoBean(
                seeLz,
                pid.toString(),
                threadUiState.forum?.get { name },
                floor?.toString()
            )
            History(
                title = title,
                data = threadId.toString(),
                type = HistoryUtil.TYPE_THREAD,
                extras = bean.toJson(),
                avatar =  StringUtil.getAvatarUrl(author.portrait),
                username = author.nameShow
            )
        }
    }

    fun onShareThread() {
        val title = info?.title?: ""
        TiebaUtil.shareText(App.INSTANCE, "https://tieba.baidu.com/p/$threadId", title)
    }

    fun onCopyThreadLink() = TiebaUtil.copyText(
        context = App.INSTANCE,
        text = "https://tieba.baidu.com/p/$threadId?see_lz=${seeLz.booleanToString()}"
    )

    fun onReportThread(navigator: NavController) = viewModelScope.launch {
        TiebaUtil.reportPost(App.INSTANCE, navigator, firstPostId.toString())
    }

    fun onImmersiveModeChanged() {
        isImmersiveMode = !isImmersiveMode
    }

    fun onReplyPost(post: PostData) = sendUiEvent(
        ThreadUiEvent.ToReplyDestination(
            Reply(
                forumId = curForumId ?: 0,
                forumName = threadUiState.forum?.get { name } ?: "",
                threadId = threadId,
                postId = post.id,
                replyUserId = post.author.id,
                replyUserName = post.author.nameShow.takeIf { name -> name.isNotEmpty() } ?: post.author.name,
                replyUserPortrait = post.author.portrait
            )
        )
    )

    fun onReplySubPost(post: PostData, subPost: SubPostItemData) = sendUiEvent(
        ThreadUiEvent.ToReplyDestination(
            Reply(
                forumId = curForumId ?: 0,
                forumName = threadUiState.forum?.get { name } ?: "",
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
        _deletePost = post
    }

    fun onDeleteThread() {
        _deletePost = threadUiState.firstPost
    }

    override fun onCleared() {
        super.onCleared()
        history?.let {
            MainScope().launch(Dispatchers.IO) { HistoryUtil.saveHistory(it) }
        }
        LzInlineContentMap.clear()
    }

    private fun sendMsg(@StringRes int: Int, vararg formatArgs: Any) =
        sendMsg(App.INSTANCE.getString(int, *formatArgs))

    private fun sendMsg(@StringRes int: Int) = sendMsg(App.INSTANCE.getString(int))

    private fun sendMsg(msg: String) {
        if (viewModelScope.isActive) {
            sendUiEvent(CommonUiEvent.Toast(msg))
        } else {
            App.INSTANCE.toastShort(msg)
        }
    }

    private fun sendUiEvent(event: UiEvent) {
        _uiEvent.value = event
    }

    fun onUiEventReceived() {
        _uiEvent.value = null
    }

    @Throws(TiebaException::class)
    private fun updateStateFrom(response: PbPageResponse) {
        val pageData = response.data_?.page ?: throw TiebaException("Null page")
        val author = response.data_.thread?.author ?: throw TiebaException("Null author")
        val forum = response.data_.forum?: throw TiebaException("Null forum")
        val anti = response.data_.anti?: throw TiebaException("Null anti")
        val thread = response.data_.thread
        val postList = response.data_.post_list
        val firstPost = response.data_.first_floor_post
        val notFirstPosts = postList.filterNot { it.floor == 1 }
        val user = if (response.data_.user?.is_login == 1) UserData(response.data_.user, response.data_.user.id == author.id) else null
        if (user == null) {
            hideReply = true
        }
        _info = ThreadInfoData(thread)
        _threadUiState = _threadUiState.copy(
            isRefreshing = false,
            error = null,
            lz = UserData(author, true),
            user = user,
            data = notFirstPosts.map { PostData.from(it) }.toImmutableList(),
            firstPost = if (firstPost != null) PostData.from(firstPost) else _threadUiState.firstPost,
            forum = wrapImmutable(forum),
            anti = wrapImmutable(anti),
            currentPageMin = pageData.current_page,
            currentPageMax = pageData.current_page,
            totalPage = pageData.new_total_page,
            hasMore = pageData.has_more != 0,
            nextPagePostId = thread.getNextPagePostId(postList.mapTo(HashSet()) { it.id }, _threadUiState.sortType),
            hasPrevious = pageData.has_prev != 0,
            latestPosts = persistentListOf()
        )
    }

    companion object {

        private const val TAG = "ThreadViewModel"

        @Volatile
        private var LzInlineContentMap: WeakReference<Map<String, InlineTextContent>?> = WeakReference(null)

        @Composable
        fun getCachedLzInlineContent(): Map<String, InlineTextContent> {
            var map = LzInlineContentMap.get()
            if (map == null) {
                synchronized(this) {
                    if (LzInlineContentMap.get() == null) {
                        map = persistentMapOf(
                            "Lz" to buildChipInlineContent(
                                text = stringResource(id = R.string.tip_lz),
                                textStyle = MaterialTheme.typography.subtitle2.copy(fontSize = 12.sp),
                                backgroundColor = ExtendedTheme.colors.textSecondary.copy(alpha = 0.1f),
                                color = ExtendedTheme.colors.textSecondary
                            )
                        )
                        LzInlineContentMap = WeakReference(map)
                    }
                }
            }
            return map!!
        }

        private fun ThreadUiState.updateAgreePost(postId: Long, hasAgree: Int): ThreadUiState {
            val list = data.map { post ->
                if (post.id == postId) post.updateAgreeStatus(hasAgree) else post
            }
            return copy(data = list.toImmutableList())
        }

        private fun filterUnique(old: List<PostData>, new: List<Post>): List<PostData> {
            val postIds = old.mapTo(HashSet()) { it.id }
            return new
                .filterNot { it.floor == 1 || postIds.contains(it.id) }
                .map { PostData.from(it) }
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