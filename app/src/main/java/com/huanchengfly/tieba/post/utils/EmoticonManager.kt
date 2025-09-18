package com.huanchengfly.tieba.post.utils

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.bumptech.glide.integration.compose.placeholder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.ControlledRunner
import com.huanchengfly.tieba.post.fromJson
import com.huanchengfly.tieba.post.models.EmoticonCache
import com.huanchengfly.tieba.post.pxToDp
import com.huanchengfly.tieba.post.pxToSp
import com.huanchengfly.tieba.post.toJson
import com.huanchengfly.tieba.post.ui.common.PbContentRender
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future

data class Emoticon(
    val id: String,
    val name: String
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

    private lateinit var contextRef: WeakReference<Context>
    private val emoticonIds: MutableSet<String> = hashSetOf()

    private val inlineTextCache = HashMap<Int, WeakReference<Map<String, InlineTextContent>>>()

    private val lineHeightCache = HashMap<TextStyle, Int>(4)

    private val emoticonMapping: MutableMap<String, String> = ConcurrentHashMap()

    private val scope = CoroutineScope(Dispatchers.Main + CoroutineName(TAG) + SupervisorJob())

    private val cacheUpdateRunner = ControlledRunner<Unit>()

    fun getEmoticonInlineContent(sizePx: Int, emoticonScale: Float): Map<String, InlineTextContent> {
        val size = (sizePx * emoticonScale).pxToSp()
        val cached = inlineTextCache[size]?.get()
        if (cached == null) {
            val placeholder = Placeholder(size.sp, size.sp, PlaceholderVerticalAlign.TextCenter)
            return emoticonIds.associate { id ->
                "Emoticon#$id" to InlineTextContent(
                    placeholder = placeholder,
                    children = { EmoticonInlineImage(Modifier.size(sizePx.pxToDp().dp), id) }
                )
            }
            .plus(map = getIconInlineContent(sizePx))
            .apply { inlineTextCache[size] = WeakReference(this) }
        } else {
            return cached
        }
    }

    fun getIconInlineContent(sizePx: Int): Map<String, InlineTextContent> {
        val sizeSp = (sizePx * 9 / 10).pxToSp().sp
        val sizeDp = sizePx.pxToDp().dp
        val placeholder = Placeholder(sizeSp, sizeSp, PlaceholderVerticalAlign.TextCenter)

        return mapOf(
            PbContentRender.INLINE_LINK to InlineTextContent(placeholder = placeholder) {
                Icon(
                    imageVector = Icons.Rounded.Link,
                    contentDescription = stringResource(id = R.string.link),
                    modifier = Modifier.size(sizeDp),
                    tint = MaterialTheme.colorScheme.primaryContainer,
                )
            },
            PbContentRender.INLINE_LINK_MALICIOUS to InlineTextContent(placeholder = placeholder) {
                Icon(
                    imageVector = Icons.Rounded.Report,
                    contentDescription = stringResource(id = R.string.link),
                    modifier = Modifier.size(sizeDp),
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            PbContentRender.INLINE_VIDEO to InlineTextContent(placeholder = placeholder) {
                Icon(
                    imageVector = Icons.Rounded.SlowMotionVideo,
                    contentDescription = stringResource(id = R.string.desc_video),
                    modifier = Modifier.size(sizeDp),
                    tint = MaterialTheme.colorScheme.primaryContainer,
                )
            }
        )
    }

    @OptIn(ExperimentalGlideComposeApi::class)
    @Composable
    fun EmoticonInlineImage(modifier: Modifier = Modifier, id: String, description: String = id) {
        val uri = remember { getEmoticonUri(id) }
        GlideImage(
            model = uri,
            contentDescription = description,
            modifier = modifier,
            failure = placeholder(R.drawable.ic_error)
        ) {
            // Disable disk cache for asset emoticon
            if (uri.startsWith("file")) it.diskCacheStrategy(DiskCacheStrategy.NONE) else it
        }
    }

    fun init(context: Application) = scope.launch {
        contextRef = WeakReference(context)
        val emoticonCache = getEmoticonDataCache()
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

        if (firstInit) {
            updateCache()
        }
    }

    private fun updateCache() {
        scope.launch {
            inlineTextCache.clear()
            cacheUpdateRunner.cancelPreviousThenRun {
                // Limit update rate to 1/min to avoid unnecessary disk writes
                delay(60000L)
                val emoticonJson = EmoticonCache(emoticonIds, emoticonMapping).toJson()
                val emoticonDataCacheFile = File(EMOTICON_CACHE_DIR, "emoticon_data_cache")
                ensureActive()
                withContext(Dispatchers.IO) {
                    FileUtil.writeFile(emoticonDataCacheFile, emoticonJson, false)
                }
            }
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
        return emoticonMapping.mapNotNull { (name, id) ->
            if (name.isEmpty()) null else Emoticon(id = id, name = name)
        }
    }

    fun getEmoticonIdByName(name: String): String? = emoticonMapping[name]

    private fun getEmoticonUri(id: String): String {
        return if (DEFAULT_EMOTICON_MAPPING.containsValue(id)) {
            "file:///android_asset/$EMOTICON_ASSET_NAME/$id.webp"
        } else {
            "http://static.tieba.baidu.com/tb/editor/images/client/$id.png"
        }
    }

    fun getEmoticonBitmap(id: String, size: Int): Future<Bitmap> {
        val uri = getEmoticonUri(id)

        return Glide.with(getContext())
            .asBitmap()
            .diskCacheStrategy(
                // Disable disk cache for asset emoticon
                if (uri.startsWith("file")) DiskCacheStrategy.NONE else DiskCacheStrategy.AUTOMATIC
            )
            .load(uri)
            .fallback(R.drawable.ic_chrome) // Null ID
            .submit(size, size)
    }

    fun registerEmoticon(id: String, name: String) {
        if (!(id.startsWith(EMOTICON_ID_PREFIX) || id.startsWith(EMOTICON_ID_PREFIX2))) {
            return
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