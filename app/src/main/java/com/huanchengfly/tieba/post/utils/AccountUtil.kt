package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.content.SharedPreferences
import android.webkit.CookieManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.LoginBean
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.toastShort
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.litepal.LitePal.findAll
import org.litepal.LitePal.where
import org.litepal.extension.findFirst
import java.util.UUID

val LocalAccount = staticCompositionLocalOf<Account?> { error("No Account provided") }
val LocalAllAccounts = staticCompositionLocalOf<ImmutableList<Account>> { error("No Accounts provided") }

@Composable
fun LocalAccountProvider(content: @Composable () -> Unit) {
    val accountUtil = AccountUtil.getInstance()
    val account by accountUtil.currentAccount.collectAsState()
    CompositionLocalProvider(
        LocalAccount provides account,
        LocalAllAccounts provides accountUtil.allAccounts,
    ) {
        content()
    }
}

class AccountUtil private constructor(context: Context) {
    companion object {
        private const val PREFERENCE_FILE_NAME = "accountData"
        private const val KEY_CURRENT_ACCOUNT_ID = "now"

        @Volatile
        private var _instance: AccountUtil? = null

        fun getInstance(): AccountUtil {
            return _instance ?: synchronized(this) {
                _instance ?: AccountUtil(App.INSTANCE).also { _instance = it }
            }
        }

        fun <T> getAccountInfo(getter: Account.() -> T): T? = getLoginInfo()?.getter()

        fun getLoginInfo(): Account? = getInstance().currentAccount.value

        fun isLoggedIn(): Boolean = getLoginInfo() != null

        fun getSToken(): String? = getLoginInfo()?.sToken

        fun getCookie(): String? = getLoginInfo()?.cookie

        fun getUid(): String? = getLoginInfo()?.uid

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

    private val _currentAccount: MutableStateFlow<Account?> = MutableStateFlow(null)
    val currentAccount: StateFlow<Account?> = _currentAccount

    var allAccounts: ImmutableList<Account> by mutableStateOf(persistentListOf())
        private set

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCE_FILE_NAME, Context.MODE_PRIVATE)

    private val scope = MainScope()

    init {
        scope.launch {
            _currentAccount.value = getLoginAccount()
            allAccounts = listAllAccount()
        }
    }

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
                val account = getAccountInfoByUid(loginBean.user.id)
                return@zip if (account != null) {
                    account.copy(
                        bduss = bduss,
                        sToken = sToken,
                        cookie = cookie ?: getBdussCookie(bduss)
                    ).apply {
                        updateAccount(this, loginBean)
                    }
                } else Account(
                    loginBean.user.id,
                    loginBean.user.name,
                    bduss,
                    loginBean.anti.tbs,
                    loginBean.user.portrait,
                    sToken,
                    cookie ?: getBdussCookie(bduss),
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
                        account.copy(nameShow = user.nameShow, portrait = user.portrait)
                    }
                    .catch {
                        emit(account)
                    }
            }
            .onEach { account ->
                saveAccount(account.uid, account)
            }
            .flowOn(Dispatchers.IO)
    }

    private suspend fun getLoginAccount(): Account? = withContext(Dispatchers.IO) {
        runCatching {
            val loginUser = preferences.getInt(KEY_CURRENT_ACCOUNT_ID, -1)
            if (loginUser == -1) null else getAccountInfo(loginUser)
        }.getOrNull()
    }

    /**
     * Query all Accounts from disk, Non-Cancellable.
     * */
    private suspend fun listAllAccount(): ImmutableList<Account> = withContext(Dispatchers.IO) {
        findAll(Account::class.java).toImmutableList()
    }

    private suspend fun saveAccount(uid: String, account: Account): Boolean = withContext(Dispatchers.IO) {
        account.saveOrUpdate("uid = ?", uid)
    }

    private suspend fun getAccountInfo(accountId: Int): Account = withContext(Dispatchers.IO) {
        where("id = ?", accountId.toString()).findFirst(Account::class.java)
    }

    private suspend fun getAccountInfoByUid(uid: String): Account? = withContext(Dispatchers.IO) {
        where("uid = ?", uid).findFirst<Account>()
    }

    private suspend fun getAccountInfoByBduss(bduss: String): Account = withContext(Dispatchers.IO) {
        where("bduss = ?", bduss).findFirst(Account::class.java)
    }

    private suspend fun saveAccountId(id: Int): Boolean = withContext(Dispatchers.IO) {
        preferences.edit().putInt(KEY_CURRENT_ACCOUNT_ID, id).commit()
    }

    suspend fun saveNewAccount(uid: String, account: Account): Boolean = withContext(Dispatchers.Main) {
        val succeed = saveAccount(uid, account)
        if (succeed) {
            allAccounts = listAllAccount()
            val newAccount = getAccountInfoByUid(account.uid)!!
            _currentAccount.value = newAccount
            saveAccountId(newAccount.id)
        }
        return@withContext succeed
    }

    fun switchAccount(id: Int) = scope.launch(Dispatchers.Main) {
        val account = runCatching { getAccountInfo(id) }.getOrNull() ?: return@launch
        _currentAccount.value = account
        saveAccountId(id)
    }

    private suspend fun updateAccount(
        account: Account,
        loginBean: LoginBean,
    ) {
        val updated = account.copy(
            uid = loginBean.user.id,
            name = loginBean.user.name,
            portrait = loginBean.user.portrait,
            tbs = loginBean.anti.tbs,
            uuid = if (account.uuid.isNullOrBlank()) UUID.randomUUID().toString() else account.uuid
        )
        saveAccount(account.uid, updated)
    }

    fun exit(context: Context) = scope.launch {
        withContext(Dispatchers.IO) {
            _currentAccount.value!!.delete()
        }
        CookieManager.getInstance().removeAllCookies(null)
        allAccounts = listAllAccount()
        if (allAccounts.size > 1) {
            val account = allAccounts[0]
            switchAccount(account.id)
            context.toastShort("退出登录成功，已切换至账号 " + account.nameShow)
        } else {
            _currentAccount.value = null
            preferences.edit().clear().apply()
            context.toastShort(R.string.toast_exit_account_success)
        }
    }
}