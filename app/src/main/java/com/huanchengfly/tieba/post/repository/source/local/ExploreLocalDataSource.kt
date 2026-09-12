package com.huanchengfly.tieba.post.repository.source.local

import android.content.Context
import android.util.Log
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.core.common.ktx.booleanToInt
import com.huanchengfly.tieba.post.core.common.ktx.deleteQuietly
import com.huanchengfly.tieba.post.core.network.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.core.network.model.protos.Agree
import com.huanchengfly.tieba.post.core.network.model.protos.ThreadInfo
import com.huanchengfly.tieba.post.core.network.model.protos.hotThreadList.HotThreadListResponseData
import com.huanchengfly.tieba.post.core.network.model.protos.personalized.PersonalizedResponseData
import com.huanchengfly.tieba.post.core.network.model.protos.userLike.UserLikeResponseData
import com.huanchengfly.tieba.post.ui.models.Like
import com.huanchengfly.tieba.post.utils.FileUtil
import com.huanchengfly.tieba.post.utils.FileUtil.isCacheExpired
import com.huanchengfly.tieba.post.utils.ProtobufCacheUtil.decodeCache
import com.huanchengfly.tieba.post.utils.ProtobufCacheUtil.encodeCache
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

interface ExploreLocalDataSource {

    suspend fun loadHotThread(tabCode: String): HotThreadListResponseData?

    suspend fun loadPersonalized(page: Int): PersonalizedResponseData?

    suspend fun loadUserLikeDataFirstPage(uid: Long): Pair<Long, UserLikeResponseData?>

    suspend fun saveHotThread(tabCode: String, data: HotThreadListResponseData): Boolean

    suspend fun savePersonalized(data: PersonalizedResponseData, page: Int): Boolean

    suspend fun saveUserLikeFirstPage(uid: Long, data: UserLikeResponseData): Boolean

    suspend fun purgeHotThread()

    suspend fun purgePersonalized()

    suspend fun updateHotThreadLike(tabCode: String, threadId: Long, like: Like)

    suspend fun updatePersonalizedLike(threadId: Long, like: Like)

    suspend fun updateUserLike(uid: Long, threadId: Long, like: Like)

    suspend fun dislikePersonalized(threadId: Long)
}

class ExploreLocalFileDataSource(@ApplicationContext context: Context): ExploreLocalDataSource {

    companion object {
        private const val TAG = "ExploreLocalDataSource"

        private const val CACHE_DIR_NAME = "Explore"

        private const val CACHE_PERSONALIZED_PREFIX = "p_"
        private const val CACHE_HOT_PREFIX = "hot_"

        private const val HOT_THREAD_EXPIRE_MILL = 0x36EE80 // 1 hour
        private const val PERSONALIZED_EXPIRE_MILL = 0x5265C00 // 1 day

        private fun ThreadInfo.setLikeStatus(like: Like): ThreadInfo {
            return copy(agree = Agree(agreeNum = like.count, hasAgree = like.liked.booleanToInt()))
        }
    }

