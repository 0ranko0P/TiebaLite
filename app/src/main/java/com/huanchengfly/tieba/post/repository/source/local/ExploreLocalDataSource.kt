package com.huanchengfly.tieba.post.repository.source.local

import android.content.Context
import android.util.Log
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.api.models.protos.hotThreadList.HotThreadListResponseData
import com.huanchengfly.tieba.post.api.models.protos.personalized.PersonalizedResponseData
import com.huanchengfly.tieba.post.api.models.protos.userLike.UserLikeResponseData
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
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
import javax.inject.Inject
import javax.inject.Singleton

private const val CACHE_DIR_NAME = "Explore"

private const val CACHE_PERSONALIZED_PREFIX = "p_"

private const val CACHE_HOT_PREFIX = "hot_"

private const val HOT_THREAD_EXPIRE_MILL = 0x36EE80 // 1 hour
private const val PERSONALIZED_EXPIRE_MILL = 0x5265C00 // 1 day

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
            // only first page can expire
            val cacheExpireMill = if (page == 1) PERSONALIZED_EXPIRE_MILL.toLong() else null
            runCatching {
                PersonalizedResponseData.ADAPTER.decodeCache(cacheIn = cacheFile, cacheExpireMill)
            }
            .getOrNull()
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
                Log.i(CACHE_DIR_NAME, "onLoadUserLikeData: LastRequest: $lastRequestUnix, ${duration}s ago")
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
        Log.i(CACHE_DIR_NAME, "Delete $deleted files, cost ${cost}ms")
    }
    return deleted
}
