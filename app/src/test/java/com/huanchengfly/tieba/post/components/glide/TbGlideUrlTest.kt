package com.huanchengfly.tieba.post.components.glide

import org.junit.Assert.assertEquals
import org.junit.Test

class TbGlideUrlTest {

    @Test
    fun cacheKey_noChanges() {
        val url = "file:///android_asset/test.png"
        assertEquals(url, TbGlideUrl.removeQueryComponent(url))

        val url2 = "https://developer.android.com/_static/android/images/logo-x_dt.svg"
        assertEquals(url2, TbGlideUrl.removeQueryComponent(url2))
    }

    @Test
    fun cacheKey() {
        // Expect url scheme and query component removed
        val url = "http://tiebapic.baidu.com/forum/pic/item/b6c8a201a18b87d668bfc906410828381e30fd7a.jpg?tbpicau=2025-12-13-05"
        val urlNoQuery = "tiebapic.baidu.com/forum/pic/item/b6c8a201a18b87d668bfc906410828381e30fd7a.jpg"
        assertEquals(urlNoQuery, TbGlideUrl.removeQueryComponent(url))

        val url2 = "http://tiebapic.baidu.com/forum/w%3D580/sign=b9a/7010e.jpg?tbpicau=2025-12-13-05"
        val url2A = "http://tiebapic.baidu.com/forum/w%3D580/sign=b9a/7010e.jpg?tbpicau=999999999-05"
        val urlNoQuery2 = "tiebapic.baidu.com/forum/w%3D580/sign=b9a/7010e.jpg"
        assertEquals(urlNoQuery2, TbGlideUrl.removeQueryComponent(url2))
        assertEquals(urlNoQuery2, TbGlideUrl.removeQueryComponent(url2A))
    }
}