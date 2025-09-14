package com.huanchengfly.tieba.post.ui.page.main.explore.hot

import androidx.compose.runtime.Stable
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.AgreeBean
import com.huanchengfly.tieba.post.api.models.protos.FrsTabInfo
import com.huanchengfly.tieba.post.api.models.protos.RecommendTopicList
import com.huanchengfly.tieba.post.api.models.protos.hotThreadList.HotThreadListResponse
import com.huanchengfly.tieba.post.arch.BaseViewModel
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.PartialChange
import com.huanchengfly.tieba.post.arch.PartialChangeProducer
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiIntent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.models.ThreadItemData
import com.huanchengfly.tieba.post.ui.models.updateAgreeStatus
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
class HotViewModel @Inject constructor() :
    BaseViewModel<HotUiIntent, HotPartialChange, HotUiState, HotUiEvent>() {
    override fun createInitialState(): HotUiState = HotUiState()

    override fun createPartialChangeProducer(): PartialChangeProducer<HotUiIntent, HotPartialChange, HotUiState> =
        HotPartialChangeProducer

    private object HotPartialChangeProducer :
        PartialChangeProducer<HotUiIntent, HotPartialChange, HotUiState> {
        @OptIn(ExperimentalCoroutinesApi::class)
        override fun toPartialChangeFlow(intentFlow: Flow<HotUiIntent>): Flow<HotPartialChange> =
            merge(
                intentFlow.filterIsInstance<HotUiIntent.Load>()
                    .flatMapConcat { produceLoadPartialChange() },
                intentFlow.filterIsInstance<HotUiIntent.RefreshThreadList>()
                    .flatMapConcat { it.producePartialChange() },
                intentFlow.filterIsInstance<HotUiIntent.Agree>()
                    .flatMapConcat { it.producePartialChange() },
            )

        private fun produceLoadPartialChange(): Flow<HotPartialChange.Load> =
            TiebaApi.getInstance().hotThreadListFlow("all")
                .map<HotThreadListResponse, HotPartialChange.Load> {
                    HotPartialChange.Load.Success(
                        it.data_?.topicList ?: emptyList(),
                        it.data_?.hotThreadTabInfo ?: emptyList(),
                        it.data_?.threadInfo?.map { info -> ThreadItemData(info) } ?: emptyList()
                    )
                }
                .onStart { emit(HotPartialChange.Load.Start) }
                .catch { emit(HotPartialChange.Load.Failure(it)) }

        private fun HotUiIntent.RefreshThreadList.producePartialChange(): Flow<HotPartialChange.RefreshThreadList> =
            TiebaApi.getInstance().hotThreadListFlow(tabCode)
                .map<HotThreadListResponse, HotPartialChange.RefreshThreadList> {
                    HotPartialChange.RefreshThreadList.Success(
                        tabCode,
                        it.data_?.threadInfo?.map { info -> ThreadItemData(info) } ?: emptyList()
                    )
                }
                .onStart {
                    emit(HotPartialChange.RefreshThreadList.Start(tabCode))
                }
                .catch {
                    emit(HotPartialChange.RefreshThreadList.Failure(tabCode, it))
                }

        private fun HotUiIntent.Agree.producePartialChange(): Flow<HotPartialChange.Agree> =
            TiebaApi.getInstance()
                .opAgreeFlow(
                    threadId.toString(), postId.toString(), if (hasAgree) 1 else 0, objType = 3
                )
                .map<AgreeBean, HotPartialChange.Agree> {
                    HotPartialChange.Agree.Success
                }
                .onStart {
                    emit(HotPartialChange.Agree.Start(threadId))
                }
                .catch {
                    emit(HotPartialChange.Agree.Failure(threadId, it))
                }
    }
}

sealed interface HotUiIntent : UiIntent {
    object Load : HotUiIntent

    data class RefreshThreadList(val tabCode: String) : HotUiIntent

    data class Agree(
        val threadId: Long,
        val postId: Long,
        val hasAgree: Boolean
    ) : HotUiIntent
}

sealed interface HotPartialChange : PartialChange<HotUiState> {
    sealed class Load : HotPartialChange {
        override fun reduce(oldState: HotUiState): HotUiState =
            when (this) {
                Start -> oldState.copy(isRefreshing = true, error = null)

                is Success -> oldState.copy(
                    isRefreshing = false,
                    currentTabCode = "all",
                    topicList = topicList.wrapImmutable(),
                    tabList = tabList.wrapImmutable(),
                    threadList = threadList.toImmutableList()
                )

                is Failure -> oldState.copy(isRefreshing = false, error = error)
            }

        object Start : Load()

        data class Success(
            val topicList: List<RecommendTopicList>,
            val tabList: List<FrsTabInfo>,
            val threadList: List<ThreadItemData>,
        ) : Load()

        data class Failure(
            val error: Throwable
        ) : Load()
    }

    sealed class RefreshThreadList : HotPartialChange {
        override fun reduce(oldState: HotUiState): HotUiState =
            when (this) {
                is Start -> oldState.copy(isLoadingThreadList = true, currentTabCode = tabCode)
                is Success -> oldState.copy(
                    isLoadingThreadList = false,
                    currentTabCode = tabCode,
                    threadList = threadList.toImmutableList()
                )

                is Failure -> oldState.copy(isLoadingThreadList = false)
            }

        data class Start(val tabCode: String) : RefreshThreadList()

        data class Success(
            val tabCode: String,
            val threadList: List<ThreadItemData>
        ) : RefreshThreadList()

        data class Failure(
            val tabCode: String,
            val error: Throwable
        ) : RefreshThreadList()
    }

    sealed class Agree() : HotPartialChange {

        override fun reduce(oldState: HotUiState): HotUiState =
            when (this) {
                is Start -> oldState.copy(
                    threadList = oldState.threadList.updateAgreeStatus(threadId)
                )

                is Success -> oldState

                is Failure -> oldState.copy(
                    threadList = oldState.threadList.updateAgreeStatus(threadId)
                )
            }

        data class Start(val threadId: Long) : Agree()

        object Success: Agree()

        data class Failure(
            val threadId: Long,
            val error: Throwable
        ) : Agree()
    }
}

data class HotUiState(
    val isRefreshing: Boolean = false,
    val currentTabCode: String = "all",
    val isLoadingThreadList: Boolean = false,
    val topicList: ImmutableList<ImmutableHolder<RecommendTopicList>> = persistentListOf(),
    val tabList: ImmutableList<ImmutableHolder<FrsTabInfo>> = persistentListOf(),
    val threadList: List<ThreadItemData> = emptyList(),
    val error: Throwable? = null,
) : UiState

sealed interface HotUiEvent : UiEvent