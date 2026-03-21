package com.huanchengfly.tieba.post.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.net.Uri
import com.huanchengfly.tieba.post.api.BOUNDARY
import com.huanchengfly.tieba.post.api.booleanToString
import com.huanchengfly.tieba.post.api.models.UploadPictureResultBean
import com.huanchengfly.tieba.post.api.retrofit.RetrofitTiebaApi
import com.huanchengfly.tieba.post.api.retrofit.body.MyMultipartBody
import com.huanchengfly.tieba.post.api.retrofit.body.buildMultipartBody
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaException
import com.huanchengfly.tieba.post.ui.models.settings.WaterType
import com.huanchengfly.tieba.post.utils.FileUtil.deleteQuietly
import com.huanchengfly.tieba.post.utils.FileUtil.writeAll
import com.huanchengfly.tieba.post.utils.ImageUtil
import com.huanchengfly.tieba.post.utils.ImageUtil.toFile
import com.huanchengfly.tieba.post.utils.MD5Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile

class ImageUploader(
    private val forumName: String,
    private val chunkSize: Int = DEFAULT_CHUNK_SIZE
) {
    companion object {
        const val DEFAULT_CHUNK_SIZE = 512000

        const val IMAGE_MAX_SIZE = 5242880
        const val ORIGIN_IMAGE_MAX_SIZE = 10485760
    }

    suspend fun upload(
        context: Context,
        images: List<Uri>,
        @WaterType watermarkType: Int,
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
        } catch (e: Throwable) {
            throw e
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
                val firstCompressResult = ImageUtil.compressImage(bitmap, quality = 95)
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
        @WaterType watermarkType: Int,
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
            .flatMapConcat { RetrofitTiebaApi.OFFICIAL_TIEBA_API.uploadPicture(it) }
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
}

class UploadPictureFailedException(
    override val code: Int = -1,
    override val message: String = "上传图片失败",
) : TiebaException(message)