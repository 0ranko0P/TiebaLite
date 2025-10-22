package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
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
import androidx.compose.ui.unit.takeOrElse
import com.huanchengfly.tieba.post.dpToPxFloat
import com.huanchengfly.tieba.post.pxToSpFloat
import com.huanchengfly.tieba.post.spToPxFloat
import com.huanchengfly.tieba.post.theme.tokens.ColorSchemeKeyTokens
import com.huanchengfly.tieba.post.theme.tokens.value
import com.huanchengfly.tieba.post.utils.DisplayUtil.plus
import com.huanchengfly.tieba.post.utils.DisplayUtil.sp2px
import com.huanchengfly.tieba.post.utils.EmoticonManager
import com.huanchengfly.tieba.post.utils.EmoticonUtil.emoticonString

const val EMOTICON_SIZE_SCALE = 0.9f

@Composable
fun EmoticonText(
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
    minLines: Int = 1,
    inlineContent: Map<String, InlineTextContent>? = null,
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

    val emoticonInlineContent = EmoticonManager.getEmoticonInlineContent(lineHeightSp.sp2px(), EMOTICON_SIZE_SCALE)

    BasicText(
        text = text.emoticonString,
        modifier = modifier,
        style = mergedStyle,
        onTextLayout = onTextLayout,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        inlineContent = inlineContent?.let { emoticonInlineContent + it } ?: emoticonInlineContent,
    )
}

@Composable
fun buildChipInlineContent(
    text: String,
    padding: PaddingValues = PaddingValues(vertical = 2.dp, horizontal = 4.dp),
    textStyle: TextStyle = LocalTextStyle.current,
    chipTextStyle: TextStyle = LocalTextStyle.current,
    containerColor: ColorSchemeKeyTokens = ColorSchemeKeyTokens.Tertiary,
    color: ColorSchemeKeyTokens = ColorSchemeKeyTokens.OnTertiary
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
                    color = color.value,
                    modifier = Modifier
                        .padding(horizontal = 1.dp)
                        .fillMaxWidth()
                        .background(containerColor.value, CircleShape)
                        .padding(padding)
                )
            }
        }
    )
}