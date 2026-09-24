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
import android.util.Log
import android.webkit.URLUtil
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.media3.common.MimeTypes
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.core.common.ktx.deleteQuietly
import com.huanchengfly.tieba.post.core.common.ktx.ensureParents
import com.huanchengfly.tieba.post.core.data.model.settings.HabitSettings
import com.huanchengfly.tieba.post.core.data.model.settings.ImageLoadType
import com.huanchengfly.tieba.post.core.network.di.NetUtilsEntryPoint
import com.huanchengfly.tieba.post.core.network.exception.getErrorMessage
import com.huanchengfly.tieba.post.core.network.util.NetworkMonitor
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.utils.ImageUtil.downloadForShare
import com.huanchengfly.tieba.post.utils.PermissionUtils.askPermission
import com.huanchengfly.tieba.post.utils.PermissionUtils.onDenied
import com.huanchengfly.tieba.post.utils.PermissionUtils.onGranted
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import okhttp3.internal.closeQuietly
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files

object ImageUtil {

    /**
     * Directory where the shared image will be saved, keep it sync with [R.xml.file_paths_share_img]
     *
     * @see downloadForShare
     * */
    const val FILE_PROVIDER_SHARE_DIR = ".shareTemp"

    private const val TAG = "ImageUtil"

    private const val MIME_TYPE_GIF = "image/gif"

    // TODO: Refactor
    private val networkMonitor: NetworkMonitor by lazy {
        EntryPointAccessors.fromApplication<NetUtilsEntryPoint>(App.INSTANCE).networkMonitor()
    }

    private fun isGifFile(body: ResponseBody): Boolean {
        val type = body.contentType()
        return if (type == null) {
            isGifFile(body.byteStream())
        } else {
            type.toString() == MIME_TYPE_GIF
        }
    }

    private fun isGifFile(file: File?): Boolean {
        return file?.let { isGifFile(FileInputStream(it)) } == true
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
        } finally {
            inputStream.closeQuietly()
        }
        return false
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
    suspend fun downloadForShare(context: Context, url: String?): Result<Uri> {
        if (url == null) return Result.failure(NullPointerException())

        val pictureFolder = File(context.cacheDir, FILE_PROVIDER_SHARE_DIR)
        val destFile = File(pictureFolder, "share_${url.hashCode()}")

        try {
            // Check downloaded
            if (!destFile.exists() || destFile.length() < 1) {
                withContext(Dispatchers.IO) {
                    val coilCache = CoilUtil.downloadCancelable(context, url)
                    coilCache.copyTo(destFile, overwrite = true)
                }
            }

            val uri = FileProvider.getUriForFile(context,
                context.packageName + ".share.FileProvider",
                destFile
            )
            return Result.success(uri)
        } catch (e: Exception) {
            Log.w(TAG, "onDownloadForShare", e)
            destFile.deleteQuietly()
            return Result.failure(e)
        }
    }

    /**
     * Download image to external storage
     * */
    fun download(context: Context, url: String?) {
        if (url == null) return

        MainScope().launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                downloadCancelable(context.applicationContext, url)
            } else {
                context.askPermission(
                    R.string.tip_permission_storage_download,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                .onGranted { downloadBelowQ(context.applicationContext, url) }
                .onDenied { context.toastShort(R.string.toast_no_permission_save_photo) }
            }
        }
    }

    /**
     * Download image to external storage, cancelable
     * */
    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun downloadCancelable(context: Context, url: String) {
        val cr = context.contentResolver
        var uri: Uri? = null
        withContext(Dispatchers.IO) {
            try {
                val coilCache = CoilUtil.downloadCancelable(context, url)
                var mimeType = MimeTypes.IMAGE_JPEG
                var fileName = URLUtil.guessFileName(url, null, mimeType)
                if (isGifFile(coilCache)) {
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
                cr.openOutputStream(uri).use { out ->
                    Files.copy(coilCache.toPath(), out)
                }
                withContext(Dispatchers.Main) {
                    context.toastShort(R.string.toast_photo_saved, relativePath)
                }
                return@withContext uri
            } catch (e: Exception) {
                Log.w(TAG, "onDownloadCancelable: ", e)
                uri?.let { cr.delete(it, null, null) }
                withContext(Dispatchers.Main) {
                    context.toastShort(R.string.toast_exception, e.getErrorMessage())
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun downloadBelowQ(context: Context, url: String) {
        withContext(Dispatchers.IO) {
            var destFile: File? = null
            try {
                val coilCache = CoilUtil.downloadCancelable(context, url)
                val mimeType = MimeTypes.IMAGE_JPEG
                val fileName = URLUtil.guessFileName(url, null, mimeType)
                val pictureFolder =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val appDir = File(pictureFolder, FileUtil.FILE_FOLDER)
                destFile = if (isGifFile(coilCache)) {
                    File(appDir, FileUtil.changeFileExtension(fileName, ".gif"))
                } else {
                    File(appDir, fileName)
                }

                destFile.ensureParents()
                destFile.sink().buffer().use { bufferedSink ->
                    bufferedSink.writeAll(coilCache.source())
                }
                withContext(Dispatchers.Main) {
                    context.sendBroadcast(
                        Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(destFile))
                    )
                    context.toastShort(R.string.toast_photo_saved, destFile.path)
                }
            } catch (e: Exception) {
                destFile?.deleteQuietly() // Delete file if error occurred
                Log.w(TAG, "onDownloadBelowQ", e)
                withContext(Dispatchers.Main) {
                    context.toastShort(R.string.toast_exception, e.getErrorMessage())
                }
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
     * @param loadType 图片加载设置
     * @param originUrl   原图 Url
     * @param smallPicUrl 最差图片
     *
     * @see HabitSettings.imageLoadType
     */
    fun getThumbnail(@ImageLoadType loadType: Int, originUrl: String, smallPicUrl: String): String {
        // Workaround for empty srcPic, originPic in OriginThreadInfo (v12.52.1.0)
        val emptyOrigin = originUrl.isEmpty()
        return if (emptyOrigin || loadWorst(loadType)) smallPicUrl else originUrl
    }

    private fun loadWorst(@ImageLoadType loadType: Int): Boolean {
        return if (loadType == ImageLoadType.SMART_ORIGIN) {
            !networkMonitor.isCurrentlyNotMetered()
        } else {
            loadType != ImageLoadType.ALL_ORIGIN
        }
    }

    // Check is long image with given width x height size
    fun isLongImg(width: Int, height: Int): Boolean {
        if (width <= 0 || height <= 0) return false
        return height.toFloat() / width > 4f
    }
}
