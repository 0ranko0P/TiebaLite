package com.huanchengfly.tieba.post.utils;

import static android.graphics.Color.TRANSPARENT;
import static androidx.core.graphics.ColorUtils.calculateLuminance;

import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;

public final class ColorUtils {
    public static int getDarkerColor(@ColorInt int color) {
        return getDarkerColor(color, 0.1f);
    }

    public static int getDarkerColor(@ColorInt int color, float i) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv); // convert to hsv
        // make darker
        hsv[1] = hsv[1] + i; // more saturation
        hsv[2] = hsv[2] - i; // less brightness
        return Color.HSVToColor(hsv);
    }

    public static int setLuminance(@ColorInt int color, @FloatRange(from = 0f, to = 1f) float luminance) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = luminance;
        return Color.HSVToColor(hsv);
    }

    @ColorInt
    public static int alpha(@ColorInt int color, @IntRange(from = 0, to = 255) int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    public static int getLighterColor(@ColorInt int color) {
        return getLighterColor(color, 0.1f);
    }

    public static int getLighterColor(@ColorInt int color, float i) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv); // convert to hsv
        // make lighter
        hsv[1] = hsv[1] - i; // less saturation
        hsv[2] = hsv[2] + i; // more brightness
        return Color.HSVToColor(hsv);
    }

    public static int greifyColor(@ColorInt int color, @FloatRange(from = 0f, to = 1f) float sat) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = hsv[1] - sat;
        hsv[2] = hsv[2] - (sat / 3);
        return Color.HSVToColor(hsv);
    }

    /** Determines if a color should be considered light or dark. */
    public static boolean isColorLight(@ColorInt int color) {
        return color != TRANSPARENT && calculateLuminance(color) > 0.5;
    }

    @ColorInt
    public static int getIconColorByLevel(int level) {
        @ColorInt int color = 0xFFB7BCB6;
        switch (level) {
            case 1:
            case 2:
            case 3:
                color = 0xFF2FBEAB;
                break;
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
                color = 0xFF3AA7E9;
                break;
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
                color = 0xFFFFA126;
                break;
            case 16:
            case 17:
            case 18:
                color = 0xFFFF9C19;
                break;
        }
        return greifyColor(color, 0.2f);
    }
}
