package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.fromJson
import com.huanchengfly.tieba.post.models.EmoticonCache
import com.huanchengfly.tieba.post.pxToDp
import com.huanchengfly.tieba.post.pxToSp
import com.huanchengfly.tieba.post.toJson
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

data class Emoticon(
    val id: String,
    val name: String
)

object EmoticonManager {
    private const val TAG = "EmoticonManager"

    private const val EMOTICON_ASSET_NAME = "emoticon"
    private val DEFAULT_EMOTICON_MAPPING: Map<String, String> by lazy {
        val jsonStr = FileUtil.readAssetFile(App.INSTANCE, "emoticon.json")
        val newMap: HashMap<String, String> = jsonStr!!.fromJson()
        return@lazy newMap
    }

    /**
     * Default emoticon download directory
     * */
    private val EMOTICON_CACHE_DIR: File by lazy {
        getContext().run { File(externalCacheDir ?: cacheDir, EMOTICON_ASSET_NAME) }
    }

    private lateinit var contextRef: WeakReference<Context>
    private val emoticonIds: MutableList<String> = mutableListOf()

    private val inlineTextCache by lazy {
        HashMap<Int, WeakReference<Map<String, InlineTextContent>>>()
    }

    private val lineHeightCache by lazy { HashMap<TextStyle, Int>(4) }

    private val emoticonMapping: MutableMap<String, String> = ConcurrentHashMap()

    private val scope = CoroutineScope(Dispatchers.Main + CoroutineName(TAG))
    private var cacheUpdateJob: Job = Job()

    fun getEmoticonInlineContent(sizePx: Int): Map<String, InlineTextContent> {
        val size = sizePx.pxToSp()
        val cached = inlineTextCache[size]?.get()
        if (cached == null) {
            val placeholder = Placeholder(size.sp, size.sp, PlaceholderVerticalAlign.TextCenter)
            return emoticonIds.associate { id ->
                "Emoticon#$id" to InlineTextContent(
                    placeholder = placeholder,
                    children = { EmoticonInlineImage(Modifier.size(sizePx.pxToDp().dp), id) }
                )
            }.apply { inlineTextCache[size] = WeakReference(this) }
        } else {
            return cached
        }
    }

    @OptIn(ExperimentalGlideComposeApi::class)
    @Composable
    fun EmoticonInlineImage(modifier: Modifier = Modifier, id: String, description: String = id) {
        val uri = remember { getEmoticonUri(id) }
        GlideImage(model = uri, contentDescription = description, modifier = modifier) {
            // Disable disk cache for asset emoticon
            if (uri.startsWith("file")) it.diskCacheStrategy(DiskCacheStrategy.NONE) else it
        }
    }

    suspend fun init(context: Context) {
        contextRef = WeakReference(context)
        val emoticonCache = getEmoticonDataCache()
        if (emoticonCache.ids.isEmpty()) {
            for (i in 1..50) {
                emoticonIds.add("image_emoticon$i")
            }
            for (i in 61..101) {
                emoticonIds.add("image_emoticon$i")
            }
            for (i in 125..137) {
                emoticonIds.add("image_emoticon$i")
            }
        } else {
            emoticonIds.addAll(emoticonCache.ids)
        }
        if (emoticonCache.mapping.isEmpty()) {
            emoticonMapping.putAll(DEFAULT_EMOTICON_MAPPING)
        } else {
            emoticonMapping.putAll(emoticonCache.mapping)
        }
        updateCache()
    }

    private fun updateCache() {
        if (cacheUpdateJob.isActive) cacheUpdateJob.cancel()

        cacheUpdateJob = scope.launch(Dispatchers.IO) {
            val emoticonDataCacheFile = File(EMOTICON_CACHE_DIR, "emoticon_data_cache")
            val emoticonJson = EmoticonCache(emoticonIds, emoticonMapping).toJson()
            ensureActive()
            FileUtil.writeFile(emoticonDataCacheFile, emoticonJson, false)
        }
    }

    private fun getContext(): Context = contextRef.get() ?: App.INSTANCE

    private suspend fun getEmoticonDataCache(): EmoticonCache = withContext(Dispatchers.IO) {
        runCatching {
            File(EMOTICON_CACHE_DIR, "emoticon_data_cache").takeIf { it.exists() }
                ?.fromJson<EmoticonCache>()
        }.getOrNull() ?: EmoticonCache()
    }

    fun getAllEmoticon(): List<Emoticon> {
        return emoticonIds.map { id ->
            Emoticon(id = id, name = getEmoticonNameById(id) ?: "")
        }
    }

    fun getEmoticonIdByName(name: String): String? = emoticonMapping[name]

    private fun getEmoticonNameById(id: String): String? {
        return emoticonMapping.firstNotNullOfOrNull { (name, emoticonId) ->
            if (emoticonId == id) name else null
        }
    }

    private fun getEmoticonUri(id: String): String {
        return if (DEFAULT_EMOTICON_MAPPING.containsValue(id)) {
            "file:///android_asset/$EMOTICON_ASSET_NAME/$id.webp"
        } else {
            "http://static.tieba.baidu.com/tb/editor/images/client/$id.png"
        }
    }

    fun getEmoticonBitmap(id: String?, size: Int): Deferred<Bitmap> = scope.async(Dispatchers.IO) {
        var builder = Glide.with(getContext()).asBitmap()
        val uri = if (id != null) getEmoticonUri(id) else null
        // Disable disk cache for asset emoticon
        if (uri?.startsWith("file") == true) {
            builder = builder.diskCacheStrategy(DiskCacheStrategy.NONE)
        }
        return@async builder.load(uri)
            .fallback(R.drawable.ic_chrome) // Null ID
            .submit(size, size)
            .get()
    }

    fun registerEmoticon(id: String, name: String) {
        val realId = if (id == "image_emoticon") "image_emoticon1" else id
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
    }

    @Composable
    fun calcLineHeightPx(style: TextStyle): Int {
        val cachedSize = lineHeightCache[style]
        return if (cachedSize != null) {
            cachedSize
        } else {
            val textLayoutResult = rememberTextMeasurer().measure(
                text = stringResource(id = R.string.single_chinese_char),
                style = style
            )
            val height = textLayoutResult.size.height
            lineHeightCache[style] = height
            height
        }
    }

    fun clear() {
        inlineTextCache.clear()
        lineHeightCache.clear()
        contextRef.clear()
    }
}