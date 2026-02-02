package com.huanchengfly.tieba.post.workers

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.ListenableWorker.Result.Failure
import androidx.work.ListenableWorker.Result.Success
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.huanchengfly.tieba.post.api.models.MsgBean
import com.huanchengfly.tieba.post.coroutines.runTest
import com.huanchengfly.tieba.post.models.database.TbLiteDatabase
import com.huanchengfly.tieba.post.repository.HomeRepository
import com.huanchengfly.tieba.post.repository.source.TestData
import com.huanchengfly.tieba.post.repository.source.network.HomeNetworkDataSource
import com.huanchengfly.tieba.post.repository.source.network.HomeNetworkFakeDataSource
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
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
    @Inject lateinit var _networkDataSource: HomeNetworkDataSource
    val networkDataSource: HomeNetworkFakeDataSource
        get() = _networkDataSource as HomeNetworkFakeDataSource

    private lateinit var workerFactory: WorkerFactory

    @Before
    fun setUp() {
        hiltRule.inject()
        tbLiteDatabase.clearAllTables()
        workerFactory = NewMessageWorkerFactory(homeRepository)
        // Simulate user login
        TestData.insertAccount(database = tbLiteDatabase, settingsRepository = settingsRepo)
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
            settingsRepo.accountUid.set(-1) // Simulate user log out
            val result = worker.doWork()
            assertTrue(result is Failure)
        }
    }
}