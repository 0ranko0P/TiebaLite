package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.panpf.sketch.compose.AsyncImage
import com.github.panpf.sketch.datasource.DataFrom
import com.github.panpf.sketch.fetch.newAssetUri
import com.github.panpf.sketch.request.DisplayRequest
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.fromJson
import com.huanchengfly.tieba.post.models.EmoticonCache
import com.huanchengfly.tieba.post.pxToDp
import com.huanchengfly.tieba.post.pxToSp
import com.huanchengfly.tieba.post.toJson
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.ref.WeakReference

@Composable
fun calcLineHeightPx(style: TextStyle): Int {
    val textMeasurer = rememberTextMeasurer()
    val textLayoutResult = textMeasurer.measure(
        AnnotatedString(stringResource(id = R.string.single_chinese_char)),
        style
    )
    return textLayoutResult.size.height
}

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
    private val emoticonMapping: MutableMap<String, String> = mutableMapOf()
    private val drawableCache: MutableMap<String, Drawable> by lazy { mutableMapOf() }

    private val scope = CoroutineScope(Dispatchers.Main + CoroutineName(TAG))
    private val queue = JobQueue()
    private var cacheUpdateJob: Job = Job()

    fun getEmoticonInlineContent(sizePx: Float): Map<String, InlineTextContent> {
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

    @Composable
    fun EmoticonInlineImage(
        modifier: Modifier = Modifier,
        id: String,
        description: String = stringResource(R.string.emoticon, getEmoticonNameById(id) ?: "")
    ) {
        AsyncImage(
            request = rememberEmoticonRequest(id = id),
            contentDescription = description,
            modifier = modifier,
            onSuccess = {
                if (it.result.dataFrom == DataFrom.NETWORK) {
                    val drawable = it.result.drawable as BitmapDrawable
                    saveEmoticons(drawable.bitmap, id)
                }
            },
            onError = {
                Log.i(TAG, "Failed load emoticon: $id", it.result.throwable)
            }
        )
    }

    @Composable
    fun rememberEmoticonRequest(id: String): DisplayRequest {
        return remember(id) { DisplayRequest(getContext(), getEmoticonUri(id)) }
    }

    fun init(context: Context) = scope.launch(Dispatchers.Main) {
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

    private fun getEmoticonFile(id: String): File = File(EMOTICON_CACHE_DIR, "$id.png")

    fun getAllEmoticon(): List<Emoticon> {
        return emoticonIds.map { id ->
            Emoticon(
                id = id,
                name = getEmoticonNameById(id) ?: ""
            )
        }
    }

    fun getEmoticonIdByName(name: String): String? = emoticonMapping[name]

    private fun getEmoticonNameById(id: String): String? {
        return emoticonMapping.firstNotNullOfOrNull { (name, emoticonId) ->
            if (emoticonId == id) name else null
        }
    }

    private fun getEmoticonAsset(context: Context, id: String): Drawable? {
        context.assets.open("$EMOTICON_ASSET_NAME/$id.webp").use {
            return Drawable.createFromStream(it, null)
        }
    }

    fun getEmoticonDrawable(context: Context, id: String?): Drawable? {
        if (id == null) {
            return null
        }
        if (drawableCache.containsKey(id)) {
            return drawableCache[id]
        }

        if (DEFAULT_EMOTICON_MAPPING.containsValue(id)) {
            return getEmoticonAsset(context, id)
                ?: throw NullPointerException("$id.webp not found in Asset!")
        }

        val drawable: BitmapDrawable? = runCatching {
            val emoticonFile = getEmoticonFile(id)
            if (emoticonFile.exists()) {
                BitmapDrawable(getContext().resources, emoticonFile.inputStream())
            } else null
        }.getOrNull()
        return drawable?.also { drawableCache[id] = it }
    }

    private fun getEmoticonUri(id: String?): String {
        id ?: return ""
        if (DEFAULT_EMOTICON_MAPPING.containsValue(id)) {
            return newAssetUri("$EMOTICON_ASSET_NAME/$id.webp")
        }
        return "http://static.tieba.baidu.com/tb/editor/images/client/$id.png"
    }

    /**
     * Save downloaded emoticon under [EMOTICON_CACHE_DIR] directory
     * */
    private fun saveEmoticons(bitmap: Bitmap, id: String) {
        queue.submit(Dispatchers.IO) {
            val emoticonFile = getEmoticonFile(id)
            try {
                if (emoticonFile.exists() && emoticonFile.length() > 0L) return@submit

                val rec = ImageUtil.bitmapToFile(bitmap, emoticonFile,100, Bitmap.CompressFormat.PNG)
                Log.d(TAG, "saveEmoticons: $rec, ID: $id, size: ${emoticonFile.length() / 1024}KiB")
            } catch (e: Exception) {
                Log.e(TAG, "saveEmoticons: save $id failed", e)
                try {
                    emoticonFile.delete()
                } catch (_: Exception) {
                }
            }
        }
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

    fun clear() {
        inlineTextCache.clear()
        queue.cancel()
        contextRef.clear()
    }
}