package com.huanchengfly.tieba.post.ui.page.subposts

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.BaseStateViewModel
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.TbLiteExceptionHandler
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
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
) : BaseStateViewModel<SubPostsUiState>() {

    private val params = savedStateHandle.toRoute<Destination.SubPosts>()

    private val currentAccount = AccountUtil.getInstance().currentAccount

    override val errorHandler = TbLiteExceptionHandler(TAG) { _, e, suppressed ->
        // Allow user browse existing posts on suppressed exceptions
        if (suppressed && currentState.subPosts.isNotEmpty()) {
            _uiState.update { it.copy(isRefreshing = false, isLoadingMore = false, error = null) }
            sendUiEvent(CommonUiEvent.ToastError(e))
        } else {
            _uiState.update { it.copy(isRefreshing = false, isLoadingMore = false, error = e) }
        }
    }

    private val threadId = params.threadId

    private val forumId: Long
        get() = currentState.thread?.simpleForum?.first ?: params.forumId

    private val postId: Long
        get() = currentState.post?.id ?: params.postId

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

    override fun createInitialState(): SubPostsUiState = SubPostsUiState()

    private fun refreshInternal() {
        _uiState.set { SubPostsUiState(isRefreshing = true) }
        launchInVM {
            val rec = threadRepo.pbFloor(threadId, postId, forumId, page = 1)
            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    post = rec.post,
                    subPosts = rec.subPosts,
                    page = rec.page,
                    tbs = rec.tbs,
                    thread = rec.thread
                )
            }
            emitUiEvent(SubPostsUiEvent.ScrollToSubPosts)
        }
    }

    fun onRefresh() {
        if (!currentState.isRefreshing) refreshInternal()
    }

    fun onLoadMore() {
        if (!currentState.isLoadingMore) _uiState.set { copy(isLoadingMore = true) } else return

        launchInVM {
            val stateSnapshot = currentState
            val page = stateSnapshot.page.current + 1
            val rec = threadRepo.pbFloor(threadId, postId, forumId, page)
            // Combine old and new SubPosts
            val data = withContext(Dispatchers.Default) {
                (stateSnapshot.subPosts + rec.subPosts).distinctBy { it.id }
            }
            _uiState.update {
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
    fun onDeletePost() = _delete.update { currentState.post!! }

    fun onDeleteCancelled() = _delete.update { null }

    fun onDeleteConfirmed(): Job = launchJobInVM {
        val uiStateSnapshot = currentState
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
            emitUiEvent(SubPostsUiEvent.DeletePostFailed(e.getErrorMessage()))
        }
        .onSuccess {
            if (target is SubPostItemData) { // remove deleted item now
                _uiState.update { it.copy(subPosts = it.subPosts.fastFilter { s -> s.id != target.id }) }
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

        launchInVM {
            val subPostId = subPost.id
            val liked = subPost.like.liked
            _uiState.update { it.updateLikesById(subPostId, !liked, loading = true) }
            runCatching {
                threadRepo.requestLikeSubPost(threadId, subPost)
            }
            .onFailure { e ->
                if (e is TiebaNotLoggedInException) {
                    sendUiEvent(ThreadLikeUiEvent.NotLoggedIn)
                } else {
                    sendUiEvent(ThreadLikeUiEvent.Failed(e))
                }
                _uiState.update { it.updateLikesById(subPostId, liked, loading = false) } // revert changes
            }
            .onSuccess {
                _uiState.update { it.updateLikesById(subPostId, !liked, loading = false) }
            }
        }
    }

    companion object {
        private const val TAG = "SubPostsViewModel"

        private fun SubPostsUiState.updateLikesById(id: Long, liked: Boolean, loading: Boolean) = copy(
            subPosts = subPosts.fastMap {
                if (it.id == id) it.updateLikesCount(liked = liked, loading = loading) else it
            }
        )
    }
}