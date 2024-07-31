package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.text.Spannable
import android.text.SpannableString
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.text.withStyle
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.components.spans.EmoticonSpanV2
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils
import com.huanchengfly.tieba.post.utils.EmoticonManager.getEmoticonIdByName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

object StringUtil {

    suspend fun getEmoticonContent(
        size: Int,
        source: CharSequence?,
        emoticonType: Int = EmoticonUtil.EMOTICON_ALL_TYPE
    ): SpannableString = withContext(Dispatchers.IO) {
        if (source == null) {
            return@withContext SpannableString("")
        }
        return@withContext try {
            val spannableString: SpannableString = if (source is SpannableString) {
                source
            } else {
                SpannableString(source)
            }
            val patternEmoticon = EmoticonUtil.getRegexPattern(emoticonType)
            val matcherEmoticon = patternEmoticon.matcher(spannableString)
            while (matcherEmoticon.find()) {
                val key = matcherEmoticon.group()
                val start = matcherEmoticon.start()
                val end = start + key.length
                val group1 = matcherEmoticon.group(1) ?: ""
                val id = getEmoticonIdByName(group1)

                val bitmap = EmoticonManager.getEmoticonBitmap(id, size).await()
                val emoticonDrawable = BitmapDrawable(App.INSTANCE.resources, bitmap)
                val span = EmoticonSpanV2(emoticonDrawable, size)
                spannableString.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                yield()
            }
            spannableString
        } catch (e: Exception) {
            e.printStackTrace()
            if (source is SpannableString) source else SpannableString(source)
        }
    }

    fun getUserNameString(context: Context, username: String, nickname: String?): String {
        val showBoth = App.isInitialized && context.appPreferences.showBothUsernameAndNickname
        return if (showBoth && !nickname.isNullOrBlank() && username != nickname && username.isNotBlank()) {
            "$nickname $username"
        } else {
            nickname ?: username
        }
    }

    @OptIn(ExperimentalTextApi::class)
    @Stable
    fun buildAnnotatedStringWithUser(
        userId: String,
        username: String,
        nickname: String?,
        content: String,
        context: Context = App.INSTANCE,
    ): AnnotatedString {
        return buildAnnotatedString {
            withAnnotation(tag = "user", annotation = userId) {
                withStyle(
                    SpanStyle(
                        color = Color(ThemeUtils.getColorByAttr(context, R.attr.colorNewPrimary))
                    )
                ) {
                    append("@")
                    append(getUserNameString(context, username, nickname))
                }
            }
            append(": ")
            append(content)
        }
    }

    @JvmStatic
    @Stable
    fun getAvatarUrl(portrait: String?): String {
        if (portrait.isNullOrEmpty()) {
            return ""
        }
        return if (portrait.startsWith("http://") || portrait.startsWith("https://")) {
            portrait
        } else "http://tb.himg.baidu.com/sys/portrait/item/$portrait"
    }

    @JvmStatic
    fun getBigAvatarUrl(portrait: String?): String {
        if (portrait.isNullOrEmpty()) {
            return ""
        }
        return if (portrait.startsWith("http://") || portrait.startsWith("https://")) {
            portrait
        } else "http://tb.himg.baidu.com/sys/portraith/item/$portrait"
    }

    fun String.getShortNumString(): String {
        val long = toLongOrNull() ?: return ""
        return long.getShortNumString()
    }

    fun Int.getShortNumString(): String {
        return toLong().getShortNumString()
    }

    fun Long.getShortNumString(): String {
        val long = this
        return if (long > 9999) {
            val longW = long * 10 / 10000L / 10F
            if (longW > 999) {
                val longKW = longW.toLong() * 10 / 1000L / 10F
                "${longKW}KW"
            } else {
                "${longW}W"
            }
        } else {
            "$this"
        }
    }
}