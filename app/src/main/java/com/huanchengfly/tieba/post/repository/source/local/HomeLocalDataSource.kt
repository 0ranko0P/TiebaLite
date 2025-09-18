package com.huanchengfly.tieba.post.repository.source.local

import android.content.Context
import com.huanchengfly.tieba.post.api.models.protos.forumRecommend.LikeForum
import com.huanchengfly.tieba.post.utils.FileUtil.deleteQuietly
import com.huanchengfly.tieba.post.utils.FileUtil.ensureParents
import com.squareup.wire.ProtoReader
import com.squareup.wire.ReverseProtoWriter
import com.squareup.wire.internal.ProtocolException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val PROTO_CODEC_TAG: Int = 114514

private const val CACHE_DIR_NAME = "likedForum"
private const val CACHE_EXPIRE_MILL= 0xA4CB800 // 2 Days

private val File.isCacheExpired: Boolean
    get() = lastModified() + CACHE_EXPIRE_MILL < System.currentTimeMillis()

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
     * @return cached, cleaned [LikeForum] list from, **null** if not exists or expired.
     *
     * @see isCacheExpired
     * @see clean
     * */
    suspend fun get(uid: Long): List<LikeForum>? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val cacheFile = cacheFile(uid)
            runCatching {
                if (cacheFile.exists() && cacheFile.length() > 0) {
                    if (cacheFile.isCacheExpired) {
                        cacheFile.deleteQuietly()
                        return@runCatching null
                    }

                    cacheFile.source().buffer().use { source ->
                        val reader = ProtoReader(source)
                        mutableListOf<LikeForum>().also {
                            reader.forEachTag { tag ->
                                if (tag == PROTO_CODEC_TAG) {
                                    it.add(LikeForum.ADAPTER.decode(reader))
                                } else {
                                    throw ProtocolException("Decode LikeForum failed: unknown tag:$tag.")
                                }
                            }
                        }
                    }
                } else throw IOException("Empty proto file ${cacheFile.name}.")
            }
            .onFailure {
                it.printStackTrace()
                cacheFile.deleteQuietly()
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
        mutex.withLock {
            val cacheFile = cacheFile(uid)
            val forumList = forums.clean()
            runCatching {
                val writer = ReverseProtoWriter()
                LikeForum.ADAPTER.asRepeated().encodeWithTag(writer, PROTO_CODEC_TAG, forumList)
                cacheFile.sink().buffer().use { sink -> writer.writeTo(sink) }
                true
            }
            .onFailure {
                it.printStackTrace()
                cacheFile.deleteQuietly()
            }
            .getOrNull() == true
        }
    }

    suspend fun delete(uid: Long) = withContext(Dispatchers.IO) {
        mutex.withLock {
            cacheFile(uid).deleteQuietly()
        }
    }

    @Throws(IOException::class)
    private fun cacheFile(uid: Long): File {
        require(uid > 0)
        return File(cacheDir, uid.toString()).also { it.ensureParents() }
    }
}

// Erase unused content for smaller file size, this reduces the size by 70%
private suspend fun List<LikeForum>.clean(): List<LikeForum> = if (isNotEmpty()) {
    withContext(Dispatchers.Default) {
        map { it.copy(content = "", theme_color = null, private_forum_info = null, tab_info = emptyList()) }
    }
} else {
    emptyList()
}
