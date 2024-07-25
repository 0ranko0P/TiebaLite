package com.huanchengfly.tieba.post.ui.page.subposts

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.protos.Anti
import com.huanchengfly.tieba.post.api.models.protos.SimpleForum
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.contentRenders
import com.huanchengfly.tieba.post.api.models.protos.plainText
import com.huanchengfly.tieba.post.api.models.protos.renders
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorCode
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.PbContentRender
import com.huanchengfly.tieba.post.ui.models.PostData
import com.huanchengfly.tieba.post.ui.models.SubPostItemData
import com.huanchengfly.tieba.post.ui.models.UserData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@HiltViewModel
class SubPostsViewModel @Inject constructor(savedStateHandle: SavedStateHandle) : ViewModel() {

    // TODO: Unsafe arguments
    val threadId = savedStateHandle.get<Long>("threadId")?: throw IllegalArgumentException()
    val forumId = savedStateHandle.get<Long>("forumId")?: throw IllegalArgumentException()
    val postId = savedStateHandle.get<Long>("postId")?: throw IllegalArgumentException()
    val subPostId = savedStateHandle.get<Long>("subPostId")?: throw IllegalArgumentException()
    private val loadFromSubPost = savedStateHandle.get<Boolean>("loadFromSubPost")?: throw IllegalArgumentException()

    private val _state = mutableStateOf(SubPostsUiState())
    val state: State<SubPostsUiState> get() = _state

    val error: Throwable? get() = _state.value.error

    val refreshing: Boolean get() = _state.value.isRefreshing
    val loading: Boolean get() = _state.value.isLoading

    private val handler = CoroutineExceptionHandler { _, e ->
        Log.e(TAG, "onError: ", e)
        _state.value = _state.value.copy(isLoading = false, isRefreshing = false, error = e)
    }

    /**
     * One-off [UiEvent], but guaranteed to be received.
     *
     * @see onUiEventReceived
     * */
    private val _uiEvent: MutableState<UiEvent?> = mutableStateOf(null)
    val uiEvent: State<UiEvent?> = _uiEvent

    var initialized = false
        private set

    @Suppress("UNUSED_PARAMETER")
    fun initialize(loadFromSubPost: Boolean = false) {
        initialized = true
        requestLoad()
    }

    fun requestLoad(pageNum: Int = 1) {
        if (refreshing) return
        _state.value = _state.value.copy(isRefreshing = true)
        val subPostID = subPostId.takeIf { loadFromSubPost } ?: 0L

        viewModelScope.launch(handler) {
            TiebaApi.getInstance().pbFloorFlow(threadId, postId, forumId, pageNum, subPostID)
                .collect { response ->
                    val post = checkNotNull(response.data_?.post)
                    val page = checkNotNull(response.data_?.page)
                    val forum = checkNotNull(response.data_?.forum)
                    val lzId = response.data_?.thread?.origin_thread_info?.author?.id ?: -1L
                    val anti = checkNotNull(response.data_?.anti)
                    val subPosts = response.data_?.subpost_list.orEmpty()
                        .toItemDataList(lzId)
                        .toImmutableList()

                    _state.value = _state.value.copy(
                        isRefreshing = false,
                        hasMore = page.current_page < page.total_page,
                        currentPage = page.current_page,
                        totalPage = page.total_page,
                        totalCount = page.total_count,
                        anti = anti.wrapImmutable(),
                        forum = forum.wrapImmutable(),
                        post = PostData.from(post),
                        postContentRenders = post.contentRenders,
                        subPosts = subPosts,
                    )
                    sendUiEvent(SubPostsUiEvent.ScrollToSubPosts)
                }
        }
    }

    fun requestLoadMore(subPostId: Long = 0L) {
        if (loading) return
        _state.value = _state.value.copy(isLoading = true)
        val loadPage = _state.value.currentPage + 1
        viewModelScope.launch(handler) {
            TiebaApi.getInstance()
                .pbFloorFlow(threadId, postId, forumId, loadPage, subPostId)
                .collect { response ->
                    val page = checkNotNull(response.data_?.page)
                    val lzId = response.data_?.thread?.origin_thread_info?.author?.id ?: -1L
                    val subPosts = response.data_?.subpost_list
                        .orEmpty()
                        .toItemDataList(lzId)
                        .toImmutableList()
                    _state.value = _state.value.copy(
                        isLoading = false,
                        hasMore = page.current_page < page.total_page,
                        currentPage = page.current_page,
                        totalPage = page.total_page,
                        totalCount = page.total_count,
                        subPosts = (_state.value.subPosts + subPosts).toImmutableList()
                    )
                }
        }
    }

