package com.huanchengfly.tieba.post.ui.page.main.user

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.api.Error.ERROR_NETWORK
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorCode
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.utils.AccountUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        refreshInternal(cached = true)
    }

    private fun refreshInternal(cached: Boolean) = viewModelScope.launch {
        _isLoading.update { true }

        runCatching {
            AccountUtil.getInstance().refreshCurrent(force = !cached)
        }
        .onFailure { e ->
            if (e !is TiebaNotLoggedInException && e.getErrorCode() != ERROR_NETWORK) {
                Log.e("UserViewModel", "onRefresh: ${e.getErrorMessage()}")
            }
        }
        _isLoading.update { false }
    }

    fun onRefresh() {
        if (!_isLoading.value) refreshInternal(cached = false)
    }
}