package com.huanchengfly.tieba.post.mockk

import io.mockk.MockKMatcherScope
import org.junit.Assert.assertEquals

/**
 * Vararg matcher as Array
 *
 * see issue#432
 * */
inline fun <reified T : Any> MockKMatcherScope.varargArray(expected: Array<T>) =
    varargAll<T> {
        assertEquals(expected[position], it)
        true
    }
