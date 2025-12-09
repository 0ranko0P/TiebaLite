package com.huanchengfly.tieba.post.repository.source.local

import android.content.Context
import androidx.collection.MutableLongSet
import com.huanchengfly.tieba.post.api.models.protos.PostInfoList
import com.huanchengfly.tieba.post.utils.FileUtil.deleteQuietly
import com.huanchengfly.tieba.post.utils.FileUtil.isCacheExpired
import com.huanchengfly.tieba.post.utils.ProtobufCacheUtil.decodeListCache
import com.huanchengfly.tieba.post.utils.ProtobufCacheUtil.encodeListCache
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val CACHE_DIR_NAME = "User"

private const val THREAD_POST_EXPIRE_MILL = 0x36EE80 // 1 hour

@Singleton
class UserProfileLocalDataSource @Inject constructor(@ApplicationContext context: Context) {

    private val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)

    private val mutex = Mutex()

    /**
     * Load user threads or posts from cache file, **null** if not exists or expired.
     *
     * @return list of [PostInfoList] (Thread and Post share the same network model)
     * */
    suspend fun loadUserThreadPost(uid: Long, page: Int, isThread: Boolean): List<PostInfoList>? {
        return withContext(Dispatchers.IO) {
            val cacheFile = userThreadPostCacheFile(uid, page, isThread)
            // only first page will expire
            val postCacheExpireMill = if (page == 1) THREAD_POST_EXPIRE_MILL.toLong() else null

            mutex.withLock {
                runCatching {
                    PostInfoList.ADAPTER.decodeListCache(cacheFile, postCacheExpireMill)
                }
                .getOrNull()
            }
        }
    }

    /**
     * Save user threads or posts to cache file.
     * */
    suspend fun saveUserThreadPost(uid: Long, page: Int, data: List<PostInfoList>, isThread: Boolean): Boolean {
        return withContext(Dispatchers.IO) {
            val cacheFile = userThreadPostCacheFile(uid, page, isThread)
            mutex.withLock {
                runCatching { PostInfoList.ADAPTER.encodeListCache(cacheFile, data) }.isSuccess
            }
        }
    }

    /**
     * Remove all cached post and thread of this user
     * */
    suspend fun purgeByUid(uid: Long): Int = withContext(Dispatchers.IO) {
        mutex.withLock {
            deleteWithPrefixSafe(cacheDir, prefix = "${uid}_")
        }
    }

    suspend fun purgeUserThreadPost(uid: Long, isThread: Boolean) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val prefix = if (isThread) "${uid}_t_" else "${uid}_p_"
            deleteWithPrefixSafe(cacheDir, prefix)
        }
    }

    private fun userThreadPostCacheFile(uid: Long, page: Int, isThread: Boolean): File {
        require(uid > 0) { "Invalid user ID: $uid." }
        require(page >= 0) { "Invalid page number: $page" }
        val prefix = if (isThread) "${uid}_t_" else "${uid}_p_"
        return File(cacheDir, prefix + page)
    }

    private val File.uid: Long
        get() {
            val index = name.indexOf('_')
            return if (index > 0) name.substring(0, index).toLong() else name.toLong()
        }

    suspend fun cleanUpExpired(): Int = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                val caches = cacheDir.listFiles() ?: return@runCatching 0
                var deleted = 0
                val expiredUsers = MutableLongSet()
                val expireMill = THREAD_POST_EXPIRE_MILL.toLong()
                caches.forEach {
                    val uid = it.uid
                    if (uid in expiredUsers || it.length() <= 0 || it.isCacheExpired(expireMill)) {
                        expiredUsers.add(uid)
                        it.deleteQuietly()
                        deleted++
                    }
                }
                deleted
            }
            .getOrNull() ?: 0
        }
    }
}