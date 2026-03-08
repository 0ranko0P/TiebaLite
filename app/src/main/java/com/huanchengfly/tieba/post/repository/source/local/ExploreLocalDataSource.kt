package com.huanchengfly.tieba.post.repository.source.local

import android.content.Context
import android.util.Log
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.api.booleanToInt
import com.huanchengfly.tieba.post.api.models.protos.Agree
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.hotThreadList.HotThreadListResponseData
import com.huanchengfly.tieba.post.api.models.protos.personalized.PersonalizedResponseData
import com.huanchengfly.tieba.post.api.models.protos.userLike.UserLikeResponseData
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.ui.models.Like
import com.huanchengfly.tieba.post.utils.FileUtil.deleteQuietly
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
import javax.inject.Inject
import javax.inject.Singleton

private const val CACHE_DIR_NAME = "Explore"

private const val CACHE_PERSONALIZED_PREFIX = "p_"

private const val CACHE_HOT_PREFIX = "hot_"

private const val HOT_THREAD_EXPIRE_MILL = 0x36EE80 // 1 hour
private const val PERSONALIZED_EXPIRE_MILL = 0x5265C00 // 1 day

private const val TAG = "ExploreLocalDataSource"

@Singleton
class ExploreLocalDataSource @Inject constructor(@ApplicationContext context: Context) {

    private val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)

    private val mutex = Mutex()

    suspend fun loadHotThread(tabCode: String): HotThreadListResponseData? = withContext(Dispatchers.IO) {
        val cacheFile = hotCacheFile(tabCode)
        mutex.withLock {
            runCatching {
                HotThreadListResponseData.ADAPTER.decodeCache(cacheFile, HOT_THREAD_EXPIRE_MILL.toLong())
            }
            .getOrNull()
        }
    }

    suspend fun saveHotThread(tabCode: String, data: HotThreadListResponseData) = withContext(Dispatchers.IO) {
        val cacheFile = hotCacheFile(tabCode)
        mutex.withLock {
            runCatching { HotThreadListResponseData.ADAPTER.encodeCache(cacheFile, data) }.isSuccess
        }
    }

    suspend fun updateHotThreadLike(tabCode: String, threadId: Long, like: Like) = withContext(Dispatchers.IO) {
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

    suspend fun purgeHotThread() = withContext(Dispatchers.IO) {
        mutex.withLock {
            deleteWithPrefixSafe(cacheDir, CACHE_HOT_PREFIX)
        }
    }

    /**
     * @return Cached personalized data at [page]
     * */
    suspend fun loadPersonalized(page: Int): PersonalizedResponseData? = withContext(Dispatchers.IO) {
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

    suspend fun savePersonalized(data: PersonalizedResponseData, page: Int) = withContext(Dispatchers.IO) {
        val cacheFile = personalizedCacheFile(page)
        mutex.withLock {
            runCatching {
                PersonalizedResponseData.ADAPTER.encodeCache(cacheOut = cacheFile, data)
            }
            .isSuccess
        }
    }

    suspend fun dislikePersonalized(threadId: Long) = withContext(Dispatchers.IO) {
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

    suspend fun updatePersonalizedLike(threadId: Long, like: Like) = withContext(Dispatchers.IO) {
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
    suspend fun purgePersonalized() = withContext(Dispatchers.IO) {
        mutex.withLock {
            deleteWithPrefixSafe(cacheDir, CACHE_PERSONALIZED_PREFIX)
        }
    }

    /**
     * @return last request unix time (10-digit Unix timestamp), cached user like data of
     *   first page (**null** if not exists or expired).
     * */
    suspend fun loadUserLikeDataFirstPage(uid: Long): Pair<Long, UserLikeResponseData?> = withContext(Dispatchers.IO) {
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

    suspend fun saveUserLikeFirstPage(uid: Long, data: UserLikeResponseData) = withContext(Dispatchers.IO) {
        val cacheFile = userLikeCacheFile(uid)
        mutex.withLock {
            runCatching { UserLikeResponseData.ADAPTER.encodeCache(cacheFile, data) }.isSuccess
        }
    }

    suspend fun updateUserLike(uid: Long, threadId: Long, like: Like) = withContext(Dispatchers.IO) {
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

fun deleteWithPrefixSafe(dir: File, prefix: String): Int {
    val start = System.currentTimeMillis()
    var deleted = 0
    // list all files safely
    val files = runCatching { dir.listFiles() }.getOrNull() ?: return 0
    files.forEach { f ->
        try {
            // delete file with prefix
            if (f.isFile && f.name.startsWith(prefix) && f.delete()) {
                deleted++
            }
        } catch (e: Throwable) {
            Log.w(CACHE_DIR_NAME, "Delete ${f.name} Failed: ${e.message}.")
        }
    }

    if (BuildConfig.DEBUG) {
        val cost = System.currentTimeMillis() - start
        Log.i(CACHE_DIR_NAME, "Delete $deleted files of $prefix, cost ${cost}ms")
    }
    return deleted
}

private fun ThreadInfo.setLikeStatus(like: Like): ThreadInfo {
    return copy(agree = Agree(agreeNum = like.count, hasAgree = like.liked.booleanToInt()))
}
