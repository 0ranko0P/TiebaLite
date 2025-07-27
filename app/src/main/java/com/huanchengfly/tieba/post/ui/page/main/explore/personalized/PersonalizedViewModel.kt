package com.huanchengfly.tieba.post.ui.page.main.explore.personalized

import androidx.compose.runtime.Stable
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.AgreeBean
import com.huanchengfly.tieba.post.api.models.CommonResponse
import com.huanchengfly.tieba.post.api.models.protos.personalized.DislikeReason
import com.huanchengfly.tieba.post.api.models.protos.personalized.PersonalizedResponse
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.BaseViewModel
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.PartialChange
import com.huanchengfly.tieba.post.arch.PartialChangeProducer
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiIntent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.models.DislikeBean
import com.huanchengfly.tieba.post.repository.PersonalizedRepository
import com.huanchengfly.tieba.post.ui.models.ThreadInfoItem
import com.huanchengfly.tieba.post.ui.models.ThreadItemData
import com.huanchengfly.tieba.post.ui.models.distinctById
import com.huanchengfly.tieba.post.ui.models.updateAgreeStatus
import com.huanchengfly.tieba.post.utils.appPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

@Stable
@HiltViewModel
class PersonalizedViewModel @Inject constructor() :
    BaseViewModel<PersonalizedUiIntent, PersonalizedPartialChange, PersonalizedUiState, PersonalizedUiEvent>() {
    override fun createInitialState(): PersonalizedUiState = PersonalizedUiState()

    override fun createPartialChangeProducer(): PartialChangeProducer<PersonalizedUiIntent, PersonalizedPartialChange, PersonalizedUiState> =
        ExplorePartialChangeProducer

    override fun dispatchEvent(partialChange: PersonalizedPartialChange): UiEvent? =
        when (partialChange) {
            is PersonalizedPartialChange.Refresh.Failure -> CommonUiEvent.Toast(partialChange.error.getErrorMessage())
            is PersonalizedPartialChange.LoadMore.Failure -> CommonUiEvent.Toast(partialChange.error.getErrorMessage())
            is PersonalizedPartialChange.Refresh.Success -> PersonalizedUiEvent.RefreshSuccess(
                partialChange.data.size
            )

            else -> null
        }

    private object ExplorePartialChangeProducer : PartialChangeProducer<PersonalizedUiIntent, PersonalizedPartialChange, PersonalizedUiState> {
        @OptIn(ExperimentalCoroutinesApi::class)
        override fun toPartialChangeFlow(intentFlow: Flow<PersonalizedUiIntent>): Flow<PersonalizedPartialChange> =
            merge(
                intentFlow.filterIsInstance<PersonalizedUiIntent.Refresh>().flatMapConcat { produceRefreshPartialChange() },
                intentFlow.filterIsInstance<PersonalizedUiIntent.LoadMore>().flatMapConcat { it.producePartialChange() },
                intentFlow.filterIsInstance<PersonalizedUiIntent.Dislike>().flatMapConcat { it.producePartialChange() },
                intentFlow.filterIsInstance<PersonalizedUiIntent.Agree>().flatMapConcat { it.producePartialChange() },
            )

        private fun produceRefreshPartialChange(): Flow<PersonalizedPartialChange.Refresh> =
            PersonalizedRepository
                .personalizedFlow(1, 1)
                .map<PersonalizedResponse, PersonalizedPartialChange.Refresh> { response ->
                    PersonalizedPartialChange.Refresh.Success(
                        data = response.toData()
                    )
                }
                .onStart { emit(PersonalizedPartialChange.Refresh.Start) }
                .catch { emit(PersonalizedPartialChange.Refresh.Failure(it)) }

        private fun PersonalizedUiIntent.LoadMore.producePartialChange(): Flow<PersonalizedPartialChange.LoadMore> =
            PersonalizedRepository
                .personalizedFlow(2, page)
                .map<PersonalizedResponse, PersonalizedPartialChange.LoadMore> { response ->
                    PersonalizedPartialChange.LoadMore.Success(
                        currentPage = page,
                        data = response.toData(),
                    )
                }
                .onStart { emit(PersonalizedPartialChange.LoadMore.Start) }
                .catch { emit(PersonalizedPartialChange.LoadMore.Failure(currentPage = page, error = it)) }

        private fun PersonalizedUiIntent.Dislike.producePartialChange(): Flow<PersonalizedPartialChange.Dislike> =
            TiebaApi.getInstance().submitDislikeFlow(
                DislikeBean(
                    threadId.toString(),
                    reasons.joinToString(",") { it.get { dislikeId }.toString() },
                    forumId?.toString(),
                    clickTime,
                    reasons.joinToString(",") { it.get { extra } },
                )
            ).map<CommonResponse, PersonalizedPartialChange.Dislike> { PersonalizedPartialChange.Dislike.Success(threadId) }
                .catch { emit(PersonalizedPartialChange.Dislike.Failure(threadId, it)) }
                .onStart { emit(PersonalizedPartialChange.Dislike.Start(threadId)) }

        private fun PersonalizedUiIntent.Agree.producePartialChange(): Flow<PersonalizedPartialChange.Agree> =
            TiebaApi.getInstance()
                .opAgreeFlow(
                    threadId.toString(), postId.toString(), if (hasAgree) 1 else 0, objType = 3
                )
                .map<AgreeBean, PersonalizedPartialChange.Agree> {
                    PersonalizedPartialChange.Agree.Success
                }
                .catch {
                    emit(PersonalizedPartialChange.Agree.Failure(threadId, it))
                }
                .onStart {
                    emit(PersonalizedPartialChange.Agree.Start(threadId))
                }

        private fun PersonalizedResponse.toData(): ImmutableList<ThreadItemData> {
            val threadPersonalizedData = data_?.thread_personalized
            val threadList = data_?.thread_list ?: return persistentListOf()

            return threadList
                .filter { !App.INSTANCE.appPreferences.blockVideo || it.videoInfo == null }
                .filter { it.ala_info == null }
                .map { thread ->
                    val personalized = threadPersonalizedData?.firstOrNull { it.tid == thread.id }
                    ThreadItemData(
                        thread = ThreadInfoItem(thread),
                        personalized = personalized?.wrapImmutable()
                    )
                }
                .toImmutableList()
        }
    }
}

