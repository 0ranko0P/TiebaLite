package com.huanchengfly.tieba.post.api.models.protos

import android.net.Uri
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.components.ClipBoardLinkDetector.isTieba
import com.huanchengfly.tieba.post.theme.RedA700
import com.huanchengfly.tieba.post.ui.common.PbContentRender
import com.huanchengfly.tieba.post.ui.common.PbContentRender.Companion.INLINE_LINK
import com.huanchengfly.tieba.post.ui.common.PbContentRender.Companion.INLINE_LINK_MALICIOUS
import com.huanchengfly.tieba.post.ui.common.PbContentRender.Companion.INLINE_VIDEO
import com.huanchengfly.tieba.post.ui.common.PbContentRender.Companion.TAG_LZ
import com.huanchengfly.tieba.post.ui.common.PbContentRender.Companion.TAG_URL
import com.huanchengfly.tieba.post.ui.common.PbContentRender.Companion.TAG_USER
import com.huanchengfly.tieba.post.ui.common.PicContentRender
import com.huanchengfly.tieba.post.ui.common.PureTextContentRender
import com.huanchengfly.tieba.post.ui.common.TextContentRender.Companion.appendText
import com.huanchengfly.tieba.post.ui.common.VideoContentRender
import com.huanchengfly.tieba.post.ui.common.VoiceContentRender
import com.huanchengfly.tieba.post.ui.utils.getPhotoViewData
import com.huanchengfly.tieba.post.utils.EmoticonManager
import com.huanchengfly.tieba.post.utils.EmoticonUtil
import com.huanchengfly.tieba.post.utils.EmoticonUtil.emoticonString
import com.huanchengfly.tieba.post.utils.ImageUtil
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.ThemeUtil
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

private val abstractTextPattern by lazy { Regex(" {2,}") }

val List<Abstract>.abstractText: String
    get() = joinToString(separator = "") {
        when (it.type) {
            0 -> it.text.replace(abstractTextPattern, " ")
            4 -> it.text

            else -> ""
        }
    }

val ThreadInfo.abstractText: String
    get() = richAbstract.joinToString(separator = "") {
        when (it.type) {
            0 -> it.text.replace(abstractTextPattern, " ")
            2 -> {
                EmoticonManager.registerEmoticon(it.text, it.c)
                EmoticonUtil.inlineTextFormat(name = it.c)
            }

            else -> ""
        }
    }

val PostInfoList.abstractText: String
    get() = rich_abstract.joinToString(separator = "") {
        when (it.type) {
            0 -> it.text.replace(abstractTextPattern, " ")
            2 -> {
                EmoticonManager.registerEmoticon(it.text, it.c)
                EmoticonUtil.inlineTextFormat(name = it.c)
            }

            else -> ""
        }
    }

val ThreadInfo.hasAgree: Int
    get() = agree?.hasAgree ?: 0

fun ThreadInfo.updateAgreeStatus(
    hasAgree: Int
) = if (agree != null) {
    if (hasAgree != agree.hasAgree) {
        if (hasAgree == 1) {
            copy(
                agreeNum = agreeNum + 1,
                agree = agree.copy(
                    agreeNum = agree.agreeNum + 1,
                    diffAgreeNum = agree.diffAgreeNum + 1,
                    hasAgree = 1
                )
            )
        } else {
            copy(
                agreeNum = agreeNum - 1,
                agree = agree.copy(
                    agreeNum = agree.agreeNum - 1,
                    diffAgreeNum = agree.diffAgreeNum - 1,
                    hasAgree = 0
                )
            )
        }
    } else {
        this
    }
} else {
    copy(
        agreeNum = if (hasAgree == 1) agreeNum + 1 else agreeNum - 1
    )
}

fun PostInfoList.updateAgreeStatus(
    hasAgree: Int,
) = if (agree != null) {
    if (hasAgree != agree.hasAgree) {
        if (hasAgree == 1) {
            copy(
                agree = agree.copy(
                    agreeNum = agree.agreeNum + 1,
                    diffAgreeNum = agree.diffAgreeNum + 1,
                    hasAgree = 1
                ),
                agree_num = agree_num + 1
            )
        } else {
            copy(
                agree = agree.copy(
                    agreeNum = agree.agreeNum - 1,
                    diffAgreeNum = agree.diffAgreeNum - 1,
                    hasAgree = 0
                ),
                agree_num = agree_num - 1
            )
        }
    } else {
        this
    }
} else {
    this
}

private val PbContent.picUrl: String
    get() = ImageUtil.getThumbnail(
        // originSrc, // Best  quality in [PbContent]
        bigCdnSrc,
        cdnSrc        // Worst quality in [PbContent]
    )

val List<PbContent>.plainText: String
    get() {
        val builder = StringBuilder()
        var text: String

        forEach {
            text = when (it.type) {
                in PureTextType -> it.text

                1 -> "[${it.link}]"

                2 -> EmoticonUtil.inlineTextFormat(name = it.c)

                3, 20 -> PbContentRender.MEDIA_PICTURE

                5 -> {
                    if (it.src.isNotBlank()) PbContentRender.MEDIA_VIDEO else PbContentRender.MEDIA_VIDEO + it.text
                }

                10 -> PbContentRender.MEDIA_VOICE

                else -> it.text
            }
            builder.append(text).append('\n')
        }

        return builder.toString()
    }

val List<PbContent>.plainTexts: List<String>
    get() = mapNotNull { it.text.takeUnless { t -> t.isEmpty() } }

