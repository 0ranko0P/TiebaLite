package com.huanchengfly.tieba.post.ui.page.main.notifications.list

import android.content.Context
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.MessageListBean
import com.huanchengfly.tieba.post.api.models.MessageListBean.MessageInfoBean
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaException
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.BaseViewModel
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.PartialChange
import com.huanchengfly.tieba.post.arch.PartialChangeProducer
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiIntent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.stateInViewModel
import com.huanchengfly.tieba.post.repository.BlockRepository
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.models.Author
import com.huanchengfly.tieba.post.ui.models.message.MessageItemData
import com.huanchengfly.tieba.post.ui.models.message.ReplyUser
import com.huanchengfly.tieba.post.ui.page.main.notifications.list.NotificationsListViewModel.Companion.NotificationsListVmFactory
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.EmoticonUtil.emoticonString
import com.huanchengfly.tieba.post.utils.StringUtil
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

@HiltViewModel(assistedFactory = NotificationsListVmFactory::class)
class NotificationsListViewModel @AssistedInject constructor(
    @Assisted private val type: NotificationsType,
    @ApplicationContext private val context: Context,
    private val blockRepo: BlockRepository,
    settingsRepo: SettingsRepository,
) :
    BaseViewModel<NotificationsListUiIntent, NotificationsListPartialChange, NotificationsListUiState, NotificationsListUiEvent>() {

    val hideBlocked: StateFlow<Boolean> = settingsRepo.blockSettings
        .map { it.hideBlocked }
        .stateInViewModel(initialValue = false)

    override fun createInitialState(): NotificationsListUiState = NotificationsListUiState()

    override fun createPartialChangeProducer():
            PartialChangeProducer<NotificationsListUiIntent, NotificationsListPartialChange, NotificationsListUiState> {
        return NotificationsListPartialChangeProducer(context, type, blockRepo::isBlocked)
    }

    override fun dispatchEvent(partialChange: NotificationsListPartialChange): UiEvent? =
        when (partialChange) {
            is NotificationsListPartialChange.Refresh.Failure -> CommonUiEvent.Toast(partialChange.error.getErrorMessage())
            is NotificationsListPartialChange.LoadMore.Failure -> CommonUiEvent.Toast(partialChange.error.getErrorMessage())
            else -> null
        }

    companion object {

        @AssistedFactory
        interface NotificationsListVmFactory {
            fun create(type: NotificationsType): NotificationsListViewModel
        }
    }
}

private class NotificationsListPartialChangeProducer(
    private val context: Context,
    private val type: NotificationsType,
    private val isBlocked: suspend (uid: Long, content: String) -> Boolean,
) : PartialChangeProducer<NotificationsListUiIntent, NotificationsListPartialChange, NotificationsListUiState> {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun toPartialChangeFlow(intentFlow: Flow<NotificationsListUiIntent>): Flow<NotificationsListPartialChange> =
        merge(
            intentFlow.filterIsInstance<NotificationsListUiIntent.Refresh>().flatMapConcat { produceRefreshPartialChange() },
            intentFlow.filterIsInstance<NotificationsListUiIntent.LoadMore>().flatMapConcat { it.produceLoadMorePartialChange() },
        )

    private fun produceRefreshPartialChange(): Flow<NotificationsListPartialChange.Refresh> {
        if (!AccountUtil.isLoggedIn()) {
            return flowOf(NotificationsListPartialChange.Refresh.Failure(TiebaNotLoggedInException()))
        }
        return (when (type) {
            NotificationsType.ReplyMe -> TiebaApi.getInstance().replyMeFlow()
            NotificationsType.AtMe -> TiebaApi.getInstance().atMeFlow()
        }).map<MessageListBean, NotificationsListPartialChange.Refresh> { messageListBean ->
            val data =
                ((if (type == NotificationsType.ReplyMe) messageListBean.replyList else messageListBean.atList)
                    ?: emptyList()).mapUiModel(context, type, isBlocked)
            NotificationsListPartialChange.Refresh.Success(
                data = data,
                hasMore = messageListBean.page?.hasMore == "1"
            )
        }
            .onStart { emit(NotificationsListPartialChange.Refresh.Start) }
            .catch { emit(NotificationsListPartialChange.Refresh.Failure(it)) }
    }

    private fun NotificationsListUiIntent.LoadMore.produceLoadMorePartialChange() =
        (when (type) {
            NotificationsType.ReplyMe -> TiebaApi.getInstance().replyMeFlow(page = page)
            NotificationsType.AtMe -> TiebaApi.getInstance().atMeFlow(page = page)
        }).map<MessageListBean, NotificationsListPartialChange.LoadMore> { messageListBean ->
            val data =
                ((if (type == NotificationsType.ReplyMe) messageListBean.replyList else messageListBean.atList)
                    ?: emptyList()).mapUiModel(context, type, isBlocked)
            NotificationsListPartialChange.LoadMore.Success(
                currentPage = page,
                data = data,
                hasMore = messageListBean.page?.hasMore == "1"
            )
        }
            .onStart { emit(NotificationsListPartialChange.LoadMore.Start) }
            .catch { emit(NotificationsListPartialChange.LoadMore.Failure(currentPage = page, error = it)) }
}

