package com.huanchengfly.tieba.post.ui.page.settings.blocklist

import androidx.compose.ui.util.fastAny
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.arch.unsafeLazy
import com.huanchengfly.tieba.post.models.database.BlockForum
import com.huanchengfly.tieba.post.models.database.BlockKeyword
import com.huanchengfly.tieba.post.models.database.BlockUser
import com.huanchengfly.tieba.post.repository.BlockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import javax.inject.Inject

@Suppress("PropertyName")
abstract class BaseBlockListViewModel<T>: ViewModel() {

    protected abstract val _blackList: Flow<List<T>?>
    val blackList: StateFlow<List<T>?> by unsafeLazy {
        _blackList.stateIn(viewModelScope, SharingStarted.Lazily, initialValue = null)
    }

    protected abstract val _whiteList: Flow<List<T>?>
    val whiteList: StateFlow<List<T>?> by unsafeLazy {
        _whiteList.stateIn(viewModelScope, SharingStarted.Lazily, initialValue = null)
    }

    protected val _updating: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val updating: StateFlow<Boolean> = _updating.asStateFlow()

    protected abstract suspend fun upsertInternal(item: T)

    protected abstract suspend fun deleteInternal(item: T)

    protected abstract suspend fun deleteListInternal(items: List<T>)

    open fun upsert(item: T) = update {
        upsertInternal(item)
    }

    open fun delete(item: T) = update {
        deleteInternal(item)
    }

    open fun delete(items: List<T>) = update {
        if (items.size <= 1) {
            deleteInternal(items.first())
        } else {
            deleteListInternal(items)
        }
    }

    protected inline fun update(crossinline block: suspend CoroutineScope.() -> Unit) {
        if (!_updating.value) {
            viewModelScope.launch(Dispatchers.Default) {
                _updating.update { true }
                block()
                _updating.update { false }
            }
        }
    }
}

@HiltViewModel
class ForumBlockListViewModel @Inject constructor(
    private val blockRepo: BlockRepository
): BaseBlockListViewModel<String>() {

    override val _blackList: Flow<List<String>?> = blockRepo.observeForums()

    override val _whiteList: Flow<List<String>?> = flowOf(null)

    override suspend fun upsertInternal(item: String) {
        val forumName = item.trim().run { if (endsWith("吧")) substring(0, lastIndex) else this }
        blockRepo.upsertForum(BlockForum(forumName))
    }

    override suspend fun deleteInternal(item: String) {
        blockRepo.deleteForum(forumName = item)
    }

    override suspend fun deleteListInternal(items: List<String>) {
        blockRepo.deleteForums(items)
    }

    fun isForumInvalid(name: String): Boolean {
        if (name.isEmpty() || name.isBlank() || (name.length == 1 && name == "吧")) {
            return true
        }

        val forumName = name.trim().run { if (endsWith("吧")) substring(0, lastIndex) else this }
        return blackList.value?.contains(forumName) == true
    }
}

@HiltViewModel
class KeywordBlockListViewModel @Inject constructor(
    private val blockRepo: BlockRepository
): BaseBlockListViewModel<BlockKeyword>() {

    override val _blackList: Flow<List<BlockKeyword>?> = blockRepo.observeKeyword(whitelisted = false)

    override val _whiteList: Flow<List<BlockKeyword>?> = blockRepo.observeKeyword(whitelisted = true)

    override suspend fun upsertInternal(item: BlockKeyword) {
        blockRepo.addKeyword(keyword = item.keyword.trim(), isRegex = item.isRegex, whitelisted = item.whitelisted)
    }

    override suspend fun deleteInternal(item: BlockKeyword) {
        blockRepo.deleteKeyword(keyword = item)
    }

    override suspend fun deleteListInternal(items: List<BlockKeyword>) {
        blockRepo.deleteKeywords(items)
    }

    fun isInvalid(keyword: String, isRegex: Boolean): Boolean {
        return keyword.isEmpty() || keyword.isBlank() ||
                (isRegex && isRegexInvalid(keyword)) ||
                blackList.value?.fastAny { it.keyword == keyword } == true ||
                whiteList.value?.fastAny { it.keyword == keyword } == true
    }

    private fun isRegexInvalid(regex: String): Boolean {
        try {
            Pattern.compile(regex)
            return false
        } catch (_: PatternSyntaxException) {
            return true
        }
    }
}

@HiltViewModel
class UserBlockListViewModel @Inject constructor(
    private val blockRepo: BlockRepository
): BaseBlockListViewModel<BlockUser>() {

    override val _blackList: Flow<List<BlockUser>?> = blockRepo.observeUsers(whitelisted = false)

    override val _whiteList: Flow<List<BlockUser>?> = blockRepo.observeUsers(whitelisted = true)

    override suspend fun upsertInternal(item: BlockUser) {
    }

    override suspend fun deleteInternal(item: BlockUser) {
        blockRepo.deleteUser(uid = item.uid)
    }

    override suspend fun deleteListInternal(items: List<BlockUser>) {
        blockRepo.deleteUsers(items)
    }
}