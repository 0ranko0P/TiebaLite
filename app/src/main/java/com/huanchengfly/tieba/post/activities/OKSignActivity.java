package com.huanchengfly.tieba.post.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.pm.ShortcutManagerCompat;

import com.huanchengfly.tieba.post.components.ShortcutInitializer.Companion.TbShortcut;
import com.huanchengfly.tieba.post.utils.TiebaUtil;

public class OKSignActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TiebaUtil.startSign(getApplicationContext());
        ShortcutManagerCompat.reportShortcutUsed(getApplicationContext(), TbShortcut.OK_SIGN.getId());
        finish();
    }
}
