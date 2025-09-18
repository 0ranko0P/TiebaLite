package com.huanchengfly.tieba.post.repository

import androidx.annotation.IntDef
import com.huanchengfly.tieba.post.App.Companion.AppBackgroundScope
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.repository.source.local.HistoryDao
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import javax.inject.Inject
import javax.inject.Singleton

@IntDef(HistoryType.FORUM, HistoryType.THREAD)
@Retention(AnnotationRetention.SOURCE)
annotation class HistoryType {
    companion object  {
        const val FORUM = 1
        const val THREAD = 2
    }
}

@Singleton
class HistoryRepository @Inject constructor(private val localDataSource: HistoryDao) {

    companion object {
        const val PAGE_SIZE = 100
    }

    private val refresh by lazy { Channel<Unit>(capacity = Channel.CONFLATED) }

    // TODO: Migrate to Room DataBase for native Flow support
    fun getHistoryFlow(@HistoryType type: Int, page: Int = 0): Flow<List<History>> {
        return flow {
            val iterator = refresh.iterator()
            do {
                emit(localDataSource.get(type, page, limit = PAGE_SIZE))
                yield()
                if (iterator.hasNext()) iterator.next() else break
            } while (true)
        }
    }

    suspend fun getHistory(@HistoryType type: Int, page: Int = 0): Result<List<History>> = runCatching {
        localDataSource.get(type, page, limit = PAGE_SIZE)
    }

    fun save(history: History): Deferred<Boolean> = AppBackgroundScope.async {
        runCatching {
            localDataSource.saveOrUpdateAsync(history).also { notifyDataChanged(it) }
        }
        .onFailure { it.printStackTrace() }
        .getOrNull() == true
    }

    suspend fun delete(history: History): Boolean {
        return runCatching {
            localDataSource.delete(history).also { notifyDataChanged(it) }
        }
        .onFailure { it.printStackTrace() }
        .getOrNull() == true
    }

    suspend fun deleteAll(): Boolean {
        return runCatching {
            localDataSource.deleteAll().also { notifyDataChanged(it) }
        }
        .onFailure { it.printStackTrace() }
        .getOrNull() == true
    }

    private fun notifyDataChanged(succeed: Boolean) {
        if (succeed) refresh.trySend(Unit)
    }
}
