package com.huanchengfly.tieba.post.core.database.dao

import com.huanchengfly.tieba.post.core.database.DatabaseTest
import com.huanchengfly.tieba.post.core.database.model.SearchPostHistory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Objects

internal class SearchPostDaoTest : DatabaseTest() {

    private val forumId = 100L // Default Forum ID

    @Test
    fun upsertAndObserve() = runTest {
        val history = testHistoryEntity(forumId, keyword = "keyword1")
        searchPostDao.upsert(history)

        val keywords = searchPostDao.observeAllKeywords(forumId).first()
        assertEquals(1, keywords.size)
        assertEquals(history.keyword, keywords[0])
    }

    @Test
    fun upsertUpdate() = runTest {
        searchPostDao.upsert(testHistoryEntity(forumId, "search 1", timestamp = 10))
        searchPostDao.upsert(testHistoryEntity(forumId, "search 2", timestamp = 20))

        // Expect search record sorted descending by timestamp
        val all = searchPostDao.observeAllKeywords(forumId).first()
        assertEquals(2, all.size)
        assertEquals("search 2", all[0])
        assertEquals("search 1", all[1])

        searchPostDao.upsert(testHistoryEntity(forumId, "search 1", timestamp = 100))
        val all2 = searchPostDao.observeAllKeywords(forumId).first()
        assertEquals("search 1", all2[0])
    }

    @Test
    fun deleteAllByForum() = runTest {
        searchPostDao.upsert(testHistoryEntity(forumId = forumId, "a", timestamp = 10))
        searchPostDao.upsert(testHistoryEntity(forumId = forumId, "b", timestamp = 20))
        searchPostDao.upsert(testHistoryEntity(forumId = 200, "c")) // Different forum

        val deleted = searchPostDao.deleteAll(forumId)
        assertEquals(2, deleted)
        assertTrue(searchPostDao.observeAllKeywords(forumId).first().isEmpty())
        // Expect other forum still exists
        val other = searchPostDao.observeAllKeywords(200).first()
        assertEquals(1, other.size)
    }

    @Test
    fun delete() = runTest {
        searchPostDao.upsert(SearchPostHistory(forumId, "key"))
        val deleted = searchPostDao.delete(forumId, "key")
        assertEquals(1, deleted)
        assertTrue(searchPostDao.observeAllKeywords(forumId).first().isEmpty())
    }

    @Test
    fun deleteById() = runTest {
        val history = SearchPostHistory(forumId, "key")
        searchPostDao.upsert(history)
        val deleted = searchPostDao.deleteById(history.id)
        assertEquals(1, deleted)
        assertTrue(searchPostDao.observeAllKeywords(forumId).first().isEmpty())
    }

    // Create test history entity
    private fun testHistoryEntity(
        forumId: Long,
        keyword: String,
        timestamp: Long = System.currentTimeMillis(),
    ) = SearchPostHistory(id = Objects.hash(forumId, keyword), forumId, keyword, timestamp)
}
