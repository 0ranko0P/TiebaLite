package com.huanchengfly.tieba.post.utils

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.collection.ArraySet
import coil3.BitmapImage
import coil3.executeBlocking
import coil3.imageLoader
import coil3.network.HttpException
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.core.network.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.ControlledRunner
import com.huanchengfly.tieba.post.fromJson
import com.huanchengfly.tieba.post.core.common.ktx.toJson
import com.huanchengfly.tieba.post.utils.CoilUtil.downloadOnly
import com.huanchengfly.tieba.post.core.common.ktx.deleteQuietly
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

data class Emoticon(
    val id: String,
    val name: String
)

private data class EmoticonCache(
    var ids: Set<String> = emptySet(),
    var mapping: Map<String, String> = emptyMap()
)

object EmoticonManager {
    private const val TAG = "EmoticonManager"

    private const val EMOTICON_ID_PREFIX = "image_emoticon"
    private const val EMOTICON_ID_PREFIX2 = "shoubai_emoji"

    private const val EMOTICON_ASSET_NAME = "emoticon"
    private val DEFAULT_EMOTICON_MAPPING: Map<String, String> by lazy {
        val jsonStr = FileUtil.readAssetFile(App.INSTANCE, "emoticon.json")
        val newMap: HashMap<String, String> = jsonStr!!.fromJson()
        return@lazy newMap
    }

    /**
     * Default emoticon download directory
     * */
    private val EMOTICON_CACHE_DIR: File
        get() = with(getContext()) {
            File(externalCacheDir ?: cacheDir, EMOTICON_ASSET_NAME)
        }

    private val EMOTICON_BAD_ID_FILE: File
        get() = File(getContext().filesDir, "emoticon_bad_id")

    private lateinit var contextRef: WeakReference<Context>

    private val emoticonIds: MutableSet<String> = Collections.synchronizedSet(ArraySet())
    private val badEmoticonIds: MutableSet<String> = Collections.synchronizedSet(ArraySet())

    private val emoticonMapping: MutableMap<String, String> = ConcurrentHashMap()

    private val scope = CoroutineScope(Dispatchers.Main + CoroutineName(TAG) + SupervisorJob())

    private val cacheUpdateRunner = ControlledRunner<Unit>()

    fun init(context: Application) = scope.launch {
        contextRef = WeakReference(context)
        val (emoticonCache, blacklist) = getEmoticonDataCache()
        val firstInit = emoticonCache.mapping.isEmpty()
        if (emoticonCache.ids.isEmpty()) {
            for (i in 1..50) {
                emoticonIds.add("$EMOTICON_ID_PREFIX$i")
            }
            for (i in 61..101) {
                emoticonIds.add("$EMOTICON_ID_PREFIX$i")
            }
            for (i in 125..137) {
                emoticonIds.add("$EMOTICON_ID_PREFIX$i")
            }
        } else {
            emoticonIds.addAll(emoticonCache.ids)
        }
        if (emoticonCache.mapping.isEmpty()) {
            emoticonMapping.putAll(DEFAULT_EMOTICON_MAPPING)
        } else {
            emoticonMapping.putAll(emoticonCache.mapping)
        }
        if (blacklist.isNotEmpty()) {
            badEmoticonIds.addAll(blacklist)
        }

        if (firstInit) {
            updateCache()
        } else {
            preloadEmoticons()
        }
    }

    private fun updateCache() {
        scope.launch {
            cacheUpdateRunner.cancelPreviousThenRun(Dispatchers.IO) {
                // Limit update rate to 1/min to avoid unnecessary disk writes
                delay(60000L)
                val emoticonJson = EmoticonCache(emoticonIds, emoticonMapping).toJson()
                val emoticonDataCacheFile = File(EMOTICON_CACHE_DIR, "emoticon_data_cache")
                ensureActive()
                FileUtil.writeFile(emoticonDataCacheFile, emoticonJson, false)
                FileUtil.writeLines(EMOTICON_BAD_ID_FILE, badEmoticonIds.toList())
            }
        }
    }

