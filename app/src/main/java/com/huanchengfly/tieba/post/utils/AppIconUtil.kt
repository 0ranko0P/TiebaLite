package com.huanchengfly.tieba.post.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.util.fastForEach
import com.huanchengfly.tieba.post.App
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

object LauncherIcons {
    const val NEW_ICON = "com.huanchengfly.tieba.post.MainActivityV2"
    const val NEW_ICON_THEMED = "com.huanchengfly.tieba.post.MainActivityIconThemed"
    const val NEW_ICON_INVERT = "com.huanchengfly.tieba.post.MainActivityIconInvert"
    const val OLD_ICON = "com.huanchengfly.tieba.post.MainActivityIconOld"

    const val DEFAULT_ICON = NEW_ICON

    val ICONS: ImmutableList<String> by lazy {
        persistentListOf(NEW_ICON, NEW_ICON_THEMED, NEW_ICON_INVERT, OLD_ICON)
    }

    const val SUPPORT_THEMED_ICON = NEW_ICON
}

object AppIconUtil {
    const val KEY_APP_ICON = "app_icon"
    const val KEY_APP_THEMED_ICON = "app_themed_icon"

    private val context: Context
        get() = App.INSTANCE

    fun setIcon(icon: String = LauncherIcons.DEFAULT_ICON) {
        context.packageManager.enableComponent(ComponentName(context, icon))
        LauncherIcons.ICONS.fastForEach {
            if (it != icon) {
                context.packageManager.disableComponent(ComponentName(context, it))
            }
        }
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