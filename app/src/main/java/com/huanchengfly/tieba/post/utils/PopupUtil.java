package com.huanchengfly.tieba.post.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.View;

import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.MenuPopupWindow;
import androidx.appcompat.widget.PopupMenu;

import com.huanchengfly.tieba.post.R;
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils;

import java.lang.reflect.Field;

@SuppressLint("RestrictedApi")
public class PopupUtil {
    private PopupUtil() {
    }

    private static void replaceBackground(PopupMenu popupMenu, Context context) {
        try {
            Field field = PopupMenu.class.getDeclaredField("mPopup");
            field.setAccessible(true);
            MenuPopupHelper menuPopupHelper = (MenuPopupHelper) field.get(popupMenu);
            Object obj = menuPopupHelper.getPopup();
            Field popupField = obj.getClass().getDeclaredField("mPopup");
            popupField.setAccessible(true);
            MenuPopupWindow menuPopupWindow = (MenuPopupWindow) popupField.get(obj);

            boolean isDarkMode = ThemeUtil.INSTANCE.getDarkModeState().getValue();
            int backgroundColor = isDarkMode ? Color.BLACK : Color.WHITE;
            menuPopupWindow.setBackgroundDrawable(
                    ThemeUtils.tintDrawable(context.getDrawable(R.drawable.bg_popup), backgroundColor)
            );
        } catch (NoSuchFieldException | IllegalAccessException | NullPointerException e) {
            Log.w(PopupUtil.class.getSimpleName(), "onReplaceBackground", e);
        }
    }

    public static PopupMenu create(View anchor) {
        PopupMenu popupMenu = new PopupMenu(anchor.getContext(), anchor);
        replaceBackground(popupMenu, anchor.getContext());
        return popupMenu;
    }
}
