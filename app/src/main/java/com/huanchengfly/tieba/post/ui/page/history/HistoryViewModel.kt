package com.huanchengfly.tieba.post.ui.page.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.huanchengfly.tieba.post.models.database.ForumHistory
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.models.database.ThreadHistory
import com.huanchengfly.tieba.post.repository.HistoryRepository
import com.huanchengfly.tieba.post.repository.UserHistory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(private val historyRepo: HistoryRepository) : ViewModel() {

    val forumHistory: Flow<PagingData<ForumHistory>> = historyRepo.getForumHistory()
        .cachedIn(viewModelScope)

    val threadHistory: Flow<PagingData<ThreadHistory>> = historyRepo.getThreadHistory()
        .cachedIn(viewModelScope)

    val userHistory: Flow<PagingData<UserHistory>> = historyRepo.getUserHistory()
        .cachedIn(viewModelScope)

    fun onDelete(history: History) = historyRepo.deleteHistory(history)

    /**
     * Called when delete all button clicked
     * */
    fun onDeleteAll() = historyRepo.deleteAll()
}