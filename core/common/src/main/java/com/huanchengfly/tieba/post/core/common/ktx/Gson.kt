package com.huanchengfly.tieba.post.core.common.ktx

import com.google.gson.reflect.TypeToken
import com.huanchengfly.tieba.post.utils.GsonUtil
import java.io.File

fun Any.toJson(): String = GsonUtil.getGson().toJson(this)

inline fun <reified Data> String.fromJson(): Data {
    val type = object : TypeToken<Data>() {}.type
    return GsonUtil.getGson().fromJson(this, type)
}

inline fun <reified Data> File.fromJson(): Data {
    val type = object : TypeToken<Data>() {}.type
    return GsonUtil.getGson().fromJson(reader(), type)
}
