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

@file:Suppress("DEPRECATION")

package com.huanchengfly.tieba.post.components.imageProcessor

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import kotlin.math.roundToInt

/**
 * Commit: c1de268e2d4064e7febf67983155aa11e623a72d
 *
 * 0Ranko0p changes:
 *     [1]: Force Intrinsics blur
 *     [2]: Workaround 25.0f radius limitation by down scale the image
 * */
class RenderScriptImageProcessor(context: Context) : ImageProcessor {

    override val name = "RenderScript Intrinsics"

    // Renderscript scripts
    private val mRS: RenderScript = RenderScript.create(context)
    private val mIntrinsicBlur = ScriptIntrinsicBlur.create(mRS, Element.U8_4(mRS))

    // Input image
    private var mInputImage: Bitmap? = null

    override fun configureInputAndOutput(inputImage: Bitmap) {
        val oldInputImage = mInputImage

        // Skip bitmap that already configured
        if (oldInputImage != null && oldInputImage === inputImage) return

        mInputImage = inputImage
    }

    override fun blur(radius: Float): Bitmap {
        // Down scale the image if needed
        val inputImage: Bitmap = downScale(mInputImage, radius)
        var inputAlloc: Allocation? = null

        // Output images and allocations
        val outputImage = Bitmap.createBitmap(inputImage.width, inputImage.height, Bitmap.Config.ARGB_8888)
        var outputAlloc: Allocation? = null

        try {
            inputAlloc = Allocation.createFromBitmap(mRS, inputImage)
            outputAlloc = Allocation.createFromBitmap(mRS, outputImage)

            // Set blur kernel size
            mIntrinsicBlur.setRadius(radius.coerceIn(1.0f, 25.0f))

            // Invoke filter kernel
            mIntrinsicBlur.setInput(inputAlloc)
            mIntrinsicBlur.forEach(outputAlloc)

            // Copy to bitmap, this should cause a synchronization rather than a full copy.
            outputAlloc.copyTo(outputImage)
            return outputImage
        } finally {
            inputAlloc?.destroy()
            outputAlloc?.destroy()
        }
    }

    // Downsampling bitmap
    private fun downScale(inputImage: Bitmap?, radius: Float): Bitmap {
        if (inputImage == null) throw RuntimeException("Not configured!")
        if (radius > 25.0f) {
            var scale = 0.9f / (radius / 25.0f)
            var scaledWidth = (inputImage.width * scale).roundToInt()
            if (scaledWidth > 16) {
                scaledWidth = scaledWidth.shr(4).shl(4)
                scale = scaledWidth / inputImage.width.toFloat()
            }
            val scaledHeight = (inputImage.height * scale).roundToInt()
            return Bitmap.createScaledBitmap(inputImage, scaledWidth, scaledHeight, false)
        } else {
            return inputImage
        }
    }

    override fun getInputImage(): Bitmap? = mInputImage

    override fun cleanup() {
        mIntrinsicBlur.destroy()
    }
}