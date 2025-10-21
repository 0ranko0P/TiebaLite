package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import androidx.compose.runtime.staticCompositionLocalOf
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.LoginBean
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.arch.shareInBackground
import com.huanchengfly.tieba.post.components.modules.SettingsRepositoryEntryPoint
import com.huanchengfly.tieba.post.dataStoreScope
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.models.database.TbLiteDatabase
import com.huanchengfly.tieba.post.repository.source.network.UserProfileNetworkDataSource
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

val LocalAccount = staticCompositionLocalOf<Account?> { error("No Account provided") }

class AccountUtil private constructor(context: Context) {

    companion object {
        private const val TAG = "AccountUtil"

        private const val UPDATE_EXPIRE_MILL = 0x240C8400 // one week

        @Volatile
        private var _instance: AccountUtil? = null

        fun getInstance(): AccountUtil {
            return _instance ?: synchronized(this) {
                _instance ?: AccountUtil(App.INSTANCE).also { _instance = it }
            }
        }

        inline fun <T> getAccountInfo(getter: Account.() -> T): T? = getLoginInfo()?.getter()

        fun getLoginInfo(): Account? = runBlocking { getInstance().currentAccount.first() }

        fun isLoggedIn(): Boolean = getLoginInfo() != null

        fun getSToken(): String? = getLoginInfo()?.sToken

        fun getCookie(): String? = getLoginInfo()?.cookie

        fun getUid(): String? = getLoginInfo()?.uid?.toString()

        fun getBduss(): String? = getLoginInfo()?.bduss

        fun getBdussCookie(): String? {
            val bduss = getBduss()
            return if (bduss != null) {
                getBdussCookie(bduss)
            } else null
        }

        fun getBdussCookie(bduss: String): String {
            return "BDUSS=$bduss; Path=/; Max-Age=315360000; Domain=.baidu.com; Httponly"
        }

        fun parseCookie(cookie: String): Map<String, String> {
            return cookie
                .split(";")
                .map { it.trim().split("=") }
                .filter { it.size > 1 }
                .associate { it.first() to it.drop(1).joinToString("=") }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main + CoroutineName(TAG) + SupervisorJob())

    private val networkDataSource = UserProfileNetworkDataSource

    private val accountUidSettings: Settings<Long> = EntryPointAccessors
        .fromApplication<SettingsRepositoryEntryPoint>(context) // get settings repository manually
        .settingsRepository()
        .accountUid

    private val accountDao = TbLiteDatabase.getInstance(context).accountDao()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentAccount: StateFlow<Account?> = accountUidSettings.flow
        .flatMapMerge { uid ->
            if (uid != -1L) accountDao.observeById(uid) else flowOf(null)
        }
        .stateIn(dataStoreScope, SharingStarted.Eagerly, null)

    val allAccounts: SharedFlow<List<Account>> = accountDao.observeAll()
        .shareInBackground()

    fun fetchAccountFlow(account: Account): Flow<Account> {
        return fetchAccountFlow(account.bduss, account.sToken, account.cookie)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun fetchAccountFlow(
        bduss: String,
        sToken: String,
        cookie: String? = null
    ): Flow<Account> {
        return TiebaApi.getInstance()
            .loginFlow(bduss, sToken)
            .zip(TiebaApi.getInstance().initNickNameFlow(bduss, sToken)) { loginBean, _ ->
                val account = accountDao.getById(loginBean.user.id.toLong())
                return@zip if (account != null) {
                    account.copy(
                        bduss = bduss,
                        sToken = sToken,
                        cookie = cookie ?: getBdussCookie(bduss)
                    ).apply {
                        updateAccount(this, loginBean)
                    }
                } else Account(
                    uid = loginBean.user.id.toLong(),
                    name = loginBean.user.name,
                    bduss = bduss,
                    tbs = loginBean.anti.tbs,
                    portrait = loginBean.user.portrait,
                    sToken = sToken,
                    cookie = cookie ?: getBdussCookie(bduss),
                )
            }
            .zip(SofireUtils.fetchZid()) { account, zid ->
                account.copy(zid = zid)
            }
            .flatMapConcat { account ->
                TiebaApi.getInstance()
                    .getUserInfoFlow(account.uid.toLong(), account.bduss, account.sToken)
                    .map { checkNotNull(it.data_?.user) }
                    .map { user ->
                        account.copy(nickname = user.nameShow, portrait = user.portrait)
                    }
                    .catch {
                        emit(account)
                    }
            }
            .onEach { account ->
                accountDao.upsert(account)
            }
            .flowOn(Dispatchers.IO)
    }

    /**
     * Refresh user profile
     * */
    suspend fun refreshCurrent(force: Boolean = false): Account {
        val account = currentAccount.first() ?: throw TiebaNotLoggedInException()
        val duration = System.currentTimeMillis() - (account.lastUpdate + UPDATE_EXPIRE_MILL)
        // not force-refresh && not expire
        if (!force && duration < 0) {
            return account
        } else if (duration > 0) {
            Log.i(TAG, "onRefreshCurrent: Cache of ${account.uid} expired for ${duration / 1000}s")
        }

        val user = networkDataSource.loadUserProfile(uid = account.uid)
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
            lastUpdate = System.currentTimeMillis()
        )
        accountDao.upsert(updated)
        return updated
    }

    suspend fun saveNewAccount(account: Account) = accountDao.upsert(account)

    fun switchAccount(uid: Long) {
        if (currentAccount.value?.uid != uid) {
            accountUidSettings.set(uid)
        }
    }

    private suspend fun updateAccount(
        account: Account,
        loginBean: LoginBean,
    ) {
        val updated = account.copy(
            uid = loginBean.user.id.toLong(),
            name = loginBean.user.name,
            portrait = loginBean.user.portrait,
            tbs = loginBean.anti.tbs,
        )
        accountDao.upsert(updated)
    }

    fun exit(context: Context, oldAccount: Account) {
        scope.launch {
            val nextAccount = accountDao.getAll().firstOrNull { it.uid != oldAccount.uid }
            CookieManager.getInstance().removeAllCookies(null)
            // switch to next account (if exists)
            accountUidSettings.set(nextAccount?.uid ?: -1)
            accountDao.deleteById(uid = oldAccount.uid)
            if (nextAccount != null) {
                context.toastShort(R.string.toast_exit_account_switched, nextAccount.nickname ?: nextAccount.name)
            } else {
                context.toastShort(R.string.toast_exit_account_success)
            }
        }
    }
}