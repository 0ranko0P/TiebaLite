package com.huanchengfly.tieba.post.workers

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.ListenableWorker.Result.Failure
import androidx.work.ListenableWorker.Result.Success
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.huanchengfly.tieba.post.core.data.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.core.database.TbLiteDatabase
import com.huanchengfly.tieba.post.core.network.model.MsgBean
import com.huanchengfly.tieba.post.coroutines.runTest
import com.huanchengfly.tieba.post.repository.HomeRepository
import com.huanchengfly.tieba.post.repository.source.TestData
import com.huanchengfly.tieba.post.repository.source.network.HomeNetworkFakeDataSource
import com.huanchengfly.tieba.post.ui.page.main.notifications.list.NotificationsType
import com.huanchengfly.tieba.post.utils.NotificationUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import javax.inject.Inject

private class NewMessageWorkerFactory(val homeRepository: HomeRepository) : WorkerFactory() {
    override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker? {
        return NewMessageWorker(appContext, workerParameters, homeRepository)
    }
}

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NewMessageWorkerTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @ApplicationContext @Inject lateinit var context: Context
    @Inject lateinit var tbLiteDatabase: TbLiteDatabase
    @Inject lateinit var settingsRepo: SettingsRepository
    @Inject lateinit var homeRepository: HomeRepository
    private val networkDataSource = HomeNetworkFakeDataSource

    private lateinit var workerFactory: WorkerFactory
    private val notificationIds = setOf(20, 21, 15)
    private val testChannelId = "new_message_worker_test"
    private var ownsNotifications = false
    private val notificationManager: NotificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @Before
    fun setUp() {
        hiltRule.inject()
        assumeTrue(notificationManager.activeNotifications.none { it.tag == null && it.id in notificationIds })
        ownsNotifications = true
        tbLiteDatabase.clearAllTables()
        workerFactory = NewMessageWorkerFactory(homeRepository)
        // Simulate user login
        TestData.insertAccount(database = tbLiteDatabase, settingsRepository = settingsRepo)
    }

    @After
    fun tearDownNotifications() {
        if (!ownsNotifications) return
        notificationIds.forEach(notificationManager::cancel)
        try {
            assertActiveNotificationIds(emptySet())
        } finally {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager.deleteNotificationChannel(testChannelId)
            }
        }
    }

    @Test
    fun testNewMessageWorker() {
        val worker = TestListenableWorkerBuilder<NewMessageWorker>(context = context)
            .setWorkerFactory(workerFactory)
            .build()

        // Prepare dummy message
        val newMessage = MsgBean.MessageBean(replyMe = 99, atMe = 12)
        val newMessageCount = newMessage.replyMe + newMessage.atMe
        networkDataSource.nextMessage = newMessage

        runTest {
            val result = worker.doWork()
            val resultCount = result.outputData.getInt(NewMessageWorker.KEY_NEW_MESSAGE_COUNT, -1)
            assertTrue(result is Success)
            assertEquals(newMessageCount, resultCount)
        }
    }

    @Test
    fun testNewMessageWorkerLogOut() {
        val worker = TestListenableWorkerBuilder<NewMessageWorker>(context = context)
            .setWorkerFactory(workerFactory)
            .build()

        runTest {
            // Simulate user log out
            TestData.purgeAccount(tbLiteDatabase, settingsRepo)
            val result = worker.doWork()
            assertTrue(result is Failure)
        }
    }

    @Test
    fun clearNotificationOnlyCancelsRequestedType() {
        postTestNotifications()

        NewMessageWorker.clearNotification(NotificationsType.ReplyMe)
        assertActiveNotificationIds(setOf(21, 15))

        NewMessageWorker.clearNotification(NotificationsType.AtMe)
        assertActiveNotificationIds(setOf(15))
    }

    @Test
    fun zeroReplyCountClearsStaleReplyButKeepsAtAndSignNotifications() {
        postTestNotifications()
        networkDataSource.nextMessage = MsgBean.MessageBean(replyMe = 0, atMe = 1)
        val worker = TestListenableWorkerBuilder<NewMessageWorker>(context = context)
            .setWorkerFactory(workerFactory)
            .build()

        runTest {
            assertTrue(worker.doWork() is Success)
        }
        assertActiveNotificationIds(setOf(21, 15))
    }

    @Test
    fun zeroAtCountClearsStaleAtButKeepsReplyAndSignNotifications() {
        postTestNotifications()
        networkDataSource.nextMessage = MsgBean.MessageBean(replyMe = 1, atMe = 0)
        val worker = TestListenableWorkerBuilder<NewMessageWorker>(context = context)
            .setWorkerFactory(workerFactory)
            .build()

        runTest {
            assertTrue(worker.doWork() is Success)
        }
        assertActiveNotificationIds(setOf(20, 15))
    }

    @Test
    fun failedFetchKeepsExistingNotifications() {
        postTestNotifications()
        val failingRepository = object : HomeRepository by homeRepository {
            override suspend fun fetchNewMessage(): MsgBean.MessageBean {
                throw IOException("Message fetch failed")
            }
        }
        val worker = TestListenableWorkerBuilder<NewMessageWorker>(context = context)
            .setWorkerFactory(NewMessageWorkerFactory(failingRepository))
            .build()

        runTest {
            assertTrue(worker.doWork() is Failure)
        }
        assertActiveNotificationIds(notificationIds)
    }

    @SuppressLint("MissingPermission")
    private fun postTestNotifications() {
        assumeTrue(NotificationUtils.checkPermission(context))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(testChannelId, "Message notification regression", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val notification = NotificationCompat.Builder(context, testChannelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Message notification regression")
            .build()
        notificationIds.forEach { notificationManager.notify(it, notification) }
        assertActiveNotificationIds(notificationIds)
    }

    private fun assertActiveNotificationIds(expected: Set<Int>) {
        val deadline = SystemClock.elapsedRealtime() + 5_000
        var actual: Set<Int>
        do {
            actual = notificationManager.activeNotifications
                .filter { it.tag == null && it.id in notificationIds }
                .map { it.id }
                .toSet()
            if (actual == expected) return
            SystemClock.sleep(20)
        } while (SystemClock.elapsedRealtime() < deadline)
        assertEquals(expected, actual)
    }
}