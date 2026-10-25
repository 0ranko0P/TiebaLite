package com.huanchengfly.tieba.post.session

import com.huanchengfly.tieba.post.components.SessionManager
import com.huanchengfly.tieba.post.core.common.di.ApplicationScope
import com.huanchengfly.tieba.post.core.data.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.core.database.dao.AccountDao
import com.huanchengfly.tieba.post.core.database.model.Account
import com.huanchengfly.tieba.post.core.network.exception.TiebaNotLoggedInException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake implementation of the [SessionManager]
 */
@Singleton
class FakeSessionManager @Inject constructor(
    @param:ApplicationScope private val scope: CoroutineScope,
    private val accountDao: AccountDao,
    settingsRepo: SettingsRepository,
) : SessionManager {

    private val accountUidSettings = settingsRepo.accountUid

    private val _currentAccount: Flow<Account?> = accountUidSettings
        .flatMapLatest { uid ->
            if (uid > 0) accountDao.observeById(uid) else flowOf(null)
        }
    override val currentAccount: SharedFlow<Account?> = _currentAccount
        .shareIn(scope, started = SharingStarted.Eagerly, replay = 1)

    override val allAccounts: SharedFlow<List<Account>> = accountDao.observeAll()
        .shareIn(scope, started = SharingStarted.Eagerly, replay = 1)

    override suspend fun fetchAccount(
        bduss: String,
        sToken: String,
        cookie: String?,
        zid: String
    ): Account {
        TODO("Not yet implemented")
    }

    override suspend fun updateSigningAccount(): Account = _currentAccount.first()!!

    override suspend fun refreshCurrent(force: Boolean): Account {
        TODO("Not yet implemented")
    }

    override fun saveNewAccount(account: Account) {
        TODO("Not yet implemented")
    }

    override fun switchAccount(uid: Long) {
        TODO("Not yet implemented")
    }

    override suspend fun logout(oldAccount: Account): Account? {
        TODO("Not yet implemented")
    }

    fun getLoginInfo(): Account? = runBlocking { _currentAccount.first() }

    override fun getBduss(): String? = getLoginInfo()?.bduss

    override fun getCookie(): String? = getLoginInfo()?.cookie

    override fun getNickname(): String? = getLoginInfo()?.nickname

    override fun getSToken(): String? = getLoginInfo()?.sToken

    override fun getTbs(): String? = getLoginInfo()?.tbs

    override fun getUid(): String? = getLoginInfo()?.uid?.toString()

    override fun getZid(): String? = getLoginInfo()?.zid

    override fun isLoggedIn(): Boolean = getLoginInfo() != null

    override fun requireUid(): Long = runBlocking {
        accountUidSettings.snapshot().takeUnless { it <= 0 } ?: throw TiebaNotLoggedInException()
    }
}