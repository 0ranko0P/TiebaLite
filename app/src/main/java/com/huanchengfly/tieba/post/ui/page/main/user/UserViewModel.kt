package com.huanchengfly.tieba.post.ui.page.main.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.utils.AccountUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val accountUtil = AccountUtil.getInstance()

    init {
        onRefresh()
    }

    fun onRefresh() = viewModelScope.launch {
        if (_isLoading.value) return@launch
        val account = accountUtil.currentAccount.firstOrNull() ?: return@launch

        _isLoading.update { true }
        val updatedAccount = TiebaApi.getInstance()
            .userProfileFlow(account.uid.toLong())
            .cancellable()
            .catch {
                _isLoading.update { false }
                CommonUiEvent.Toast(it.getErrorMessage())
            }
            .map { profile ->
                val user = checkNotNull(profile.data_?.user) { "Empty user profile" }
                account.copy(
                    nameShow = user.nameShow,
                    portrait = user.portrait,
                    intro = user.intro.takeUnless { it.isEmpty() },
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
            }
            .firstOrNull() // Ignore network errors, display old account

        if (updatedAccount != null && account != updatedAccount) {
            accountUtil.saveNewAccount(account.uid, updatedAccount)
        }
        _isLoading.update { false }
    }
}