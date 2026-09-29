package com.huanchengfly.tieba.post.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.VideoViewActivity
import com.huanchengfly.tieba.post.components.media.MediaCache.getBdMediaId
import com.huanchengfly.tieba.post.models.PhotoViewData
import com.huanchengfly.tieba.post.core.navigation.LocalNavigator
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.isWindowWidthCompact
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedVideoShutter
import com.huanchengfly.tieba.post.ui.widgets.compose.NetworkImage
import com.huanchengfly.tieba.post.ui.widgets.compose.PbContentText
import com.huanchengfly.tieba.post.ui.widgets.compose.VoicePlayer
import com.huanchengfly.tieba.post.ui.widgets.compose.singleMediaFraction
import com.huanchengfly.tieba.post.ui.widgets.compose.video.LocalVideoPreviewState
import com.huanchengfly.tieba.post.utils.ThemeUtil

@Immutable
interface PbContentRender {
    @Composable
    fun Render()

    fun toAnnotationString(): AnnotatedString = highlightContent(toString())

    companion object {
        const val TAG_URL = "url"
        const val TAG_USER = "user"

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
        NetworkImage(
            modifier = Modifier
                .clip(shape = MaterialTheme.shapes.small)
                .fillMaxWidth(singleMediaFraction)
                .aspectRatio(ratio = dimensions?.run { width * 1f / height } ?: 1.0f),
            imageUrl = picUrl,
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

    private val mediaId: String = videoUrl.toUri().getBdMediaId()

    init {
        require(picUrl.isNotBlank() && picUrl.isNotEmpty()) { "Invalid video cover url" }
    }

    @Composable
    override fun Render() {
        val widthFraction = if (isWindowWidthCompact()) 1f else 0.5f

        val picModifier = Modifier
            .fillMaxWidth(widthFraction)
            .aspectRatio(ratio = dimensions?.run { width * 1f / height } ?: 1.0f)
            .clip(shape = MaterialTheme.shapes.small)

        if (videoUrl.isNotBlank()) {
            val context = LocalContext.current
            val previewState = LocalVideoPreviewState.current
            FeedVideoShutter(
                modifier = picModifier.clickableNoIndication {
                    VideoViewActivity.launch(context, videoUrl, picUrl)
                },
                thumbnailUrl = picUrl,
                isPipMode = previewState?.videoViewMediaId == mediaId && previewState.isInPipMode
            )
        } else {
            val navigator = LocalNavigator.current
            AsyncImage(
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