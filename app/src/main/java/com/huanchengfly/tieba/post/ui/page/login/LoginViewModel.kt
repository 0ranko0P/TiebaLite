package com.huanchengfly.tieba.post.ui.page.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.set
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.ClientUtils
import com.huanchengfly.tieba.post.utils.SofireUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State of Login Page
 * */
data class LoginUiState(
    val zid: String? = null,
    val isLoadingZid: Boolean = false,
    val error: Throwable? = null
)

sealed interface LoginUiEvent : UiEvent {
    object Start: LoginUiEvent

    object Success: LoginUiEvent

    class Error(val msg: String): LoginUiEvent
}

@HiltViewModel
class LoginViewModel @Inject constructor(@ApplicationContext val context: Context) : ViewModel() {

    private val _uiEvent: MutableSharedFlow<UiEvent> = MutableSharedFlow()
    val uiEvent: Flow<UiEvent> = _uiEvent.asSharedFlow()

    private val _uiState: MutableStateFlow<LoginUiState> = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var loginJob: Job? = null

    init {
        fetchZid()
    }

    private suspend fun fetchZidInternal() {
        runCatching {
            SofireUtils.fetchZid().firstOrNull() // Blocked by 99% of AD blockers
        }
        .onFailure { e ->
            _uiState.update { it.copy(isLoadingZid = false, error = e) }
        }
        .onSuccess { zid ->
            _uiState.update { it.copy(isLoadingZid = false, zid = zid, error = null) }
        }
    }

    fun fetchZid() {
        if (!_uiState.value.isLoadingZid) _uiState.set { LoginUiState(isLoadingZid = true) } else return

        viewModelScope.launch {
            fetchZidInternal()
        }
    }

    fun onLogin(bduss: String, sToken: String, baiduId: String?, cookie: String) {
        if (loginJob?.isActive == true) return
        loginJob = viewModelScope.launch {
            _uiEvent.emit(LoginUiEvent.Start)
            val accountUtil = AccountUtil.getInstance()
            runCatching {
                if (ClientUtils.baiduId.isNullOrEmpty()) {
                    ClientUtils.saveBaiduId(baiduId)
                }
                val account = accountUtil.fetchAccount(bduss, sToken, cookie, zid = uiState.first().zid!!)
                accountUtil.saveNewAccount(context, account)
            }
            .onFailure {
                _uiEvent.emit(LoginUiEvent.Error(it.getErrorMessage()))
            }
            .onSuccess {
                _uiEvent.emit(LoginUiEvent.Success)
            }
            loginJob = null
        }
    }
}