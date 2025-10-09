package com.huanchengfly.tieba.post.repository.source.local

import com.huanchengfly.tieba.post.models.database.SearchPostHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.litepal.LitePal
import org.litepal.extension.delete
import org.litepal.extension.deleteAll
import org.litepal.extension.find

object SearchPostHistoryDao {

    suspend fun listDesc(forumName: String, limit: Int = 50): List<SearchPostHistory> = withContext(Dispatchers.IO) {
        LitePal.where("forumName = ?", forumName)
            .order("timestamp DESC")
            .let {
                if (limit > 0) it.limit(limit) else it
            }
            .find<SearchPostHistory>()
    }

    suspend fun saveOrUpdate(history: SearchPostHistory): Boolean = withContext(Dispatchers.IO) {
        history.saveOrUpdate("content = ? and forumName = ?", history.content, history.forumName)
    }

    suspend fun delete(history: SearchPostHistory): Boolean = withContext(Dispatchers.IO) {
        val rows = if (history.isSaved) {
            LitePal.delete<SearchPostHistory>(history.id)
        } else {
            LitePal.deleteAll<SearchPostHistory>("content = ? and forumName = ?", history.content, history.forumName)
        }
        return@withContext rows > 0
    }

    suspend fun deleteForum(forumName: String): Int = withContext(Dispatchers.IO) {
        LitePal.deleteAll<SearchPostHistory>("forumName = ?", forumName)
    }
}