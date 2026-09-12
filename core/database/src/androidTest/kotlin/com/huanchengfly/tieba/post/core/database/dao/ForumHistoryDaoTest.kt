package com.huanchengfly.tieba.post.core.database.dao

import androidx.paging.PagingSource
import com.huanchengfly.tieba.post.core.database.DatabaseTest
import com.huanchengfly.tieba.post.core.database.model.ForumHistory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal class ForumHistoryDaoTest : DatabaseTest() {

    @Test
    fun deleteById() = runTest {
        forumHistoryDao.upsert(testHistoryEntity(1, "a", 100))
        val deleted = forumHistoryDao.deleteById(1)
        assertEquals(1, deleted)
        assertTrue(forumHistoryDao.observeTop(10).first().isEmpty())
    }

    @Test
    fun deleteByIdList() = runTest {
        forumHistoryDao.upsert(testHistoryEntity(1, "a", 100))
        forumHistoryDao.upsert(testHistoryEntity(2, "b", 200))
        assertEquals(2, historyCount())

        val deleted = forumHistoryDao.deleteByIdList(listOf(1, 2))
        assertEquals(2, deleted)
        assertTrue(forumHistoryDao.observeTop(10).first().isEmpty())
    }

    @Test
    fun deleteAll() = runTest {
        forumHistoryDao.upsert(testHistoryEntity(1, "a", 100))
        assertEquals(1, historyCount())

        forumHistoryDao.deleteAll()
        assertTrue(forumHistoryDao.observeTop(10).first().isEmpty())
    }

    @Test
    fun observeTop_areOrderedByTimestampDesc() = runTest {
        val h1 = testHistoryEntity(1, "a", 100)
        val h2 = testHistoryEntity(2, "b", 200)
        val h3 = testHistoryEntity(3, "c", 300)
        forumHistoryDao.upsert(h1)
        forumHistoryDao.upsert(h2)
        forumHistoryDao.upsert(h3)
        assertEquals(3, historyCount())

        // Top 2 record sorted descending by timestamp
        val top = forumHistoryDao.observeTop(limit = 2).first()
        assertEquals(2, top.size)
        assertEquals(h3, top[0])
        assertEquals(h2, top[1])
    }

    @Test
    fun pagingSource_areOrderedByTimestampDesc() = runTest {
        (0..<20L).forEach { i ->
            forumHistoryDao.upsert(testHistoryEntity(i, "f$i", i))
        }

        val pagingSource = forumHistoryDao.pagingSource()
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 10,
                placeholdersEnabled = false
            )
        )
        assert(result is PagingSource.LoadResult.Page)
        val page = result as PagingSource.LoadResult.Page
        assertEquals(10, page.data.size)
        // Should be sorted by timestamp desc
        assertEquals(19, page.data.first().id)
    }

    // Create test history entity
    private fun testHistoryEntity(id: Long, name: String, ts: Long) = ForumHistory(id, name, avatar = "", timestamp = ts)

    // Test version of SELECT COUNT(1)
    private suspend fun historyCount(): Int = forumHistoryDao.observeTop(Int.MAX_VALUE).first().size
}
