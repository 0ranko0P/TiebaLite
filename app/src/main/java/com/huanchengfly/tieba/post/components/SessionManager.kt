package com.huanchengfly.tieba.post.components

import android.content.Context
import android.os.Build
import android.util.Log
import android.webkit.CookieManager
import com.huanchengfly.tieba.post.core.common.di.ApplicationScope
import com.huanchengfly.tieba.post.core.database.dao.AccountDao
import com.huanchengfly.tieba.post.core.database.dao.TimestampDao
import com.huanchengfly.tieba.post.core.database.model.Account
import com.huanchengfly.tieba.post.core.network.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.core.network.session.CredentialProvider
import com.huanchengfly.tieba.post.core.network.source.AuthNetworkDataSource
import com.huanchengfly.tieba.post.core.network.source.UserProfileNetworkDataSource
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import com.huanchengfly.tieba.post.utils.workManager
import com.huanchengfly.tieba.post.workers.NewMessageWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

interface SessionManager: CredentialProvider {

    val currentAccount: SharedFlow<Account?>

    val allAccounts: SharedFlow<List<Account>>

    suspend fun fetchAccount(bduss: String, sToken: String, cookie: String? = null, zid: String): Account

    suspend fun updateSigningAccount(): Account

    suspend fun refreshCurrent(force: Boolean = false): Account

    fun saveNewAccount(account: Account)

    fun switchAccount(uid: Long)

    suspend fun logout(oldAccount: Account): Account?
}

/**
 * Default [SessionManager] implementation, serves as the central component for managing user
 * session state.
 *
 * Moved from [com.huanchengfly.tieba.post.utils.AccountUtil]
 *
 * @author HuanChengFly
 * @author 0Ranko0P
 *
 * @since 3.8.1 α
 * */
