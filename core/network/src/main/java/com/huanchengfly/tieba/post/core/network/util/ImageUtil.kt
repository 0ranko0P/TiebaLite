package com.huanchengfly.tieba.post.core.network.util

import java.io.InputStream
import android.util.Base64
import java.io.File
import java.io.FileInputStream
import java.io.IOException

/**
 * Image Base64 Encoder
 *
 * From com.huanchengfly.tieba.post.utils.ImageUtil
 *
 * @author HuanChengFly
 * @since 3.8.1 α
 * */
internal object ImageUtil {

    fun imageToBase64(inputStream: InputStream?): String? {
        if (inputStream == null) {
            return null
        }
        return runCatching {
            inputStream.use {
                Base64.encodeToString(inputStream.readBytes(), Base64.DEFAULT)
            }
        }.getOrNull()
    }

    fun imageToBase64(file: File?): String? {
        if (file == null) {
            return null
        }
        var result: String? = null
        try {
            val `is`: InputStream = FileInputStream(file)
            result = imageToBase64(`is`)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return result
    }
}