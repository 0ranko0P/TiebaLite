package com.huanchengfly.tieba.post.utils

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.webkit.URLUtil
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.media3.common.MimeTypes
import com.huanchengfly.tieba.post.App.Companion.INSTANCE
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.components.NetworkObserver
import com.huanchengfly.tieba.post.components.glide.ProgressListener
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.getInt
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.utils.FileUtil.deleteQuietly
import com.huanchengfly.tieba.post.utils.FileUtil.ensureParents
import com.huanchengfly.tieba.post.utils.PermissionUtils.askPermission
import com.huanchengfly.tieba.post.utils.PermissionUtils.onDenied
import com.huanchengfly.tieba.post.utils.PermissionUtils.onGranted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import okhttp3.internal.closeQuietly
import okio.buffer
import okio.sink
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

object ImageUtil {
    /**
     * 智能省流
     */
    const val SETTINGS_SMART_ORIGIN = 0

    /**
     * 智能无图
     */
    const val SETTINGS_SMART_LOAD = 1

    /**
     * 始终高质量
     */
    const val SETTINGS_ALL_ORIGIN = 2

    /**
     * 始终无图
     */
    const val SETTINGS_ALL_NO = 3

    const val KEY_IMAGE_LOAD_TYPE = "image_load_type"

    val imageLoadSettings: Int
        get() = INSTANCE.dataStore.getInt(KEY_IMAGE_LOAD_TYPE, SETTINGS_SMART_ORIGIN)

    /**
     * Directory where the shared image will be saved, keep it sync with [R.xml.file_paths_share_img]
     *
     * @see downloadForShare
     * */
    const val FILE_PROVIDER_SHARE_DIR = ".shareTemp"

    private const val TAG = "ImageUtil"

    private const val MIME_TYPE_GIF = "image/gif"

    private fun isGifFile(body: ResponseBody): Boolean {
        val type = body.contentType()
        return if (type == null) {
            body.byteStream().use { isGifFile(it) }
        } else {
            type.toString() == MIME_TYPE_GIF
        }
    }

    private fun isGifFile(file: File?): Boolean {
        if (file == null) return false
        try {
            return FileInputStream(file).use { isGifFile(it) }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        return false
    }

    //判断是否为GIF文件
    private fun isGifFile(inputStream: InputStream?): Boolean {
        if (inputStream == null) return false
        val bytes = ByteArray(4)
        try {
            inputStream.read(bytes)
            val str = String(bytes)
            return str.equals("GIF8", ignoreCase = true)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }

    fun compressImage(
        bitmap: Bitmap,
        quality: Int = 100
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        bitmap.compress(CompressFormat.JPEG, quality, baos)
        return baos.use { it.toByteArray() }
    }

    @Throws(FileNotFoundException::class, IOException::class)
    fun Bitmap.toFile(output: File, quality: Int = 100, format: CompressFormat = CompressFormat.JPEG) {
        output.ensureParents()
        FileOutputStream(output).use { out ->
            if (!this.compress(format, quality, out)) {
                throw IOException("Unable to compress $output to $format.")
            }
        }
    }

    /**
     * Download image and share it via [FileProvider]
     *
     * @see R.xml.file_paths_share_img
     *
     * @return Content URI of this image file
     * */
    suspend fun downloadForShare(context: Context, url: String?, onProgress: ProgressListener?): Result<Uri> {
        if (url == null) return Result.failure(NullPointerException())

        val pictureFolder = File(context.cacheDir, FILE_PROVIDER_SHARE_DIR)
        val destFile = File(pictureFolder, "share_${url.hashCode()}")

        try {
            DownloadUtil.downloadCancelable(url, destFile, onProgress)
            val uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".share.FileProvider",
                destFile
            )
            return Result.success(uri)
        } catch (e: Exception) {
            Log.w(TAG, "downloadForShare: ", e)
            return Result.failure(e)
        }
    }

    /**
     * Download image to external storage
     * */
    fun download(context: Context, url: String?, onProgress: ProgressListener? = null) {
        if (url == null) return

        MainScope().launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                downloadCancelable(context.applicationContext, url, onProgress)
            } else {
                context.askPermission(
                    R.string.tip_permission_storage_download,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                .onGranted { downloadBelowQ(context.applicationContext, url, onProgress) }
                .onDenied { context.toastShort(R.string.toast_no_permission_save_photo) }
            }
        }
    }

