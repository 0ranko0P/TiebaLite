package com.huanchengfly.tieba.post.core.common.ktx

import java.io.Closeable
import java.io.File
import java.io.IOException

@Throws(IOException::class)
fun File.ensureParents() {
    val parent = parentFile?: throw IOException("Invalid parent dir of $this")
    if (!parent.exists() && !parent.mkdirs()) throw IOException("Create $parent failed!")
}

fun File.deleteQuietly() {
    try {
        this.delete()
    } catch (_: Exception) {}
}

/** Closes this, ignoring any checked exceptions. */
fun Closeable.closeQuietly() {
    try {
        close()
    } catch (rethrown: RuntimeException) {
        throw rethrown
    } catch (_: Exception) {
    }
}
