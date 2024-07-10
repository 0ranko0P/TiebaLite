package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.panpf.sketch.compose.AsyncImage
import com.github.panpf.sketch.fetch.newAssetUri
import com.github.panpf.sketch.fetch.newFileUri
import com.github.panpf.sketch.request.LoadRequest
import com.github.panpf.sketch.request.LoadResult
import com.github.panpf.sketch.request.execute
import com.google.accompanist.drawablepainter.rememberDrawablePainter
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
    private val emoticonMapping: MutableMap<String, String> = mutableMapOf()
    private val drawableCache: MutableMap<String, Drawable> = mutableMapOf()
    private val scope = CoroutineScope(Dispatchers.Main + CoroutineName(TAG))

    fun getEmoticonInlineContent(
        sizePx: Float
    ): Map<String, InlineTextContent> {
        return emoticonIds.associate { id ->
            "Emoticon#$id" to InlineTextContent(
                placeholder = Placeholder(
                    sizePx.pxToSp().sp,
                    sizePx.pxToSp().sp,
                    PlaceholderVerticalAlign.TextCenter
                ),
                children = {
                    AsyncImage(
                        imageUri = rememberEmoticonUri(id = id),
                        contentDescription = stringResource(
                            id = R.string.emoticon,
                            getEmoticonNameById(id) ?: ""
                        ),
                        modifier = Modifier.size(sizePx.pxToDp().dp)
                    )
                }
            )
        }
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
        fetchEmoticons(context)
    }

    private suspend fun updateCache() = withContext(Dispatchers.IO) {
        runCatching {
            val emoticonDataCacheFile = File(EMOTICON_CACHE_DIR, "emoticon_data_cache")
            val emoticonJson = EmoticonCache(emoticonIds, emoticonMapping).toJson()
            FileUtil.writeFile(emoticonDataCacheFile, emoticonJson, false)
        }
    }

    private fun getContext(): Context {
        return contextRef.get() ?: App.INSTANCE
    }

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

    fun getEmoticonIdByName(name: String): String? {
        return emoticonMapping[name]
    }

    private fun getEmoticonNameById(id: String): String? {
        return emoticonMapping.firstNotNullOfOrNull { (name, emoticonId) ->
            if (emoticonId == id) {
                name
            } else {
                null
            }
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
        val emoticonFile = getEmoticonFile(id)
        if (!emoticonFile.exists()) {
            return ""
        }
        return newFileUri(emoticonFile)
    }

    @Composable
    fun rememberEmoticonPainter(id: String): Painter {
        val context = LocalContext.current
        val drawable = remember(id) { getEmoticonDrawable(context, id) }
        return rememberDrawablePainter(drawable = drawable)
    }

    @Composable
    fun rememberEmoticonUri(id: String): String = remember(id) { getEmoticonUri(id) }

    private suspend fun fetchEmoticons(context: Context) {
        emoticonIds.forEach {
            if (DEFAULT_EMOTICON_MAPPING.containsValue(it)) return@forEach

            val emoticonFile = getEmoticonFile(it)
            if (!emoticonFile.exists()) {
                val loadEmoticonResult =
                    withContext(Dispatchers.IO) {
                        LoadRequest(
                            context,
                            "http://static.tieba.baidu.com/tb/editor/images/client/$it.png"
                        ).execute()
                    }
                if (loadEmoticonResult is LoadResult.Success) {
                    ImageUtil.bitmapToFile(
                        loadEmoticonResult.bitmap,
                        emoticonFile,
                        Bitmap.CompressFormat.PNG
                    )
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
            scope.launch {
                updateCache()
            }
        }
    }
}