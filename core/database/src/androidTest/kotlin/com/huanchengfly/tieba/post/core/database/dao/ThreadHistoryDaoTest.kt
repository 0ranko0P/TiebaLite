package com.huanchengfly.tieba.post.core.database.dao

import androidx.paging.PagingSource
import com.huanchengfly.tieba.post.core.database.DatabaseTest
import com.huanchengfly.tieba.post.core.database.model.ThreadHistory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal class ThreadHistoryDaoTest : DatabaseTest() {

    @Test
    fun deleteAll() = runTest {
        threadHistoryDao.upsert(testHistoryEntity(1, 100))
        threadHistoryDao.deleteAll()
        assertTrue(threadHistoryDao.getAllSorted().isEmpty())
    }

    @Test
    fun deleteById() = runTest {
        threadHistoryDao.upsert(testHistoryEntity(1, 100))
        threadHistoryDao.upsert(testHistoryEntity(2, 200))

        val deleted = threadHistoryDao.deleteById(threadId = 1)
        assertEquals(1, deleted)
        assertEquals(1, threadHistoryDao.getAllSorted().size)
    }

    @Test
    fun deleteByIdList() = runTest {
        threadHistoryDao.upsert(testHistoryEntity(1, 100))
        threadHistoryDao.upsert(testHistoryEntity(2, 200))
        val deleted = threadHistoryDao.deleteByIdList(listOf(1, 2))
        assertEquals(2, deleted)
        assertTrue(threadHistoryDao.getAllSorted().isEmpty())
    }

    @Test
    fun pagingSourceSorted_areOrderedByTimestampDesc() = runTest {
        val h1 = testHistoryEntity(1, 100)
        val h2 = testHistoryEntity(2, 200)
        threadHistoryDao.upsert(h1)
        threadHistoryDao.upsert(h2)

        val pagingSource = threadHistoryDao.pagingSourceSorted()
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(null, 10, false)
        ) as PagingSource.LoadResult.Page
        assertEquals(2, result.data.size)
        // Expect sorted by timestamp desc
        assertEquals(h2, result.data[0])
        assertEquals(h1, result.data[1])
    }

    @Test
    fun getAllSorted_areOrderedByTimestampDesc() = runTest {
        threadHistoryDao.upsert(testHistoryEntity(1, 100))
        threadHistoryDao.upsert(testHistoryEntity(2, 200))

        val list = threadHistoryDao.getAllSorted()
        assertEquals(2, list.size)
        // Expect sorted by timestamp desc
        assertEquals(2, list[0].id)
        assertEquals(1, list[1].id)
    }

    private fun testHistoryEntity(id: Long, timestamp: Long) = ThreadHistory(
        id = id,
        avatar = "av$id",
        name = "user$id",
        forum = "forum$id",
        title = "title$id",
        isSeeLz = false,
        pid = id,
        timestamp = timestamp
    )
}
