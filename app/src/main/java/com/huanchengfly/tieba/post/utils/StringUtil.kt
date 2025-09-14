package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.text.Spannable
import android.text.SpannableString
import androidx.compose.runtime.Stable
import com.huanchengfly.tieba.post.components.spans.EmoticonSpanV2
import com.huanchengfly.tieba.post.utils.EmoticonManager.getEmoticonIdByName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

object StringUtil {

    suspend fun getEmoticonContent(
        context: Context,
        size: Int,
        source: CharSequence?,
        emoticonType: Int = EmoticonUtil.EMOTICON_ALL_TYPE
    ): SpannableString = withContext(Dispatchers.IO) {
        val spannableString = source as? SpannableString ?: SpannableString(source ?: "")
        if (spannableString.length < 4) { // minimum length of emotion text
            return@withContext spannableString
        }

        try {
            val patternEmoticon = EmoticonUtil.getRegexPattern(emoticonType)
            val matcherEmoticon = patternEmoticon.matcher(spannableString)
            while (matcherEmoticon.find()) {
                val key = matcherEmoticon.group()
                val start = matcherEmoticon.start()
                val end = start + key.length
                val group1 = matcherEmoticon.group(1) ?: ""
                val id = getEmoticonIdByName(group1) ?: continue
                val glideBitmapRec = runCatching { EmoticonManager.getEmoticonBitmap(id, size).get() }
                val bitmap: Bitmap = glideBitmapRec.getOrNull() ?: continue
                val emoticonDrawable = BitmapDrawable(context.resources, bitmap)
                val span = EmoticonSpanV2(emoticonDrawable, size)
                ensureActive()
                spannableString.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext spannableString
    }

    fun getUserNameString(showBoth: Boolean, username: String, nickname: String?): String {
        val canShowBoth = !nickname.isNullOrBlank() && username != nickname && username.isNotBlank()
        return if (canShowBoth && showBoth) {
            "$nickname $username"
        } else {
            nickname ?: username
        }
    }

    @Deprecated("Deprecated")
    fun getUserNameString(context: Context, username: String, nickname: String?): String {
        val canShowBoth = !nickname.isNullOrBlank() && username != nickname && username.isNotBlank()
        return if (canShowBoth && context.appPreferences.showBothUsernameAndNickname) {
            "$nickname $username"
        } else {
            nickname ?: username
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

    // Convert formatted number string
    fun tiebaNumToLong(str: String): Long {
        if (str == "0") return 0L

        try {
            return str.toLongOrNull() ?: str.run {
                var num = 0L
                forEachIndexed { i, c ->
                    if (c.isDigit()) {
                        if (i != 0) num *= 10
                        num += c.digitToInt()
                    } else if (c.equals('W', ignoreCase = true)) {
                        num *= 10000
                    } else if (c.equals('K', ignoreCase = true)) {
                        num *= 1000
                    }
                }
                return@run num
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return 0L
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