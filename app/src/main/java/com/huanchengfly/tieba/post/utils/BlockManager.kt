package com.huanchengfly.tieba.post.utils

import androidx.annotation.VisibleForTesting
import androidx.core.util.Predicate
import com.huanchengfly.tieba.post.api.models.MessageListBean
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.abstractText
import com.huanchengfly.tieba.post.models.database.Block
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.litepal.LitePal
import org.litepal.extension.delete
import org.litepal.extension.findAll

object BlockManager {
    private val _blockList = MutableStateFlow<ImmutableList<Block>>(persistentListOf())
    val blockList: StateFlow<ImmutableList<Block>>
        get() = _blockList

    suspend fun init() = withContext(Dispatchers.IO) {
        val blocks = LitePal.findAll<Block>()
        _blockList.value = blocks.toPersistentList()
    }

    fun addBlockAsync(
        block: Block,
        callback: ((Boolean) -> Unit)? = null,
    ) {
        block.saveAsync()
            .listen {
                callback?.invoke(it)
                val list = blockList.value.toMutableList()
                list.add(block)
                _blockList.value = list.toPersistentList()
            }
    }

    fun removeBlock(id: Long) {
        LitePal.delete<Block>(id)
        val list = blockList.value.toMutableList()
        list.removeAll { it.id == id }
        _blockList.value = list.toPersistentList()
    }

    fun hasKeyword(keyword: String): Boolean {
        return blockList.value.find { it.type == Block.TYPE_KEYWORD && it.keyword == keyword } != null
    }

    fun hasUser(userId: Long): Boolean {
        return blockList.value.find { it.type == Block.TYPE_USER && it.uid == userId } != null
    }

    // Returns a predicate that can test multiple keywords at one time
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun keyword(vararg keywords: String): Predicate<Block> = Predicate { block ->
        block.type == Block.TYPE_KEYWORD && keywords.any { it.contains(block.keyword!!) }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun user(userId: Long): Predicate<Block> = Predicate<Block> { block ->
        block.type == Block.TYPE_USER && block.uid == userId
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun shouldBlock(blocks: List<Block>, predicate: Predicate<Block>): Boolean {
        var isBlocked = false
        for (block in blocks) {
            if (predicate.test(block)) {
                // Whitelist has highest priority
                if (block.category == Block.CATEGORY_WHITE_LIST) return false

                isBlocked = true
            }
        }
        return isBlocked
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun shouldBlock(blocks: List<Block>, userId: Long, vararg content: String): Boolean {
        val predicate = if (content.isEmpty()) {
            user(userId = userId)
        } else {
            keyword(*content).or(user(userId = userId))
        }
        return shouldBlock(blocks = blocks, predicate = predicate)
    }

    fun shouldBlock(userId: Long, vararg content: String): Boolean = shouldBlock(blockList.value, userId, *content)

    fun ThreadInfo.shouldBlock(): Boolean = shouldBlock(userId = authorId, title, abstractText)

    fun MessageListBean.MessageInfoBean.shouldBlock(): Boolean =
        shouldBlock(userId = replyer?.id?.toLongOrNull() ?: -1, content.orEmpty())
}