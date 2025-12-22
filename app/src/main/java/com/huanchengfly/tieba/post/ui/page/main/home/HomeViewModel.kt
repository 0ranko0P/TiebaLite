package com.huanchengfly.tieba.post.ui.page.main.home

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.huanchengfly.tieba.post.arch.BaseStateViewModel
import com.huanchengfly.tieba.post.arch.TbLiteExceptionHandler
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.stateInViewModel
import com.huanchengfly.tieba.post.models.database.ForumHistory
import com.huanchengfly.tieba.post.repository.HistoryRepository
import com.huanchengfly.tieba.post.repository.HomeRepository
import com.huanchengfly.tieba.post.repository.user.OKSignRepository
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.models.LikedForum
import com.huanchengfly.tieba.post.ui.models.settings.UISettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject

private const val TAG = "HomeViewModel"

/**
 * Ui State for Home Page
 *
 * @param isLoading loading forum list
 * @param error throwable error
 * */
@Immutable
data class HomeUiState(
    val isLoading: Boolean = true,
    val error: Throwable? = null,
) : UiState

@HiltViewModel
@Stable
class HomeViewModel @Inject constructor(
    private val homeRepo: HomeRepository,
    private val okSignRepo: OKSignRepository,
    historyRepo: HistoryRepository,
    settingsRepo: SettingsRepository
) : BaseStateViewModel<HomeUiState>() {

    private val uiSettings: Settings<UISettings> = settingsRepo.uiSettings

    override val errorHandler = TbLiteExceptionHandler(TAG) { _, e, _ ->
        _uiState.update { it.copy(isLoading = false, error = e) }
    }

    /**
     * PagingData of user forums.
     * */
    val forums: Flow<PagingData<LikedForum>> = homeRepo.getLikedForums(pinned = false)
        .catch { e -> errorHandler.handleException(currentCoroutineContext(), e) }
        .cachedIn(viewModelScope)

    /**
     * PagingData of pinned top forums.
     *
     * @see onPinnedForumChanged
     * */
    val pinnedForums: Flow<PagingData<LikedForum>> = homeRepo.getLikedForums(pinned = true)
        .catch { e -> errorHandler.handleException(currentCoroutineContext(), e) }
        .cachedIn(viewModelScope)

    /**
     * Recent forum history.
     *
     * @see SettingsRepository.habitSettings
     * */
    @OptIn(ExperimentalCoroutinesApi::class)
    val historyFlow: StateFlow<List<ForumHistory>?> = settingsRepo.habitSettings
        .map { it.showHistoryInHome }
        .distinctUntilChanged()
        .flatMapLatest { showHistory ->
            if (showHistory) historyRepo.getForumHistoryTop10() else flowOf(emptyList())
        }
        .stateInViewModel(started = SharingStarted.Lazily, initialValue = null)

    val isOkSignWorkerRunning: SharedFlow<Boolean>
        get() = okSignRepo.isOKSignWorkerRunning

    init {
        refreshInternal(cached = true)
    }

    override fun createInitialState(): HomeUiState = HomeUiState(isLoading = true)

    private fun refreshInternal(cached: Boolean): Unit = launchInVM {
        _uiState.update { HomeUiState(isLoading = true) }
        homeRepo.refresh(cached)
        delay(200) // wait 200ms for data mapping in forumListsFlow
        _uiState.update { it.copy(isLoading = false, error = null) }
    }

    fun onRefresh() {
        if (!currentState.isLoading) refreshInternal(cached = false)
    }

    fun onDislikeForum(forum: LikedForum): Unit = launchInVM {
        homeRepo.requestDislikeForum(forum)
    }

    fun onPinnedForumChanged(forum: LikedForum, isTop: Boolean): Unit = launchInVM {
        if (isTop) {
            homeRepo.addTopForum(forum)
        } else {
            homeRepo.removeTopForum(forum)
        }
    }

    fun onListModeChanged() = uiSettings.save { it.copy(homeForumList = !it.homeForumList) }
}
