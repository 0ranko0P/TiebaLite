package com.huanchengfly.tieba.post.ui.page.subposts

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.protos.Anti
import com.huanchengfly.tieba.post.api.models.protos.SimpleForum
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorCode
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.firstOrThrow
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.models.PostData
import com.huanchengfly.tieba.post.ui.models.SubPostItemData
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.set
import com.huanchengfly.tieba.post.utils.AccountUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Stable
@HiltViewModel
class SubPostsViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    settingsRepo: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val params = savedStateHandle.toRoute<Destination.SubPosts>()

    val threadId = params.threadId
    val forumId = params.forumId
    val postId = params.postId
    val subPostId = params.subPostId

    private val accountRepo = AccountUtil.getInstance()

    private val _state = MutableStateFlow(SubPostsUiState())
    val state: StateFlow<SubPostsUiState> = _state.asStateFlow()

    private val handler = CoroutineExceptionHandler { _, e ->
        Log.e(TAG, "onError: ", e)
        _state.update { it.copy(isLoading = false, isRefreshing = false, error = e) }
    }

    /**
     * One-off [UiEvent], but no guarantee to be received.
     * */
    private val _uiEvent: MutableSharedFlow<UiEvent?> = MutableSharedFlow()
    val uiEvent: Flow<UiEvent?>
        get() = _uiEvent

    val canReply: StateFlow<Boolean> = combine(
        flow = accountRepo.currentAccount,
        flow2 = settingsRepo.habitSettings.flow,
        transform = { account, habit -> account != null && !habit.hideReply }
    )
    .distinctUntilChanged()
    .stateIn(viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = false)

    init {
        requestLoad()
    }

    fun requestLoad(pageNum: Int = 1) {
        // Check is refreshing
        if (_state.value.isRefreshing) return else _state.set { copy(isRefreshing = true) }

        viewModelScope.launch(handler) {
            val response = TiebaApi.getInstance()
                .pbFloorFlow(threadId, postId, forumId, pageNum, 0L)
                .firstOrThrow()

            val post = checkNotNull(response.data_?.post)
            val page = checkNotNull(response.data_.page)
            val forum = checkNotNull(response.data_.forum)
            val lzId = response.data_.thread?.author?.id ?: -1L
            val anti = checkNotNull(response.data_.anti)
            val subPosts = response.data_.subpost_list
                .toItemDataList(lzId)
                .toImmutableList()

            _state.update {
                it.copy(
                    isRefreshing = false,
                    hasMore = page.current_page < page.total_page,
                    currentPage = page.current_page,
                    totalPage = page.total_page,
                    totalCount = page.total_count,
                    anti = anti.wrapImmutable(),
                    forum = forum.wrapImmutable(),
                    post = PostData.from(post = post.copy(sub_post_list = null), lzId = lzId),
                    subPosts = subPosts,
                )
            }
            sendUiEvent(SubPostsUiEvent.ScrollToSubPosts)
        }
    }

    fun requestLoadMore(subPostId: Long = 0L) {
        // Check is loading
        if (_state.value.isLoading) return else _state.set { copy(isLoading = true, isRefreshing = false) }

        viewModelScope.launch(handler) {
            val stateSnapshot = _state.first()
            val loadPage = stateSnapshot.currentPage + 1
            val response = TiebaApi.getInstance()
                .pbFloorFlow(threadId, postId, forumId, loadPage, subPostId)
                .firstOrThrow()

            val page = checkNotNull(response.data_?.page)
            val lzId = response.data_.thread?.author?.id ?: -1L

            // Combine old and new SubPosts
            val subPosts = withContext(Dispatchers.Default) {
                val newSubPosts = response.data_.subpost_list.toItemDataList(lzId)
                stateSnapshot.subPosts + newSubPosts
            }

            _state.update {
                it.copy(
                    isLoading = false,
                    hasMore = page.current_page < page.total_page,
                    currentPage = page.current_page,
                    totalPage = page.total_page,
                    totalCount = page.total_count,
                    subPosts = subPosts
                )
            }
        }
    }

    fun requestDeletePost(subPostId: Long? = null, deleteMyPost: Boolean) {
        val forumName = _state.value.forum?.get { name }.orEmpty()
        val tbs = _state.value.anti?.get { tbs }
        val deletePostID = subPostId ?: postId

        viewModelScope.launch(handler) {
            TiebaApi.getInstance()
                .delPostFlow(forumId, forumName, threadId, deletePostID, tbs, false, deleteMyPost)
                .catch {
                    sendMsg(R.string.toast_delete_failure, it.getErrorMessage())
                }
                .firstOrNull() ?: return@launch

            _state.update {
                val newSubPost = if (subPostId == null) {
                    it.subPosts
                } else {
                    it.subPosts.filter { s -> s.id != subPostId }
                }
                it.copy(subPosts = newSubPost)
            }
        }
    }

    fun onPostLikeClicked(post: PostData) {
        if (post.like.loading) {
            sendMsg(R.string.toast_connecting); return
        }

        viewModelScope.launch(handler) {
            val liked = post.like.liked
            val opType = if (liked) 1 else 0 // 操作 0 = 点赞, 1 = 取消点赞

            TiebaApi.getInstance()
                .opAgreeFlow(threadId.toString(), postId.toString(), opType, objType = 1)
                .catch {
                    sendMsg(R.string.snackbar_agree_fail, it.getErrorCode(), it.getErrorMessage())
                    // Revert agree status
                    _state.update { s -> s.copy(post = s.post!!.updateLikesCount(liked = liked, false)) }
                }
                .onStart {
                    _state.update { it.copy(post = it.post!!.updateLikesCount(liked = !liked, true)) }
                }
                .firstOrNull() ?: return@launch

            _state.update { it.copy(post = it.post!!.updateLikesCount(liked = !liked, false)) }
        }
    }

    fun onSubPostLikeClicked(subPost: SubPostItemData) {
        val pId = subPost.id
        if (subPost.like.loading) {
            sendMsg(R.string.toast_connecting); return
        }

        viewModelScope.launch(handler) {
            val liked = subPost.like.liked
            val opType = if (liked) 1 else 0 // 操作 0 = 点赞, 1 = 取消点赞

            TiebaApi.getInstance()
                .opAgreeFlow(threadId.toString(), pId.toString(), opType, objType = 2)
                .catch {
                    sendMsg(R.string.snackbar_agree_fail, it.getErrorCode(), it.getErrorMessage())
                    // Revert agree status
                    _state.update { s -> s.updateLikesById(pId, liked, loading = false) }
                }
                .onStart {
                    _state.update { it.updateLikesById(pId, !liked, loading = true) }
                }
                .firstOrNull() ?: return@launch

            _state.update { it.updateLikesById(pId, !liked, loading = false) }
        }
    }

    private fun sendUiEvent(event: UiEvent?) = viewModelScope.launch { _uiEvent.emit(event) }

    private fun sendMsg(@StringRes int: Int, vararg formatArgs: Any) {
        sendMsg(context.getString(int, *formatArgs))
    }

    private fun sendMsg(msg: String) {
        if (viewModelScope.isActive) {
            sendUiEvent(CommonUiEvent.Toast(msg))
        } else {
            context.toastShort(msg)
        }
    }

    companion object {
        private const val TAG = "SubPostsViewModel"

        private fun List<SubPostList>.toItemDataList(lzId: Long): List<SubPostItemData> {
            return map { subPost -> SubPostItemData(subPost, lzId, fromSubPost = true) }
        }

        private fun SubPostsUiState.updateLikesById(
            subPostId: Long,
            liked: Boolean,
            loading: Boolean
        ) =
            copy(
                subPosts = subPosts.fastMap {
                    if (it.id == subPostId) it.updateLikesCount(liked = liked, loading = loading) else it
                }
            )
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
    val subPosts: List<SubPostItemData> = emptyList(),
) : UiState

sealed interface SubPostsUiEvent : UiEvent {
    data object ScrollToSubPosts : SubPostsUiEvent
}