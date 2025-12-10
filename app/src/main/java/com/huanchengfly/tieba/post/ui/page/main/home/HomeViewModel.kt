package com.huanchengfly.tieba.post.ui.page.main.home

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.huanchengfly.tieba.post.api.retrofit.exception.NoConnectivityException
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.models.database.ForumHistory
import com.huanchengfly.tieba.post.repository.HistoryRepository
import com.huanchengfly.tieba.post.repository.HomeRepository
import com.huanchengfly.tieba.post.repository.user.OKSignRepository
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.models.LikedForum
import com.huanchengfly.tieba.post.ui.models.settings.UISettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
) : ViewModel() {

    private val uiSettings: Settings<UISettings> = settingsRepo.uiSettings

    private val _uiState: MutableStateFlow<HomeUiState> = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val handler = CoroutineExceptionHandler { _, e ->
        if (e !is NoConnectivityException && e !is TiebaNotLoggedInException) {
            Log.e(TAG, "onError: ", e)
        }
        _uiState.update { it.copy(isLoading = false, error = e) }
    }

    /**
     * PagingData of user forums.
     * */
    val forums: Flow<PagingData<LikedForum>> = homeRepo.getLikedForums(pinned = false)
        .catch { e -> handler.handleException(currentCoroutineContext(), e) }
        .cachedIn(viewModelScope)

    /**
     * PagingData of pinned top forums.
     *
     * @see onPinnedForumChanged
     * */
    val pinnedForums: Flow<PagingData<LikedForum>> = homeRepo.getLikedForums(pinned = true)
        .catch { e -> handler.handleException(currentCoroutineContext(), e) }
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
        .stateIn(viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = null)

    val isOkSignWorkerRunning: SharedFlow<Boolean>
        get() = okSignRepo.isOKSignWorkerRunning

    init {
        refreshInternal(cached = true)
    }

    private fun refreshInternal(cached: Boolean) = viewModelScope.launch(handler) {
        _uiState.update { HomeUiState(isLoading = true) }
        homeRepo.refresh(cached)
        delay(200) // wait 200ms for data mapping in forumListsFlow
        _uiState.update { it.copy(isLoading = false, error = null) }
    }

    fun onRefresh() {
        if (!_uiState.value.isLoading) refreshInternal(cached = false)
    }

    fun onDislikeForum(forum: LikedForum) {
        viewModelScope.launch(handler) { homeRepo.requestDislikeForum(forum) }
    }

    fun onPinnedForumChanged(forum: LikedForum, isTop: Boolean) {
        viewModelScope.launch(handler) {
            if (isTop) {
                homeRepo.addTopForum(forum)
            } else {
                homeRepo.removeTopForum(forum)
            }
        }
    }

    fun onListModeChanged() = uiSettings.save { it.copy(homeForumList = !it.homeForumList) }
}
