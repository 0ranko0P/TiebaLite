package com.huanchengfly.tieba.post.core.database.dao

import org.junit.Before
import androidx.paging.PagingSource
import com.huanchengfly.tieba.post.core.database.DatabaseTest
import com.huanchengfly.tieba.post.core.database.model.LocalLikedForum
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal class LikedForumDaoTest : DatabaseTest() {

    private val testAccount = testAccountEntity(uid = 114514)

    private val uid: Long
        get() = testAccount.uid

    @Before
    fun setupAccount() {
        runBlocking { accountDao.upsert(testAccount) }
    }

    @Test
    fun upsertAndObserveAll() = runTest {
        val forum = testLikedForumEntity(1, 10, 100)
        likedForumDao.upsert(forum)

        val all = likedForumDao.observeAllSorted(uid).first()
        assertEquals(1, all.size)
        assertEquals(forum, all[0])
    }

    @Test
    fun upsertAll() = runTest {
        val list = listOf(
            testLikedForumEntity(1, 10, 100),
            testLikedForumEntity(2, 5, 200)
        )
        likedForumDao.upsertAll(uid, list)

        val all = likedForumDao.observeAllSorted(uid).first()
        assertEquals(2, all.size)
        // Sorted by level desc
        assertEquals(10, all[0].level)
        assertEquals(5, all[1].level)
    }

    @Test
    fun updateSignIn() = runTest {
        val forum = testLikedForumEntity(Long.MAX_VALUE, 10, 100)
        likedForumDao.upsert(forum)

        likedForumDao.updateSignIn(uid, Long.MAX_VALUE, timestamp = 999)
        val updated = likedForumDao.observeAllSorted(uid).first()[0]
        assertEquals(999, updated.signInTimestamp)
    }

    @Test
    fun deleteById() = runTest {
        likedForumDao.upsert(testLikedForumEntity(Long.MAX_VALUE, 10, 100))

        val deleted = likedForumDao.deleteById(uid, forumId = Long.MAX_VALUE)
        assertEquals(1, deleted)
        assertTrue(likedForumDao.observeAllSorted(uid).first().isEmpty())
    }

    @Test
    fun deleteAllByUid() = runTest {
        likedForumDao.upsertAll(uid, listOf(testLikedForumEntity(1, 10, 100), testLikedForumEntity(2, 5, 200)))

        val deleted = likedForumDao.deleteAllByUid(uid)
        assertEquals(2, deleted)
        assertTrue(likedForumDao.observeAllSorted(uid).first().isEmpty())
    }

    @Test
    fun pinAndUnpinForum() = runTest {
        val testForum = testLikedForumEntity(1, 10, 100)
        likedForumDao.upsert(testForum)
        likedForumDao.pinForum(1)

        val pinned = likedForumDao.observePinnedForums().first()
        assertEquals(listOf(1L), pinned)

        // paging source for pinned should include this forum
        val pagingPinned = likedForumDao.pagingSourcePinned(uid)
        val result = pagingPinned.load(
            PagingSource.LoadParams.Refresh(null, 10, false)
        ) as PagingSource.LoadResult.Page
        assertEquals(1, result.data.size)
        assertEquals(testForum, result.data.first())

        likedForumDao.unpinForum(1)
        assertTrue(likedForumDao.observePinnedForums().first().isEmpty())
    }

    @Test
    fun pagingSource_excludesPinned() = runTest {
        likedForumDao.upsertAll(
            uid = uid,
            forums = listOf(testLikedForumEntity(1, 10, 100), testLikedForumEntity(2, 5, 200))
        )
        likedForumDao.pinForum(1)

        val paging = likedForumDao.pagingSource(uid)
        val result = paging.load(
            PagingSource.LoadParams.Refresh(null, 10, false)
        ) as PagingSource.LoadResult.Page
        // only unpinned forums
        assertEquals(1, result.data.size)
        assertEquals(2, result.data[0].id)
    }

    private fun testLikedForumEntity(id: Long, level: Int, sign: Long) =
        LocalLikedForum(id, uid, avatar = "", name = "name$id", level = level, signInTimestamp = sign)
}
