package com.huanchengfly.tieba.post.ui.page.main.home

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.api.retrofit.exception.NoConnectivityException
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.repository.HistoryRepository
import com.huanchengfly.tieba.post.repository.HistoryType
import com.huanchengfly.tieba.post.repository.HomeRepository
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.models.LikedForum
import com.huanchengfly.tieba.post.ui.models.settings.UISettings
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.set
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// TopForum, Forum, History
private typealias ForumLists = Triple<List<LikedForum>, List<LikedForum>, List<History>?>

private const val TAG = "HomeViewModel"

/**
 * Ui State for Home Page
 *
 * @param isLoading loading forum list
 * @param listSingle show forums in list
 * @param topForums list of pinned [LikedForum]
 * @param forums list of [LikedForum]
 * @param history history list
 * @param error throwable error
 * */
@Immutable
/*data */class HomeUiState(
    val isLoading: Boolean = true,
    val listSingle: Boolean = false,
    val topForums: List<LikedForum> = emptyList(),
    val forums: List<LikedForum> = emptyList(),
    val history: List<History>? = emptyList(),
    val error: Throwable? = null,
) : UiState {

    val isEmpty: Boolean
        get() = forums.isEmpty()

    fun copy(
        isLoading: Boolean = this.isLoading,
        listSingle: Boolean = this.listSingle,
        topForums: List<LikedForum> = this.topForums,
        forums: List<LikedForum> = this.forums,
        history: List<History>? = this.history,
        error: Throwable? = this.error,
    ) = HomeUiState(isLoading, listSingle, topForums, forums, history, error)
}

@HiltViewModel
@Stable
class HomeViewModel @Inject constructor(
    private val homeRepo: HomeRepository,
    historyRepo: HistoryRepository,
    settingsRepo: SettingsRepository
) : ViewModel() {

    private val uiSettings: Settings<UISettings> = settingsRepo.uiSettings

    private val _uiState: MutableStateFlow<HomeUiState> = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val handler = CoroutineExceptionHandler { _, e ->
        if (e !is NoConnectivityException) {
            Log.e(TAG, "onError: ", e)
        }
        _uiState.update { it.copy(isLoading = false, error = e) }
    }

    private val forumListsFlow: Flow<ForumLists> =
        combine(
            flow = homeRepo.getTopForumIds(),
            flow2 = homeRepo.likedForums,
            flow3 = historyRepo.getHistoryFlow(type = HistoryType.FORUM),
            flow4 = settingsRepo.habitSettings.flow
        ) { topForumIds, forumList, history, habit ->
            // check hide history
            val historyList = if (habit.showHistoryInHome) history else null
            if (forumList == null) return@combine ForumLists(emptyList(), emptyList(), historyList)

            val forums = mutableListOf<LikedForum>()
            val topForums = mutableListOf<LikedForum>()
            // Split into TopForums and Forums
            forumList.fastForEach {
                if (topForumIds.contains(it.id)) topForums.add(it) else forums.add(it)
            }
            ForumLists(topForums, forums, historyList)
        }
        .catch { e -> _uiState.update { it.copy(isLoading = false, error = e) } }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            forumListsFlow.collect { (topForums, forums, history) ->
                _uiState.update { it.copy(topForums = topForums, forums = forums, history = history) }
            }
        }

        viewModelScope.launch(handler) { refreshInternal(true) }
    }

    private suspend fun refreshInternal(cached: Boolean) {
        val listSingle = uiSettings.flow.first().homeForumList
        _uiState.update { HomeUiState(isLoading = true, listSingle = listSingle) }
        homeRepo.refreshForumList(cached)
        _uiState.update { it.copy(isLoading = false, error = null) }
    }

    fun refresh() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch(handler) { refreshInternal(cached = false) }
    }

    fun onDislikeForum(forum: LikedForum) {
        viewModelScope.launch(handler) { homeRepo.dislikeForum(forum) }
    }

    fun onTopStateChanged(forum: LikedForum, isTop: Boolean) {
        viewModelScope.launch(handler) {
            if (isTop) {
                homeRepo.removeTopForum(forum)
            } else {
                homeRepo.addTopForum(forum)
            }
        }
    }

    fun onListModeChanged() {
        val newMode = !_uiState.value.listSingle
        _uiState.set { copy(listSingle = newMode) }
        uiSettings.save { it.copy(homeForumList = newMode) }
    }
}
