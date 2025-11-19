package com.huanchengfly.tieba.post.ui.page.subposts

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.repository.PageData
import com.huanchengfly.tieba.post.repository.PbPageRepository
import com.huanchengfly.tieba.post.ui.models.PostData
import com.huanchengfly.tieba.post.ui.models.SubPostItemData
import com.huanchengfly.tieba.post.ui.models.ThreadInfoData
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.thread.ThreadLikeUiEvent
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.set
import com.huanchengfly.tieba.post.utils.AccountUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface SubPostsUiEvent : UiEvent {
    class DeletePostFailed(val message: String) : SubPostsUiEvent

    object ScrollToSubPosts : SubPostsUiEvent
}

@Immutable
data class SubPostsUiState(
    val isRefreshing: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: Throwable? = null,
    val post: PostData? = null,
    val subPosts: List<SubPostItemData> = emptyList(),
    val totalSubPosts: Int = 0,
    val page: PageData = PageData(),
    val tbs: String? = null,
    val thread: ThreadInfoData? = null,
) : UiState {

    val forumName: String?
        get() = thread?.simpleForum?.second
}

@Stable
@HiltViewModel
class SubPostsViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    private val threadRepo: PbPageRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val params = savedStateHandle.toRoute<Destination.SubPosts>()

    private val currentAccount = AccountUtil.getInstance().currentAccount

    /**
     * One-off [UiEvent], but no guarantee to be received.
     * */
    private val _uiEvent: MutableSharedFlow<UiEvent?> = MutableSharedFlow()
    val uiEvent: Flow<UiEvent?>
        get() = _uiEvent

    private val _state = MutableStateFlow(SubPostsUiState())
    val state: StateFlow<SubPostsUiState> = _state.asStateFlow()

    private val handler = CoroutineExceptionHandler { _, e ->
        Log.e(TAG, "onError: ", e)
        _state.update { it.copy(isRefreshing = false, isLoadingMore = false, error = e) }
    }

    private val threadId = params.threadId

    private val forumId: Long
        get() = _state.value.thread?.simpleForum?.first ?: params.forumId

    private val postId: Long
        get() = _state.value.post?.id ?: params.postId

    /**
     * Post or SubPost marked for deletion.
     *
     * @see onDeletePost
     * @see onDeleteSubPost
     * */
    private val _delete: MutableStateFlow<Any?> = MutableStateFlow(null)
    val delete: StateFlow<Any?> = _delete.asStateFlow()

    init {
        refreshInternal()
    }

    private fun refreshInternal() {
        _state.set { SubPostsUiState(isRefreshing = true) }
        viewModelScope.launch(handler) {
            val rec = threadRepo.pbFloor(threadId, postId, forumId, page = 1)
            _state.update {
                it.copy(
                    isRefreshing = false,
                    post = rec.post,
                    subPosts = rec.subPosts,
                    page = rec.page,
                    tbs = rec.tbs,
                    thread = rec.thread
                )
            }
            sendUiEvent(SubPostsUiEvent.ScrollToSubPosts)
        }
    }

    fun onRefresh() {
        if (!_state.value.isRefreshing) refreshInternal()
    }

    fun onLoadMore() {
        if (!_state.value.isLoadingMore) _state.set { copy(isLoadingMore = true) } else return

        viewModelScope.launch(handler) {
            val stateSnapshot = _state.value
            val page = stateSnapshot.page.current + 1
            val rec = threadRepo.pbFloor(threadId, postId, forumId, page)
            // Combine old and new SubPosts
            val data = withContext(Dispatchers.Default) {
                (stateSnapshot.subPosts + rec.subPosts).distinctBy { it.id }
            }
            _state.update {
                it.copy(
                    isLoadingMore = false,
                    post = rec.post,
                    subPosts = data,
                    page = rec.page,
                    tbs = rec.tbs,
                    thread = rec.thread
                )
            }
        }
    }

    /**
     * Mark this sub post for deletion
     *
     * @see onDeleteConfirmed
     * */
    fun onDeleteSubPost(subPost: SubPostItemData) = _delete.update { subPost }

    /**
     * Mark this post for deletion
     *
     * @see onDeleteConfirmed
     * */
    fun onDeletePost() = _delete.update { _state.value.post!! }

    fun onDeleteCancelled() = _delete.update { null }

    fun onDeleteConfirmed(): Job = viewModelScope.launch(handler) {
        val uiStateSnapshot = _state.value
        val target = _delete.getAndUpdate { null }
        runCatching {
            val thread = uiStateSnapshot.thread!!
            val tbs = uiStateSnapshot.tbs
            val myUid = currentAccount.first()?.uid ?: throw TiebaNotLoggedInException()
            when (target) {
                is SubPostItemData -> threadRepo.deleteSubPost(target.id, thread, tbs, delMyPost = target.authorId == myUid)

                is PostData -> threadRepo.deletePost(target.id, thread, tbs, delMyPost = target.author.id == myUid)

                else -> throw IllegalStateException()
            }
        }
        .onFailure { e ->
            sendUiEvent(SubPostsUiEvent.DeletePostFailed(e.getErrorMessage()))
        }
        .onSuccess {
            if (target is SubPostItemData) { // remove deleted item now
                _state.update { it.copy(subPosts = it.subPosts.fastFilter { s -> s.id != target.id }) }
            }
        }
    }

    /**
     * Called when like subPost button clicked
     * */
    fun onSubPostLikeClicked(subPost: SubPostItemData) {
        if (subPost.like.loading) {
            sendUiEvent(ThreadLikeUiEvent.Connecting); return
        }

        viewModelScope.launch(handler) {
            val subPostId = subPost.id
            val liked = subPost.like.liked
            _state.update { it.updateLikesById(subPostId, !liked, loading = true) }
            runCatching {
                threadRepo.requestLikeSubPost(threadId, subPost)
            }
            .onFailure { e ->
                if (e is TiebaNotLoggedInException) {
                    sendUiEvent(ThreadLikeUiEvent.NotLoggedIn)
                } else {
                    sendUiEvent(ThreadLikeUiEvent.Failed(e))
                }
                _state.update { it.updateLikesById(subPostId, liked, loading = false) } // revert changes
            }
            .onSuccess {
                _state.update { it.updateLikesById(subPostId, !liked, loading = false) }
            }
        }
    }

    private fun sendUiEvent(event: UiEvent?) = viewModelScope.launch { _uiEvent.emit(event) }

    companion object {
        private const val TAG = "SubPostsViewModel"

        private fun SubPostsUiState.updateLikesById(id: Long, liked: Boolean, loading: Boolean) = copy(
            subPosts = subPosts.fastMap {
                if (it.id == id) it.updateLikesCount(liked = liked, loading = loading) else it
            }
        )
    }
}