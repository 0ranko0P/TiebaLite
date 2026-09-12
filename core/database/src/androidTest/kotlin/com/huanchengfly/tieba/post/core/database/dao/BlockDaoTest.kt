package com.huanchengfly.tieba.post.core.database.dao

import com.huanchengfly.tieba.post.core.database.DatabaseTest
import com.huanchengfly.tieba.post.core.database.model.BlockForum
import com.huanchengfly.tieba.post.core.database.model.BlockKeyword
import com.huanchengfly.tieba.post.core.database.model.BlockUser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

internal class BlockDaoTest : DatabaseTest() {

    @Test
    fun addKeyword() = runTest {
        blockDao.addKeyword("test", isRegex = false, whitelisted = false)
        blockDao.addKeyword("\"", isRegex = false, whitelisted = false)

        val blacklist = blockDao.observeTypedKeywords(false).first()
        assertEquals(2, blacklist.size)
        assertEquals("test", blacklist[0].keyword)
        assertEquals("\"", blacklist[1].keyword)
        assertFalse(blacklist[0].isRegex)
    }

    @Test
    fun insertKeywordsReplace() = runTest {
        val kw = BlockKeyword(id = 1, keyword = "test", isRegex = false, whitelisted = false)
        blockDao.insertKeywords(kw)

        val updated = kw.copy(keyword = "new")
        blockDao.insertKeywords(updated)

        val rules = blockDao.observeKeywordRules(false).first()
        assertEquals(1, rules.size)
        assertEquals("new", rules[0].keyword)
    }

    @Test
    fun deleteKeywordById() = runTest {
        blockDao.addKeyword("test", isRegex = false, whitelisted = false)
        val id = blockDao.observeKeywordRules(false).first().first().id

        val deleted = blockDao.deleteKeywordById(id)
        assertEquals(1, deleted)
        assertEquals(0, blockDao.observeKeywordRules(false).first().size)
    }

    @Test
    fun deleteKeywordByIdList() = runTest {
        blockDao.insertKeywords(
            BlockKeyword(1, keyword = "a", isRegex = false, whitelisted = false),
            BlockKeyword(2, keyword = "b", isRegex = false, whitelisted = true)
        )
        assertEquals(1, blockDao.observeKeywordRules(whitelisted = false).first().size)
        assertEquals(1, blockDao.observeKeywordRules(whitelisted = true).first().size)

        val deleted = blockDao.deleteKeywordByIdList(listOf(1L, 2L))
        assertEquals(2, deleted)
        assertTrue(blockDao.observeKeywordRules(whitelisted = false).first().isEmpty())
        assertTrue(blockDao.observeKeywordRules(whitelisted = true).first().isEmpty())
    }

    @Test
    fun getAllKeywords() = runTest {
        val testKeywords = listOf(
            KeywordCSV(keyword = "a", isRegex = false, whitelisted = false),
            KeywordCSV(keyword = "b", isRegex = true, whitelisted = true)
        )
        testKeywords.forEachIndexed { i, (keyword, isRegex, whitelisted) ->
            blockDao.insertKeywords(BlockKeyword(id = i.toLong(), keyword, isRegex, whitelisted))
        }

        val all = blockDao.getAllKeywords()
        assertThat(all, `is`(testKeywords))
    }

    @Test
    fun upsertUser() = runTest {
        val user = BlockUser(uid = 1, name = "test", whitelisted = true)
        blockDao.upsertUser(user)

        val allUsers = blockDao.getAllUsers()
        assertEquals(1, allUsers.size)
        assertEquals(user, blockDao.getUser(uid = 1))
    }

    @Test
    fun insertUsersReplace() = runTest {
        val user = BlockUser(uid = Long.MAX_VALUE, name = "test", whitelisted = false)
        blockDao.insertUsers(user)

        val updated = user.copy(whitelisted = true)
        blockDao.insertUsers(updated)

        val users = blockDao.observeUsers(true).first()
        assertEquals(1, users.size)
        assertEquals(updated, users[0])
    }

    @Test
    fun deleteUserById() = runTest {
        blockDao.upsertUser(BlockUser(1, "test", false))
        val deleted = blockDao.deleteUserById(1)
        assertEquals(1, deleted)
        assertNull(blockDao.getUser(1))
    }

    @Test
    fun deleteUserByIdList() = runTest {
        blockDao.insertUsers(
            BlockUser(1, "a", false),
            BlockUser(2, "b", false)
        )
        val deleted = blockDao.deleteUserByIdList(listOf(1L, 2L))
        assertEquals(2, deleted)
        assertTrue(blockDao.observeUsers(whitelisted = false).first().isEmpty())
        assertTrue(blockDao.observeUsers(whitelisted = true).first().isEmpty())
    }

    @Test
    fun observeUsers() = runTest {
        blockDao.upsertUser(BlockUser(1, "a", false))
        blockDao.upsertUser(BlockUser(Long.MAX_VALUE, "b", true))

        val blacklist = blockDao.observeUsers(false).first()
        assertEquals(1, blacklist.size)
        assertEquals(1, blacklist[0].uid)

        val whitelist = blockDao.observeUsers(true).first()
        assertEquals(1, whitelist.size)
        assertEquals(Long.MAX_VALUE, whitelist[0].uid)
    }

    @Test
    fun getAllUsers() = runTest {
        val testUsers = listOf(
            UserCSV(1, "a", false),
            UserCSV(Long.MAX_VALUE, "b", true)
        )
        blockDao.insertUsers(*testUsers.toTypedArray())

        assertThat(blockDao.getAllUsers(), `is`(testUsers))
    }

    @Test
    fun upsertForum() = runTest {
        val forum = BlockForum("test")
        blockDao.upsertForum(forum)

        val forums = blockDao.getForums()
        assertEquals(1, forums.size)
        assertEquals("test", forums[0])
    }

    @Test
    fun insertForumsReplace() = runTest {
        val testForums = listOf(BlockForum("a"), BlockForum("b"))
        blockDao.insertForums(*testForums.toTypedArray())

        val forums = blockDao.getForums()
        assertEquals(2, forums.size)
        assertTrue(forums.contains("a"))
        assertTrue(forums.contains("b"))
    }

    @Test
    fun deleteForum() = runTest {
        blockDao.upsertForum(BlockForum("test"))
        val deleted = blockDao.deleteForum("test")
        assertEquals(1, deleted)
        assertTrue(blockDao.getForums().isEmpty())
    }

    @Test
    fun deleteForums() = runTest {
        blockDao.insertForums(BlockForum("a"), BlockForum("b"))
        val deleted = blockDao.deleteForums(listOf("a", "b"))
        assertEquals(2, deleted)
        assertTrue(blockDao.getForums().isEmpty())
    }

    @Test
    fun getForum() = runTest {
        blockDao.upsertForum(BlockForum("test"))
        val name = blockDao.getForum("test")
        assertEquals("test", name)
    }

    @Test
    fun observeForums() = runTest {
        blockDao.upsertForum(BlockForum("test"))
        val flow = blockDao.observeForums().first()
        assertEquals(listOf("test"), flow)
    }
}
