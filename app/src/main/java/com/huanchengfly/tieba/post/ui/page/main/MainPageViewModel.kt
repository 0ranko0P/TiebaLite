package com.huanchengfly.tieba.post.ui.page.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.arch.stateInViewModel
import com.huanchengfly.tieba.post.repository.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainPageViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    private val homeRepo: HomeRepository,
): ViewModel() {

    val messageCountFlow: StateFlow<String?> = homeRepo.observeNewMessage()
        .catch { /* Ignored */ }
        .map {
            when {
                it > 99 -> "99+"
                it <= 0 -> null
                else -> it.toString()
            }
        }
        .stateInViewModel(initialValue = null)

    fun onNavigateNotification() = viewModelScope.launch {
        homeRepo.clearNewMessage()
    }
}