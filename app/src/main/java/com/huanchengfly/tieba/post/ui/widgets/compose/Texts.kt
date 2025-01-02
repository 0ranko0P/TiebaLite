package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.dpToPxFloat
import com.huanchengfly.tieba.post.pxToSpFloat
import com.huanchengfly.tieba.post.spToPxFloat
import com.huanchengfly.tieba.post.ui.common.PbContentText
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.utils.EmoticonManager
import com.huanchengfly.tieba.post.utils.EmoticonManager.calcLineHeightPx
import com.huanchengfly.tieba.post.utils.EmoticonUtil.emoticonString
import java.util.regex.Pattern

const val EMOTICON_SIZE_SCALE = 0.9f

@Composable
fun EmoticonText(
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
    val emoticonString = remember(key1 = text) { text.emoticonString }

    EmoticonText(
        emoticonString,
        modifier,
        color,
        fontSize,
        fontStyle,
        fontWeight,
        fontFamily,
        letterSpacing,
        textDecoration,
        textAlign,
        lineHeight,
        lineSpacing,
        overflow,
        softWrap,
        maxLines,
        minLines,
        inlineContent = emptyMap(),
        onTextLayout = onTextLayout,
        style = style
    )
}

@Composable
fun EmoticonText(
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
    style: TextStyle = LocalTextStyle.current
) {
    val textColor = color.takeOrElse {
        style.color.takeOrElse {
            LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
        }
    }
    val mergedStyle = style.merge(
        TextStyle(
            color = textColor,
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = textAlign ?: TextAlign.Unspecified,
            lineHeight = lineHeight,
            fontFamily = fontFamily,
            textDecoration = textDecoration,
            fontStyle = fontStyle,
            letterSpacing = letterSpacing
        )
    )
    val sizePx = calcLineHeightPx(mergedStyle)
    val spacingLineHeight = (sizePx + lineSpacing.value.spToPxFloat()).pxToSpFloat().sp
    val emoticonInlineContent = EmoticonManager.getEmoticonInlineContent(sizePx, EMOTICON_SIZE_SCALE)

    Text(
        text.emoticonString,
        modifier,
        color,
        fontSize,
        fontStyle,
        fontWeight,
        fontFamily,
        letterSpacing,
        textDecoration,
        textAlign,
        spacingLineHeight,
        overflow,
        softWrap,
        maxLines,
        minLines,
        inlineContent?.let { emoticonInlineContent + it} ?: emoticonInlineContent,
        onTextLayout,
        style
    )
}

@Composable
fun buildChipInlineContent(
    text: String,
    padding: PaddingValues = PaddingValues(vertical = 2.dp, horizontal = 4.dp),
    textStyle: TextStyle = LocalTextStyle.current,
    chipTextStyle: TextStyle = LocalTextStyle.current,
    backgroundColor: Color = ExtendedTheme.colors.chip,
    color: Color = ExtendedTheme.colors.onChip
): InlineTextContent {
    val textMeasurer = rememberTextMeasurer()
    val textSize = remember(text, textStyle) { textMeasurer.measure(text, textStyle).size }
    val heightPx = textSize.height
    val heightSp = heightPx.pxToSpFloat().sp
    val textHeightPx = textStyle.fontSize.value.spToPxFloat() -
            padding.calculateTopPadding().value.dpToPxFloat() -
            padding.calculateBottomPadding().value.dpToPxFloat()
    val fontSize = textHeightPx.pxToSpFloat().sp
    val textWidthPx = textSize.width
    val widthPx = textWidthPx +
            padding.calculateStartPadding(LocalLayoutDirection.current).value.dpToPxFloat() +
            padding.calculateEndPadding(LocalLayoutDirection.current).value.dpToPxFloat()
    val widthSp = widthPx.pxToSpFloat().sp
    return InlineTextContent(
        placeholder = Placeholder(
            width = widthSp,
            height = heightSp,
            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
        ),
        children = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = it.takeIf { it.isNotBlank() && it != "\uFFFD" } ?: text,
                    style = chipTextStyle.copy(
                        fontSize = fontSize,
                        lineHeight = fontSize,
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both
                        )
                    ),
                    textAlign = TextAlign.Center,
                    color = color,
                    modifier = Modifier
                        .padding(horizontal = 1.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(100))
                        .background(backgroundColor)
                        .padding(padding)
                )
            }
        }
    )
}

@Composable
fun HighlightText(
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
    highlightKeywords: List<String> = emptyList(),
    highlightColor: Color = ExtendedTheme.colors.primary,
    highlightStyle: TextStyle = style,
) {
    HighlightText(
        text = AnnotatedString(text),
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
        onTextLayout = onTextLayout,
        style = style,
        highlightKeywords = highlightKeywords,
        highlightColor = highlightColor,
        highlightStyle = highlightStyle,
    )
}

@Composable
fun HighlightText(
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
    highlightKeywords: List<String> = emptyList(),
    highlightColor: Color = ExtendedTheme.colors.primary,
    highlightStyle: TextStyle = style,
) {
    val mergedHighlightStyle = remember(highlightStyle, highlightColor) {
        highlightStyle.copy(color = highlightColor)
    }
    val highlightText = remember(text, highlightKeywords) {
        if (highlightKeywords.isEmpty()) {
            text
        } else {
            buildAnnotatedString {
                append(text)
                highlightKeywords.forEach { keyword ->
                    val regexPattern = keyword.toPattern(Pattern.CASE_INSENSITIVE)
                    val matcher = regexPattern.matcher(text.text)
                    while (matcher.find()) {
                        val start = matcher.start()
                        val end = matcher.end()
                        addStyle(
                            mergedHighlightStyle.toSpanStyle(),
                            start,
                            end
                        )
                    }
                }
            }
        }
    }
    PbContentText(
        text = highlightText,
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
        inlineContent = inlineContent,
        onTextLayout = onTextLayout,
        style = style,
    )
}