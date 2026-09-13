package com.huanchengfly.tieba.post.core.data.util

import android.annotation.SuppressLint

object SystemPropertyUtil {

    @SuppressLint("PrivateApi")
    fun getInt(key: String, default: Int): Int {
        try {
            val systemPropertiesClass = Class.forName("android.os.SystemProperties")
            val getIntMethod = systemPropertiesClass.getMethod("getInt", String::class.java, Int::class.javaPrimitiveType)
            return getIntMethod.invoke(null, key, default) as Int
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return default
    }
}