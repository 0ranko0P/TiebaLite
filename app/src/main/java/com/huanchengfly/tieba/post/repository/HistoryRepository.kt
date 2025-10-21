package com.huanchengfly.tieba.post.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room.withTransaction
import com.huanchengfly.tieba.post.App.Companion.AppBackgroundScope
import com.huanchengfly.tieba.post.arch.unsafeLazy
import com.huanchengfly.tieba.post.models.database.TbLiteDatabase
import com.huanchengfly.tieba.post.models.database.dao.ForumHistoryDao
import com.huanchengfly.tieba.post.models.database.dao.ThreadHistoryDao
import com.huanchengfly.tieba.post.models.database.ForumHistory
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.models.database.ThreadHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository that manages history data.
 * */
@Singleton
class HistoryRepository @Inject constructor(
    private val dataBase: TbLiteDatabase
) {
    private val scope = AppBackgroundScope

    private val threadHistoryDao: ThreadHistoryDao = dataBase.threadHistoryDao()

    private val forumHistoryDao: ForumHistoryDao = dataBase.forumHistoryDao()

    private val defaultConfig by unsafeLazy {
        PagingConfig(pageSize = 20, prefetchDistance = 4, maxSize = 80)
    }

    fun getForumHistoryTop10(): Flow<List<ForumHistory>> = forumHistoryDao.observeTop(limit = 10)

    fun getForumHistory(config: PagingConfig = defaultConfig): Flow<PagingData<ForumHistory>> {
        return Pager(
            config = config,
            pagingSourceFactory = { forumHistoryDao.pagingSource() }
        ).flow
    }

    fun getThreadHistory(config: PagingConfig = defaultConfig): Flow<PagingData<ThreadHistory>> {
        return Pager(
            config = config,
            pagingSourceFactory = { threadHistoryDao.pagingSourceSorted() }
        ).flow
    }

    fun saveHistory(history: History) {
        scope.launch {
            when (history) {
                is ThreadHistory -> threadHistoryDao.upsert(history)

                is ForumHistory -> forumHistoryDao.upsert(history)

                else -> throw RuntimeException()
            }
        }
    }

    fun deleteHistory(history: History) {
        scope.launch {
            when (history) {
                is ThreadHistory -> threadHistoryDao.deleteById(threadId = history.id)

                is ForumHistory -> forumHistoryDao.deleteById(forumId = history.id)

                else -> throw RuntimeException()
            }
        }
    }

    fun deleteAll() {
        scope.launch {
            dataBase.withTransaction {
                threadHistoryDao.deleteAll()
                forumHistoryDao.deleteAll()
            }
        }
    }
}
