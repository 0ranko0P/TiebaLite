package com.huanchengfly.tieba.post.repository.source.local

import com.huanchengfly.tieba.post.models.database.History
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.litepal.LitePal
import org.litepal.extension.deleteAll
import org.litepal.extension.find
import javax.inject.Inject

class HistoryDao @Inject constructor() {

    suspend fun get(type: Int, page: Int, limit: Int): List<History> = withContext(Dispatchers.IO) {
        LitePal.where("type = ?", "$type")
            .order("timestamp desc, count desc")
            .limit(limit)
            .let { if (page == 0) it else it.offset(page * 100) }
            .find()
    }

    suspend fun saveOrUpdateAsync(history: History): Boolean = withContext(Dispatchers.IO) {
        history.copy(count = history.count + 1/* unused statistics? */)
            .saveOrUpdate("data = ?", history.data)
    }

    suspend fun delete(history: History): Boolean = withContext(Dispatchers.IO) {
        val rows = if (history.isSaved) {
            history.delete()
        } else {
            LitePal.deleteAll<History>("data = ?", history.data)
        }
        return@withContext rows > 0
    }

    suspend fun deleteAll(): Boolean = withContext(Dispatchers.IO) {
        LitePal.deleteAll<History>() > 0
    }
}