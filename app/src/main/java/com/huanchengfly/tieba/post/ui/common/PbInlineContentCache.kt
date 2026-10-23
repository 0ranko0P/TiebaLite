package com.huanchengfly.tieba.post.ui.common

import androidx.collection.LruCache
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.core.designsystem.theme.RedA700
import com.huanchengfly.tieba.post.pxToDp
import com.huanchengfly.tieba.post.pxToSp
import com.huanchengfly.tieba.post.ui.widgets.compose.EmoticonInlineImage
import com.huanchengfly.tieba.post.ui.widgets.compose.PbContentText
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberChipInlineContent
import com.huanchengfly.tieba.post.utils.EmoticonManager
import com.huanchengfly.tieba.post.utils.EmoticonUtil
import com.huanchengfly.tieba.post.core.media.R as mediaUiR

val LocalPbInlineContentCache = staticCompositionLocalOf<PbInlineContentCache> { error("No PbInlineContentCache provided!") }

enum class PbInlineType {
    EMOTICON, LINK, LINK_MALICIOUS, VIDEO, LZ,
}

/**
 * Simple [InlineTextContent] cache holder for [PbContentText].
 * */
class PbInlineContentCache private constructor(
    maxCacheSize: Int = 4,
) {

    private val cache = LruCache<Int, Map<String, InlineTextContent>>(maxCacheSize)

    private var lzInlineContent: InlineTextContent? = null

    fun getCachedInlineContent(sizePx: Int): Map<String, InlineTextContent> {
        var inlineContent = cache[sizePx]
        if (inlineContent == null) {
            inlineContent = buildIconInlineContent(sizePx) +
                    buildEmoticonInlineContent(sizePx) +
                    buildLzInlineContent()
            cache.put(sizePx, inlineContent)
        }
        return inlineContent
    }

    private fun buildLzInlineContent(): Map<String, InlineTextContent> {
        return lzInlineContent?.let { mapOf(PbInlineType.LZ.name to it) } ?: emptyMap()
    }

    fun dispose() {
        cache.evictAll()
    }

    companion object {

        /**
         * Emoticon size scale factor
         *
         * @since 4.0.0 alpha 14 (emoticonSize)
         * */
        private const val EMOTICON_SIZE_SCALE = 0.9f

        private fun buildEmoticonInlineContent(sizePx: Int): Pair<String, InlineTextContent> {
            val size = (sizePx * EMOTICON_SIZE_SCALE).pxToSp()
            return PbInlineType.EMOTICON.name to InlineTextContent(
                placeholder = Placeholder(size.sp, size.sp, PlaceholderVerticalAlign.TextCenter),
                children = {
                    val id = EmoticonManager.getEmoticonIdByName(EmoticonUtil.inlineTextToName(it))
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmoticonInlineImage(
                            id = id ?: "",
                            size = sizePx.pxToDp().dp,
                            onError = id?.let {
                                { err -> EmoticonManager.onLoadError(id, err.result.throwable) }
                            }
                        )
                    }
                }
            )
        }

        private fun buildIconInlineContent(sizePx: Int): Map<String, InlineTextContent> {
            val sizeSp = (sizePx * 9 / 10).pxToSp().sp
            val sizeDp = sizePx.pxToDp().dp
            val placeholder = Placeholder(sizeSp, sizeSp, PlaceholderVerticalAlign.TextCenter)

            return mapOf(
                PbInlineType.LINK.name to InlineTextContent(placeholder = placeholder) {
                    Icon(
                        imageVector = Icons.Rounded.Link,
                        contentDescription = stringResource(id = R.string.link),
                        modifier = Modifier.size(sizeDp),
                        tint = MaterialTheme.colorScheme.primaryContainer,
                    )
                },
                PbInlineType.LINK_MALICIOUS.name to InlineTextContent(placeholder = placeholder) {
                    Icon(
                        imageVector = Icons.Rounded.Report,
                        contentDescription = stringResource(id = R.string.link),
                        modifier = Modifier.size(sizeDp),
                        tint = RedA700,
                    )
                },
                PbInlineType.VIDEO.name to InlineTextContent(placeholder = placeholder) {
                    Icon(
                        imageVector = Icons.Rounded.SlowMotionVideo,
                        contentDescription = stringResource(id = mediaUiR.string.desc_video),
                        modifier = Modifier.size(sizeDp),
                        tint = MaterialTheme.colorScheme.primaryContainer,
                    )
                },
            )
        }

        /** Create and [remember] a [PbInlineContentCache] */
        @Composable
        fun rememberPbInlineContentCache(maxCacheSize: Int = 4): PbInlineContentCache {
            val cache = remember { PbInlineContentCache(maxCacheSize) }
            val lzInlineContent = rememberChipInlineContent(
                text = stringResource(id = R.string.tip_lz),
                textStyle = MaterialTheme.typography.labelMedium
            )

            SideEffect {
                cache.lzInlineContent = lzInlineContent
            }

            DisposableEffect(lzInlineContent) {
                onDispose(cache::dispose)
            }
            return cache
        }
    }
}