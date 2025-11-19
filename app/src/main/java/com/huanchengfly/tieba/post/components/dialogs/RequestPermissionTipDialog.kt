package com.huanchengfly.tieba.post.components.dialogs

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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.components.dialogs.RequestPermissionTipDialog.Companion.PermissionRationale
import com.huanchengfly.tieba.post.components.dialogs.RequestPermissionTipDialog.Companion.PermissionTip
import com.huanchengfly.tieba.post.enableBackgroundBlur
import com.huanchengfly.tieba.post.theme.Grey600
import com.huanchengfly.tieba.post.theme.Grey800
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultDialogMargin
import com.huanchengfly.tieba.post.ui.widgets.compose.NegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.PositiveButton
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogAdaptiveFraction
import com.huanchengfly.tieba.post.utils.PermissionUtils
import com.huanchengfly.tieba.post.utils.PermissionUtils.Result
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.buildAppSettingsIntent
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
     * Whether display permission tip or rationale message
     *
     * @see shouldShowRequestPermissionRationale
     * @see PermissionTip
     * @see PermissionRationale
     * */
    private var showRationale by mutableStateOf(false)

    override fun getTheme(): Int = R.style.Dialog_RequestPermissionTip

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val bundle = requireArguments()
        permissions = bundle.getStringArray(KEY_PERMISSIONS)!!
        description = bundle.getString(KEY_DESCRIPTION)!!
        permissionName = PermissionUtils.transformText(context, *permissions).first()
        message = context.getString(R.string.message_request_permission_tip_dialog, description)
    }

    @Composable
    override fun BoxScope.ContentView(savedInstanceState: Bundle?) {
        if (showRationale) {
            PermissionRationale(
                modifier = Modifier.align(Alignment.Center),
                title = stringResource(R.string.title_permission_rationale, permissionName),
                message = message,
                onDeny = {
                    onActivityResult(permissions.associateWith { false })
                },
                onGrant = this@RequestPermissionTipDialog::openSettings
            )
        } else {
            PermissionTip(
                title =  stringResource(R.string.title_request_permission_tip_dialog, permissionName),
                message = message
            )
        }
    }

    override fun onSetupWindow(window: Window, backgroundBlur: Boolean) {
        super.onSetupWindow(window, backgroundBlur = false)
        showRationale = permissions.any { shouldShowRequestPermissionRationale(it)}

        window.attributes = window.attributes.apply {
            // Enable blur effect only when showing rationale message
            backgroundColor = if (showRationale) {
                enableBackgroundBlur()
                ThemeUtil.currentColorScheme().background.copy(0.2f)
            } else {
                ThemeUtil.currentColorScheme().background.copy(0.86f)
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
                putExtra(Settings.EXTRA_APP_PACKAGE, BuildConfig.APPLICATION_ID)
            }
        } else {
            buildAppSettingsIntent(BuildConfig.APPLICATION_ID)
        }

        runCatching {
            settingsLauncher.launch(intent)
        }
        .onFailure {
            context?.toastShort(R.string.error_open_settings)
            dismiss()
        }
    }

    companion object {
        private const val TAG = "PermissionTipDialog"

        private const val KEY_PERMISSIONS = "d_permissions"

        private const val KEY_DESCRIPTION = "d_desc"

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
            Row(
                modifier = modifier
                    .fillMaxWidth(dialogAdaptiveFraction)
                    .background(Color.White, MaterialTheme.shapes.large) // Hardcode background color
                    .padding(DefaultDialogMargin),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_round),
                    contentDescription = null,
                    modifier = Modifier.size(Sizes.Small)
                )

                Column(
                    modifier = Modifier.weight(1.0f)
                ) {
                    Text(title, color = Grey800, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(message, color = Grey600, fontSize = 12.sp)
                }
            }
        }

        @Composable
        private fun PermissionRationale(
            modifier: Modifier = Modifier,
            title: String,
            message: String,
            onGrant: () -> Unit,
            onDeny: () -> Unit
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth(dialogAdaptiveFraction)
                    .padding(DefaultDialogMargin),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = title, fontWeight = FontWeight.Bold)

                Text(message, Modifier.align(Alignment.Start))

                Row(
                    modifier = Modifier.align(Alignment.End)
                ) {
                    NegativeButton(text = stringResource(R.string.button_cancel), onClick = onDeny)

                    Spacer(modifier = Modifier.width(8.dp))

                    PositiveButton(text = stringResource(R.string.button_go_to_grant), onClick = onGrant)
                }
            }
        }
    }
}