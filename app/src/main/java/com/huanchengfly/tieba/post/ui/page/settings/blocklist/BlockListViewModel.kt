package com.huanchengfly.tieba.post.ui.page.settings.blocklist

import android.text.TextUtils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.models.database.BlockKeyword
import com.huanchengfly.tieba.post.models.database.BlockUser
import com.huanchengfly.tieba.post.repository.BlockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import javax.inject.Inject

@HiltViewModel
class BlockListViewModel @Inject constructor(private val blockRepo: BlockRepository): ViewModel() {

    val userBlacklist: StateFlow<List<BlockUser>> = blockRepo.observeUsers(whitelisted = false)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val userWhitelist: StateFlow<List<BlockUser>> = blockRepo.observeUsers(whitelisted = true)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val keywordBlacklist: StateFlow<List<BlockKeyword>> = blockRepo.observeKeyword(whitelisted = false)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val keywordWhitelist: StateFlow<List<BlockKeyword>> = blockRepo.observeKeyword(whitelisted = true)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addKeyword(keyword: String, isRegex: Boolean, whitelisted: Boolean) = blockRepo.addKeyword(keyword.trim(), isRegex, whitelisted)

    fun isKeywordInvalid(keyword: String, isRegex: Boolean): Boolean {
        return TextUtils.isEmpty(keyword) ||
                (isRegex && isRegexInvalid(keyword)) ||
                keywordWhitelist.value.any { it.keyword == keyword } ||
                keywordBlacklist.value.any { it.keyword == keyword }
    }

    private fun isRegexInvalid(regex: String): Boolean {
        try {
            Pattern.compile(regex)
            return false
        } catch (_: PatternSyntaxException) {
            return true
        }
    }

    fun onDelete(keyword: BlockKeyword) = blockRepo.deleteKeyword(keyword)

    fun onDelete(user: BlockUser) = blockRepo.deleteUser(uid = user.uid)
}