package com.huanchengfly.tieba.post.repository.user

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.testing.WorkManagerTestInitHelper
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaException
import com.huanchengfly.tieba.post.coroutines.runTest
import com.huanchengfly.tieba.post.models.database.TbLiteDatabase
import com.huanchengfly.tieba.post.models.database.dao.TimestampDao
import com.huanchengfly.tieba.post.models.database.dao.TimestampDao.Companion.TYPE_FORUM_LAST_UPDATED
import com.huanchengfly.tieba.post.repository.HomeRepository
import com.huanchengfly.tieba.post.repository.source.TestData
import com.huanchengfly.tieba.post.repository.source.network.OKSignFakeDataSource
import com.huanchengfly.tieba.post.repository.source.network.OKSignNetworkDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import javax.inject.Inject
import javax.net.ssl.SSLHandshakeException

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class OKSignRepositoryImpTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @ApplicationContext @Inject lateinit var context: Context
    @Inject lateinit var tbLiteDatabase: TbLiteDatabase
    @Inject lateinit var timestampDao: TimestampDao
    @Inject lateinit var settingsRepo: SettingsRepository
    @Inject lateinit var homeRepository: HomeRepository
    @Inject lateinit var okSignRepository: OKSignRepository

    @Inject lateinit var _networkDataSource: OKSignNetworkDataSource
    val networkDataSource: OKSignFakeDataSource
        get() = _networkDataSource as OKSignFakeDataSource

    @Before
    fun setUp() {
        hiltRule.inject()
        tbLiteDatabase.clearAllTables()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)

        // Prepare test account and enable autoSign
        settingsRepo.signConfig.save { it.copy(autoSign = true) }
        TestData.insertAccount(database = tbLiteDatabase, settingsRepository = settingsRepo)
    }

    @Test
    fun testSign() = runTest {
        val forums = TestData.DummyGetForumListBean.forumInfo
        val progressListener = TestProgressListener()
        val homeForumFlow = homeRepository.getLikedForums()
        val lastUpdate = timestampDao.observe(uid = TestData.DummyAccount.uid, type = TYPE_FORUM_LAST_UPDATED)

        // Ensure data is empty before testing
        assertEquals(0, homeForumFlow.first().size)
        assertNull(lastUpdate.first())

        okSignRepository.sign(listener = progressListener)

        // Test is HomeRepository observing the signing data changes
        assertEquals(forums.size, homeForumFlow.first().size)
        assertNotNull(lastUpdate.first())

        // Test progress
        assertEquals(forums.size, progressListener.succeed)
        assertEquals(0, progressListener.failed.size)
        assertEquals(TestData.DummyAccount.name, progressListener.userName)
        assertNull(progressListener.mSignError)
    }

    @Test
    fun testMSignFail() = runTest {
        val forums = TestData.DummyGetForumListBean.forumInfo
        val progressListener = TestProgressListener()

        // Throws IOException on next call on MSign
        val error = IOException("一键签到失败!")
        networkDataSource.setNextOfficialSignThrow(error)

        // Expect caught IOException in MSign
        okSignRepository.sign(listener = progressListener)

        // Expect MSign exception exists and equals
        assertEquals(error.message, progressListener.mSignError?.message)
        // Expect all forums are signed
        assertEquals(forums.size, progressListener.succeed)
    }

    @Test
    fun testSignFail() {
        val progressListener = TestProgressListener()

        // Throw exceptions on next call on MSign and Sign
        networkDataSource.setNextOfficialSignThrow(IOException("一键签到失败!"))
        networkDataSource.setNextSignThrow(TiebaException("签到失败!"))

        // Expect caught IOException in MSign
        // Expect caught TiebaException in Sign
        runTest { okSignRepository.sign(listener = progressListener) }
        assertEquals(1, progressListener.failed.size)

        // Throw exceptions on next call on MSign and Sign
        networkDataSource.setNextOfficialSignThrow(IOException("一键签到失败!"))
        networkDataSource.setNextSignThrow(SSLHandshakeException("Handshake failed"))

        // Expect caught IOException in MSign
        // Expect throws SSLHandshakeException in Sign
        assertThrows(SSLHandshakeException::class.java) {
            runTest { okSignRepository.sign(listener = progressListener) }
        }
    }
}

private class TestProgressListener(): OKSignRepository.ProgressListener {
    var total: Int = -1
        private set

    val failed = hashSetOf<String>()

    var succeed: Int = 0
        private set

    var userName: String? = null
        private set

    var mSignError: Throwable? = null
        private set

    override fun onInit(total: Int, userName: String) {
        this.total = total
        this.userName = userName
    }

    override fun onSigned(progress: Int, forum: String, signBonusPoint: Int?) {}

    override fun onFailed(progress: Int, forum: String, error: String) {
        failed.add(forum)
    }

    override fun onMSignFailed(e: Throwable) {
        mSignError = e
    }

    override fun onFinish(succeed: Int) {
        this.succeed = succeed
    }
}