    private val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)

    private val mutex = Mutex()

    override suspend fun loadHotThread(tabCode: String): HotThreadListResponseData? = withContext(Dispatchers.IO) {
        val cacheFile = hotCacheFile(tabCode)
        mutex.withLock {
            runCatching {
                HotThreadListResponseData.ADAPTER.decodeCache(cacheFile, HOT_THREAD_EXPIRE_MILL.toLong())
            }
            .getOrNull()
        }
    }

    override suspend fun saveHotThread(tabCode: String, data: HotThreadListResponseData) = withContext(Dispatchers.IO) {
        val cacheFile = hotCacheFile(tabCode)
        mutex.withLock {
            runCatching { HotThreadListResponseData.ADAPTER.encodeCache(cacheFile, data) }.isSuccess
        }
    }

    override suspend fun updateHotThreadLike(tabCode: String, threadId: Long, like: Like) = withContext(Dispatchers.IO) {
        val cacheFile = hotCacheFile(tabCode)
        mutex.withLock {
            try {
                val lastModified: Long = cacheFile.lastModified()
                val data = HotThreadListResponseData.ADAPTER.decodeCache(cacheFile) ?: return@withLock
                val threadInfo = withContext(Dispatchers.Default) {
                    data.threadInfo.map { if (it.id == threadId) it.setLikeStatus(like) else it }
                }
                HotThreadListResponseData.ADAPTER.encodeCache(cacheFile, data.copy(threadInfo = threadInfo))
                cacheFile.setLastModified(lastModified)
            } catch (e: Throwable) {
                Log.e(TAG, "onUpdateHotThreadLikeStatus", e)
            }
        }
    }

    override suspend fun purgeHotThread() = withContext(Dispatchers.IO) {
        mutex.withLock {
            FileUtil.deleteWithPrefixSafe(cacheDir, CACHE_HOT_PREFIX)
        }
    }

    /**
     * @return Cached personalized data at [page]
     * */
    override suspend fun loadPersonalized(page: Int): PersonalizedResponseData? = withContext(Dispatchers.IO) {
        val cacheFile = personalizedCacheFile(page)
        mutex.withLock {
            val cacheExpireMill = PERSONALIZED_EXPIRE_MILL.takeIf { page == 1 }?.toLong()
            try {
                PersonalizedResponseData.ADAPTER.decodeCache(cacheIn = cacheFile, cacheExpireMill)
            } catch (e: Throwable) {
                Log.e(TAG, "onLoadPersonalized", e)
                null
            }
        }
    }

    override suspend fun savePersonalized(data: PersonalizedResponseData, page: Int) = withContext(Dispatchers.IO) {
        val cacheFile = personalizedCacheFile(page)
        mutex.withLock {
            runCatching {
                PersonalizedResponseData.ADAPTER.encodeCache(cacheOut = cacheFile, data)
            }
            .isSuccess
        }
    }

    override suspend fun dislikePersonalized(threadId: Long) = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                // Remove target thread from local cache
                updatePersonalized { threadInfo -> threadInfo.takeUnless { threadInfo.id == threadId } }
            } catch (e: Throwable) {
                Log.e(TAG, "onDislikePersonalized", e)
                purgePersonalized()
            }
        }
    }

    override suspend fun updatePersonalizedLike(threadId: Long, like: Like) = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                updatePersonalized { threadInfo ->
                    if (threadInfo.id == threadId) threadInfo.setLikeStatus(like) else threadInfo
                }
            } catch (e: Throwable) {
                Log.e(TAG, "onUpdatePersonalizedLikeStatus", e)
                purgePersonalized()
            }
        }
    }

    /**
     * Delete all cached personalized page
     * */
    override suspend fun purgePersonalized() = withContext(Dispatchers.IO) {
        mutex.withLock {
            FileUtil.deleteWithPrefixSafe(cacheDir, CACHE_PERSONALIZED_PREFIX)
        }
    }

    /**
     * @return last request unix time (10-digit Unix timestamp), cached user like data of
     *   first page (**null** if not exists or expired).
     * */
    override suspend fun loadUserLikeDataFirstPage(uid: Long): Pair<Long, UserLikeResponseData?> = withContext(Dispatchers.IO) {
        val cacheFile = userLikeCacheFile(uid)
        val userExpireMill = HOT_THREAD_EXPIRE_MILL
        mutex.withLock {
            var data = runCatching {
                UserLikeResponseData.ADAPTER.decodeCache(cacheFile, cacheExpireMill = null)
            }
            .getOrNull()

            val lastRequestUnix = data?.requestUnix ?: 0
            // Check cache expired
            if (data != null && cacheFile.isCacheExpired(userExpireMill.toLong())) {
                data = null
            }

            if (BuildConfig.DEBUG && lastRequestUnix > 0) {
                val duration = System.currentTimeMillis() / 1000 - lastRequestUnix
                Log.i(TAG, "onLoadUserLikeData: LastRequest: $lastRequestUnix, ${duration}s ago")
            }
            Pair(lastRequestUnix, data)
        }
    }

    override suspend fun saveUserLikeFirstPage(uid: Long, data: UserLikeResponseData) = withContext(Dispatchers.IO) {
        val cacheFile = userLikeCacheFile(uid)
        mutex.withLock {
            runCatching { UserLikeResponseData.ADAPTER.encodeCache(cacheFile, data) }.isSuccess
        }
    }

    override suspend fun updateUserLike(uid: Long, threadId: Long, like: Like) = withContext(Dispatchers.IO) {
        val cacheFile = userLikeCacheFile(uid)
        mutex.withLock {
            try {
                val lastModified: Long = cacheFile.lastModified()
                val data = UserLikeResponseData.ADAPTER.decodeCache(cacheFile) ?: return@withLock
                val threadInfo = withContext(Dispatchers.Default) {
                    // Update target thread like status
                    data.threadInfo.map {
                        val thread: ThreadInfo = it.threadList!!
                        if (thread.id == threadId) it.copy(threadList = thread.setLikeStatus(like)) else it
                    }
                }
                UserLikeResponseData.ADAPTER.encodeCache(cacheFile, data.copy(threadInfo = threadInfo))
                cacheFile.setLastModified(lastModified)
            } catch (e: Throwable) {
                Log.e(TAG, "onUpdateUserLikeStatus", e)
            }
        }
    }

    suspend fun purgeUserLike(uid: Long) = withContext(Dispatchers.IO) {
        val cacheFile = userLikeCacheFile(uid)
        mutex.withLock {
            cacheFile.deleteQuietly()
        }
    }

    private fun userLikeCacheFile(uid: Long): File {
        if (uid <= 0) throw TiebaNotLoggedInException()
        return File(cacheDir, "concern_$uid")
    }

    private fun hotCacheFile(tabCode: String): File {
        // Concat prefix and tab code: hot_all, hot_shipin ...
        return File(cacheDir, "$CACHE_HOT_PREFIX$tabCode")
    }

    private fun personalizedCacheFile(page: Int): File {
        require(page > 0) { "Illegal page number: $page" }
        // Concat prefix and page number: p_1, p_2, p_3 ...
        return File(cacheDir, "$CACHE_PERSONALIZED_PREFIX$page")
    }

    // Update local file cache of personalized threads
    // Migrate to Room Database?
    @Throws(IOException::class)
    private suspend fun updatePersonalized(transform: (ThreadInfo) -> ThreadInfo?) {
        val start = System.currentTimeMillis()
        val cachedPages = cacheDir.listFiles { it.name.startsWith(CACHE_PERSONALIZED_PREFIX) } ?: return
        for (cacheFile in cachedPages) {
            val page = PersonalizedResponseData.ADAPTER.decodeCache(cacheFile) ?: continue
            val newData = withContext(Dispatchers.Default) {
                // Associate thread_personalized by thread id
                val threadPersonalizedMap = page.thread_personalized.associateByTo(hashMapOf()) { it.tid }
                val threads = mutableListOf<ThreadInfo>()
                var changed = false
                for (thread in page.thread_list) {
                    when (val updatedThread = transform(thread)) {
                        null -> {
                            threadPersonalizedMap -= thread.id
                            changed = true
                            Log.i(TAG, "onUpdatePersonalized: Thread ${thread.id} removed")
                        }

                        thread -> threads.add(thread)

                        else -> {
                            threads.add(updatedThread)
                            changed = true
                            Log.i(TAG, "onUpdatePersonalized: Thread ${thread.id} updated")
                        }
                    }
                }
                return@withContext if (changed) {
                    PersonalizedResponseData(threads, threadPersonalizedMap.values.toList())
                } else {
                    null
                }
            }

            if (newData != null) {
                val lastModified: Long = cacheFile.lastModified()
                PersonalizedResponseData.ADAPTER.encodeCache(cacheFile, newData)
                cacheFile.setLastModified(lastModified)
                Log.w(TAG, "onUpdatePersonalized: Cached page ${cacheFile.name} updated")
                break
            }
        }
        val cost = System.currentTimeMillis() - start
        Log.w(TAG, "onUpdatePersonalized: Done, cost: $cost ms")
    }
}