    fun requestDeletePost(subPostId: Long? = null, deleteMyPost: Boolean) {
        val forumName = _state.value.forum?.get { name }.orEmpty()
        val tbs = _state.value.anti?.get { tbs }
        val deletePostID = subPostId ?: postId

        viewModelScope.launch {
            TiebaApi.getInstance()
                .delPostFlow(forumId, forumName, threadId, deletePostID, tbs, false, deleteMyPost)
                .catch {
                    sendMsg(R.string.toast_delete_failure, it.getErrorMessage())
                }
                .collect {
                    val newSubPost = if (subPostId == null) {
                        _state.value.subPosts
                    } else {
                        _state.value.subPosts.filter { it.id != subPostId }.toImmutableList()
                    }
                    _state.value = _state.value.copy(subPosts = newSubPost)
                }
        }
    }

    fun onAgreePost(agree: Boolean) {
        _state.value = _state.value.copy(
            post = _state.value.post?.updateAgreeStatus(if (agree) 1 else 0)
        )
        viewModelScope.launch {
            TiebaApi.getInstance()
                .opAgreeFlow(threadId.toString(), postId.toString(), if (agree) 0 else 1, objType = 1)
                .catch {
                    sendMsg(R.string.snackbar_agree_fail, it.getErrorCode(), it.getErrorMessage())
                    // Revert agree status
                    _state.value = _state.value.copy(
                        post = _state.value.post?.updateAgreeStatus(if (agree) 0 else 1)
                    )
                }
                .collect { /*** no-op ***/ }
        }
    }

    fun onAgreeSubPost(subPostId: Long, agree: Boolean) {
        _state.value = _state.value.copy(
            subPosts = _state.value.subPosts.updateAgreeStatus(subPostId, agree)
        )

        viewModelScope.launch {
            TiebaApi.getInstance()
                .opAgreeFlow(threadId.toString(), subPostId.toString(), if (agree) 0 else 1, objType = 2)
                .catch {
                    sendMsg(R.string.snackbar_agree_fail, it.getErrorCode(), it.getErrorMessage())
                    // Revert agree status
                    _state.value = _state.value.copy(
                        subPosts = _state.value.subPosts.updateAgreeStatus(subPostId, !agree)
                    )
                }
                .collect { /*** no-op ***/ }
        }
    }

    private fun sendUiEvent(event: UiEvent) {
        _uiEvent.value = event
    }

    fun onUiEventReceived() {
        _uiEvent.value = null
    }

    private fun sendMsg(@StringRes int: Int, vararg formatArgs: Any) =
        sendMsg(App.INSTANCE.getString(int, *formatArgs))

    private fun sendMsg(msg: String) {
        App.INSTANCE.toastShort(msg)
    }

    companion object {
        private const val TAG = "SubPostsViewModel"

        private fun List<SubPostList>.toItemDataList(lzId: Long) = map { subPost ->
            SubPostItemData(
                id = subPost.id,
                author = UserData(subPost.author!!, lzId == subPost.author_id),
                time = subPost.time.toLong(),
                content = subPost.content
                    .renders
                    .map { it.toAnnotationString() }
                    .reduce { acc, annotatedString -> acc + annotatedString },
                plainText = subPost.content.plainText,
                isLz = lzId == subPost.author_id,
                authorId = subPost.author_id,
                hasAgree = subPost.agree?.hasAgree == 1,
                agreeNum = subPost.agree?.agreeNum ?: 0L,
                diffAgreeNum = subPost.agree?.diffAgreeNum ?: 0L
            )
        }

        private fun List<SubPostItemData>.updateAgreeStatus(subPostId: Long, agreed: Boolean): ImmutableList<SubPostItemData> =
            this.map { if (it.id == subPostId) it.updateAgreeStatus(agreed) else it }.toImmutableList()
    }
}

data class SubPostsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: Throwable? = null,

    val hasMore: Boolean = true,
    val currentPage: Int = 1,
    val totalPage: Int = 1,
    val totalCount: Int = 0,

    val anti: ImmutableHolder<Anti>? = null,
    val forum: ImmutableHolder<SimpleForum>? = null,
    val post: PostData? = null,
    val postContentRenders: ImmutableList<PbContentRender> = persistentListOf(),
    val subPosts: ImmutableList<SubPostItemData> = persistentListOf(),
) : UiState

sealed interface SubPostsUiEvent : UiEvent {
    data object ScrollToSubPosts : SubPostsUiEvent
}