    private fun getContext(): Context = contextRef.get() ?: App.INSTANCE

    private suspend fun getEmoticonDataCache(): Pair<EmoticonCache, List<String>> = withContext(Dispatchers.IO) {
        val emoticonCacheFile = File(EMOTICON_CACHE_DIR, "emoticon_data_cache")
        val emoticonCache = try {
            emoticonCacheFile.fromJson<EmoticonCache>()
        } catch (_: Throwable) {
            emoticonCacheFile.deleteQuietly()
            EmoticonCache()
        }
        val emoticonBlacklist = try {
            EMOTICON_BAD_ID_FILE.readLines()
        } catch (_: Throwable) {
            EMOTICON_BAD_ID_FILE.deleteQuietly()
            emptyList()
        }
        return@withContext emoticonCache to emoticonBlacklist
    }

    fun getAllEmoticon(): List<Emoticon> {
        return emoticonMapping.mapNotNull { (name, id) ->
            if (name.isEmpty()) null else Emoticon(id = id, name = name)
        }
    }

    fun getEmoticonIdByName(name: String): String? = emoticonMapping[name]

    fun getEmoticonUri(id: String): String {
        return if (DEFAULT_EMOTICON_MAPPING.containsValue(id)) {
            "file:///android_asset/$EMOTICON_ASSET_NAME/$id.webp"
        } else {
            "http://static.tieba.baidu.com/tb/editor/images/client/$id.png"
        }
    }

    suspend fun getEmoticonBitmap(id: String, size: Int): Bitmap {
        val request = ImageRequest.Builder(getContext())
            .data(getEmoticonUri(id))
            .size(size)
            .listener(onError = { _, result -> onLoadError(id, error = result.throwable) })
            .build()
        val result = getContext().imageLoader.execute(request)
        return (result.image as BitmapImage).bitmap
    }

    fun registerEmoticon(id: String, name: String): Boolean {
        if (id.isEmpty() || name.isEmpty()) {
            return false
        }
        if (!(id.startsWith(EMOTICON_ID_PREFIX) || id.startsWith(EMOTICON_ID_PREFIX2))) {
            return false
        }
        if (badEmoticonIds.contains(id)) {
            return false
        }

        val realId = if (id == EMOTICON_ID_PREFIX) "image_emoticon1" else id
        var changed = false
        if (!emoticonIds.contains(realId)) {
            emoticonIds.add(realId)
            changed = true
        }
        if (!emoticonMapping.containsKey(name)) {
            emoticonMapping[name] = realId
            changed = true
        }
        if (changed) {
            updateCache()
        }
        return true
    }

    private suspend fun preloadEmoticons() {
        val start = System.currentTimeMillis()
        val context = getContext()
        val idList = emoticonIds.toTypedArray()
        val imageLoader = context.imageLoader
        withContext(Dispatchers.IO) {
            var errorCount = 0
            idList.forEach { id ->
                val request = ImageRequest.Builder(context)
                    .data(getEmoticonUri(id))
                    .downloadOnly()
                    .build()
                val result = imageLoader.executeBlocking(request)
                if (result is ErrorResult) {
                    onLoadError(id, error = result.throwable, updateCache = false)
                    errorCount++
                }
            }
            if (errorCount > 0) {
                updateCache()
            }
            val cost = System.currentTimeMillis() - start
            Log.i(TAG, "onPreloadEmoticons: Done, size ${idList.size}, $errorCount error, cost ${cost}ms")
        }
    }

    fun onLoadError(id: String, error: Throwable, updateCache: Boolean = true) {
        if (error is HttpException && error.response.code == 404) {
            Log.e(TAG, "onLoadError: Unsupported ID: $id")
            badEmoticonIds.add(id)
            emoticonIds.remove(id)
            emoticonMapping.filterValues { it == id }.keys.forEach { key ->
                emoticonMapping.remove(key)
            }
            if (updateCache) {
                updateCache()
            }
        } else {
            Log.w(TAG, "onLoadError: Unable to load emoticon $id: ${error.getErrorMessage()}")
        }
    }
}