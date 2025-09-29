package com.huanchengfly.tieba.post.repository.source.local

import android.content.Context
import com.huanchengfly.tieba.post.api.models.protos.PostInfoList
import com.huanchengfly.tieba.post.api.models.protos.profile.ProfileResponseData
import com.huanchengfly.tieba.post.utils.FileUtil.deleteQuietly
import com.huanchengfly.tieba.post.utils.ProtobufCacheUtil.decodeCache
import com.huanchengfly.tieba.post.utils.ProtobufCacheUtil.decodeListCache
import com.huanchengfly.tieba.post.utils.ProtobufCacheUtil.encodeCache
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

private const val PROFILE_EXPIRE_MILL = 0x240C8400 // 7 days
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

    suspend fun purgeUserThreadPost(uid: Long, isThread: Boolean) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val prefix = if (isThread) "${uid}_t_" else "${uid}_p_"
            deleteWithPrefixSafe(cacheDir, prefix)
        }
    }

    suspend fun loadUserProfile(uid: Long): ProfileResponseData? = withContext(Dispatchers.IO) {
        val cacheFile = userCacheFile(uid)
        mutex.withLock {
            runCatching {
                ProfileResponseData.ADAPTER.decodeCache(cacheFile, PROFILE_EXPIRE_MILL.toLong())
            }
            .getOrNull()
        }
    }

    suspend fun saveUserProfile(uid: Long, data: ProfileResponseData) = withContext(Dispatchers.IO) {
        val cacheFile = userCacheFile(uid)
        mutex.withLock {
            runCatching {
                ProfileResponseData.ADAPTER.encodeCache(cacheFile, data)
            }
            .isSuccess
        }
    }

    suspend fun deleteUserProfile(uid: Long) = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching { userCacheFile(uid).deleteQuietly() }
        }
    }

    private fun userThreadPostCacheFile(uid: Long, page: Int, isThread: Boolean): File {
        require(uid > 0) { "Invalid user ID: $uid." }
        require(page >= 0) { "Invalid page number: $page" }
        val prefix = if (isThread) "${uid}_t_" else "${uid}_p_"
        return File(cacheDir, prefix + page)
    }

    private fun userCacheFile(uid: Long): File {
        require(uid > 0) { "Invalid user ID: $uid." }
        return File(cacheDir, uid.toString())
    }
}