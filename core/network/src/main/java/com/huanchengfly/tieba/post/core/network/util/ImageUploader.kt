package com.huanchengfly.tieba.post.core.network.util

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.net.Uri
import com.huanchengfly.tieba.post.core.common.ktx.booleanToString
import com.huanchengfly.tieba.post.core.common.ktx.deleteQuietly
import com.huanchengfly.tieba.post.core.common.ktx.ensureParents
import com.huanchengfly.tieba.post.core.network.exception.UploadPictureFailedException
import com.huanchengfly.tieba.post.core.network.model.UploadPictureResultBean
import com.huanchengfly.tieba.post.core.network.retrofit.body.MyMultipartBody
import com.huanchengfly.tieba.post.core.network.retrofit.body.buildMultipartBody
import com.huanchengfly.tieba.post.core.network.retrofit.impls.BOUNDARY
import com.huanchengfly.tieba.post.core.network.retrofit.interfaces.OfficialTiebaApi
import com.huanchengfly.tieba.post.core.network.session.ClientConfigProvider
import com.huanchengfly.tieba.post.utils.MD5Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext
import okhttp3.RequestBody.Companion.toRequestBody
import okio.buffer
import okio.sink
import okio.source
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile

/**
 * Image Uploader
 *
 * From com.huanchengfly.tieba.post.components.ImageUploader
 *
 * @author HuanChengFly
 * @since 4.0.0 Alpha 15
 * */
