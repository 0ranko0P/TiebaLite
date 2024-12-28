package com.huanchengfly.tieba.post.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.VideoViewActivity
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.LocalWindowSizeClass
import com.huanchengfly.tieba.post.models.PhotoViewData
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.WindowWidthSizeClass
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.widgets.compose.EmoticonText
import com.huanchengfly.tieba.post.ui.widgets.compose.NetworkImage
import com.huanchengfly.tieba.post.ui.widgets.compose.VoicePlayer
import com.huanchengfly.tieba.post.ui.widgets.compose.video.VideoThumbnail
import com.huanchengfly.tieba.post.utils.EmoticonUtil.emoticonString
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.launchUrl

@Stable
interface PbContentRender {
    @Composable
    fun Render()

    fun toAnnotationString(): AnnotatedString = AnnotatedString(this.toString())
}

private fun highlightContent(content: String): AnnotatedString {
    val theme by ThemeUtil.themeState
    return AnnotatedString(content, SpanStyle(fontWeight = FontWeight.Bold, color = theme.primary))
}

class PureTextContentRender(val text: String): PbContentRender {

    @Composable
    override fun Render() = Text(text = text, style = MaterialTheme.typography.body1)

    override fun toAnnotationString(): AnnotatedString = AnnotatedString(text)

    override fun toString(): String = text

    override fun equals(other: Any?): Boolean = text == other

    override fun hashCode(): Int = text.hashCode()
}

@Suppress("unused")
@Stable
data class TextContentRender(
    val text: AnnotatedString
) : PbContentRender {
    constructor(text: String) : this(AnnotatedString(text))

    override fun toString(): String {
        return text.toString()
    }

    @Composable
    override fun Render() {
        PbContentText(
            text = text,
            style = MaterialTheme.typography.body1,
            lineSpacing = 0.8.sp
        )
    }

    override fun toAnnotationString() = text

    operator fun plus(text: String): TextContentRender {
        return TextContentRender(this.text + AnnotatedString(text))
    }

    operator fun plus(text: AnnotatedString): TextContentRender {
        return TextContentRender(this.text + text)
    }

    companion object {
        fun MutableList<PbContentRender>.appendText(
            text: String
        ) {
            val lastRender = lastOrNull()
            if (lastRender is TextContentRender) {
                removeLast()
                add(lastRender + text)
            } else
                add(TextContentRender(text))
        }

        fun MutableList<PbContentRender>.appendText(
            text: AnnotatedString
        ) {
            val lastRender = lastOrNull()
            if (lastRender is TextContentRender) {
                removeLast()
                add(lastRender + text)
            } else
                add(TextContentRender(text))
        }
    }
}

@Stable
data class PicContentRender(
    val picUrl: String,
    val originUrl: String,
    val originSize: Int, // Bytes
    val dimensions: IntSize?,
    val picId: String,
    val photoViewData: PhotoViewData? = null,
) : PbContentRender {
    @Composable
    override fun Render() {
        val widthFraction =
            if (LocalWindowSizeClass.current.widthSizeClass == WindowWidthSizeClass.Compact) 1f else 0.5f

        NetworkImage(
            imageUri = picUrl,
            contentDescription = null,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .fillMaxWidth(widthFraction)
                .aspectRatio(ratio = dimensions?.run { width * 1f / height } ?: 1.0f),
            contentScale = ContentScale.Crop,
            photoViewDataProvider = { photoViewData },
        )
    }

    override fun toString(): String {
        return "[图片]"
    }
}

@Stable
data class VoiceContentRender(
    val voiceMd5: String,
    val duration: Int
) : PbContentRender {
    @Composable
    override fun Render() {
        val voiceUrl = remember(voiceMd5) {
            "https://tiebac.baidu.com/c/p/voice?voice_md5=$voiceMd5&play_from=pb_voice_play"
        }
        VoicePlayer(url = voiceUrl, duration = duration)
    }

    override fun toAnnotationString() = highlightContent(toString())

    override fun toString(): String {
        return "[语音]"
    }
}

@Stable
data class VideoContentRender(
    val videoUrl: String,
    val picUrl: String,
    val webUrl: String,
    val width: Int,
    val height: Int
) : PbContentRender {

    @OptIn(ExperimentalGlideComposeApi::class)
    @Composable
    override fun Render() {
        val widthFraction =
            if (LocalWindowSizeClass.current.widthSizeClass == WindowWidthSizeClass.Compact) 1f else 0.5f
        val navigator = LocalNavController.current
        val context = LocalContext.current

        if (picUrl.isNotBlank()) {
            val picModifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .fillMaxWidth(widthFraction)
                .aspectRatio(width * 1f / height)

            if (videoUrl.isNotBlank()) {
                VideoThumbnail(
                    modifier = picModifier,
                    thumbnailUrl = picUrl,
                    onClick = { VideoViewActivity.launch(context, videoUrl, picUrl) }
                )
            } else {
                GlideImage(
                    model  = picUrl,
                    contentDescription = stringResource(id = R.string.desc_video),
                    modifier = picModifier.clickable {
                        navigator.navigate(Destination.WebView(webUrl))
                    },
                    contentScale = ContentScale.Crop
                )
            }
        }
    }

    override fun toAnnotationString() = highlightContent(toString())

    override fun toString(): String {
        return "[视频]"
    }
}

@Composable
fun PbContentText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    lineSpacing: TextUnit = 0.sp,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
) {
    PbContentText(
        text = text.emoticonString,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        lineSpacing = lineSpacing,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        inlineContent = emptyMap(),
        onTextLayout = onTextLayout,
        style = style
    )
}

@Composable
fun PbContentText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    lineSpacing: TextUnit = 0.sp,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    inlineContent: Map<String, InlineTextContent> = emptyMap(),
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
) {
    val context = LocalContext.current
    val navigator = LocalNavController.current

    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    EmoticonText(
        text = text,
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                val change = awaitFirstDown()
                val annotation =
                    layoutResult.value?.getOffsetForPosition(change.position)?.let { offset ->
                        text.getStringAnnotations(start = offset, end = offset)
                            .firstOrNull { it.tag == "url" || it.tag == "user" }
                    }
                if (annotation != null) {
                    if (change.pressed != change.previousPressed) change.consume()
                    val up =
                        waitForUpOrCancellation()?.also { if (it.pressed != it.previousPressed) it.consume() }
                    if (up != null) {
                        when (annotation.tag) {
                            "url" -> {
                                val url = annotation.item
                                launchUrl(context, navigator, url)
                            }

                            "user" -> {
                                val uid = annotation.item.toLong()
                                navigator.navigate(Destination.UserProfile(uid))
                            }
                        }
                    }
                }
            }
        },
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        lineSpacing = lineSpacing,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        inlineContent = inlineContent,
        onTextLayout = {
            layoutResult.value = it
            onTextLayout(it)
        },
        style = style
    )
}