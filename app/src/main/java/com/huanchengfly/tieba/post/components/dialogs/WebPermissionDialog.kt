package com.huanchengfly.tieba.post.components.dialogs

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.preferencesDataStore
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.components.dialogs.WebPermissionDialog.Companion.STATE_ALLOW
import com.huanchengfly.tieba.post.components.dialogs.WebPermissionDialog.Companion.STATE_DENY
import com.huanchengfly.tieba.post.components.dialogs.WebPermissionDialog.Companion.STATE_UNSET
import com.huanchengfly.tieba.post.components.dialogs.WebPermissionDialog.Companion.WebPermission.APP
import com.huanchengfly.tieba.post.components.dialogs.WebPermissionDialog.Companion.WebPermission.CLIPBOARD
import com.huanchengfly.tieba.post.components.dialogs.WebPermissionDialog.Companion.WebPermission.File
import com.huanchengfly.tieba.post.components.dialogs.WebPermissionDialog.Companion.WebPermission.LOCATION
import com.huanchengfly.tieba.post.getInt
import com.huanchengfly.tieba.post.putInt
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultDialogContentPadding
import com.huanchengfly.tieba.post.ui.widgets.compose.NegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.PositiveButton
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes

class WebPermissionDialog<Result>(): ResultDialog<Result>() {

    lateinit var permission: WebPermission<Result>

    lateinit var host: String

    lateinit var message: String

    /**
     * Whether the permission should be retained
     * */
    private var retainPermission: Boolean by mutableStateOf(false)

    override fun getTAG(): String? = TAG

    private var permissionLauncher: ActivityResultLauncher<Array<String>>? = null

    private var filePickerLauncher: ActivityResultLauncher<Intent>? = null

    @Suppress("UNCHECKED_CAST")
    override fun onAttach(context: Context) {
        super.onAttach(context)
        val bundle = requireArguments()
        host = bundle.getString(KEY_HOST)!!
        message = bundle.getString(KEY_MESSAGE)!!
        permission = bundle.getPermission() as WebPermission<Result>

        when(permission) {
            is File -> {
                filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    dismissWithState(result = it)
                }
            }
            is LOCATION -> {
                permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                    val granted = it.values.any { it == true }
                    dismissWithState(result = granted)
                }
            }
            else -> {}
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState)
    }

    @Composable
    override fun ContentView() {
        val colorScheme = MaterialTheme.colorScheme
        val hideRetainBox = permission is File || permission is APP

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DefaultDialogContentPadding)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    painter = rememberVectorPainter(image = permission.getIcon()),
                    contentDescription = null,
                    modifier = Modifier.size(Sizes.Small),
                    tint = colorScheme.primary
                )

                Text(message, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Row(
                modifier = Modifier.padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!hideRetainBox) {
                    Checkbox(
                        checked = retainPermission,
                        onCheckedChange = { retainPermission = it }
                    )

                    Text(
                        text = stringResource(R.string.title_not_ask),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Spacer(modifier = Modifier.weight(1.0f))

                NegativeButton(text = getString(R.string.button_denied), onClick = ::onDenyClicked)

                Spacer(modifier = Modifier.width(8.dp))

                PositiveButton(text = getString(R.string.button_allow), onClick = ::onGrantClicked)
            }
        }
    }

    private fun onGrantClicked() {
        val permission = this.permission
        when(permission) {
            is File -> {
                filePickerLauncher!!.launch(permission.intent)
            }

            is LOCATION -> {
                permissionLauncher!!.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
            }

            else -> dismissWithState(result = true)
        }
    }

    private fun onDenyClicked() {
        if (permission is File) {
            dismissWithState(result = ActivityResult(Activity.RESULT_CANCELED, null))
        } else {
            dismissWithState(result = false)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun dismissWithState(result: Any) {
        dismissAllowingStateLoss()
        mResult.trySend(result as Result)

        if (result is Boolean && retainPermission) {
            requireContext().permissionDataStore.putInt(
                key = permission.toKey(host),
                value = if (result) STATE_ALLOW else STATE_DENY
            )
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {
        private const val TAG = "WebPermissionDialog"

        private val Context.permissionDataStore by preferencesDataStore(name = "permission")

        private const val KEY_HOST = "wp_host"
        private const val KEY_MESSAGE = "wp_msg"
        private const val KEY_PERMISSION = "wp_permission"
        private const val KEY_PERMISSION_EXTRA = "wp_permission_ex"

        const val STATE_ALLOW = 1
        const val STATE_DENY = 2
        const val STATE_UNSET = 0

        fun <Result> newInstance(permission: WebPermission<Result>, host: String, message: String): WebPermissionDialog<Result> {
            return WebPermissionDialog<Result>().apply {
                arguments = Bundle().also {
                    it.putString(KEY_HOST, host)
                    it.putString(KEY_MESSAGE, message)
                    it.putPermission(permission)
                }
            }
        }

        sealed class WebPermission<Result>(val ordinal: Int) {

            object LOCATION: WebPermission<Boolean>(ordinal = 0)

            object APP: WebPermission<Boolean>(ordinal = 1)

            object CLIPBOARD: WebPermission<Boolean>(ordinal = 2)

            class File(val intent: Intent): WebPermission<ActivityResult>(ordinal = 3)

            fun getIcon(): ImageVector = when(this) {
                is LOCATION -> Icons.Rounded.LocationOn
                is APP -> Icons.Rounded.Android
                is CLIPBOARD -> Icons.Rounded.ContentPaste
                is File -> Icons.Rounded.FileOpen
            }

            fun toKey(host: String): String {
                return "${ordinal}_$host"
            }
        }

        private fun Bundle.putPermission(permission: WebPermission<*>) {
            putInt(KEY_PERMISSION, permission.ordinal)
            if (permission is File) {
                putParcelable(KEY_PERMISSION_EXTRA, permission.intent)
            }
        }

        private fun Bundle.getPermission(): WebPermission<*> {
            val ordinal = getInt(KEY_PERMISSION, -1)
            if (ordinal == 3) {
                val extra: Intent = getParcelable(KEY_PERMISSION_EXTRA)!!
                return File(extra)
            } else {
                return listOf(LOCATION, APP, CLIPBOARD)
                    .getOrNull(ordinal) ?: throw IllegalArgumentException("$ordinal is not a WebPermission")
            }
        }

        /**
         * @return Retained permission state
         *
         * [STATE_ALLOW] Permission is granted forever to this [host] website
         *
         * [STATE_DENY] Permission is denied forever to this [host] website
         *
         * [STATE_UNSET] Unset
         * */
        fun getState(context: Context, host: String, permission: WebPermission<*>): Int {
            return context.permissionDataStore.getInt(
                key = permission.toKey(host),
                defaultValue = STATE_UNSET
            )
        }
    }
}