package com.huanchengfly.tieba.post.utils

import android.content.Context
import com.huanchengfly.tieba.post.R
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

object DateTimeUtils {

    private const val MIN_VALID_MILLIS = 1000000000000L

    @JvmStatic
    fun getRelativeTimeString(
        context: Context,
        timestampString: String
    ): String {
        return getRelativeTimeString(context, fixTimestamp(timestampString.toLongOrNull() ?: MIN_VALID_MILLIS))
    }

    @JvmStatic
    fun getRelativeTimeString(
        context: Context,
        timestamp: Long
    ): String {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = fixTimestamp(timestamp)
        }
        val currentCalendar = Calendar.getInstance()
        return if (currentCalendar.after(calendar)) {
            if (calendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR)) {
                if (calendar.get(Calendar.DAY_OF_MONTH) == currentCalendar.get(Calendar.DAY_OF_MONTH) &&
                    calendar.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH)
                ) {
                    if (calendar.get(Calendar.HOUR_OF_DAY) == currentCalendar.get(Calendar.HOUR_OF_DAY)) {
                        if (calendar.get(Calendar.MINUTE) == currentCalendar.get(Calendar.MINUTE)) {
                            if (calendar.get(Calendar.SECOND) == currentCalendar.get(Calendar.SECOND)) {
                                calendar.format(context.getString(R.string.relative_date_after))
                            } else {
                                context.getString(
                                    R.string.relative_date_second,
                                    currentCalendar.get(Calendar.SECOND) - calendar.get(Calendar.SECOND)
                                )
                            }
                        } else {
                            context.getString(
                                R.string.relative_date_minute,
                                currentCalendar.get(Calendar.MINUTE) - calendar.get(Calendar.MINUTE)
                            )
                        }
                    } else {
                        context.getString(
                            R.string.relative_date_today,
                            calendar.format("HH:mm")
                        )
                    }
                } else {
                    calendar.format("MM-dd HH:mm")
                }
            } else {
                calendar.format("yyyy-MM-dd HH:mm")
            }
        } else {
            calendar.format(context.getString(R.string.relative_date_after))
        }
    }

    private fun Calendar.clearTime() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }

    @JvmStatic
    fun isToday(timestamp: Long): Boolean {
        val date = DateFormat.getDateInstance().format(timestamp)
        val todayDate = DateFormat.getDateInstance().format(System.currentTimeMillis())
        return date == todayDate
    }

    private fun Calendar.format(pattern: String): String {
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timeInMillis))
    }

    fun fixTimestamp(timestamp: Long): Long {
        if (timestamp <= 0 || timestamp > MIN_VALID_MILLIS) return timestamp

        var rec = timestamp
        while (rec < MIN_VALID_MILLIS) {
            rec *= 10
        }
        return rec
    }
}