@Singleton
internal class SessionManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
    private val accountDao: AccountDao,
    private val timeDao: TimestampDao,
    private val authDataSourceProvider: Provider<AuthNetworkDataSource>,
    private val userDataSourceProvider: Provider<UserProfileNetworkDataSource>,
    settingsRepo: SettingsRepository,
) : SessionManager {

    private val accountUidSettings: Settings<Long> = settingsRepo.accountUid

    override val currentAccount: SharedFlow<Account?> = accountUidSettings
        .flatMapMerge { uid ->
            if (uid != -1L) accountDao.observeById(uid) else flowOf(null)
        }
        .shareIn(scope, started = SharingStarted.Eagerly, replay = 1)

    override val allAccounts: SharedFlow<List<Account>> = accountDao.observeAll()
        .shareIn(scope, started = SharingStarted.Eagerly, replay = 1)

    override suspend fun fetchAccount(bduss: String, sToken: String, cookie: String?, zid: String): Account {
        require(zid.isNotEmpty())
        // Fetching account login info, this is non-cancellable
        return scope.async {
            val (loginBean, userInfo) = authDataSourceProvider.get().loginWithInit(bduss, sToken)
            val uid = loginBean.user.id.toLong()
            val nameShow = userInfo.nameShow.takeIf { it != loginBean.user.name }
            var account = accountDao.getById(uid)
            // Update existing account
            if (account != null) {
                account = account.copy(
                    name = loginBean.user.name,
                    nickname = nameShow,
                    bduss = bduss,
                    tbs = loginBean.anti.tbs,
                    portrait = loginBean.user.portrait,
                    sToken = sToken,
                    cookie = cookie ?: AccountUtil.getBdussCookie(bduss),
                    tiebaUid = userInfo.tiebaUid,
                    zid = zid,
                )
            } else {
                account = Account(
                    uid = uid,
                    name = loginBean.user.name,
                    nickname = nameShow,
                    bduss = bduss,
                    tbs = loginBean.anti.tbs,
                    portrait = loginBean.user.portrait,
                    sToken = sToken,
                    cookie = cookie ?: AccountUtil.getBdussCookie(bduss),
                    tiebaUid = userInfo.tiebaUid,
                    zid = zid,
                )
            }
            // Save to the database
            account.also { accountDao.upsert(account = it) }
        }
            .await()
    }

    override suspend fun updateSigningAccount(): Account {
        val account = currentAccount.first() ?: throw TiebaNotLoggedInException()
        val lastUpdate = timeDao.get(account.uid, TimestampDao.TYPE_SIGN_INFO_UPDATED) ?: -1
        val duration = System.currentTimeMillis() - (lastUpdate + FETCH_EXPIRE_MILL)
        if (duration > 0) {
            Log.i(TAG, "onUpdateSigningAccount: Expired for ${duration / 1000}s")
            val zid = account.zid!! // SofireUtils.fetchZid().firstOrThrow()
            return fetchAccount(account.bduss, account.sToken, account.cookie, zid)
        } else {
            return account
        }
    }

    /**
     * Refresh user profile
     * */
    override suspend fun refreshCurrent(force: Boolean): Account {
        val account = currentAccount.first() ?: throw TiebaNotLoggedInException()
        val duration = System.currentTimeMillis() - (account.lastUpdate + UPDATE_EXPIRE_MILL)
        // not force-refresh && not expire
        if (!force && duration < 0) {
            return account
        } else if (duration > 0) {
            Log.i(TAG, "onRefreshCurrent: Cache of ${account.uid} expired for ${duration / 1000}s")
        }

        val (user, anti) = userDataSourceProvider.get().loadUserProfile(uid = account.uid)
        val birthday = user.birthday_info
        val updated = account.copy(
            nickname = user.nameShow,
            portrait = user.portrait,
            intro = user.intro,
            sex = user.sex,
            fans = user.fans_num.getShortNumString(),
            posts = user.post_num.getShortNumString(),
            threads = user.thread_num.getShortNumString(),
            concerned = user.concern_num.getShortNumString(),
            tbAge = user.tb_age.toFloatOrNull() ?: account.tbAge,
            age = birthday?.age?: account.age,
            birthdayShow = birthday?.birthday_show_status == 1,
            birthdayTime = birthday?.birthday_time ?: account.birthdayTime,
            constellation = birthday?.constellation,
            tiebaUid = user.tieba_uid,
            lastUpdate = System.currentTimeMillis(),
            blockDays = anti?.days_tofree?.takeIf { anti.block_stat == 1 } ?: 0,
        )
        accountDao.upsert(account = updated)
        return updated
    }

    override fun saveNewAccount(account: Account) {
        scope.launch(Dispatchers.Main) {
            accountDao.upsert(account)
            if (currentAccount.first() == null) {
                accountUidSettings.set(account.uid)
                // Init shortcuts that requires user login
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                    ShortcutInitializer.initialize(loggedIn = true, context)
                }
            }
            val workManager = context.workManager()
            NewMessageWorker.schedulePeriodically(workManager)
            NewMessageWorker.startNow(workManager)
        }
    }

    override fun switchAccount(uid: Long) {
        scope.launch {
            if (currentAccount.first()?.uid != uid) {
                accountUidSettings.set(uid)
            }
        }
    }

    override suspend fun logout(oldAccount: Account): Account? = scope.async(Dispatchers.Main) {
        val nextAccount = accountDao.getAll().firstOrNull { it.uid != oldAccount.uid }
        CookieManager.getInstance().removeAllCookies(null)
        // switch to next account (if exists)
        accountUidSettings.set(nextAccount?.uid ?: -1)
        accountDao.deleteById(uid = oldAccount.uid)
        if (nextAccount == null) {
            context.workManager().cancelAllWorkByTag(NewMessageWorker.TAG)
            // Remove shortcuts that requires user login
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                ShortcutInitializer.initialize(loggedIn = false, context)
            }
        }
        nextAccount
    }.await()

    fun getLoginInfo(): Account? = runBlocking { currentAccount.first() }

    override fun getBduss(): String? = getLoginInfo()?.bduss

    override fun getCookie(): String? = getLoginInfo()?.cookie

    override fun getNickname(): String? = getLoginInfo()?.nickname

    override fun getSToken(): String? = getLoginInfo()?.sToken

    override fun getTbs(): String? = getLoginInfo()?.tbs

    override fun getUid(): String? = getLoginInfo()?.uid?.toString()

    override fun getZid(): String? = getLoginInfo()?.zid

    override fun isLoggedIn(): Boolean = getLoginInfo() != null

    override fun requireUid(): Long {
        return getLoginInfo()?.uid ?: throw TiebaNotLoggedInException()
    }

    companion object {
        private const val TAG = "SessionManager"

        private const val UPDATE_EXPIRE_MILL = 0x240C8400 // one week

        private const val FETCH_EXPIRE_MILL = 0x112A880 // 5 hours
    }
}
