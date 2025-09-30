package com.huanchengfly.tieba.post.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastFirstOrNull
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.VideoViewActivity
import com.huanchengfly.tieba.post.models.PhotoViewData
import com.huanchengfly.tieba.post.ui.common.PbContentRender.Companion.TAG_URL
import com.huanchengfly.tieba.post.ui.common.PbContentRender.Companion.TAG_USER
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.isWindowWidthCompact
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.widgets.compose.EmoticonText
import com.huanchengfly.tieba.post.ui.widgets.compose.NetworkImage
import com.huanchengfly.tieba.post.ui.widgets.compose.VoicePlayer
import com.huanchengfly.tieba.post.ui.widgets.compose.video.VideoThumbnail
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.launchUrl

@Immutable
interface PbContentRender {
    @Composable
    fun Render()

    fun toAnnotationString(): AnnotatedString = highlightContent(toString())

    companion object {
        const val TAG_URL = "url"
        const val TAG_USER = "user"
        const val TAG_LZ = "Lz"

        const val INLINE_LINK = "link_icon"
        const val INLINE_LINK_MALICIOUS = "link_icon_malicious"
        const val INLINE_VIDEO = "video_icon"

        const val MEDIA_PICTURE = "[图片]"
        const val MEDIA_VIDEO = "[视频]"
        const val MEDIA_VOICE = "[语音]"
    }
}

private fun highlightContent(content: String): AnnotatedString {
    val colorScheme = ThemeUtil.currentColorScheme()
    return AnnotatedString(content, SpanStyle(colorScheme.primary, fontWeight = FontWeight.Bold))
}

@Immutable
@JvmInline
value class PureTextContentRender(val value: String) : PbContentRender {

    @Composable
    override fun Render() = Text(text = value, style = MaterialTheme.typography.bodyLarge)

    override fun toAnnotationString(): AnnotatedString = AnnotatedString(value)

    override fun toString(): String = value
}

@Immutable
@JvmInline
value class TextContentRender(val value: AnnotatedString) : PbContentRender {

    constructor(text: String) : this(AnnotatedString(text))

    override fun toString(): String = value.text

    @Composable
    override fun Render() {
        PbContentText(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            lineSpacing = 0.8.sp
        )
    }

    override fun toAnnotationString() = value

    operator fun plus(text: String): TextContentRender = this + AnnotatedString(text)

    operator fun plus(text: AnnotatedString): TextContentRender = TextContentRender(value + text)

    companion object {
        fun MutableList<PbContentRender>.appendText(
            text: String
        ) {
            val lastRender = lastOrNull()
            if (lastRender is TextContentRender) {
                this[lastIndex] = lastRender + text
            } else {
                add(TextContentRender(text))
            }
        }

        fun MutableList<PbContentRender>.appendText(
            text: AnnotatedString
        ) {
            val lastRender = lastOrNull()
            if (lastRender is TextContentRender) {
                this[lastIndex] = lastRender + text
            } else {
                add(TextContentRender(text))
            }
        }
    }
}

@Immutable
/*data */class PicContentRender(
    val picUrl: String,
    val originUrl: String,
    val originSize: Int, // Bytes
    val dimensions: IntSize?,
    val picId: String,
    val photoViewData: PhotoViewData? = null,
) : PbContentRender {

    @Composable
    override fun Render() {
        val widthFraction = if (isWindowWidthCompact()) 1f else 0.5f

        NetworkImage(
            imageUri = picUrl,
            contentDescription = null,
            modifier = Modifier
                .clip(shape = MaterialTheme.shapes.small)
                .fillMaxWidth(widthFraction)
                .aspectRatio(ratio = dimensions?.run { width * 1f / height } ?: 1.0f),
            contentScale = ContentScale.Crop,
            photoViewDataProvider = { photoViewData },
        )
    }

    fun copy(
        picUrl: String  = this.picUrl,
        originUrl: String = this.originUrl,
        originSize: Int = this.originSize,
        dimensions: IntSize? = this.dimensions,
        picId: String = this.picId,
        photoViewData: PhotoViewData? = this.photoViewData,
    ): PicContentRender {
        return PicContentRender(picUrl,  originUrl, originSize, dimensions, picId, photoViewData)
    }

    override fun toString(): String = PbContentRender.MEDIA_PICTURE
}

@Immutable
class VoiceContentRender(
    val voiceMd5: String,
    val duration: Int
) : PbContentRender {
    @Composable
    override fun Render() {
        val voiceUrl = remember {
            "https://tiebac.baidu.com/c/p/voice?voice_md5=$voiceMd5&play_from=pb_voice_play"
        }
        VoicePlayer(url = voiceUrl, duration = duration)
    }

    override fun toString(): String = PbContentRender.MEDIA_VOICE
}

@Immutable
class VideoContentRender(
    val videoUrl: String,
    val picUrl: String,
    val webUrl: String,
    val dimensions: IntSize?
) : PbContentRender {

    init {
        require(picUrl.isNotBlank() && picUrl.isNotEmpty()) { "Invalid video cover url" }
    }

    @OptIn(ExperimentalGlideComposeApi::class)
    @Composable
    override fun Render() {
        val widthFraction = if (isWindowWidthCompact()) 1f else 0.5f

        val picModifier = Modifier
            .fillMaxWidth(widthFraction)
            .aspectRatio(ratio = dimensions?.run { width * 1f / height } ?: 1.0f)
            .clip(shape = MaterialTheme.shapes.small)

        if (videoUrl.isNotBlank()) {
            val context = LocalContext.current
            VideoThumbnail(
                modifier = picModifier,
                thumbnailUrl = picUrl,
                onClick = { VideoViewActivity.launch(context, videoUrl, picUrl) }
            )
        } else {
            val navigator = LocalNavController.current
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

    override fun toString(): String = PbContentRender.MEDIA_VIDEO
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
    inlineContent: Map<String, InlineTextContent>? = null,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
) {
    val context = LocalContext.current
    val navigator = LocalNavController.current

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    EmoticonText(
        text = text,
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                val change = awaitFirstDown()
                val annotation =
                    layoutResult?.getOffsetForPosition(change.position)?.let { offset ->
                        text.getStringAnnotations(start = offset, end = offset)
                            .fastFirstOrNull { it.tag == TAG_URL || it.tag == TAG_USER }
                    }
                if (annotation != null) {
                    if (change.pressed != change.previousPressed) change.consume()
                    val up =
                        waitForUpOrCancellation()?.also { if (it.pressed != it.previousPressed) it.consume() }
                    if (up != null) {
                        when (annotation.tag) {
                            TAG_URL -> {
                                val url = annotation.item
                                launchUrl(context, navigator, url)
                            }

                            TAG_USER -> {
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
            layoutResult = it
            onTextLayout(it)
        },
        style = style
    )
}