package com.huanchengfly.tieba.post.components.dialogs

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Window
import android.view.WindowManager
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.enableBackgroundBlur
import com.huanchengfly.tieba.post.theme.Grey600
import com.huanchengfly.tieba.post.theme.Grey800
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.NegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.PositiveButton
import com.huanchengfly.tieba.post.utils.PermissionUtils
import com.huanchengfly.tieba.post.utils.PermissionUtils.Result
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.buildAppSettingsIntent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

class RequestPermissionTipDialog() : DialogFragment(), ActivityResultCallback<Map<String, Boolean>> {

    private var backgroundColor by mutableStateOf(Color.Transparent)

    private val _result = Channel<Result>(capacity = 1)
    val result: ReceiveChannel<Result>
        get() = _result

    private lateinit var permissions: Array<String>
    private var permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions(), callback = this)
    private var launcherShowed = false

    // Launch app settings, let user grant permission manually
    private val settingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val context = this.context?: return@registerForActivityResult
        val permissionMap = permissions.associate {
            it to (ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED)
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

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val bundle = requireArguments()
        permissions = bundle.getStringArray(KEY_PERMISSIONS)!!
        val description = bundle.getString(KEY_DESCRIPTION)!!
        val permissionName = PermissionUtils.transformText(context, *permissions).first()
        val message = context.getString(R.string.message_request_permission_tip_dialog, description)

        return AlertDialog.Builder(context, theme)
            .setView(ComposeView(context).apply {
                setContent {
                    val theme by ThemeUtil.themeState
                    TiebaLiteTheme(theme) {
                        DialogContent(permissionName, message)
                    }
                }
            })
            .create()
    }

    @Composable
    private fun DialogContent(name: String, message: String) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(16.dp)
        ) {
            if (showRationale) {
                PermissionRationale(
                    modifier = Modifier.align(Alignment.Center),
                    title = stringResource(R.string.title_permission_rationale, name),
                    message = message,
                    onDeny = {
                        onActivityResult(permissions.associate { it to false })
                    },
                    onGrant = this@RequestPermissionTipDialog::openSettings
                )
            } else {
                PermissionTip(
                    title =  stringResource(R.string.title_request_permission_tip_dialog, name),
                    message = message
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        showRationale = permissions.any { shouldShowRequestPermissionRationale(it)}

        dialog?.window?.apply {
            onSetupWindow(window = this)
            if (!showRationale && !launcherShowed) {
                launcherShowed = true
                permissionLauncher.launch(permissions)
            }
        }
    }

    // Setup Fullscreen dialog
    private fun onSetupWindow(window: Window) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.attributes = window.attributes.apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT

            // Enable blur effect when showing rationale message
            if (showRationale && this.enableBackgroundBlur(window.context) != null) {
                backgroundColor = ThemeUtil.getRawTheme().windowBackground.copy(0.2f)
            } else {
                backgroundColor = ThemeUtil.getRawTheme().windowBackground.copy(0.86f)
            }
        }
    }

    override fun onActivityResult(result: Map<String, Boolean>) {
        val denylist = result.filter { it.value == false }.keys
        val rec = if (denylist.isEmpty()) Result.Grant else Result.Deny(denylist)
        _result.trySend(rec)
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
        with(Intent()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                permissions.size == 1 && permissions[0] == Manifest.permission.POST_NOTIFICATIONS
            ) {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, BuildConfig.APPLICATION_ID)
            } else {
                buildAppSettingsIntent(BuildConfig.APPLICATION_ID)
            }
            settingsLauncher.launch(this)
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
                    .fillMaxWidth()
                    .shadow(AppBarDefaults.TopAppBarElevation, RoundedCornerShape(16.dp))
                    .background(Color.White, RoundedCornerShape(16.dp)) // Hardcode background color
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_round),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
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
            val theme = ExtendedTheme.colors

            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .shadow(AppBarDefaults.TopAppBarElevation, RoundedCornerShape(16.dp))
                    .background(theme.windowBackground, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = title, color = theme.text, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(12.dp))

                Text(message, Modifier.align(Alignment.Start), color = theme.textSecondary)

                Spacer(modifier = Modifier.height(12.dp))

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