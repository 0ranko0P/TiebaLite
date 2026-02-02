package com.huanchengfly.tieba.post.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.huanchengfly.tieba.post.api.models.MsgBean
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.coroutines.runTest
import com.huanchengfly.tieba.post.models.database.LocalLikedForum
import com.huanchengfly.tieba.post.models.database.TbLiteDatabase
import com.huanchengfly.tieba.post.models.database.dao.TimestampDao
import com.huanchengfly.tieba.post.models.database.dao.TimestampDao.Companion.TYPE_FORUM_LAST_UPDATED
import com.huanchengfly.tieba.post.repository.HomeRepository.Companion.mapEntity
import com.huanchengfly.tieba.post.repository.source.TestData
import com.huanchengfly.tieba.post.repository.source.network.HomeNetworkDataSource
import com.huanchengfly.tieba.post.repository.source.network.HomeNetworkFakeDataSource
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.models.LikedForum
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HomeRepositoryTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject lateinit var tbLiteDatabase: TbLiteDatabase
    @Inject lateinit var timestampDao: TimestampDao
    @Inject lateinit var settingsRepo: SettingsRepository
    @Inject lateinit var homeRepository: HomeRepository

    @Inject lateinit var _networkDataSource: HomeNetworkDataSource
    val networkDataSource: HomeNetworkFakeDataSource
        get() = _networkDataSource as HomeNetworkFakeDataSource

    private val dummyAccount = TestData.DummyAccount

    @Before
    fun init() {
        hiltRule.inject()
        tbLiteDatabase.clearAllTables()
        // Simulate User login
        TestData.insertAccount(account = dummyAccount, database = tbLiteDatabase, settingsRepository = settingsRepo)
    }

    @Test
    fun testRefresh() = runTest {
        val forumFlow: Flow<List<LikedForum>> = homeRepository.getLikedForums()
        assertTrue(forumFlow.first().isEmpty())

        homeRepository.refresh(cached = false)
        assertTrue(forumFlow.first().isNotEmpty())
    }

    @Test(expected = TiebaNotLoggedInException::class)
    fun testRefreshLoggedOut() = runTest {
        TestData.purgeAccount(database = tbLiteDatabase, settingsRepository = settingsRepo)
        homeRepository.refresh(cached = false)
    }

    @Test
    fun testUpdateLikedForums() = runTest {
        // Prepare Dummy account, forums
        val uid = dummyAccount.uid
        val likedForums = networkDataSource.getLikedForums().mapEntity(uid)
        val lastUpdateFlow: Flow<Long?> = timestampDao.observe(uid = uid, type = TYPE_FORUM_LAST_UPDATED)
        val forumFlow: Flow<List<LikedForum>> = homeRepository.getLikedForums()

        // Ensure data is empty before testing
        assertTrue(lastUpdateFlow.first() == null)
        assertTrue(forumFlow.first().isEmpty())

        homeRepository.updateLikedForums(uid = uid, forums = likedForums)
        assertTrue(lastUpdateFlow.first() != null)
        assertEquals(likedForums.size, forumFlow.first().size)
    }

    @Test(expected = SQLiteConstraintException::class)
    fun testUpdateLikedForumsLoggedOut() = runTest {
        TestData.purgeAccount(database = tbLiteDatabase, settingsRepository = settingsRepo)
        // User logged out while repository is updating liked forums
        homeRepository.updateLikedForums(uid = dummyAccount.uid, forums = emptyList())
    }

    @Test
    fun testOnDislikeForum() = runTest {
        val forumFlow: Flow<List<LikedForum>> = homeRepository.getLikedForums()

        // Prepare dummy forum
        val uid = dummyAccount.uid
        val dummyForum = LocalLikedForum(id = 99, uid, avatar = "", name = "Test Forum", level = 10, -1)

        // Insert dummy forum
        homeRepository.updateLikedForums(uid, listOf(dummyForum))
        assertEquals(1, forumFlow.first().size)

        homeRepository.onDislikeForum(forumId = dummyForum.id)
        assertEquals(0, forumFlow.first().size)
    }

    @Test
    fun testFetchNewMessage() = runTest {
        val messageFlow = homeRepository.observeNewMessage()

        // Prepare dummy message
        val dummyMessage = MsgBean.MessageBean(replyMe = 99, atMe = 1)
        val messageCount = dummyMessage.replyMe + dummyMessage.atMe
        networkDataSource.nextMessage = dummyMessage
        homeRepository.fetchNewMessage()
        assertEquals("Expected message count to be $messageCount", messageCount, messageFlow.first())

        // Prepare dummy message
        networkDataSource.nextMessage = MsgBean.MessageBean(replyMe = 1)
        homeRepository.fetchNewMessage()
        // Expect message count is 1
        assertEquals("Expected message count to be 1",1, messageFlow.first())
    }

    @Test
    fun testClearNewMessage() = runTest {
        val messageFlow = homeRepository.observeNewMessage()
        // Prepare dummy message
        networkDataSource.nextMessage = MsgBean.MessageBean(replyMe = 99, atMe = 1)
        homeRepository.fetchNewMessage()

        homeRepository.clearNewMessage()
        assertEquals("Expected message count to be 0", 0, messageFlow.first())
    }
}