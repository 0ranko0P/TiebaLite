package com.huanchengfly.tieba.post.core.network.util

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

internal fun String.gzipCompress(): ByteArray {
    val bos = ByteArrayOutputStream()
    GZIPOutputStream(bos).bufferedWriter(Charsets.UTF_8).use { it.write(this) }
    return bos.toByteArray()
}