sealed interface PersonalizedUiIntent : UiIntent {
    data object Refresh : PersonalizedUiIntent

    data class LoadMore(val page: Int) : PersonalizedUiIntent

    data class Agree(
        val threadId: Long,
        val postId: Long,
        val hasAgree: Boolean
    ) : PersonalizedUiIntent

    data class Dislike(
        val forumId: Long?,
        val threadId: Long,
        val reasons: List<ImmutableHolder<DislikeReason>>,
        val clickTime: Long
    ) : PersonalizedUiIntent
}

sealed interface PersonalizedPartialChange : PartialChange<PersonalizedUiState> {
    sealed class Agree() : PersonalizedPartialChange {

        override fun reduce(oldState: PersonalizedUiState): PersonalizedUiState =
            when (this) {
                is Start -> oldState.copy(data = oldState.data.updateAgreeStatus(threadId))

                is Success -> oldState

                is Failure -> oldState.copy(data = oldState.data.updateAgreeStatus(threadId))
            }

        data class Start(val threadId: Long) : Agree()

        object Success: Agree()

        data class Failure(
            val threadId: Long,
            val error: Throwable
        ) : Agree()
    }

    sealed class Dislike() : PersonalizedPartialChange {
        override fun reduce(oldState: PersonalizedUiState): PersonalizedUiState =
            when (this) {
                is Start -> {
                    if (!oldState.hiddenThreadIds.contains(threadId)) {
                        oldState.copy(hiddenThreadIds = (oldState.hiddenThreadIds + threadId).toImmutableList())
                    } else {
                        oldState
                    }
                }
                is Success -> {
                    if (!oldState.hiddenThreadIds.contains(threadId)) {
                        oldState.copy(hiddenThreadIds = (oldState.hiddenThreadIds + threadId).toImmutableList())
                    } else {
                        oldState
                    }
                }
                is Failure -> oldState
            }

        data class Start(
            val threadId: Long,
        ) : Dislike()

        data class Success(
            val threadId: Long,
        ) : Dislike()

        data class Failure(
            val threadId: Long,
            val error: Throwable,
        ) : Dislike()
    }

    sealed class Refresh() : PersonalizedPartialChange {
        override fun reduce(oldState: PersonalizedUiState): PersonalizedUiState =
            when (this) {
                Start -> oldState.copy(isRefreshing = true, error = null)
                is Success -> {
                    val oldSize = oldState.data.size
                    val newData = (data + oldState.data).distinctById()
                    oldState.copy(
                        isRefreshing = false,
                        error = null,
                        currentPage = 1,
                        data = newData,
                        refreshPosition = if (oldState.data.isEmpty()) 0 else (newData.size - oldSize),
                    )
                }

                is Failure -> oldState.copy(
                    isRefreshing = false,
                    error = error.wrapImmutable()
                )
            }

        data object Start : Refresh()

        data class Success(
            val data: List<ThreadItemData>,
        ) : Refresh()

        data class Failure(
            val error: Throwable,
        ) : Refresh()
    }

    sealed class LoadMore : PersonalizedPartialChange {
        override fun reduce(oldState: PersonalizedUiState): PersonalizedUiState =
            when (this) {
                Start -> oldState.copy(isLoadingMore = true, error = null)
                is Success -> oldState.copy(
                    isLoadingMore = false,
                    error = null,
                    currentPage = currentPage,
                    data = (oldState.data + data).distinctById(),
                )

                is Failure -> oldState.copy(
                    isLoadingMore = false,
                    error = error.wrapImmutable()
                )
            }

        data object Start : LoadMore()

        data class Success(
            val currentPage: Int,
            val data: List<ThreadItemData>,
        ) : LoadMore()

        data class Failure(
            val currentPage: Int,
            val error: Throwable,
        ) : LoadMore()
    }
}

data class PersonalizedUiState(
    val isRefreshing: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: ImmutableHolder<Throwable>? = null,
    val currentPage: Int = 1,
    val data: ImmutableList<ThreadItemData> = persistentListOf(),
    val hiddenThreadIds: ImmutableList<Long> = persistentListOf(),
    val refreshPosition: Int = 0,
): UiState

sealed interface PersonalizedUiEvent : UiEvent {
    data class RefreshSuccess(val count: Int) : PersonalizedUiEvent
}