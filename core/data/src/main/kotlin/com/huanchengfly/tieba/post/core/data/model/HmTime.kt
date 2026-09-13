package com.huanchengfly.tieba.post.core.data.model

import com.huanchengfly.tieba.post.core.common.ktx.packInts
import com.huanchengfly.tieba.post.core.common.ktx.unpackInt1
import com.huanchengfly.tieba.post.core.common.ktx.unpackInt2

/**
 * Pack "HH:mm" formatted time into long. For persisting [androidx.compose.material3.TimePickerState]
 * into settings.
 * */
@JvmInline
value class HmTime(val value: Long) {

    constructor(hourOfDay: Int, minute: Int) : this(packInts(hourOfDay, minute))

    /** The hour of the day (0 - 23). */
    val hourOfDay: Int
        get() = unpackInt1(value)

    /** The minute within the hour (0-59). */
    val minute: Int
        get() = unpackInt2(value)

    operator fun component1(): Int = hourOfDay

    operator fun component2(): Int = minute

    override fun toString(): String = String.format(null, "%02d:%02d", hourOfDay, minute)
}