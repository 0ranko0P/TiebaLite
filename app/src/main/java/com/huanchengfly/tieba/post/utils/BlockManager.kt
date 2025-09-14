package com.huanchengfly.tieba.post.utils

import android.database.sqlite.SQLiteException
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.core.util.Predicate
import com.huanchengfly.tieba.post.api.models.MessageListBean
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.abstractText
import com.huanchengfly.tieba.post.models.database.Block
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.set
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.litepal.LitePal
import org.litepal.extension.delete
import org.litepal.extension.findFirst

object BlockManager {
    private val _blockList = MutableStateFlow<ImmutableList<Block>>(persistentListOf())
    val blockList: StateFlow<ImmutableList<Block>>
        get() = _blockList

    fun init() {
        LitePal.findAllAsync(Block::class.java).listen { list ->
            if (list.isNotEmpty()) {
                _blockList.update { list.toPersistentList() }
            }
        }
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
                _blockList.update { list.toPersistentList() }
            }
    }

    fun removeBlock(id: Long) {
        LitePal.delete<Block>(id)
        val list = blockList.value.toMutableList()
        list.removeAll { it.id == id }
        _blockList.set { list.toPersistentList() }
    }

    suspend fun saveOrUpdateBlock(block: Block): Block = withContext(Dispatchers.IO) {
        if (block.id > 0L) {
            if (block.update(block.id) <= 0) throw SQLiteException("Update with invalid id: ${block.id}")
        } else {
            if (!block.saveOrUpdate("type = ? and uid = ? and keyword = ?", block.type.toString(), block.uid.toString(), block.keyword?: "NULL")) {
                throw SQLiteException("Error while save or update the Block data")
            }
        }
        // Maintain the block list manually...
        val newList = ArrayList(blockList.value)
        val index = blockList.value.indexOfFirst { it.type == block.type && it.uid == block.uid && it.keyword == block.keyword }
        if (index == -1) {
            newList.add(block)
        } else {
            newList[index] = block
        }
        _blockList.update { newList.toPersistentList() }
        return@withContext block
    }

    fun hasKeyword(keyword: String): Boolean {
        return blockList.value.fastFirstOrNull { it.type == Block.TYPE_KEYWORD && it.keyword == keyword } != null
    }

    suspend fun findUserById(userId: Long): Block? = withContext(Dispatchers.IO) {
        var rec = blockList.value.fastFirstOrNull { it.type == Block.TYPE_USER && it.uid == userId }
        if (rec == null) {
            rec = LitePal.where("type = ? and uid = ?", Block.TYPE_USER.toString(), userId.toString())
                .findFirst<Block>()
        }
        return@withContext rec
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
        blocks.fastForEach { block ->
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