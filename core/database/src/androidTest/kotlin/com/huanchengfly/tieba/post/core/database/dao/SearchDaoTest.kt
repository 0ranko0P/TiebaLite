package com.huanchengfly.tieba.post.core.database.dao

import com.huanchengfly.tieba.post.core.database.DatabaseTest
import com.huanchengfly.tieba.post.core.database.model.SearchHistory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal class SearchDaoTest: DatabaseTest() {

    @Test
    fun upsert() = runTest {
        searchDao.upsert(SearchHistory(id = Int.MAX_VALUE, keyword = "114514", timestamp = 0))
        assertEquals("114514", searchDao.observeAllKeywords().first()[0])
    }

    @Test
    fun deleteAll() = runTest {
        searchDao.upsert(SearchHistory(0, keyword = "hey", timestamp = 0))
        searchDao.upsert(SearchHistory(1, keyword = "1", timestamp = 1))
        searchDao.upsert(SearchHistory(2, keyword = "2", timestamp = 2))
        searchDao.upsert(SearchHistory(3, keyword = "3", timestamp = 3))
        assertEquals(4, historyCount())

        searchDao.deleteAll()
        assertEquals(0, historyCount())
    }

    @Test
    fun deleteById() = runTest {
        searchDao.upsert(SearchHistory(0, keyword = ":)", timestamp = 0))
        searchDao.upsert(SearchHistory(1, keyword = "1", timestamp = 1))
        searchDao.upsert(SearchHistory(2, keyword = "2", timestamp = 2))
        assertEquals(3, historyCount())

        searchDao.deleteById(id = 1)
        assertEquals(2, historyCount())
    }

    @Test
    fun deleteByKeyword() = runTest {
        searchDao.upsert(SearchHistory(0, keyword = "hey", timestamp = 0))
        searchDao.upsert(SearchHistory(1, keyword = "1", timestamp = 1))
        searchDao.upsert(SearchHistory(2, keyword = "2", timestamp = 2))

        searchDao.delete(keyword = "1")
        assertEquals(2, historyCount())
        searchDao.observeAllKeywords().first().forEach { keyword ->
            assertTrue(keyword != "1")
        }
    }

    @Test
    fun observeAllKeywords_areOrderedByTimestampDesc() = runTest {
        searchDao.upsert(SearchHistory(0, keyword = "114514", timestamp = 10))
        searchDao.upsert(SearchHistory(1, keyword = "MySearch", timestamp = 200))

        // Expect sorted by timestamp desc
        val searchRecords = searchDao.observeAllKeywords().first()
        assertEquals(2, searchRecords.size)
        assertEquals("MySearch", searchRecords[0])
        assertEquals("114514", searchRecords[1])
    }

    // Test version of SELECT COUNT(1)
    private suspend fun historyCount(): Int = searchDao.observeAllKeywords().first().size
}