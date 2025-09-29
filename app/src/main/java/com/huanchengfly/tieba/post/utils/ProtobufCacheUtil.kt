package com.huanchengfly.tieba.post.utils

import android.util.Log
import androidx.annotation.WorkerThread
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.utils.FileUtil.deleteQuietly
import com.huanchengfly.tieba.post.utils.FileUtil.ensureParents
import com.huanchengfly.tieba.post.utils.FileUtil.isCacheExpired
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ReverseProtoWriter
import com.squareup.wire.internal.ProtocolException
import okhttp3.internal.closeQuietly
import okio.BufferedSink
import okio.BufferedSource
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.io.IOException

object ProtobufCacheUtil {

    private const val PROTO_CODEC_TAG: Int = 114514

    private const val TAG = "ProtobufCacheUtil"

    /**
     * Decode [T] from [cacheIn], **null** if not exists or expired.
     * */
    @Throws(IOException::class)
    @WorkerThread
    fun <T> ProtoAdapter<T>.decodeCache(cacheIn: File, cacheExpireMill: Long? = null): T? {
        if (!cacheIn.exists()) return null

        // Check cache expired
        if (cacheExpireMill != null && cacheIn.isCacheExpired(cacheExpireMill)) {
            if (BuildConfig.DEBUG) {
                val duration = System.currentTimeMillis() - (cacheIn.lastModified() + cacheExpireMill)
                Log.i(TAG, "onDecodeCache: ${cacheIn.name} expired for ${duration / 1000}s")
            }
            return null
        }

        var source: BufferedSource? = null
        try {
            source = cacheIn.source().buffer()
            return this.decode(source)
        } catch (e: Throwable) {
            Log.e(TAG, "onDecodeCache: Unable to decode ${cacheIn.name}: ${e.message}.")
            // possible file corruption, delete it
            cacheIn.deleteQuietly()
            throw e
        } finally {
            source?.closeQuietly()
        }
    }

    /**
     * Decode list of [T] from [cacheIn], **null** if file not exists or expired.
     * */
    @Throws(IOException::class)
    @WorkerThread
    fun <T> ProtoAdapter<T>.decodeListCache(cacheIn: File, cacheExpireMill: Long? = null): List<T>? {
        if (!cacheIn.exists()) return null

        // Check cache expired
        if (cacheExpireMill != null && cacheIn.isCacheExpired(cacheExpireMill)) {
            if (BuildConfig.DEBUG) {
                val duration = System.currentTimeMillis() - (cacheIn.lastModified() + cacheExpireMill)
                Log.i(TAG, "onDecodeCache: ${cacheIn.name} expired for ${duration / 1000}s")
            }
            return null
        }

        val result = mutableListOf<T>()
        var source: BufferedSource? = null
        try {
            source = cacheIn.source().buffer()
            val reader = ProtoReader(source)
            reader.forEachTag { tag ->
                if (tag == PROTO_CODEC_TAG) {
                    result.add(this.decode(reader))
                } else {
                    throw ProtocolException("Decode failed: Unknown Tag: $tag")
                }
            }
            return result
        } catch (e: Throwable) {
            Log.e(TAG, "decodeListCache: Unable to decode ${cacheIn.name}: ${e.message}.")
            // possible file corruption, delete it
            cacheIn.deleteQuietly()
            throw e
        } finally {
            source?.closeQuietly()
        }
    }

    @Throws(IOException::class)
    @WorkerThread
    fun <T> ProtoAdapter<T>.encodeCache(cacheOut: File, data: T) {
        cacheOut.ensureParents()

        var sink: BufferedSink? = null
        try {
            sink = cacheOut.sink().buffer()
            this.encode(sink, data)
        } catch (e: Throwable) {
            Log.e(TAG, "onEncodeCache: Unable to encode ${cacheOut.name}: ${e.message}.")
            cacheOut.deleteQuietly()
            throw e
        } finally {
            sink?.closeQuietly()
        }
    }

    @Throws(IOException::class)
    @WorkerThread
    fun <T> ProtoAdapter<T>.encodeListCache(cacheOut: File, data: List<T>) {
        cacheOut.ensureParents()

        var sink: BufferedSink? = null
        try {
            sink = cacheOut.sink().buffer()
            val writer = ReverseProtoWriter()
            this.asRepeated().encodeWithTag(writer, PROTO_CODEC_TAG, data)
            writer.writeTo(sink)
        } catch (e: Throwable) {
            Log.e(TAG, "onEncodeListCache: Unable to encode ${cacheOut.name}: ${e.message}.")
            cacheOut.deleteQuietly()
            throw e
        } finally {
            sink?.closeQuietly()
        }
    }
}