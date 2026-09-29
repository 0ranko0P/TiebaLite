package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.takeOrElse
import androidx.compose.ui.util.fastFirstOrNull
import com.huanchengfly.tieba.post.core.navigation.LocalNavigator
import com.huanchengfly.tieba.post.ui.common.LocalPbInlineContentCache
import com.huanchengfly.tieba.post.ui.common.PbContentRender.Companion.TAG_URL
import com.huanchengfly.tieba.post.ui.common.PbContentRender.Companion.TAG_USER
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.utils.DisplayUtil.plus
import com.huanchengfly.tieba.post.utils.DisplayUtil.sp2px
import com.huanchengfly.tieba.post.utils.launchUrl

@Composable
fun BasicPbContentText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    lineSpacing: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
) {
    val lineHeightSp = lineHeight.takeOrElse { style.lineHeight }.takeOrElse { 24.sp/* BodyLarge */}
    val mergedStyle = style.merge(
        color = color.takeOrElse { style.color.takeOrElse { LocalContentColor.current } },
        fontSize = fontSize,
        fontWeight = fontWeight,
        textAlign = textAlign ?: TextAlign.Unspecified,
        lineHeight = lineHeightSp + lineSpacing,
        fontFamily = fontFamily,
        textDecoration = textDecoration,
        fontStyle = fontStyle,
        letterSpacing = letterSpacing
    )

    BasicText(
        text = text,
        modifier = modifier,
        style = mergedStyle,
        onTextLayout = onTextLayout,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        inlineContent = LocalPbInlineContentCache.current.getCachedInlineContent(lineHeightSp.sp2px()),
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
    lineSpacing: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    BasicPbContentText(
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
        maxLines = maxLines,
        onTextLayout = {
            layoutResult = it
            onTextLayout(it)
        },
        style = style
    )
}