enum class NotificationsType {
    ReplyMe, AtMe
}

sealed interface NotificationsListUiIntent : UiIntent {
    data object Refresh : NotificationsListUiIntent

    data class LoadMore(val page: Int) : NotificationsListUiIntent
}

sealed interface NotificationsListPartialChange : PartialChange<NotificationsListUiState> {
    sealed class Refresh() : NotificationsListPartialChange {
        override fun reduce(oldState: NotificationsListUiState): NotificationsListUiState =
            when (this) {
                Start -> oldState.copy(isRefreshing = true)
                is Success -> oldState.copy(
                    isRefreshing = false,
                    currentPage = 1,
                    data = data.toImmutableList(),
                    hasMore = hasMore
                )

                is Failure -> oldState.copy(isRefreshing = false)
            }

        data object Start : Refresh()

        data class Success(
            val data: List<MessageItemData>,
            val hasMore: Boolean,
        ) : Refresh()

        data class Failure(
            val error: Throwable,
        ) : Refresh()
    }

    sealed class LoadMore() : NotificationsListPartialChange {
        override fun reduce(oldState: NotificationsListUiState): NotificationsListUiState =
            when (this) {
                Start -> oldState.copy(isLoadingMore = true)
                is Success -> {
                    val newKeys = data.mapTo(HashSet()) { it.lazyListItemKey }
                    // distinct data by item key
                    val oldData = oldState.data.filterNot { item -> newKeys.contains(item.lazyListItemKey) }
                    oldState.copy(
                        isLoadingMore = false,
                        currentPage = currentPage,
                        data = oldData + data,
                        hasMore = hasMore
                    )
                }

                is Failure -> oldState.copy(isLoadingMore = false)
            }

        object Start : LoadMore()

        data class Success(
            val currentPage: Int,
            val data: List<MessageItemData>,
            val hasMore: Boolean,
        ) : LoadMore()

        data class Failure(
            val currentPage: Int,
            val error: Throwable,
        ) : LoadMore()
    }
}

/**
 * Convert MessageInfo to UI Model
 *
 * @param context application context
 * @param type notifications type
 * @param isBlocked check author, title or content is blocked
 * */
private suspend fun List<MessageInfoBean>.mapUiModel(
    context: Context,
    type: NotificationsType,
    isBlocked: suspend (uid: Long, content: String) -> Boolean,
): List<MessageItemData> {
    return withContext(Dispatchers.Default) {
        val isReply = type == NotificationsType.ReplyMe
        map {
            val isFloor = it.isFloor == "1"
            val replyUser = it.replyer!!.run {
                ReplyUser(
                    id = id?.toLongOrNull() ?: throw TiebaException("Invalid reply user ID: $id"),
                    nameShow = nameShow ?: name ?: "",
                    avatarUrl = if (portrait.isNullOrEmpty()) null else StringUtil.getAvatarUrl(portrait),
                    isFans = isFans == "1"
                )
            }

            // Note: conditions from NotificationsListPage, do not touch
            val title = when {
                it.title.isNullOrEmpty() -> null

                isReply && !isFloor -> {
                    context.getString(R.string.text_message_list_item_reply_my_thread, it.title)
                }

                !isReply -> it.title

                else -> null
            }

            val quoteContent = if (!it.quoteContent.isNullOrEmpty() && isReply && isFloor) {
                it.quoteContent.emoticonString
            } else {
                null
            }

            MessageItemData(
                replyUser = replyUser,
                threadId = it.threadId?.toLongOrNull() ?: throw TiebaException("Invalid thread ID ${it.threadId}."),
                postId = it.postId?.toLongOrNull() ?: throw TiebaException("Invalid post ID ${it.postId}."),
                isBlocked = isBlocked(replyUser.id, it.content.orEmpty()),
                isFloor = isFloor,
                title = title?.emoticonString,
                content = it.content?.emoticonString,
                time = DateTimeUtils.fixTimestamp(it.time!!.toLong()),
                quoteContent = quoteContent,
                quoteUser = it.quoteUser?.run {
                    Author(
                        id = id?.toLongOrNull() ?: throw TiebaException("Invalid quote user ID: $id"),
                        name = nameShow ?: name ?: "",
                        avatarUrl = StringUtil.getAvatarUrl(portrait)
                    )
                },
                quotePid = it.quotePid?.toLongOrNull(),
                forumName = it.forumName,
                threadType = it.threadType,
                unread = it.unread
            )
        }
    }
}

data class NotificationsListUiState(
    val isRefreshing: Boolean = true,
    val isLoadingMore: Boolean = false,
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val data: List<MessageItemData> = emptyList(),
) : UiState

sealed interface NotificationsListUiEvent : UiEvent