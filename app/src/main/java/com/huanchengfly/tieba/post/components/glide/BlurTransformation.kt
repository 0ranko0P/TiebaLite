package com.huanchengfly.tieba.post.components.glide

import android.graphics.Bitmap
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.huanchengfly.tieba.post.components.imageProcessor.ImageProcessor
import java.security.MessageDigest

class BlurTransformation(private val imgProcessor: ImageProcessor, val radius: Float): BitmapTransformation() {

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        return synchronized(imgProcessor) {
            imgProcessor.configureInputAndOutput(toTransform)
            imgProcessor.blur(radius)
        }
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(toString().toByteArray())
    }

    override fun hashCode(): Int = 31 * TAG.hashCode() + radius.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        return radius == (other as BlurTransformation).radius
    }

    override fun toString(): String = "$TAG(radius=$radius)"

    companion object {
        private const val TAG = "BlurTransformation"
    }
}