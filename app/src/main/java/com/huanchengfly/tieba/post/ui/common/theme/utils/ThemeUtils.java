package com.huanchengfly.tieba.post.ui.common.theme.utils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

public class ThemeUtils {

    public static Drawable tintDrawable(Drawable drawable, ColorStateList colorStateList) {
        if (drawable == null) return null;
        Drawable wrapper = DrawableCompat.wrap(drawable.mutate());
        DrawableCompat.setTintList(wrapper, colorStateList);
        DrawableCompat.setTintMode(drawable, PorterDuff.Mode.SRC_IN);
        return drawable;
    }

    public static Drawable tintDrawable(Drawable drawable, @ColorInt int color) {
        if (drawable == null) return null;
        Drawable wrapper = DrawableCompat.wrap(drawable.mutate());
        DrawableCompat.setTint(wrapper, color);
        DrawableCompat.setTintMode(drawable, PorterDuff.Mode.SRC_IN);
        return drawable;
    }

    @ColorInt
    public static int getColorById(Context context, @ColorRes int colorId) {
        return ContextCompat.getColor(context, colorId);
    }
}
