package com.huanchengfly.tieba.post.ui.page.main.user

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.utils.AccountUtil
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class UserViewModel : ViewModel() {

    private val scope = MainScope()

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    init {
        onRefresh()
    }

    fun onRefresh() = scope.launch {
        if (_isLoading.value) return@launch
        val accountUtil = AccountUtil.getInstance()
        val account = accountUtil.currentAccount.value ?: return@launch
        _isLoading.value = true
        TiebaApi.getInstance()
            .userProfileFlow(account.uid.toLong())
            .catch {
                _isLoading.value = false
                it.printStackTrace()
                CommonUiEvent.Toast(it.getErrorMessage())
            }
            .cancellable()
            .collect { profile ->
                val user = checkNotNull(profile.data_?.user) { "Empty user profile" }
                val updatedAccount = account.copy(
                    nameShow = user.nameShow,
                    portrait = user.portrait,
                    intro = user.intro,
                    sex = user.sex.toString(),
                    fansNum = user.fans_num.toString(),
                    postNum = user.post_num.toString(),
                    threadNum = user.thread_num.toString(),
                    concernNum = user.concern_num.toString(),
                    tbAge = user.tb_age,
                    age = user.birthday_info?.age?.toString(),
                    birthdayShowStatus =
                    user.birthday_info?.birthday_show_status?.toString(),
                    birthdayTime = user.birthday_info?.birthday_time?.toString(),
                    constellation = user.birthday_info?.constellation,
                    tiebaUid = user.tieba_uid,
                    loadSuccess = true,
                )
                ensureActive()
                if (account != updatedAccount) {
                    accountUtil.saveNewAccount(account.uid, updatedAccount)
                }
                _isLoading.value = false
            }
    }

    override fun onCleared() {
        super.onCleared()
        scope.cancel()
    }
}