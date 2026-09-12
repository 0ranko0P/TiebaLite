package com.huanchengfly.tieba.post.utils

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import android.webkit.URLUtil
import androidx.annotation.WorkerThread
import androidx.core.content.FileProvider
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.core.common.ktx.closeQuietly
import com.huanchengfly.tieba.post.core.common.ktx.ensureParents
import com.huanchengfly.tieba.post.utils.PermissionUtils.askPermission
import com.huanchengfly.tieba.post.utils.PermissionUtils.onGranted
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.nio.charset.StandardCharsets

object FileUtil {

    private const val TAG = "FileUtil"

    const val FILE_TYPE_DOWNLOAD = 0
    const val FILE_TYPE_VIDEO = 1
    const val FILE_TYPE_AUDIO = 2
    const val FILE_FOLDER = "TiebaLite"

    private fun getDataColumn(
        context: Context,
        uri: Uri?,
        selection: String?,
        selectionArgs: Array<String>?,
    ): String? {
        val column = "_data"
        val projection = arrayOf(column)
        context.contentResolver.query(uri!!, projection, selection, selectionArgs, null)
            .use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val column_index = cursor.getColumnIndexOrThrow(column)
                    return cursor.getString(column_index)
                }
            }
        return null
    }

    fun downloadBySystem(context: Context, fileType: Int, url: String?) {
        val fileName = URLUtil.guessFileName(url, null, null)
        downloadBySystem(context, fileType, url, fileName)
    }

    private fun downloadBySystemWithPermission(
        context: Context,
        fileType: Int,
        url: String?,
        fileName: String,
    ) {
        // 指定下载地址
        val request = DownloadManager.Request(Uri.parse(url))
        // 允许媒体扫描，根据下载的文件类型被加入相册、音乐等媒体库
        request.allowScanningByMediaScanner()
        // 设置通知的显示类型，下载进行时和完成后显示通知
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        // 允许该记录在下载管理界面可见
        request.setVisibleInDownloadsUi(true)
        // 允许漫游时下载
        request.setAllowedOverRoaming(false)
        // 设置下载文件保存的路径和文件名
        val directory: String = when (fileType) {
            FILE_TYPE_VIDEO -> Environment.DIRECTORY_MOVIES
            FILE_TYPE_AUDIO -> Environment.DIRECTORY_PODCASTS
            FILE_TYPE_DOWNLOAD -> Environment.DIRECTORY_DOWNLOADS
            else -> Environment.DIRECTORY_DOWNLOADS
        }
        request.setDestinationInExternalPublicDir(
            directory,
            FILE_FOLDER + File.separator + fileName
        )
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        // 添加一个下载任务
        downloadManager.enqueue(request)
    }

    fun downloadBySystem(context: Context, fileType: Int, url: String?, fileName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            downloadBySystemWithPermission(context, fileType, url, fileName)
            return
        }

        MainScope().launch {
            context.askPermission(
                desc = R.string.tip_permission_storage_download,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            .onGranted {
                downloadBySystemWithPermission(context, fileType, url, fileName)
            }
        }
    }

    @JvmStatic
    @WorkerThread
    fun readAssetFile(context: Context, fileName: String): String? {
        try {
            val ins = context.assets.open(fileName)
            BufferedReader(InputStreamReader(ins, StandardCharsets.UTF_8)).use { reader ->
                return reader.readText()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    @JvmStatic
    @WorkerThread
    fun readFile(file: File): String? {
        try {
            file.bufferedReader(StandardCharsets.UTF_8).use { ins ->
                return ins.readText()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    @JvmStatic
    @WorkerThread
    fun writeFile(file: File, content: String, append: Boolean): Boolean {
        try {
            file.ensureParents()
            PrintWriter(FileOutputStream(file, append)).use { writer ->
                writer.println(content)
                return true
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * Sets the content of the [file] as [texts] encoded using UTF-8. If the file exists, it
     * becomes overwritten.
     *
     * @param file destination file
     * @param texts list of text to write into file.
     * */
    @WorkerThread
    fun writeLines(file: File, texts: List<CharSequence>): Boolean {
        var writer: BufferedWriter? = null
        var outputStreamWriter: OutputStreamWriter? = null
        try {
            file.ensureParents()
            outputStreamWriter = OutputStreamWriter(file.outputStream())
            writer = BufferedWriter(outputStreamWriter)
            texts.forEach {
                writer.appendLine(it)
            }
            return true
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            writer?.closeQuietly()
            outputStreamWriter?.closeQuietly()
        }
        return false
    }

    fun File.toSharedUri(context: Context): Uri {
        return FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".share.FileProvider", this)
    }

    fun Context.createFileInCacheDir(file: String) = File(cacheDir, "misc/$file")

    fun deleteWithPrefixSafe(dir: File, prefix: String) {
        val start = System.currentTimeMillis()
        var deleted = 0
        val files = runCatching { dir.listFiles() }.getOrNull() ?: return
        files.forEach { f ->
            try {
                // delete file with prefix
                if (f.isFile && f.name.startsWith(prefix) && f.delete()) {
                    deleted++
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Delete ${f.name} Failed: ${e.message}.")
            }
        }

        if (BuildConfig.DEBUG) {
            val cost = System.currentTimeMillis() - start
            Log.i(TAG, "Delete $deleted files of $prefix, cost ${cost}ms")
        }
    }

    fun File.isCacheExpired(expireMill: Long): Boolean {
        return lastModified() + expireMill < System.currentTimeMillis()
    }

    //修改文件扩展名
    fun changeFileExtension(fileName: String, newExtension: String): String {
        if (TextUtils.isEmpty(fileName)) {
            return fileName
        }
        val index = fileName.lastIndexOf(".")
        return if (index == -1) {
            fileName + newExtension
        } else fileName.substring(0, index) + newExtension
    }
}