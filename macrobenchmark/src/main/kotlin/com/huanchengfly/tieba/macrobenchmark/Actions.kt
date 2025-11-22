package com.huanchengfly.tieba.macrobenchmark

import android.content.ComponentName
import android.content.Intent
import androidx.test.uiautomator.UiAutomatorTestScope
import androidx.test.uiautomator.textAsString

fun UiAutomatorTestScope.startActivityAndSetup(
    welcomeScreen: Boolean = false,
    intentBuilder: (Intent.() -> Unit)? = null
) {
    startIntent(Intent().apply {
        setComponent(ComponentName(TARGET_PACKAGE, "com.huanchengfly.tieba.post.MainActivityV2"))
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        putExtra(KEY_WELCOME_SETUP, welcomeScreen)
        if (intentBuilder != null) {
            intentBuilder()
        }
    })
}

fun UiAutomatorTestScope.walkWelcomeSetup() {
    onElement { textAsString() == "继续" && isVisibleToUser }.click()
    onElement { textAsString() == "确定" && isVisibleToUser }.click()
    onElement { textAsString() == "继续" }.click() // Habit settings
    Thread.sleep(600)
    onElement { textAsString() == "继续" }.click() // UI settings
    Thread.sleep(600)
    onElement { textAsString() == "以游客身份浏览" }.click()
}
