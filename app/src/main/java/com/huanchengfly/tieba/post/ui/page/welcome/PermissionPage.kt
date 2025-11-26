package com.huanchengfly.tieba.post.ui.page.welcome

import android.Manifest
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.util.fastForEach
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.ui.icons.EncryptedMinusCircle
import com.huanchengfly.tieba.post.ui.models.settings.PrivacySettings
import com.huanchengfly.tieba.post.ui.page.settings.AppLinkPreference
import com.huanchengfly.tieba.post.ui.page.settings.ClipboardPreference
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.BasePreference
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.PrefsScreen
import com.huanchengfly.tieba.post.utils.PermissionUtils.askPermission
import com.huanchengfly.tieba.post.utils.PermissionUtils.onDenied
import com.huanchengfly.tieba.post.utils.PermissionUtils.onGranted
import kotlinx.coroutines.launch

@Immutable
private class PermissionInfo(
    val icon: ImageVector,
    val name: Int,
    val description: Int,
    val permission: String
)

@Composable
private fun PermissionPref(
    modifier: Modifier = Modifier,
    info: PermissionInfo,
    onRequest: (PermissionInfo) -> Unit
) {
    BasePreference(
        modifier = modifier.clickable {
            onRequest(info)
        },
        title = stringResource(id = info.name),
        summary = stringResource(id = info.description),
        leadingIcon = info.icon,
    ) {
        Text(
            text = stringResource(R.string.button_grant),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PermissionPage(
    modifier: Modifier = Modifier,
    settings: Settings<PrivacySettings>,
    uiState: WelcomeState,
    onPermissionResult: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Map permissions to UI Model
    val permissionEssential = uiState.permissionEssential?.let {
        remember(it.size) { it.mapToPermissionInfo() }
    }
    val permissionOpt = uiState.permissionOptional?.let {
        remember(it.size) { it.mapToPermissionInfo() }
    }

    val onRequestPermission: (PermissionInfo) -> Unit = { info ->
        coroutineScope.launch {
            context.askPermission(desc = info.description, info.permission)
                .onGranted { onPermissionResult(info.permission, true) }
                .onDenied { onPermissionResult(info.permission, false) }
        }
    }

    DualTitleContent(
        modifier = modifier,
        icon = rememberVectorPainter(Icons.Rounded.EncryptedMinusCircle),
        title = R.string.welcome_permission,
        subtitle = when {
            uiState.essentialGranted && permissionOpt.isNullOrEmpty() -> R.string.welcome_permission_done
            uiState.essentialGranted -> R.string.welcome_permission_essential
            else -> R.string.welcome_permission_subtitle
        }
    ) {
        PrefsScreen(
            settings = settings,
            initialValue = PrivacySettings(),
        ) {
            if (!permissionEssential.isNullOrEmpty()) {
                Group(
                    titleRes = R.string.title_permission_essential
                ) {
                    permissionEssential.fastForEach { info ->
                        PermissionPref(info = info, onRequest = onRequestPermission)
                    }
                }
            }

            if (!permissionOpt.isNullOrEmpty()) {
                Group(
                    titleRes = R.string.title_permission_optional
                ) {
                    permissionOpt.fastForEach { info ->
                        PermissionPref(info = info, onRequest = onRequestPermission)
                    }
                }
            }

            Group(
                titleRes = R.string.title_settings_privacy
            ) {
                AppLinkPreference()

                ClipboardPreference()
            }
        }
    }
}

private fun List<String>.mapToPermissionInfo(): List<PermissionInfo> = map {
    when (it) {
        Manifest.permission.READ_PHONE_STATE -> PermissionInfo(
            icon = Icons.Rounded.PhoneAndroid,
            name = R.string.common_permission_phone,
            description = R.string.tip_permission_phone,
            permission = it
        )

        Manifest.permission.POST_NOTIFICATIONS -> PermissionInfo(
            icon = Icons.Rounded.NotificationsActive,
            name = R.string.common_permission_post_notifications,
            description = R.string.desc_permission_post_notifications,
            permission = it
        )

        else -> throw RuntimeException()
    }
}