    /**
     * Download image to external storage, cancelable
     * */
    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun downloadCancelable(context: Context, url: String, onProgress: ProgressListener?) {
        val cr = context.contentResolver
        var uri: Uri? = null
        withContext(Dispatchers.IO) {
            val responseBody: ResponseBody = DownloadUtil.downloadCancelable(url, onProgress)
            try {
                var mimeType = responseBody.contentType()?.toString() ?: MimeTypes.IMAGE_JPEG

                var fileName = URLUtil.guessFileName(url, null, mimeType)
                if (isGifFile(responseBody)) {
                    mimeType = MIME_TYPE_GIF
                    fileName = FileUtil.changeFileExtension(fileName, ".gif")
                }

                val relativePath =
                    Environment.DIRECTORY_PICTURES + File.separator + FileUtil.FILE_FOLDER
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Images.Media.DESCRIPTION, fileName)
                }

                uri = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
                cr.openOutputStream(uri!!)!!.sink().buffer().use { bufferedSink ->
                    bufferedSink.writeAll(responseBody.source())
                }
                withContext(Dispatchers.Main) {
                    context.toastShort(R.string.toast_photo_saved, relativePath)
                }
                return@withContext uri
            } catch (e: Exception) {
                uri?.let { cr.delete(it, null, null) }
                throw e
            } finally {
                responseBody.closeQuietly()
            }
        }
    }

    private suspend fun downloadBelowQ(context: Context, url: String, onProgress: ProgressListener?) {
        withContext(Dispatchers.IO) {
            var destFile: File? = null
            val responseBody: ResponseBody = DownloadUtil.downloadCancelable(url, onProgress)
            try {
                val mimeType = responseBody.contentType()?.toString() ?: MimeTypes.IMAGE_JPEG
                val fileName = URLUtil.guessFileName(url, null, mimeType)
                val pictureFolder =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val appDir = File(pictureFolder, FileUtil.FILE_FOLDER)
                destFile = if (isGifFile(responseBody)) {
                    File(appDir, FileUtil.changeFileExtension(fileName, ".gif"))
                } else {
                    File(appDir, fileName)
                }

                destFile.ensureParents()
                destFile.sink().buffer().use { bufferedSink ->
                    bufferedSink.writeAll(responseBody.source())
                }
                withContext(Dispatchers.Main) {
                    context.sendBroadcast(
                        Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(destFile))
                    )
                    context.toastShort(R.string.toast_photo_saved, destFile.path)
                }
            } catch (e: Exception) {
                destFile?.deleteQuietly() // Delete file if error occurred
                throw e
            } finally {
                responseBody.closeQuietly()
            }
        }
    }

    fun getPicId(picUrl: String?): String {
        val fileName = URLUtil.guessFileName(picUrl, null, MimeTypes.IMAGE_JPEG)
        return fileName.replace(".jpg", "")
    }

    /**
     * 根据流量设置返回要加载的缩略图 Url
     *
     * @param originUrl   原图 Url
     * @param smallPicUrl 最差图片
     *
     * @see imageLoadSettings
     */
    fun getThumbnail(originUrl: String, smallPicUrl: String): String {
        return if (loadWorst()) smallPicUrl else originUrl
    }

    private fun loadWorst(): Boolean {
        return if (imageLoadSettings == SETTINGS_SMART_ORIGIN &&
            NetworkObserver.isNetworkUnMetered
        ) false
        else imageLoadSettings != SETTINGS_ALL_ORIGIN
    }

    // Check is long image with given width x height size
    fun isLongImg(width: Int, height: Int): Boolean {
        if (width <= 0) return false
        return height.toFloat() / width > 4f
    }

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
