/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huanchengfly.tieba.post.components.imageProcessor

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.HardwareRenderer
import android.graphics.PixelFormat
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.S)
class RenderEffectImageProcessor : ImageProcessor {
    override val name = "RenderEffect"
    private var params: Params? = null

    override fun configureInputAndOutput(inputImage: Bitmap) {
        val oldParams = params
        if (oldParams != null) {
            // Skip bitmap that already configured
            if (inputImage === oldParams.bitmap) return

            cleanup() // Recycle the old params now
        }
        params = Params(inputImage)
    }

    private fun applyEffect(it: Params, renderEffect: RenderEffect): Bitmap {
        it.renderNode.setRenderEffect(renderEffect)
        val renderCanvas = it.renderNode.beginRecording()
        renderCanvas.drawBitmap(it.bitmap, 0f, 0f, null)
        it.renderNode.endRecording()
        it.hardwareRenderer.createRenderRequest()
            .setWaitForPresent(true)
            .syncAndDraw()

        val image = it.imageReader.acquireNextImage() ?: throw RuntimeException("No Image")
        val hardwareBuffer = image.hardwareBuffer ?: throw RuntimeException("No HardwareBuffer")
        val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, null)
            ?: throw RuntimeException("Create Bitmap Failed")
        hardwareBuffer.close()
        image.close()
        return bitmap
    }

    override fun blur(radius: Float): Bitmap {
        params?.let {
            val blurRenderEffect = RenderEffect.createBlurEffect(
                radius, radius,
                Shader.TileMode.MIRROR
            )
            return applyEffect(it, blurRenderEffect)
        }
        throw RuntimeException("Not configured!")
    }

    override fun getInputImage(): Bitmap? = params?.bitmap

    override fun cleanup() {
        params?.let {
            params = null
            it.imageReader.close()
            it.renderNode.discardDisplayList()
            it.hardwareRenderer.destroy()
        }
    }

    private inner class Params(val bitmap: Bitmap) {
        @SuppressLint("WrongConstant")
        val imageReader = ImageReader.newInstance(
            bitmap.width, bitmap.height,
            PixelFormat.RGBA_8888, 1,
            HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
        )
        val renderNode = RenderNode(name)
        val hardwareRenderer = HardwareRenderer()

        init {
            hardwareRenderer.setSurface(imageReader.surface)
            hardwareRenderer.setContentRoot(renderNode)
            renderNode.setPosition(0, 0, imageReader.width, imageReader.height)
        }
    }
}