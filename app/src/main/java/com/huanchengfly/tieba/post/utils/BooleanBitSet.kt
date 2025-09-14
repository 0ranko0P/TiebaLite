package com.huanchengfly.tieba.post.utils

import androidx.annotation.CheckResult
import androidx.compose.runtime.Immutable

/**
 * Simpler version of [java.util.BitSet] stores fixed boolean bit.
 * */
@Immutable
@JvmInline
value class BooleanBitSet(val bits: Int = 0) {

    /**
     * Returns the value of the bit with the specified index. The value
     * is `true` if the bit with the index [index] is currently set in this
     * [BooleanBitSet] otherwise, the result is `false`.
     *
     * @param  index the bit index
     * @return the value of the bit with the specified index
     */
    fun get(index: Int): Boolean {
        if (index !in 0..Int.SIZE_BITS) { "Index $index out of bounds" }
        return bits and (1 shl index) != 0
    }

    /**
     * Sets the bit at the specified index to the specified value.
     *
     * @param  index a bit index
     * @param  value a boolean value to set
     */
    @CheckResult
    fun set(index: Int, value: Boolean): BooleanBitSet {
        require(index in 0..Int.SIZE_BITS) { "Index $index out of bounds" }

        return if (value) {
            BooleanBitSet(bits or (1 shl index))
        } else {
            BooleanBitSet(bits and (1 shl index).inv())
        }
    }
}