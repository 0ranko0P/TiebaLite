package com.huanchengfly.tieba.post.ui.page.welcome

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.LocalExtendedColors
import com.huanchengfly.tieba.post.ui.icons.EncryptedMinusCircle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

fun shouldRequestPermissions(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED
    } else false
}

@Composable
private fun PermissionInfo(
    modifier: Modifier = Modifier,
    icon: Painter,
    name: Int,
    description: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painter = icon, contentDescription = null, modifier = Modifier.size(30.dp))
        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1.0f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(stringResource(name), fontSize = 15.sp, fontWeight = FontWeight.Bold)

            Text(stringResource(description), color = LocalExtendedColors.current.textSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
fun PermissionPage(modifier: Modifier = Modifier, onProceedStateChanged: (Boolean) -> Unit) {
    val context = LocalContext.current
    var requestPhone by rememberSaveable { mutableStateOf(shouldRequestPermissions(context)) }

    DualTitleContent(
        modifier = modifier,
        icon = rememberVectorPainter(Icons.Rounded.EncryptedMinusCircle),
        title = R.string.welcome_permission,
        subtitle = if (requestPhone) R.string.welcome_permission_subtitle else R.string.welcome_permission_done,
    ) {
        AnimatedVisibility(visible = requestPhone) {
            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                requestPhone = !granted
                if (granted) {
                    MainScope().launch(Dispatchers.IO) {
                        App.Config.inited = false
                        App.Config.init(context)
                    }
                }
            }

            PermissionInfo(
                icon = rememberVectorPainter(Icons.Rounded.PhoneAndroid),
                name = R.string.common_permission_phone,
                description = R.string.tip_permission_phone,
                onClick = { launcher.launch(Manifest.permission.READ_PHONE_STATE) }
            )
        }
    }

    LaunchedEffect(requestPhone) {
        onProceedStateChanged(!requestPhone)
    }
}
