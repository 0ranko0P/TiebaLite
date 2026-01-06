package com.huanchengfly.tieba.post.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.huanchengfly.tieba.post.components.ShortcutInitializer

enum class LauncherIcons {
    NEW_ICON, NEW_ICON_THEMED, NEW_ICON_INVERT, OLD_ICON;

    fun supportThemedIcon(): Boolean = this == NEW_ICON
}

object AppIconUtil {

    private fun getComponentName(icon: LauncherIcons, context: Context): ComponentName {
        val cls = when(icon) {
           LauncherIcons.NEW_ICON -> "com.huanchengfly.tieba.post.MainActivityV2"
           LauncherIcons.NEW_ICON_THEMED -> "com.huanchengfly.tieba.post.MainActivityIconThemed"
           LauncherIcons.NEW_ICON_INVERT -> "com.huanchengfly.tieba.post.MainActivityIconInvert"
           LauncherIcons.OLD_ICON -> "com.huanchengfly.tieba.post.MainActivityIconOld"
        }
        return ComponentName(context, cls)
    }

    fun setIcon(icon: LauncherIcons, ctx: Context) {
        val context = ctx.applicationContext
        val packageManager = context.packageManager
        packageManager.enableComponent(getComponentName(icon, context))
        LauncherIcons.entries.forEach {
            if (it != icon) {
                packageManager.disableComponent(getComponentName(it, context))
            }
        }
        // 重新初始化快捷方式
        ShortcutInitializer().create(context)
    }

    /**
     * 启用组件
     *
     * @param componentName 组件名
     */
    private fun PackageManager.enableComponent(componentName: ComponentName) {
        setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    /**
     * 禁用组件
     *
     * @param componentName 组件名
     */
    private fun PackageManager.disableComponent(componentName: ComponentName) {
        setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}