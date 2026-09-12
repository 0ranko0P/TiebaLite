package com.huanchengfly.tieba.post.core.database.dao

import com.huanchengfly.tieba.post.core.database.DatabaseTest
import com.huanchengfly.tieba.post.core.database.model.Account
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertEquals
import org.junit.Test

internal class AccountDaoTest : DatabaseTest() {

    @Test
    fun upsert() = runTest {
        val account = testAccountEntity(1)
        accountDao.upsert(account)

        val all = accountDao.observeAll().first()
        assertEquals(1, all.size)
        assertEquals(account, all[0])
    }

    @Test
    fun upsertUpdateExisting() = runTest {
        val account = testAccountEntity(uid = 1)
        accountDao.upsert(account)

        val updated = account.copy(name = "newName")
        accountDao.upsert(updated)
        assertEquals("newName", accountDao.getById(1)!!.name)
    }

    @Test
    fun deleteAll() = runTest {
        accountDao.upsert(testAccountEntity(uid = 1))
        accountDao.upsert(testAccountEntity(uid = 2))
        assertEquals(2, accountCount())

        accountDao.deleteAll()
        assertEquals(0, accountCount())
    }

    @Test
    fun deleteById() = runTest {
        accountDao.upsert(testAccountEntity(uid = 1))
        accountDao.upsert(testAccountEntity(uid = 2))
        assertEquals(2, accountCount())

        val deleted = accountDao.deleteById(uid = 1)
        assertEquals(1, deleted)
        assertEquals(1, accountCount())
    }

    @Test
    fun observeById() = runTest {
        val account = testAccountEntity(1)
        accountDao.upsert(account)

        val observed = accountDao.observeById(1).first()
        assertEquals(account, observed)
    }

    @Test
    fun getAll() = runTest {
        val list = listOf(testAccountEntity(1), testAccountEntity(2))
        list.forEach { accountDao.upsert(it) }

        val all = accountDao.getAll()
        assertThat(all, `is`(list))
    }

    // Test version of SELECT COUNT(1)
    private suspend fun accountCount() = accountDao.observeAll().first().size
}

// Create test Account entity
internal fun testAccountEntity(uid: Long) = Account(
    uid = uid,
    name = "TestUser$uid",
    nickname = "nick$uid",
    bduss = "bduss$uid",
    tbs = "tbs$uid",
    portrait = "portrait$uid",
    sToken = "token$uid",
    cookie = "cookie$uid",
)
