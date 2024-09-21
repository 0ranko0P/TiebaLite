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

import android.graphics.Bitmap

// Defines the interface of an image processor that is able to apply HUE rotation and blur effects
// to a bitmap. This interface is implemented with both RenderScript and Vulkan backend as a
// comparison. The RenderScript backend may choose to use intrinsics or compiled scripts.
//
// The image processor is optimized for performing multiple filters on the same input image. The
// caller must configure the input and output images by configureInputAndOutput before invoking any
// filter kernel.
// 0Ranko0p changes:
//     [1]: Nuke unused rotateHue()
interface ImageProcessor {
    val name: String

    // Set the input image from bitmap and allocate the memories for output images. The user may
    // invoke this method multiple times to switch between different input images.
    fun configureInputAndOutput(inputImage: Bitmap)

    // Apply gaussian blur to the input image. The radius must be within the range of [1.0, 25.0].
    fun blur(radius: Float): Bitmap

    // Returns unmodified bitmap
    fun getInputImage(): Bitmap?

    // Frees up any underlying native resources. After calling this method, this image processor
    // can not be used in any way.
    fun cleanup()
}