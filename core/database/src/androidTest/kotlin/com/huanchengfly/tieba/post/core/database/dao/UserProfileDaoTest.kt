package com.huanchengfly.tieba.post.core.database.dao

import androidx.paging.PagingSource
import com.huanchengfly.tieba.post.core.database.DatabaseTest
import com.huanchengfly.tieba.post.core.database.model.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertFalse

internal class UserProfileDaoTest : DatabaseTest() {

    @Test
    fun upsert() = runTest {
        val profile = testUserEntity(1, 100)
        val profile2 = testUserEntity(Long.MAX_VALUE, 200)
        userProfileDao.upsert(profile)

        val all = userProfileDao.observeAll().first()
        assertEquals(1, all.size)
        assertEquals(profile, all[0])

        // Expect profile, profile2 exists
        userProfileDao.upsert(profile2)
        val all2 = userProfileDao.observeAll().first()
        assertThat(all2, `is`(listOf(profile, profile2)))
    }

    @Test
    fun upsertUpdate() = runTest {
        userProfileDao.upsert(testUserEntity(1, 100))
        val updated = testUserEntity(1, 100).copy(name = "newName")
        userProfileDao.upsert(updated)

        val profile = userProfileDao.observeById(1).first()
        assertEquals("newName", profile?.name)
    }

    @Test
    fun deleteAll() = runTest {
        userProfileDao.upsert(testUserEntity(1, 100))
        userProfileDao.deleteAll()
        assertTrue(userProfileDao.observeAll().first().isEmpty())
    }

    @Test
    fun deleteById() = runTest {
        userProfileDao.upsert(testUserEntity(1, 100))
        val deleted = userProfileDao.deleteById(1)
        assertEquals(1, deleted)
        assertNull(userProfileDao.observeById(1).first())
    }

    @Test
    fun deleteByIdList() = runTest {
        userProfileDao.upsert(testUserEntity(1, 100))
        userProfileDao.upsert(testUserEntity(2, 200))

        val deleted = userProfileDao.deleteByIdList(listOf(1, 2))
        assertEquals(2, deleted)
        assertTrue(userProfileDao.observeAll().first().isEmpty())
    }

    @Test
    fun updateFollowState() = runTest {
        userProfileDao.upsert(testUserEntity(1, 100))
        assertFalse(userProfileDao.observeById(1).first()!!.following)

        userProfileDao.updateFollowState(1, true, 99)
        val profile = userProfileDao.observeById(1).first()!!
        assertTrue(profile.following)
        assertEquals(99, profile.fans)
    }

    @Test
    fun updateLastVisit() = runTest {
        userProfileDao.upsert(testUserEntity(1, 100))

        userProfileDao.updateLastVisit(1, 999)
        val profile = userProfileDao.observeById(1).first()!!
        assertEquals(999, profile.lastVisit)
    }

    @Test
    fun updateLastUpdate() = runTest {
        userProfileDao.upsert(testUserEntity(1, 100))

        userProfileDao.updateLastUpdate(1, 888)
        val profile = userProfileDao.observeById(1).first()!!
        assertEquals(888, profile.lastUpdate)
    }

    @Test
    fun pagingSourceSorted_areOrderedByLastVisitDesc() = runTest {
        (0..<5L).forEach { i ->
            userProfileDao.upsert(testUserEntity(uid = i, lastVisit = i * 10))
        }

        val pagingSource = userProfileDao.pagingSourceSorted()
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(null, 10, false)
        ) as PagingSource.LoadResult.Page
        assertEquals(5, result.data.size)
        // Sorted by lastVisit desc
        assertEquals(4, result.data[0].uid)
        assertEquals(0, result.data[4].uid)
    }

    @Test
    fun getAllSorted_areOrderedByLastVisitDesc() = runTest {
        userProfileDao.upsert(testUserEntity(1, lastVisit = 100))
        userProfileDao.upsert(testUserEntity(2, lastVisit = 200))

        val allUserProfiles = userProfileDao.getAllSorted()
        assertEquals(2, allUserProfiles.size)
        assertEquals(200, allUserProfiles[0].lastVisit)
        assertEquals(100, allUserProfiles[1].lastVisit)
    }

    @Test
    fun getLastUpdate() = runTest {
        userProfileDao.upsert(testUserEntity(uid = 1, lastVisit = 100).copy(lastUpdate = 999))

        val last = userProfileDao.getLastUpdate(1)
        assertEquals(999L, last)
    }

    private fun testUserEntity(uid: Long, lastVisit: Long) = UserProfile(
        uid = uid,
        portrait = "",
        name = "Test User $uid",
        nickname = "User $uid",
        tiebaUid = "tu $uid",
        intro = "Intro $uid",
        privateForum = false,
        lastUpdate = System.currentTimeMillis(),
        lastVisit = lastVisit,
        blockDays = 0
    )
}
