package com.huanchengfly.tieba.post.core.media.state

import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import java.util.Formatter
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@JvmInline
internal value class DraggingProgress(val value: Long) {

    val finalTime: Float
        get() = unpackFloat1(value)

    val diffTime: Float
        get() = unpackFloat2(value)

    constructor(finalTime: Float, diffTime: Float): this(packFloats(finalTime, diffTime))

    fun getProgressText(builder: StringBuilder, formatter: Formatter): String {
        return formatter.format(
            "%s [%s%s]",
            getDurationString(builder, formatter, finalTime.toLong()),
            if (diffTime < 0) "-" else "+",
            getDurationString(builder, formatter, abs(diffTime.toLong()))
        ).toString()
    }

    companion object {

        /**
         * Returns the specified millisecond time formatted as a string.
         *
         * @param builder The builder that {@code formatter} will write to.
         * @param formatter The formatter.
         * @param durationMs The time to format as a string, in milliseconds.
         * @param negativePrefix Whether to format the negative time with a minus sign.
         * @return The time formatted as a string.
         */
        private fun getDurationString(
            builder: StringBuilder,
            formatter: Formatter,
            durationMs: Long,
            negativePrefix: Boolean = false,
        ): String {
            val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs)

            builder.setLength(0)
            val time = if (hours > 0) {
                formatter.format(
                    "%s%d:%02d:%02d",
                    if (negativePrefix) "-" else "", hours,
                    hours,
                    minutes - TimeUnit.HOURS.toMinutes(hours),
                    seconds - TimeUnit.MINUTES.toSeconds(minutes)
                )
            } else {
                formatter.format(
                    "%s%02d:%02d",
                    if (negativePrefix) "-" else "",
                    minutes,
                    seconds - TimeUnit.MINUTES.toSeconds(minutes)
                )
            }.toString()
            builder.setLength(0)
            return time
        }
    }
}
