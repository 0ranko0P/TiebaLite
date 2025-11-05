package com.huanchengfly.tieba.post.ui.page.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.repository.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainPageViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    private val homeRepo: HomeRepository,
): ViewModel() {

    val messageCountFlow: StateFlow<Int> = homeRepo.observeNewMessage()
        .map { it?.toInt() ?: 0 }
        .catch { /* Ignored */ }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = 0)

    fun onNavigateNotification() = viewModelScope.launch {
        homeRepo.clearNewMessage()
    }
}