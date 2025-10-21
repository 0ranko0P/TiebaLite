package com.huanchengfly.tieba.post.ui.page.settings.blocklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.models.database.BlockUser
import com.huanchengfly.tieba.post.repository.BlockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BlockListViewModel @Inject constructor(private val blockRepo: BlockRepository): ViewModel() {

    val userBlacklist: StateFlow<List<BlockUser>> = blockRepo.observeUsers(whitelisted = false)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val userWhitelist: StateFlow<List<BlockUser>> = blockRepo.observeUsers(whitelisted = true)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun getBlacklist(): StateFlow<List<String>> = blockRepo.blacklist

    fun getWhitelist(): StateFlow<List<String>> = blockRepo.whitelist

    fun addKeyword(keyword: String, whitelisted: Boolean) = blockRepo.addKeyword(keyword.trim(), whitelisted)

    fun hasKeyword(keyword: String): Boolean {
        return getWhitelist().value.contains(keyword) || getBlacklist().value.contains(keyword)
    }

    fun onDelete(keyword: String) = blockRepo.deleteKeyword(keyword)

    fun onDelete(user: BlockUser) = blockRepo.deleteUser(uid = user.uid)
}