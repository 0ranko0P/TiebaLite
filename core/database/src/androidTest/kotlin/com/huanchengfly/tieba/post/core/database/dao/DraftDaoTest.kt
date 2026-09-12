package com.huanchengfly.tieba.post.core.database.dao

import com.huanchengfly.tieba.post.core.database.DatabaseTest
import com.huanchengfly.tieba.post.core.database.model.Draft
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal class DraftDaoTest : DatabaseTest() {

    private val testDraft = Draft(threadId = 1, postId = 2, subpostId = 3, "Test draft")

    @Test
    fun insertAndUpsert() = runTest {
        draftDao.insert(testDraft)

        val content = draftDao.getByIds(testDraft.threadId, testDraft.postId, testDraft.subpostId)
        assertEquals(1, content.size)
        assertEquals(testDraft.content, content[0])

        // upsert replaces existing
        val updated = testDraft.copy(content = "new")
        draftDao.upsert(updated)

        val newContent = draftDao.getByIds(testDraft.threadId, testDraft.postId, testDraft.subpostId)
        assertEquals(1, newContent.size)
        assertEquals("new", newContent[0])
    }

    @Test
    fun deleteByIds() = runTest {
        draftDao.insert(testDraft)
        val deleted = draftDao.deleteByIds(1, 2, 3)
        assertEquals(1, deleted)
        assertTrue(draftDao.getByIds(1, 2, 3).isEmpty())
    }

    @Test
    fun deleteAll() = runTest {
        draftDao.insert(Draft(1, 2, 0, "a"))
        draftDao.insert(Draft(4, 5, 6, "b"))
        draftDao.deleteAll()
        assertTrue(draftDao.getByIds(1, 2, 0).isEmpty())
        assertTrue(draftDao.getByIds(4, 5, 6).isEmpty())
    }
}
