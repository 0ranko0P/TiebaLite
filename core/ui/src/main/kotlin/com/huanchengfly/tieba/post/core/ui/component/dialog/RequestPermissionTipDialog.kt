package com.huanchengfly.tieba.post.core.ui.component.dialog

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Window
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.mediaQuery
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.window.core.layout.WindowSizeClass
import com.huanchengfly.tieba.post.core.designsystem.component.navigationsuite.WidthSizeClasses
import com.huanchengfly.tieba.post.core.designsystem.theme.Grey700
import com.huanchengfly.tieba.post.core.designsystem.theme.Grey800
import com.huanchengfly.tieba.post.core.ui.R
import com.huanchengfly.tieba.post.core.ui.component.NegativeButton
import com.huanchengfly.tieba.post.core.ui.component.PositiveButton
import com.huanchengfly.tieba.post.core.ui.component.dialog.RequestPermissionTipDialog.Companion.PermissionRationale
import com.huanchengfly.tieba.post.core.ui.component.dialog.RequestPermissionTipDialog.Companion.PermissionTip
import com.huanchengfly.tieba.post.core.ui.util.PermissionUtils
import com.huanchengfly.tieba.post.core.ui.util.PermissionUtils.Result
import com.huanchengfly.tieba.post.core.ui.util.ThemeUtil
import com.huanchengfly.tieba.post.core.ui.util.buildAppSettingsIntent
import com.huanchengfly.tieba.post.core.ui.util.enableBackgroundBlur
import com.huanchengfly.tieba.post.core.ui.util.toastShort
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

class RequestPermissionTipDialog() : ResultDialog<Result>(), ActivityResultCallback<Map<String, Boolean>> {

    private val _result = Channel<Result>(capacity = 1)
    val result: ReceiveChannel<Result>
        get() = _result

    private lateinit var description: String
    private lateinit var permissionName: String
    private lateinit var message: String

    private lateinit var permissions: Array<String>
    private var permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions(), callback = this)
    private var launcherShowed = false

    // Launch app settings, let user grant permission manually
    private val settingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val context = this.context ?: return@registerForActivityResult
        // Back from settings activity, check permissions now
        val permissionMap = permissions.associateWith {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        onActivityResult(result = permissionMap)
    }

    /**
     * Whether to show the permission tip or the rationale message.
     *
     * @see shouldShowRequestPermissionRationale
     * @see PermissionTip
     * @see PermissionRationale
     * */
    private var showRationale by mutableStateOf(false)

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val bundle = requireArguments()
        permissions = bundle.getStringArray(KEY_PERMISSIONS)!!
        description = bundle.getString(KEY_DESCRIPTION)!!
        permissionName = PermissionUtils.transformText(context, *permissions).first()
        message = context.getString(R.string.core_ui_request_permission_tip, description)
        showRationale = permissions.any { shouldShowRequestPermissionRationale(it)}
    }

    @Composable
    override fun BoxScope.DialogView() {
        if (!showRationale) {
            PermissionTip(
                modifier = Modifier.align(Alignment.TopCenter),
                title = getString(R.string.core_ui_request_permission, permissionName),
                message = message
            )
        }
    }

    @Composable
    override fun ContentView() {
        if (showRationale) {
            PermissionRationale(
                rationale = getString(R.string.core_ui_request_permission_rationale, permissionName),
                message = message,
                onDeny = {
                    onActivityResult(permissions.associateWith { false })
                },
                onGrant = this@RequestPermissionTipDialog::openSettings
            )
        }
    }

    override fun onSetupWindow(window: Window, backgroundBlur: Boolean) {
        super.onSetupWindow(window, backgroundBlur = false)
        window.attributes = window.attributes.apply {
            // Enable blur effect only when showing rationale message
            backgroundColor = if (showRationale) {
                enableBackgroundBlur()
                ThemeUtil.currentColorScheme().background.copy(0.2f)
            } else {
                ThemeUtil.currentColorScheme().background.copy(0.96f)
            }
        }

        if (!showRationale && !launcherShowed) {
            launcherShowed = true
            permissionLauncher.launch(permissions)
        }
    }

    override fun onActivityResult(result: Map<String, Boolean>) {
        val denylist = result.filter { (_, granted) -> !granted }.keys
        _result.trySend(
            if (denylist.isEmpty()) {
                Result.Grant
            } else {
                Result.Deny(denylist)
            }
        )
        dismiss()
    }

    override fun onDestroy() {
        super.onDestroy()
        permissionLauncher.unregister()
        settingsLauncher.unregister()
        _result.close()
    }

    fun show(activity: FragmentActivity): RequestPermissionTipDialog {
        show(activity.supportFragmentManager, TAG)
        return this
    }

    private fun openSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            TextUtils.equals(permissions[0], Manifest.permission.POST_NOTIFICATIONS)
        ) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
            }
        } else {
            buildAppSettingsIntent(requireContext().packageName)
        }

        runCatching {
            settingsLauncher.launch(intent)
        }
        .onFailure {
            context?.toastShort(R.string.core_ui_error_open_settings)
            dismiss()
        }
    }

    companion object {
        private const val TAG = "PermissionTipDialog"

        private const val KEY_PERMISSIONS = "d_permissions"

        private const val KEY_DESCRIPTION = "d_desc"

        private val dialogAdaptiveFraction: Float
            @Composable get() = mediaQuery {
                if (windowWidth < WindowSizeClass.WidthSizeClasses.Medium) 0.95f else 0.6f
            }

        fun newInstance(description: String, vararg permissions: String): RequestPermissionTipDialog {
            return RequestPermissionTipDialog().apply {
                arguments = Bundle().also {
                    it.putString(KEY_DESCRIPTION, description)
                    it.putStringArray(KEY_PERMISSIONS, permissions)
                }
            }
        }

        @Composable
        private fun PermissionTip(modifier: Modifier = Modifier, title: String, message: String) {
            Surface(
                modifier = modifier.fillMaxWidth(dialogAdaptiveFraction).widthIn(max = 560.dp),
                shape = MaterialTheme.shapes.large,
                color = Color.White, // Hardcode background color
                contentColor = Grey800,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        imageVector = Icons.Rounded.PrivacyTip,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1.0f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(text = title, color = Grey800, fontWeight = FontWeight.Bold)
                        Text(text = message, color = Grey700, fontSize = 13.sp)
                    }
                }
            }
        }

        @Composable
        private fun PermissionRationale(
            modifier: Modifier = Modifier,
            rationale: String,
            message: String,
            onGrant: () -> Unit,
            onDeny: () -> Unit
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth(dialogAdaptiveFraction)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.core_ui_permission_rationale_title),
                    style = MaterialTheme.typography.titleLarge
                )

                Column(
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text(text = rationale, style = MaterialTheme.typography.bodyLarge)
                    Text(text = message, style = MaterialTheme.typography.bodyLarge)
                }

                Row(
                    modifier = Modifier.align(Alignment.End)
                ) {
                    NegativeButton(
                        text = stringResource(android.R.string.cancel),
                        onClick = onDeny
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    PositiveButton(
                        text = stringResource(R.string.core_ui_btn_grant_manually),
                        onClick = onGrant
                    )
                }
            }
        }
    }
}