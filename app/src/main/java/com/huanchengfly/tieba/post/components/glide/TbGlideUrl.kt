package com.huanchengfly.tieba.post.components.glide

import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.Headers
import com.huanchengfly.tieba.post.components.glide.TbGlideUrl.Companion.removeQueryComponent
import java.net.URL

/**
 * Tieba Glide Url
 *
 * 贴吧图片链接中带有过期时间的``tbpicau``参数将导致Glide 缓存失效并再次下载同一张图片, 重写[getCacheKey]并移
 * 除查询参数.
 *
 * @see removeQueryComponent
 * */
@Suppress("unused")
class TbGlideUrl: GlideUrl {

    constructor(url: String, headers: Headers) : super(url, headers)

    constructor(url: URL, headers: Headers) : super(url, headers)

    constructor(url: String): super(url, Headers.DEFAULT)

    override fun getCacheKey(): String? {
        return removeQueryComponent(super.getCacheKey())
    }

    companion object {

        // Remove scheme and query component
        // For URL 'http://tiebapic.baidu.com/forum/pic/item/d6.jpg?tbpicau=2025-12-13-05
        // returns 'tiebapic.baidu.com/forum/pic/item/d6.jpg'
        fun removeQueryComponent(url: String?): String? = when {
            url.isNullOrEmpty() -> null

            url.startsWith("http") && url.contains("?") -> {
                url.substring(url.indexOf("://") + 3, url.indexOf('?'))
            }

            else -> url
        }
    }
}