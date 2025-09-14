package com.huanchengfly.tieba.post.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withAnnotation
import org.intellij.lang.annotations.RegExp
import java.util.regex.Pattern

object EmoticonUtil {
    const val EMOTICON_ALL_TYPE = 0
    const val EMOTICON_CLASSIC_TYPE = 1
    const val EMOTICON_EMOJI_TYPE = 2
    const val EMOTICON_ALL_WEB_TYPE = 3
    const val EMOTICON_CLASSIC_WEB_TYPE = 4
    const val EMOTICON_EMOJI_WEB_TYPE = 5
    const val INLINE_CONTENT_TAG = "androidx.compose.foundation.text.inlineContent"

    private val ALL_TYPE_REGEX_PATTERN: Pattern by lazy { Pattern.compile(REGEX) }

    private val ALL_WEB_TYPE_REGEX_PATTERN: Pattern by lazy { Pattern.compile(REGEX_WEB) }

    @RegExp
    private const val REGEX_WEB = "\\(#(([\u4e00-\u9fa5\\w\u007e])+)\\)"

    @RegExp
    private const val REGEX = "#\\((([一-龥\\w~])+)\\)"

    fun getRegexPattern(type: Int): Pattern = when (type) {
        EMOTICON_ALL_TYPE, EMOTICON_CLASSIC_TYPE, EMOTICON_EMOJI_TYPE -> ALL_TYPE_REGEX_PATTERN
        EMOTICON_ALL_WEB_TYPE, EMOTICON_CLASSIC_WEB_TYPE, EMOTICON_EMOJI_WEB_TYPE -> ALL_WEB_TYPE_REGEX_PATTERN
        else -> ALL_TYPE_REGEX_PATTERN
    }

    val String.emoticonString: AnnotatedString
        get() {
            val regexPattern = getRegexPattern(EMOTICON_ALL_TYPE)
            val matcher = regexPattern.matcher(this)
            return buildAnnotatedString {
                withAnnotation("Emoticon", "true") {
                    append(this@emoticonString)
                }
                while (matcher.find()) {
                    val start = matcher.start()
                    val end = matcher.end()
                    val emoticonName = matcher.group(1)
                    if (emoticonName != null) {
                        addStringAnnotation(
                            INLINE_CONTENT_TAG,
                            "Emoticon#${EmoticonManager.getEmoticonIdByName(emoticonName)}",
                            start,
                            end,
                        )
                    }
                }
            }
        }

    val AnnotatedString.emoticonString: AnnotatedString
        get() {
            if (hasStringAnnotations("Emoticon", 0, length)) {
                return this
            }

            val matcher = ALL_TYPE_REGEX_PATTERN.matcher(this.text)
            return buildAnnotatedString {
                withAnnotation("Emoticon", "true") {
                    append(this@emoticonString)
                }
                while (matcher.find()) {
                    val start = matcher.start()
                    val end = matcher.end()
                    val emoticonName = matcher.group(1)
                    if (emoticonName != null) {
                        addStringAnnotation(
                            INLINE_CONTENT_TAG,
                            "Emoticon#${EmoticonManager.getEmoticonIdByName(emoticonName)}",
                            start,
                            end,
                        )
                    }
                }
            }
        }

    fun inlineTextFormat(name: String) = "#($name)"
}