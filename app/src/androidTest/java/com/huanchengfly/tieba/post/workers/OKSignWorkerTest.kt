package com.huanchengfly.tieba.post.workers

import android.content.Context
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.ListenableWorker.Result.Success
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.huanchengfly.tieba.post.core.data.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.core.database.TbLiteDatabase
import com.huanchengfly.tieba.post.core.network.util.NetworkMonitor
import com.huanchengfly.tieba.post.coroutines.runTest
import com.huanchengfly.tieba.post.repository.source.TestData
import com.huanchengfly.tieba.post.repository.user.OKSignRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

private class OKSignWorkerFactory(
    val okSignRepository: OKSignRepository,
    val networkMonitor: NetworkMonitor
) : WorkerFactory() {
    override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker {
        return OKSignWorker(appContext, workerParameters, okSignRepository, networkMonitor)
    }
}

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class OKSignWorkerTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @ApplicationContext @Inject lateinit var context: Context
    @Inject lateinit var tbLiteDatabase: TbLiteDatabase
    @Inject lateinit var settingsRepo: SettingsRepository
    @Inject lateinit var okSignRepository: OKSignRepository
    @Inject lateinit var networkMonitor: NetworkMonitor

    private lateinit var workerFactory: WorkerFactory

    @Before
    fun setUp() {
        hiltRule.inject()
        workerFactory = OKSignWorkerFactory(okSignRepository, networkMonitor)
        val config = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setExecutor(SynchronousExecutor())
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    @Test
    fun testOKSignWorker() {
        val worker = TestListenableWorkerBuilder<OKSignWorker>(context = context)
            .setWorkerFactory(workerFactory)
            .build()

        // Simulate user login
        TestData.insertAccount(database = tbLiteDatabase, settingsRepository = settingsRepo)

        runTest {
            val result = worker.doWork()
            assertTrue(result is Success)
        }
    }
}