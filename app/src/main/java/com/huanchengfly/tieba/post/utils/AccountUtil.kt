package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.webkit.CookieManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastFirstOrNull
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.LoginBean
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.dataStoreScope
import com.huanchengfly.tieba.post.distinctUntilChangedByKeys
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.putInt
import com.huanchengfly.tieba.post.toastShort
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
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
        private const val KEY_CURRENT_ACCOUNT_ID = "account_now"

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

    private val dataStore: DataStore<Preferences> = context.dataStore

    val currentAccount: StateFlow<Account?> = dataStore.data
        .distinctUntilChangedByKeys(intPreferencesKey(KEY_CURRENT_ACCOUNT_ID))
        .map {
            val id = it[intPreferencesKey(KEY_CURRENT_ACCOUNT_ID)] ?: -1
            if (id != -1) getAccountInfo(id) else null
        }
        .stateIn(dataStoreScope, SharingStarted.Eagerly, null)

    var allAccounts: ImmutableList<Account> by mutableStateOf(persistentListOf())
        private set

    private val scope = MainScope()

    init {
        scope.launch {
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

    private fun setCurrentAccountId(id: Int) = dataStore.putInt(KEY_CURRENT_ACCOUNT_ID, id)

    suspend fun saveNewAccount(uid: String, account: Account): Boolean = withContext(Dispatchers.Main) {
        val succeed = saveAccount(uid, account)
        if (succeed) {
            allAccounts = listAllAccount()
            val newAccount = allAccounts.fastFirst { it.uid == account.uid }
            setCurrentAccountId(id = newAccount.id)
        }
        return@withContext succeed
    }

    fun switchAccount(id: Int) {
        // Double check
        if (currentAccount.value?.id != id && allAccounts.fastFirstOrNull { it.id == id } == null) {
            setCurrentAccountId(id)
        }
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
        currentAccount.first()!!.let { account ->
            withContext(Dispatchers.IO) { account.delete() }
        }

        CookieManager.getInstance().removeAllCookies(null)
        allAccounts = listAllAccount()
        val nextAccount = allAccounts.firstOrNull()
        if (nextAccount != null) {
            setCurrentAccountId(nextAccount.id)
            context.toastShort(R.string.toast_exit_account_switched, nextAccount.nameShow ?: nextAccount.name)
        } else {
            setCurrentAccountId(-1)
            context.toastShort(R.string.toast_exit_account_success)
        }
    }
}