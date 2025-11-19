package com.huanchengfly.tieba.post.utils

import androidx.annotation.IntRange
import androidx.compose.runtime.Immutable
import androidx.compose.ui.util.packInts
import androidx.compose.ui.util.unpackInt1
import androidx.compose.ui.util.unpackInt2

/**
 * Pack "HH:mm" formatted time in long. For persisting [androidx.compose.material3.TimePickerState]
 * into settings.
 * */
@Immutable
@JvmInline
value class HmTime(val value: Long) {

    constructor(
        @IntRange(from = 0, to = 23) hourOfDay: Int,
        @IntRange(from = 0, to = 59) minute: Int
    ) : this(packInts(hourOfDay, minute))

    /** The hour of the day (0 - 23). */
    val hourOfDay: Int
        get() = unpackInt1(value)

    /** The minute within the hour (0-59). */
    val minute: Int
        get() = unpackInt2(value)

    operator fun component1(): Int = hourOfDay

    operator fun component2(): Int = minute

    override fun toString(): String = "$hourOfDay:$minute"
}