internal class ImageUploader(
    private val clientConfigProvider: ClientConfigProvider,
    private val forumName: String,
    private val chunkSize: Int = DEFAULT_CHUNK_SIZE,
    private val tiebaApi: OfficialTiebaApi,
) {
    companion object {
        const val DEFAULT_CHUNK_SIZE = 512000

        const val IMAGE_MAX_SIZE = 5242880
        const val ORIGIN_IMAGE_MAX_SIZE = 10485760

        @Throws(IOException::class)
        fun File.writeAll(contentResolver: ContentResolver, uri: Uri): File {
            ensureParents()
            contentResolver.openInputStream(uri)!!.source().buffer().use { bufferedSource ->
                this.sink().buffer().use { bufferedSink ->
                    bufferedSink.writeAll(bufferedSource)
                }
            }
            return this
        }

        fun compressImage(
            bitmap: Bitmap,
            quality: Int = 100
        ): ByteArray {
            val baos = ByteArrayOutputStream()
            bitmap.compress(CompressFormat.JPEG, quality, baos)
            return baos.use { it.toByteArray() }
        }
    }

    suspend fun upload(
        context: Context,
        images: List<Uri>,
        watermarkType: Int,
        isOriginImage: Boolean = false
    ): List<UploadPictureResultBean> {
        require(images.isNotEmpty())
        val contentResolver = context.contentResolver
        val tempDir = File(context.cacheDir, "upload_tmp_${images.hashCode()}")
        return try {
            images.mapIndexed { i, uri ->
                val image = File(tempDir, "img_$i").writeAll(contentResolver, uri)
                uploadSinglePicture(image, watermarkType, isOriginImage)
            }
        } catch (e: IOException) {
            if (e.cause != null && e.cause is EOFException) { // 垃圾VPN
                throw IOException("网络错误!", e.cause)
            } else {
                throw e
            }
        } finally {
            runCatching { tempDir.deleteRecursively() } // Cleanup quietly
        }
    }

    private suspend fun compressImage(
        originFile: File,
        isOriginImage: Boolean
    ): File {
        return withContext(Dispatchers.IO) {
            val fileLength = originFile.length()
            val maxSize = if (isOriginImage) ORIGIN_IMAGE_MAX_SIZE else IMAGE_MAX_SIZE
            if (isOriginImage && fileLength <= maxSize) {
                return@withContext originFile
            } else {
                val tempFile = File.createTempFile("temp", ".tmp")
                val bitmap = BitmapFactory.decodeFile(originFile.path)
                val firstCompressResult = compressImage(bitmap, quality = 95)
                tempFile.writeBytes(firstCompressResult)
                if (firstCompressResult.size > maxSize) {
                    // 压缩尺寸至 1080P
                    val width = bitmap.width
                    val height = bitmap.height
                    val scale = if (width > height) {
                        1080f / width
                    } else {
                        1080f / height
                    }
                    if (scale < 1) {
                        val newWidth = (width * scale).toInt()
                        val newHeight = (height * scale).toInt()
                        Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
                            .toFile(tempFile, quality = 95, format = CompressFormat.JPEG)
                    }
                }
                return@withContext tempFile
            }
        }
    }

    @Throws(UploadPictureFailedException::class, FileNotFoundException::class)
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun uploadSinglePicture(
        image: File,
        watermarkType: Int,
        isOriginImage: Boolean = false,
    ): UploadPictureResultBean {
        val option = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        withContext(Dispatchers.IO) { BitmapFactory.decodeFile(image.path, option) }
        val width = option.outWidth
        val height = option.outHeight
        check(width > 0 && height > 0) { "图片宽高不正确" }
        val file = compressImage(originFile = image, isOriginImage)
        val fileLength = file.length()
        val maxSize = if (isOriginImage) ORIGIN_IMAGE_MAX_SIZE else IMAGE_MAX_SIZE
        check(fileLength <= maxSize) { "图片大小超过限制" }
        val fileMd5 = withContext(Dispatchers.IO) { MD5Util.toMd5(file) }
        val isMultipleChunkSize = fileLength % chunkSize == 0L
        val totalChunkNum = fileLength / chunkSize + if (isMultipleChunkSize) 0 else 1
        val requestBodies = (0 until totalChunkNum).map { chunk ->
            val isFinish = chunk == totalChunkNum - 1
            val curChunkSize = if (isFinish) {
                if (isMultipleChunkSize) {
                    chunkSize
                } else {
                    fileLength % chunkSize
                }
            } else {
                chunkSize
            }.toInt()
            val chunkBytes = ByteArray(curChunkSize)
            withContext(Dispatchers.IO) {
                RandomAccessFile(file, "r").use {
                    it.seek(chunk * chunkSize.toLong())
                    it.read(chunkBytes)
                }
            }
            buildMultipartBody(BOUNDARY) {
                setType(MyMultipartBody.FORM)
                addFormDataPart("alt", "json")
                addFormDataPart("chunkNo", "${chunk + 1}")
                if (forumName.isNotEmpty()) addFormDataPart("forum_name", forumName)
                addFormDataPart("groupId", "1")
                addFormDataPart("height", "$height")
                addFormDataPart("isFinish", isFinish.booleanToString())
                addFormDataPart("is_bjh", "0")
                addFormDataPart("pic_water_type", watermarkType.toString())
                addFormDataPart("resourceId", "$fileMd5$chunkSize")
                addFormDataPart("saveOrigin", isOriginImage.booleanToString())
                addFormDataPart("size", "$fileLength")
                if (forumName.isNotEmpty()) addFormDataPart("small_flow_fname", forumName)
                addFormDataPart("width", "$width")
                addFormDataPart("chunk", "file", chunkBytes.toRequestBody())
            }
        }
        return requestBodies.asFlow()
            .flatMapConcat {
                tiebaApi.uploadPicture(
                    body = it,
                    cookie = clientConfigProvider.getBaiduId()?.let { id -> "ka=open;BAIDUID=$id" } ?: "ka=open",
                )
            }
            .onCompletion {
                withContext(Dispatchers.IO) { file.deleteQuietly() }
            }
            .last()
            .also { resultBean ->
                val errorCode = resultBean.errorCode.toIntOrNull() ?: -1
                if (errorCode != 0) {
                    throw UploadPictureFailedException(errorCode, resultBean.errorMsg)
                }
            }
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
}