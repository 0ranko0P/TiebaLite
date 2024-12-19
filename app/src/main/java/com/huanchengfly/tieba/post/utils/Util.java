package com.huanchengfly.tieba.post.utils;

import java.util.Calendar;

public class Util {

    public static long getTimeInMillis(String timeStr) {
        return time2Calendar(timeStr).getTimeInMillis();
    }

    public static Calendar time2Calendar(String timeStr) {
        String[] time = timeStr.split(":");
        int hour = 0, minute = 0, second = 0;
        if (time.length >= 2) {
            hour = Integer.parseInt(time[0]);
            minute = Integer.parseInt(time[1]);
            if (time.length >= 3) {
                second = Integer.parseInt(time[2]);
            }
        }
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);
        return calendar;
    }
}