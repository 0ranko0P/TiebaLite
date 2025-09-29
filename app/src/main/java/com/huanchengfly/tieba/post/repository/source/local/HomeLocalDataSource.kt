package com.huanchengfly.tieba.post.repository.source.local

import android.content.Context
import com.huanchengfly.tieba.post.api.models.protos.forumRecommend.LikeForum
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

private const val CACHE_DIR_NAME = "likedForum"
private const val CACHE_EXPIRE_MILL= 0xA4CB800 // 2 Days

/**
 * Local data source to cache liked forums in file.
 * */
@Singleton
class HomeLocalDataSource @Inject constructor(@ApplicationContext context: Context) {

    private val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)

    private val mutex = Mutex()

    /**
     * @param uid account uid of current user
     *
     * @return list of cleaned [LikeForum], **null** if not exists or expired.
     *
     * @see isCacheExpired
     * @see clean
     * */
    suspend fun get(uid: Long): List<LikeForum>? = withContext(Dispatchers.IO) {
        val cacheFile = cacheFile(uid)
        mutex.withLock {
            runCatching {
                LikeForum.ADAPTER.decodeListCache(cacheFile, CACHE_EXPIRE_MILL.toLong())
            }
            .getOrNull()
        }
    }

    /**
     * Save liked forums to cache file
     *
     * Note that unused info in [LikeForum] will be erased to reduce file size, see [clean].
     *
     * @param uid account uid of current user
     * @param forums liked forums
     * */
    suspend fun saveOrUpdate(uid: Long, forums: List<LikeForum>): Boolean = withContext(Dispatchers.IO) {
        val data = forums.clean()
        val cacheFile = cacheFile(uid)
        mutex.withLock {
            runCatching {
                LikeForum.ADAPTER.encodeListCache(cacheFile, data)
            }
            .isSuccess
        }
    }

    suspend fun delete(uid: Long) = withContext(Dispatchers.IO) {
        mutex.withLock {
            cacheFile(uid).deleteQuietly()
        }
    }

    private fun cacheFile(uid: Long): File {
        require(uid > 0) { "Illegal User ID: $uid" }
        return File(cacheDir, uid.toString())
    }
}

// Erase unused content for smaller file size, this reduces the size by ~70%
private suspend fun List<LikeForum>.clean(): List<LikeForum> = if (isNotEmpty()) {
    withContext(Dispatchers.Default) {
        map { it.copy(content = "", theme_color = null, private_forum_info = null, tab_info = emptyList()) }
    }
} else {
    emptyList()
}
