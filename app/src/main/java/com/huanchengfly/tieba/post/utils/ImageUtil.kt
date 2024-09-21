package com.huanchengfly.tieba.post.utils

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.webkit.URLUtil
import android.widget.ImageView
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.media3.common.MimeTypes
import com.github.panpf.sketch.request.Depth
import com.github.panpf.sketch.request.DisplayRequest
import com.github.panpf.sketch.request.enqueue
import com.github.panpf.sketch.transform.CircleCropTransformation
import com.github.panpf.sketch.transform.RoundedCornersTransformation
import com.huanchengfly.tieba.post.App.Companion.INSTANCE
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.components.glide.ProgressListener
import com.huanchengfly.tieba.post.dpToPxFloat
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.utils.FileUtil.deleteQuietly
import com.huanchengfly.tieba.post.utils.FileUtil.ensureParents
import com.huanchengfly.tieba.post.utils.PermissionUtils.PermissionData
import com.huanchengfly.tieba.post.utils.PermissionUtils.askPermission
import com.huanchengfly.tieba.post.utils.ThemeUtil.isNightMode
import com.zhihu.matisse.MimeType
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
    const val LOAD_TYPE_SMALL_PIC = 0
    const val LOAD_TYPE_AVATAR = 1
    const val LOAD_TYPE_NO_RADIUS = 2
    const val LOAD_TYPE_ALWAYS_ROUND = 3
    const val TAG = "ImageUtil"

    private fun isGifFile(body: ResponseBody): Boolean {
        val type = body.contentType()
        return if (type == null) {
            body.byteStream().use { isGifFile(it) }
        } else {
            type.toString() == MimeType.GIF.toString()
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

    fun compressImage(
        bitmap: Bitmap,
        output: File,
        maxSizeKb: Int = 100,
        initialQuality: Int = 100
    ): File {
        var baos: ByteArrayOutputStream? = null
        try {
            baos = ByteArrayOutputStream()
            var quality = initialQuality
            bitmap.compress(CompressFormat.JPEG, quality, baos) //质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
            while (baos.toByteArray().size / 1024 > maxSizeKb && quality > 0) {  //循环判断如果压缩后图片是否大于设置的最大值,大于继续压缩
                baos.reset() //重置baos即清空baos
                quality -= 5 //每次都减少5
                bitmap.compress(CompressFormat.JPEG, quality, baos) //这里压缩options%，把压缩后的数据存放到baos中
            }
            FileOutputStream(output).use { fos ->
                fos.write(baos.toByteArray())
                fos.flush()
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } finally {
            baos?.closeQuietly()
        }
        return output
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

    fun drawableToBitmap(drawable: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth, drawable.intrinsicHeight,
            if (drawable.opacity != PixelFormat.OPAQUE) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable.draw(canvas)
        return bitmap
    }

    private fun changeBrightness(imageView: ImageView, brightness: Int) {
        val cMatrix = ColorMatrix()
        cMatrix.set(
            floatArrayOf(
                1f, 0f, 0f, 0f, brightness.toFloat(), 0f, 1f, 0f, 0f, brightness.toFloat(),  // 改变亮度
                0f, 0f, 1f, 0f, brightness.toFloat(), 0f, 0f, 0f, 1f, 0f
            )
        )
        imageView.colorFilter = ColorMatrixColorFilter(cMatrix)
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

        // FileProvider: keep it sync with R.xml.file_paths_share_img
        val pictureFolder = File(context.cacheDir, ".shareTemp")
        val destFile = File(pictureFolder, "share_${url.hashCode()}")

        try {
            DownloadUtil.downloadCancelable(url, destFile, onProgress)
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    context.packageName + ".share.FileProvider",
                    destFile
                )
            } else {
                Uri.fromFile(destFile)
            }
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MainScope().launch {
                downloadCancelable(context.applicationContext, url, onProgress)
            }
            return
        }
        askPermission(
            context,
            PermissionData(
                listOf(
                    PermissionUtils.READ_EXTERNAL_STORAGE,
                    PermissionUtils.WRITE_EXTERNAL_STORAGE
                ),
                context.getString(R.string.tip_permission_storage)
            ),
            R.string.toast_no_permission_save_photo
        ) {
            MainScope().launch {
                downloadBelowQ(context.applicationContext, url, onProgress)
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
                    mimeType = MimeType.GIF.toString()
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

    private fun checkGifFile(file: File) {
        if (isGifFile(file)) {
            val gifFile = File(file.parentFile, FileUtil.changeFileExtension(file.name, ".gif"))
            if (gifFile.exists()) {
                file.delete()
            } else {
                file.renameTo(gifFile)
            }
        }
    }

    fun getPicId(picUrl: String?): String {
        val fileName = URLUtil.guessFileName(picUrl, null, MimeType.JPEG.toString())
        return fileName.replace(".jpg", "")
    }

    private fun getRadiusDp(context: Context): Int {
        return context.appPreferences.radius
    }

    fun getPlaceHolder(context: Context, radius: Int): Drawable {
        val drawable = GradientDrawable()
        val colorResId =
            if (isNightMode()) R.color.color_place_holder_night else R.color.color_place_holder
        val color = ContextCompat.getColor(context, colorResId)
        drawable.setColor(color)
        drawable.cornerRadius =
            DisplayUtil.dp2px(context, radius.toFloat()).toFloat()
        return drawable
    }

    fun getPlaceHolder(context: Context, radius: Float): Drawable {
        val drawable = GradientDrawable()
        val colorResId =
            if (isNightMode()) R.color.color_place_holder_night else R.color.color_place_holder
        val color = ContextCompat.getColor(context, colorResId)
        drawable.setColor(color)
        drawable.cornerRadius = radius.dpToPxFloat()
        return drawable
    }

    @SuppressLint("CheckResult")
    fun load(
        imageView: ImageView,
        @LoadType type: Int,
        url: String?,
        skipNetworkCheck: Boolean,
        noTransition: Boolean,
    ) {
        if (!Util.canLoadGlide(imageView.context)) {
            return
        }
        if (isNightMode()) {
            changeBrightness(imageView, -35)
        } else {
            imageView.clearColorFilter()
        }
        val radius = getRadiusDp(imageView.context).toFloat()
        val requestBuilder =
            if (skipNetworkCheck ||
                type == LOAD_TYPE_AVATAR ||
                imageLoadSettings == SETTINGS_ALL_ORIGIN ||
                imageLoadSettings == SETTINGS_SMART_ORIGIN ||
                (imageLoadSettings == SETTINGS_SMART_LOAD && NetworkUtil.isWifiConnected())
            ) {
                imageView.setTag(R.id.image_load_tag, true)
                DisplayRequest.Builder(imageView.context, url)
            } else {
                imageView.setTag(R.id.image_load_tag, false)
                DisplayRequest.Builder(imageView.context, url).depth(Depth.LOCAL)
            }
        when (type) {
            LOAD_TYPE_SMALL_PIC -> requestBuilder
                .placeholder(getPlaceHolder(imageView.context, radius))
                .transformations(RoundedCornersTransformation(radius.dpToPxFloat()))

            LOAD_TYPE_AVATAR -> requestBuilder
                .placeholder(getPlaceHolder(imageView.context, 6f))
                .transformations(RoundedCornersTransformation(6f.dpToPxFloat()))

            LOAD_TYPE_NO_RADIUS -> requestBuilder
                .placeholder(getPlaceHolder(imageView.context, 0f))

            LOAD_TYPE_ALWAYS_ROUND -> requestBuilder
                .placeholder(getPlaceHolder(imageView.context, 100f))
                .transformations(CircleCropTransformation())
        }
        if (!noTransition) {
            requestBuilder.crossfade()
        }
        requestBuilder.target(imageView).build().enqueue()
    }

    @JvmStatic
    @JvmOverloads
    fun load(
        imageView: ImageView,
        @LoadType type: Int,
        url: String?,
        skipNetworkCheck: Boolean = false
    ) {
        load(imageView, type, url, skipNetworkCheck, false)
    }

    /**
     * 获取要加载的图片 Url
     *
     * @param isSmallPic   加载的是否为缩略图
     * @param originUrl    原图 Url
     * @param smallPicUrls 缩略图 Url，按照画质从好到差排序
     * @return 要加载的图片 Url
     */
    @JvmStatic
    fun getUrl(isSmallPic: Boolean, originUrl: String, vararg smallPicUrls: String?): String {
        val urls = mutableListOf(*smallPicUrls)
        if (isSmallPic) {
            if (needReverse()) {
                urls.reverse()
            }
            return urls.firstOrNull { !it.isNullOrEmpty() } ?: originUrl
        }
        return originUrl
    }

    private fun needReverse(): Boolean {
        return if (imageLoadSettings == SETTINGS_SMART_ORIGIN &&
            NetworkUtil.isWifiConnected()
        ) false
        else imageLoadSettings != SETTINGS_ALL_ORIGIN
    }

    @get:ImageLoadSettings
    private val imageLoadSettings: Int
        get() = INSTANCE.appPreferences.imageLoadType!!.toInt()

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

    @IntDef(LOAD_TYPE_SMALL_PIC, LOAD_TYPE_AVATAR, LOAD_TYPE_NO_RADIUS, LOAD_TYPE_ALWAYS_ROUND)
    @Retention(AnnotationRetention.SOURCE)
    annotation class LoadType

    @IntDef(SETTINGS_SMART_ORIGIN, SETTINGS_SMART_LOAD, SETTINGS_ALL_ORIGIN, SETTINGS_ALL_NO)
    annotation class ImageLoadSettings

}
