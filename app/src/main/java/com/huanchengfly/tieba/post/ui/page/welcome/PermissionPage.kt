package com.huanchengfly.tieba.post.ui.page.welcome

import android.Manifest
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.icons.EncryptedMinusCircle
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
private fun Permission(modifier: Modifier = Modifier, info: PermissionInfo, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraSmall)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painter = rememberVectorPainter(info.icon), contentDescription = null, modifier = Modifier.size(30.dp))
        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1.0f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = stringResource(info.name), style = MaterialTheme.typography.labelLarge)

            Text(
                text = stringResource(info.description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun PermissionPage(modifier: Modifier = Modifier, permissions: List<String>, onPermissionResult: (String, Boolean) -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val permissionInfoList = remember(permissions) { permissions.mapToPermissionInfo() }
    val granted = permissions.isEmpty()

    DualTitleContent(
        modifier = modifier,
        icon = rememberVectorPainter(Icons.Rounded.EncryptedMinusCircle),
        title = R.string.welcome_permission,
        subtitle = if (granted) R.string.welcome_permission_done else R.string.welcome_permission_subtitle
    ) {
        permissionInfoList.fastForEach { info ->
            Permission(info = info) {
                coroutineScope.launch {
                    context.askPermission(desc = info.description, info.permission)
                        .onGranted { onPermissionResult(info.permission, true) }
                        .onDenied { onPermissionResult(info.permission, false) }
                }
            }
        }
    }
}

private fun List<String>.mapToPermissionInfo(): List<PermissionInfo> = map {
    when(it) {
        Manifest.permission.READ_PHONE_STATE -> PermissionInfo(
            icon = Icons.Rounded.PhoneAndroid,
            name = R.string.common_permission_phone,
            description = R.string.tip_permission_phone,
            permission = it
        )

        else -> throw RuntimeException()
    }
}
