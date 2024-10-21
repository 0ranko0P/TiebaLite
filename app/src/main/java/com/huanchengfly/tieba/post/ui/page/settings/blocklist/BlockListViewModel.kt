package com.huanchengfly.tieba.post.ui.page.settings.blocklist

import androidx.lifecycle.ViewModel
import com.huanchengfly.tieba.post.models.database.Block
import com.huanchengfly.tieba.post.utils.BlockManager
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BlockListViewModel: ViewModel() {

    val blackList: Flow<ImmutableList<Block>> = BlockManager.blockList.map { list ->
        list.filter { it.category == Block.CATEGORY_BLACK_LIST }.toPersistentList()
    }

    val whiteList: Flow<ImmutableList<Block>> = BlockManager.blockList.map { list ->
        list.filter { it.category == Block.CATEGORY_WHITE_LIST }.toPersistentList()
    }

    fun addKeyword(category: Int, keyword: String) {
        BlockManager.addBlockAsync(Block(category = category, type = Block.TYPE_KEYWORD, keyword = keyword))
    }

    fun remove(block: Block) {
        BlockManager.removeBlock(block.id)
    }
}