package com.huanchengfly.tieba.post.repository.source.local

import com.huanchengfly.tieba.post.models.database.SearchHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.litepal.LitePal
import org.litepal.extension.delete
import org.litepal.extension.deleteAll
import org.litepal.extension.find

object SearchHistoryDao {

    suspend fun listDesc(limit: Int = 50): List<SearchHistory> = withContext(Dispatchers.IO) {
        LitePal.order("timestamp DESC")
            .let {
                if (limit > 0) it.limit(limit) else it
            }
            .find<SearchHistory>()
    }

    suspend fun saveOrUpdate(history: SearchHistory): Boolean = withContext(Dispatchers.IO) {
        history.saveOrUpdate("content = ?", history.content)
    }

    suspend fun delete(history: SearchHistory): Boolean = withContext(Dispatchers.IO) {
        val rows = if (history.isSaved) {
            LitePal.delete<SearchHistory>(history.id)
        } else {
            LitePal.deleteAll<SearchHistory>("content = ?", history.content)
        }
        return@withContext rows > 0
    }

    suspend fun deleteAll(): Int = withContext(Dispatchers.IO) {
        LitePal.deleteAll<SearchHistory>()
    }
}