package com.huanchengfly.tieba.post.utils;

import android.content.Context;
import android.content.res.TypedArray;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;

import java.util.Calendar;

public class Util {

    @ColorInt
    public static int getIconColorByLevel(String levelStr) {
        @ColorInt int color = 0xFFB7BCB6;
        if (levelStr == null) return color;
        switch (levelStr) {
            case "1":
            case "2":
            case "3":
                color = 0xFF2FBEAB;
                break;
            case "4":
            case "5":
            case "6":
            case "7":
            case "8":
            case "9":
                color = 0xFF3AA7E9;
                break;
            case "10":
            case "11":
            case "12":
            case "13":
            case "14":
            case "15":
                color = 0xFFFFA126;
                break;
            case "16":
            case "17":
            case "18":
                color = 0xFFFF9C19;
                break;
        }
        return ColorUtils.greifyColor(color, 0.2f);
    }

    public static @ColorInt
    int getColorByAttr(Context context, @AttrRes int attr, @ColorRes int defaultColor) {
        int[] attrs = new int[]{attr};
        TypedArray typedArray = context.obtainStyledAttributes(attrs);
        int color = typedArray.getColor(0, context.getResources().getColor(defaultColor));
        typedArray.recycle();
        return color;
    }

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