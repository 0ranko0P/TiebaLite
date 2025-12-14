/*
 * Copyright (C) 2024 Mihon and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://github.com/mihonapp/mihon/blob/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.os.Build
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.utils.FileUtil.createFileInCacheDir
import com.huanchengfly.tieba.post.utils.FileUtil.ensureParents
import com.huanchengfly.tieba.post.utils.FileUtil.toSharedUri
import com.huanchengfly.tieba.post.utils.extension.toShareIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.PrintStream
import java.nio.charset.StandardCharsets

object CrashLogUtil {

    suspend fun dumpLogs(context: Context, stackTrace: String? = null) {
        withContext(NonCancellable + Dispatchers.IO) {
            try {
                val file = context.createFileInCacheDir("crash_logs.txt")
                file.ensureParents()
                PrintStream(file.outputStream(), true, StandardCharsets.UTF_8.name()).use {
                    it.println(getDebugInfo())
                    it.println()
                    stackTrace?.let { t -> it.println(t) }
                    it.println()
                }

                val uri = file.toSharedUri(context)
                context.startActivity(uri.toShareIntent(context, "text/plain"))
            } catch (e: Throwable) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { context.toastShort("Failed to get logs: ${e.message}") }
            }
        }
    }

    fun getDebugInfo(): String {
        return """
            App version: ${BuildConfig.VERSION_NAME} ${BuildConfig.VERSION_CODE} (${BuildConfig.BUILD_GIT})
            Build type: ${BuildConfig.BUILD_TYPE}
            Android version: ${Build.VERSION.RELEASE}; (SDK ${Build.VERSION.SDK_INT}; Build/${Build.DISPLAY})
            Device name: ${Build.DEVICE} (${Build.PRODUCT})
        """.trimIndent()
    }
}