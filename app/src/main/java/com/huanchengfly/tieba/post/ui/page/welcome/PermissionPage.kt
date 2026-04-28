package com.huanchengfly.tieba.post.ui.page.welcome

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.ui.icons.EncryptedMinusCircle
import com.huanchengfly.tieba.post.ui.models.settings.PrivacySettings
import com.huanchengfly.tieba.post.ui.page.settings.appLinkPreference
import com.huanchengfly.tieba.post.ui.page.settings.clipboardPreference
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SegmentedPreference
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SegmentedPrefsScope
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SegmentedPrefsScreen
import com.huanchengfly.tieba.post.utils.PermissionUtils.askPermission
import com.huanchengfly.tieba.post.utils.PermissionUtils.onDenied
import com.huanchengfly.tieba.post.utils.PermissionUtils.onGranted
import kotlinx.coroutines.launch

private val PermissionTitleVerticalPadding: Dp = 8.dp

@Immutable
private class PermissionInfo(
    val icon: ImageVector,
    val name: Int,
    val description: Int,
    val permission: String
)

private fun SegmentedPrefsScope.permissionPreference(
    info: PermissionInfo,
    onRequest: (PermissionInfo) -> Unit
) {
    customPreference {
        SegmentedPreference(
            title = info.name,
            summary = info.description,
            leadingIcon = info.icon,
            trailingContent = {
                Text(
                    text = stringResource(R.string.button_grant),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight)
                )
            },
            onClick = { onRequest(info) }
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
        SegmentedPrefsScreen(
            settings = settings,
            initialValue = PrivacySettings(),
            verticalArrangement = Arrangement.Top,
        ) {
            if (!permissionEssential.isNullOrEmpty()) {
                group(title = R.string.title_permission_essential, titleVerticalPadding = PermissionTitleVerticalPadding) {
                    permissionEssential.fastForEach { info ->
                        permissionPreference(info = info, onRequest = onRequestPermission)
                    }
                }
            }

            if (!permissionOpt.isNullOrEmpty()) {
                group(title = R.string.title_permission_optional, titleVerticalPadding = PermissionTitleVerticalPadding) {
                    permissionOpt.fastForEach { info ->
                        permissionPreference(info = info, onRequest = onRequestPermission)
                    }
                }
            }

            group(title = R.string.title_settings_privacy, titleVerticalPadding = PermissionTitleVerticalPadding) {
                appLinkPreference()

                clipboardPreference()
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
