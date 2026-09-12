package com.huanchengfly.tieba.post.ui.page.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.stateInViewModel
import com.huanchengfly.tieba.post.components.SessionManager
import com.huanchengfly.tieba.post.core.database.model.Account
import com.huanchengfly.tieba.post.toastShort
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountManageViewModel @Inject constructor(
    @param:ApplicationContext val context: Context,
    private val sessionManager: SessionManager,
): ViewModel() {

    val currentAccount: StateFlow<Account?> = sessionManager.currentAccount
        .stateInViewModel(initialValue = null)

    val allAccounts: StateFlow<Map<Long, String>> = sessionManager.allAccounts
        .map { accounts ->
            accounts.associate { it.uid to it.name }
        }
        .stateInViewModel(initialValue = emptyMap())

    fun switchAccount(uid: Long) = sessionManager.switchAccount(uid)

    fun logout() {
        viewModelScope.launch {
            val nextAccount = sessionManager.logout(oldAccount = currentAccount.first()!!)
            if (nextAccount != null) {
                context.toastShort(R.string.toast_exit_account_switched, nextAccount.nickname ?: nextAccount.name)
            } else {
                context.toastShort(R.string.toast_exit_account_success)
            }
        }
    }
}