fun PbContent.getPicSize(): IntSize? {
    try {
        if (bsize.isEmpty()) throw IllegalArgumentException("Not a Image PbContent! type: $type")

        return bsize.split(",")
            .map { it.toIntOrNull() ?: throw NumberFormatException("Not a number $it") }
            .let { IntSize(width = it[0], height = it[1]) }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

private val PureTextType = setOf(0, 9, 27, 40)

// 显示为贴吧链接实际是外部链接, 这种情况应直接标记为恶意链接
private fun isMaliciousLink(linkPbContent: PbContent): Boolean {
    return if (Uri.parse(linkPbContent.text).isTieba()) {
        !Uri.parse(linkPbContent.link).isTieba()
    } else {
        false
    }
}

val List<PbContent>.renders: ImmutableList<PbContentRender>
    get() {
        val pureText = fastFirstOrNull { it.type !in PureTextType } == null
        if (pureText) {
            return fastMap { PureTextContentRender(it.text) }.toImmutableList()
        }
        // 富文本 Render
        val renders = mutableListOf<PbContentRender>()
        val currentColorScheme = ThemeUtil.currentColorScheme()
        val highLightStyle = SpanStyle(color = currentColorScheme.primary)
        val redHighLightStyle = SpanStyle(color = RedA700)

        fastForEach {
            when (it.type) {
                in PureTextType -> renders.appendText(it.text)

                1 -> {
                    val text = if (isMaliciousLink(it)) {
                        buildAnnotatedString {
                            appendInlineContent(INLINE_LINK_MALICIOUS, alternateText = "🔗")
                            // Display actual link when it's malicious
                            withAnnotation(tag = TAG_URL, annotation = it.link) {
                                withStyle(redHighLightStyle) { append(it.link) }
                            }
                        }
                    } else {
                        buildAnnotatedString {
                            appendInlineContent(INLINE_LINK, alternateText = "🔗")
                            withAnnotation(tag = TAG_URL, annotation = it.link) {
                                withStyle(highLightStyle) { append(it.text) }
                            }
                        }
                    }
                    renders.appendText(text)
                }

                2 -> {
                    EmoticonManager.registerEmoticon(it.text, it.c)
                    val emoticonText = EmoticonUtil.inlineTextFormat(name = it.c).emoticonString
                    renders.appendText(emoticonText)
                }

                3 -> {
                    renders.add(
                        PicContentRender(
                            picUrl = it.picUrl,
                            originUrl = it.originSrc,
                            originSize = it.originSize,
                            dimensions = it.getPicSize(),
                            picId = ImageUtil.getPicId(it.originSrc),
                        )
                    )
                }

                4 -> {
                    val text = buildAnnotatedString {
                        withAnnotation(tag = TAG_USER, annotation = "${it.uid}") {
                            withStyle(highLightStyle) {
                                append(it.text)
                            }
                        }
                    }
                    renders.appendText(text)
                }

                5 -> {
                    if (it.src.isNotBlank()) {
                        renders.add(
                            VideoContentRender(
                                videoUrl = it.link,
                                picUrl = it.src,
                                webUrl = it.text,
                                dimensions = it.getPicSize()
                            )
                        )
                    } else {
                        val text = buildAnnotatedString {
                            appendInlineContent(INLINE_VIDEO, alternateText = "🎥")
                            withAnnotation(tag = TAG_URL, annotation = it.text) {
                                withStyle(highLightStyle) {
                                    append(PbContentRender.MEDIA_VIDEO)
                                    append(it.text)
                                }
                            }
                        }
                        renders.appendText(text)
                    }
                }

                10 -> {
                    renders.add(VoiceContentRender(it.voiceMD5, it.duringTime))
                }

                20 -> {
                    renders.add(
                        PicContentRender(
                            picUrl = it.src,
                            originUrl = it.src,
                            originSize = it.originSize,
                            dimensions = it.getPicSize(),
                            picId = ImageUtil.getPicId(it.src)
                        )
                    )
                }
            }
        }

        return renders.toImmutableList()
    }

val Post.contentRenders: ImmutableList<PbContentRender>
    get() {
        val renders = content.renders

        return renders.map {
            if (it is PicContentRender) {
                it.copy(photoViewData = getPhotoViewData(post = this, it))
            } else it
        }.toImmutableList()
    }

val User.bawuType: String?
    get() = if (is_bawu == 1) {
        if (bawu_type == "manager") "吧主" else "小吧主"
    } else null

@OptIn(ExperimentalTextApi::class)
fun SubPostList.getContentText(isLz: Boolean): AnnotatedString {
    val context = App.INSTANCE
    val currentColorScheme = ThemeUtil.currentColorScheme()
    val userNameStyle = SpanStyle(color = currentColorScheme.primary, fontWeight = FontWeight.Bold)

    val userNameString = buildAnnotatedString {
        withAnnotation("user", "${author?.id}") {
            withStyle(userNameStyle) {
                append(
                    StringUtil.getUserNameString(context, author?.name ?: "", author?.nameShow)
                )
            }
            if (isLz) {
                appendInlineContent(TAG_LZ)
            }
            append(": ")
        }
    }

    val contentStrings = content.renders.map { it.toAnnotationString() }

    return userNameString + contentStrings.reduce { acc, annotatedString -> acc + annotatedString }
}

fun VideoInfo.aspectRatio(): Float = thumbnailWidth.toFloat() / thumbnailHeight
