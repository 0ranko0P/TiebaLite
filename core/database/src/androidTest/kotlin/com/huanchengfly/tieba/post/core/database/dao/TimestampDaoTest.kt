package com.huanchengfly.tieba.post.core.database.dao

import com.huanchengfly.tieba.post.core.database.DatabaseTest
import com.huanchengfly.tieba.post.core.database.dao.TimestampDao.Companion.TYPE_FORUM_LAST_UPDATED
import com.huanchengfly.tieba.post.core.database.dao.TimestampDao.Companion.TYPE_NEW_MESSAGE_COUNT
import com.huanchengfly.tieba.post.core.database.dao.TimestampDao.Companion.TYPE_NEW_MESSAGE_RECEIVED
import com.huanchengfly.tieba.post.core.database.dao.TimestampDao.Companion.TYPE_NEW_MESSAGE_UPDATED
import com.huanchengfly.tieba.post.core.database.model.Timestamp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

internal class TimestampDaoTest : DatabaseTest() {

    private val testAccount = testAccountEntity(uid = 114514)

    private val uid: Long
        get() = testAccount.uid

    @Before
    fun setupAccount() {
        runBlocking { accountDao.upsert(testAccount) }
    }

    @Test
    fun upsert() = runTest {
        val ts = Timestamp(uid, TYPE_FORUM_LAST_UPDATED, 12345L)
        timestampDao.upsert(ts)

        val time = timestampDao.get(uid, TYPE_FORUM_LAST_UPDATED)
        assertEquals(12345L, time)
    }

    @Test
    fun delete() = runTest {
        val testType = Int.MAX_VALUE
        timestampDao.upsert(Timestamp(uid, type = testType, 123L))
        assertEquals(123L, timestampDao.get(uid, type = testType))

        val deleted = timestampDao.delete(uid, type = testType)
        assertEquals(1, deleted)
        assertNull(timestampDao.get(uid, type = testType))

        assertEquals(0, timestampDao.delete(uid, type = 0))
    }

    @Test
    fun update() = runTest {
        timestampDao.upsert(Timestamp(uid, TYPE_FORUM_LAST_UPDATED, 100L))
        assertEquals(100L, timestampDao.get(uid, TYPE_FORUM_LAST_UPDATED))

        timestampDao.update(uid, TYPE_FORUM_LAST_UPDATED, 200L)
        val time = timestampDao.get(uid, TYPE_FORUM_LAST_UPDATED)
        assertEquals(200L, time)
    }

    @Test
    fun observe() = runTest {
        timestampDao.upsert(Timestamp(uid, TYPE_FORUM_LAST_UPDATED, 999L))
        val flow = timestampDao.observe(uid, TYPE_FORUM_LAST_UPDATED).first()
        assertEquals(999L, flow)
    }

    @Test
    fun updateNewMessageCount() = runTest {
        timestampDao.updateNewMessageCount(uid, timestamp = 114514, newMsgCount = 0)

        val count = timestampDao.get(uid, TYPE_NEW_MESSAGE_COUNT)
        assertEquals(0L, count)

        val updated = timestampDao.get(uid, TYPE_NEW_MESSAGE_UPDATED)
        assertEquals(114514L, updated)

        val received = timestampDao.get(uid, TYPE_NEW_MESSAGE_RECEIVED)
        assertNull(received)

        timestampDao.updateNewMessageCount(uid, 2000, 5)
        val count2 = timestampDao.get(uid, TYPE_NEW_MESSAGE_COUNT)
        assertEquals(5L, count2)
        val received2 = timestampDao.get(uid, TYPE_NEW_MESSAGE_RECEIVED)
        assertEquals(2000L, received2)
    }
}
