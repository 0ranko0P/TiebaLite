package com.huanchengfly.tieba.post.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.huanchengfly.tieba.post.App

object NotificationUtils {

    val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(App.INSTANCE)
    }

    fun checkPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            notificationManager.areNotificationsEnabled()
        }
    }

    /**
     * Create the required notification channel for O+ devices.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun createChannel(
        channelId: String,
        name: String,
        groupId: String? = null,
        importance: Int = NotificationManagerCompat.IMPORTANCE_DEFAULT,
        onBuild: ((NotificationChannelCompat.Builder) -> Unit)? = null
    ) {
        val channel = NotificationChannelCompat.Builder(channelId, importance)
            .setName(name)
            .setGroup(groupId)
            .setShowBadge(importance > NotificationManagerCompat.IMPORTANCE_LOW)
            .setLightsEnabled(importance > NotificationManagerCompat.IMPORTANCE_LOW)
            .apply {
                if (onBuild != null) onBuild(this)
            }
            .build()
        notificationManager.createNotificationChannel(channel)
    }
}