class ExploreAssetsDataSource(@ApplicationContext val context: Context): ExploreLocalDataSource {

    override suspend fun loadHotThread(tabCode: String): HotThreadListResponseData? = null

    override suspend fun loadPersonalized(page: Int) = withContext(Dispatchers.IO) {
        context.assets.open("personalized/PersonalizedResponseData_12.52.1.0.pb").use { input ->
            PersonalizedResponseData.ADAPTER.decode(input)
        }
    }

    override suspend fun loadUserLikeDataFirstPage(uid: Long): Pair<Long, UserLikeResponseData?> = Pair(0, null)

    override suspend fun saveHotThread(tabCode: String, data: HotThreadListResponseData): Boolean = true

    override suspend fun savePersonalized(data: PersonalizedResponseData, page: Int): Boolean = true

    override suspend fun saveUserLikeFirstPage(uid: Long, data: UserLikeResponseData): Boolean = true

    override suspend fun purgeHotThread() {}

    override suspend fun purgePersonalized() {}

    override suspend fun updateHotThreadLike(tabCode: String, threadId: Long, like: Like) {}

    override suspend fun updatePersonalizedLike(threadId: Long, like: Like) {}

    override suspend fun updateUserLike(uid: Long, threadId: Long, like: Like) {}

    override suspend fun dislikePersonalized(threadId: Long) {}
}
