package com.huanchengfly.tieba.post.utils

import android.Manifest
import android.Manifest.permission
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.ui.util.fastAny
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.components.dialogs.RequestPermissionTipDialog
import com.huanchengfly.tieba.post.findActivity
import kotlinx.coroutines.channels.getOrElse


object PermissionUtils {

    /**
     * Turn [Manifest.permission] into text.
     */
    fun transformText(context: Context, vararg permissions: String): List<String> {
        val rec: HashSet<String> = hashSetOf()
        if (permissions.isEmpty()) {
            throw IllegalArgumentException("Empty permission list")
        }

        @StringRes var hint: Int?
        for (permission in permissions) {
            hint = when (permission) {
                Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE -> {
                    R.string.common_permission_storage
                }

                Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO -> {
                    R.string.common_permission_image_and_video
                }

                Manifest.permission.READ_MEDIA_AUDIO -> R.string.common_permission_music_and_audio

                Manifest.permission.RECORD_AUDIO -> R.string.common_permission_microphone

                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                        !permissions.contains(Manifest.permission.ACCESS_FINE_LOCATION) &&
                        !permissions.contains(Manifest.permission.ACCESS_COARSE_LOCATION)
                    ) {
                        R.string.common_permission_location_background
                    } else {
                        R.string.common_permission_location
                    }
                }

                Manifest.permission.READ_PHONE_STATE, Manifest.permission.CALL_PHONE, Manifest.permission.ADD_VOICEMAIL, Manifest.permission.USE_SIP, Manifest.permission.READ_PHONE_NUMBERS, Manifest.permission.ANSWER_PHONE_CALLS -> {
                    R.string.common_permission_phone
                }

                Manifest.permission.POST_NOTIFICATIONS -> R.string.common_permission_post_notifications

                Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS -> R.string.common_permission_ignore_battery_optimize

                else -> null
            }

            if (hint == null) {
                rec.add(permission)
            } else {
                rec.add(context.getString(hint))
            }
        }

        return rec.toList()
    }

    suspend fun Context.askPermission(desc: Int, vararg permissions: String, noRationale: Boolean = false): Result {
        val activity = this.findActivity()
        if (activity !is FragmentActivity) {
            throw IllegalArgumentException("${this::class.simpleName} not an instance of FragmentActivity")
        } else if (activity.isFinishing || activity.isDestroyed) {
            throw IllegalStateException("${activity::class.simpleName} destroyed")
        }

        val unGranted = permissions.filter {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_DENIED
        }
        return if (unGranted.isEmpty()) {
            Result.Grant
        } else if (noRationale && unGranted.fastAny { activity.shouldShowRequestPermissionRationale(it) }) {
            // Denied and rationale disabled
            Result.Deny(unGranted.toSet())
        } else {
            RequestPermissionTipDialog
                .newInstance(getString(desc), * unGranted.toTypedArray())
                .show(activity)
                .result.receiveCatching()
                .getOrElse { Result.Deny(emptySet()) /* Dismissed without action */ }
        }
    }

    sealed interface Result {
        object Grant: Result

        class Deny(val permissions: Set<String>): Result
    }

    inline fun Result.onDenied(action: (Set<String>) -> Unit): Result {
        if (this is Result.Deny) action(this.permissions)
        return this
    }

    inline fun Result.onGranted(action: () -> Unit): Result {
        if (this is Result.Grant) action()
        return this
    }
}