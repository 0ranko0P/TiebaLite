package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import com.huanchengfly.tieba.post.R

fun Context.isPackageInstalled(packageName: String): Boolean {
    return try {
        packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: Exception) {
        false
    }
}

val Context.applicationMetaData: Bundle
    get() {
        return packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).metaData
    }

fun buildAppSettingsIntent(packageName: String): Intent {
    return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        addCategory(Intent.CATEGORY_DEFAULT)
        data = Uri.fromParts("package", packageName, null)
    }
}

fun List<ResolveInfo>.loadPackageLabel(context: Context): CharSequence? {
    if (this.isEmpty()) return null
    val packageManager = context.packageManager
    var appName: CharSequence? = null

    if (this.size == 1) {
        val info = first()
        appName = info.activityInfo?.loadLabel(packageManager) ?: info.loadLabel(packageManager)
    }
    return appName ?: context.getString(R.string.name_multi_app)
}

/**
 * Note: Require [android.Manifest.permission.QUERY_ALL_PACKAGES] on Android R
 * */
fun Context.queryDeepLink(uri: Uri): List<ResolveInfo> {
    return packageManager.queryIntentActivities(Intent(Intent.ACTION_VIEW, uri), PackageManager.MATCH_DEFAULT_ONLY)
}

val Context.packageInfo: PackageInfo
    get() = packageManager.getPackageInfo(packageName, 0)
