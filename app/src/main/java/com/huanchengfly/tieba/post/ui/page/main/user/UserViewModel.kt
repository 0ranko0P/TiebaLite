package com.huanchengfly.tieba.post.ui.page.main.user

import android.util.Log
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.components.SessionManager
import com.huanchengfly.tieba.post.core.network.Error.ERROR_NETWORK
import com.huanchengfly.tieba.post.core.network.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.core.network.exception.getErrorCode
import com.huanchengfly.tieba.post.core.network.exception.getErrorMessage
import com.huanchengfly.tieba.post.core.network.util.UIDManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@Stable
class UserViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val uidManager: UIDManager,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        refreshInternal(cached = true)
    }

    private fun refreshInternal(cached: Boolean) = viewModelScope.launch {
        _isLoading.update { true }

        runCatching {
            sessionManager.refreshCurrent(force = !cached)
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

    fun getUegServiceCenterUrl(): String {
        val newCuid = uidManager.newCUID
        return "https://tieba.baidu.com/mo/q/hybrid-main-service/uegServiceCenter?cuid=$newCuid&cuid_galaxy2=$newCuid&cuid_gid=&timestamp=${System.currentTimeMillis()}&_client_version=12.52.1.0&nohead=1"
    }
}