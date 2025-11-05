package com.huanchengfly.tieba.post.utils

import android.content.Context
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.arch.shareInBackground
import com.huanchengfly.tieba.post.coroutines.Dispatchers
import com.huanchengfly.tieba.post.models.database.Account
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

// Dummy AccountUtil
@Suppress("unused", "RedundantSuspendModifier")
class AccountUtil {

    companion object {

        private val _instance = AccountUtil()

        fun getInstance(): AccountUtil = _instance
    }

    private val _currentAccount: MutableStateFlow<Account?> = MutableStateFlow(null)

    val currentAccount: SharedFlow<Account?> = _currentAccount
        .shareInBackground(started = SharingStarted.Lazily)

    suspend fun updateSigningAccount(): Account {
        return _currentAccount.first() ?: throw TiebaNotLoggedInException()
    }

    suspend fun fetchAccount(account: Account): Account = withContext(Dispatchers.Default) {
        delay(100)
        account
    }

    suspend fun saveNewAccount(context: Context?, account: Account?) {
        _currentAccount.update { account }
    }
}