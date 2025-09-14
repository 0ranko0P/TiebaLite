package com.huanchengfly.tieba.post.ui.page.settings.blocklist

import androidx.compose.ui.util.fastFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.models.database.Block
import com.huanchengfly.tieba.post.utils.BlockManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class BlockListViewModel: ViewModel() {

    val blackList: StateFlow<List<Block>?> = BlockManager.blockList
        .map { list -> list.fastFilter { it.category == Block.CATEGORY_BLACK_LIST } }
        .stateIn(viewModelScope, started = SharingStarted.WhileSubscribed(5_000), null)

    val whiteList: StateFlow<List<Block>?> = BlockManager.blockList
        .map { list -> list.filter { it.category == Block.CATEGORY_WHITE_LIST } }
        .stateIn(viewModelScope, started = SharingStarted.WhileSubscribed(5_000), null)

    fun addKeyword(category: Int, keyword: String) {
        BlockManager.addBlockAsync(Block(category = category, type = Block.TYPE_KEYWORD, keyword = keyword))
    }

    fun remove(block: Block) {
        BlockManager.removeBlock(block.id)
    }
}