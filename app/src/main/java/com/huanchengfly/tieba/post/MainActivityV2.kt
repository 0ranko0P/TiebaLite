package com.huanchengfly.tieba.post

import android.Manifest
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonSkippableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.huanchengfly.tieba.post.arch.BaseComposeActivity
import com.huanchengfly.tieba.post.arch.collectIn
import com.huanchengfly.tieba.post.components.ClipBoardLinkDetector
import com.huanchengfly.tieba.post.services.NotifyJobService
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.animateBackground
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.RootNavGraph
import com.huanchengfly.tieba.post.ui.page.settings.theme.TranslucentThemeBackground
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogPositiveButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.AnyPopDialogProperties
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.DirectionState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.ClientUtils
import com.huanchengfly.tieba.post.utils.EmoticonManager
import com.huanchengfly.tieba.post.utils.LocalAccount
import com.huanchengfly.tieba.post.utils.PermissionUtils.askPermission
import com.huanchengfly.tieba.post.utils.QuickPreviewUtil
import com.huanchengfly.tieba.post.utils.QuickPreviewUtil.PreviewInfo
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.requestIgnoreBatteryOptimizations
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

val LocalNotificationCountFlow =
    staticCompositionLocalOf<Flow<Int>> { throw IllegalStateException("not allowed here!") }

val LocalWindowAdaptiveInfo =
    staticCompositionLocalOf<WindowAdaptiveInfo> { throw IllegalStateException("not allowed here!") }

@AndroidEntryPoint
class MainActivityV2 : BaseComposeActivity() {

    private var pendingRoute: Destination? by mutableStateOf(null)

    private val notificationCountFlow: MutableSharedFlow<Int> =
        MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private var mNewMessageReceiver: NewMessageReceiver? = null

    // Convert Deep Link intent to destination route if it's valid
    private fun handelDeepLinks(intent: Intent): Destination? = intent.data?.let { uri ->
        ClipBoardLinkDetector.parseDeepLink(uri)?.toRoute()
    }

    private val viewModel: MainViewModel by viewModels()

    override fun onNewIntent(intent: Intent) {
        val route: Destination? = handelDeepLinks(intent)
        if (route != null) {
            pendingRoute = route
        } else {
            super.onNewIntent(intent)
        }
    }

    private suspend fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && AccountUtil.isLoggedIn()) {
            askPermission(R.string.desc_permission_post_notifications, Manifest.permission.POST_NOTIFICATIONS, noRationale = true)
        }
    }

    override fun onStart() {
        super.onStart()
        AccountUtil.getInstance().currentAccount.collectIn(this) { account ->
            if (account == null) return@collectIn
            if (mNewMessageReceiver != null) return@collectIn

            runCatching {
                mNewMessageReceiver = NewMessageReceiver()
                ContextCompat.registerReceiver(this@MainActivityV2, mNewMessageReceiver!!,
                    IntentFilter(NotifyJobService.ACTION_NEW_MESSAGE), ContextCompat.RECEIVER_NOT_EXPORTED)

                val notifyJobService = ComponentName(this, NotifyJobService::class.java)
                val builder = JobInfo.Builder(appPreferences.autoSignJobId, notifyJobService)
                    .setPersisted(true)
                    .setPeriodic(30 * 60 * 1000L)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                val jobScheduler = getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
                jobScheduler.schedule(builder.build())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            ClientUtils.refreshActiveTimestamp()
            delay(2000L)
            requestNotificationPermission()
        }
        intent?.let { pendingRoute = handelDeepLinks(it) }

        runCatching {
            TiebaUtil.initAutoSign(this)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        // Due to the privacy changes in Android 10, check Clipboard only when focused
        if (hasFocus) {
            viewModel.onCheckClipBoard()
        }
    }

    @Composable
    override fun Content() {
        // val bottomSheetNavigator = rememberBottomSheetNavigator(skipPartiallyExpanded = true)
        val navController = rememberNavController(/* bottomSheetNavigator */)

        TiebaLiteLocalProvider {
            TiebaExtendedTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val okSignAlertDialogState = rememberDialogState()

                uiState.setupFinished?.let { setupFinished ->
                    val entryRoute = if (setupFinished) Destination.Main else Destination.Welcome
                    RootNavGraph(/* bottomSheetNavigator, */navController, startDestination = entryRoute)

                    if (setupFinished) {
                        LaunchedEffect(pendingRoute) {
                            pendingRoute?.let {
                                navController.navigate(route = it)
                                pendingRoute = null
                            }
                        }
                    }
                }

                ClipBoardDetectDialog(uiState.preview, viewModel::onClipBoardDetectDialogDismiss) {
                    uiState.preview?.let {
                        val route: Destination = it.clipBoardLink.toRoute(avatarUrl = it.icon?.url)
                        navController.navigate(route = route)
                    }
                }

                if (uiState.ignoreBatteryOpDialogVisible) {
                    BatteryOpDialog(
                        dialogState = okSignAlertDialogState,
                        onDismiss = viewModel::onDismissBatteryOpDialog,
                        onIgnore = viewModel::onIgnoreBatteryOpDialog,
                        onOpenSettings = this::requestIgnoreBatteryOptimizations
                    )

                    LaunchedEffect(Unit) {
                        delay(2000L)
                        okSignAlertDialogState.show()
                    }
                }
            }
        }
    }

    @Composable
    private fun TiebaExtendedTheme(content: @Composable () -> Unit) {
        val colorsExt by viewModel.extendedColorScheme.collectAsStateWithLifecycle()
        val backgroundImage by viewModel.translucentThemeBackground.collectAsStateWithLifecycle(null)

        Box(modifier = Modifier.fillMaxSize()) {
            if (backgroundImage != null) {
                TranslucentThemeBackground(Modifier.matchParentSize(), file = backgroundImage)
            } else {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .animateBackground(color = colorsExt.colorScheme.background)
                )
            }
            TiebaLiteTheme(colorSchemeExt = colorsExt, content = content)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EmoticonManager.clear()
        mNewMessageReceiver?.let { unregisterReceiver(it) }
    }

    @NonSkippableComposable
    @Composable
    private fun TiebaLiteLocalProvider(content: @Composable () -> Unit) {
        val currentAccount by viewModel.account.collectAsStateWithLifecycle()
        CompositionLocalProvider(
            LocalAccount provides currentAccount,
            LocalNotificationCountFlow provides notificationCountFlow,
            content = content
        )
    }

    private inner class NewMessageReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == NotifyJobService.ACTION_NEW_MESSAGE) {
                val channel = intent.getStringExtra("channel")
                val count = intent.getIntExtra("count", 0)
                if (channel != null && channel == NotifyJobService.CHANNEL_TOTAL) {
                    lifecycleScope.launch {
                        notificationCountFlow.emit(count)
                    }
                }
            }
        }
    }

    companion object {

        @Composable
        private fun ClipBoardDetectDialog(
            preview: PreviewInfo?,
            onDismiss: () -> Unit,
            onOpen: () -> Unit
        ) {
            val dialogState = rememberDialogState()

            if (preview == null) return
            LaunchedEffect(Unit) {
                if (!dialogState.show) dialogState.show()
            }

            Dialog(
                dialogState = dialogState,
                dialogProperties = AnyPopDialogProperties(
                    direction = DirectionState.CENTER,
                    dismissOnClickOutside = false
                ),
                onDismiss = onDismiss,
                title = {
                    Text(text = stringResource(id = R.string.title_dialog_clip_board_tieba_url))
                },
                buttons = {
                    DialogNegativeButton(text = stringResource(id = R.string.btn_close))
                    DialogPositiveButton(text = stringResource(id = R.string.button_open), onClick = onOpen)
                },
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        preview.icon?.let { icon ->
                            val iconShape = MaterialTheme.shapes.extraSmall
                            if (icon.type == QuickPreviewUtil.Icon.TYPE_DRAWABLE_RES) {
                                Avatar(data = icon.res, size = Sizes.Medium, shape = iconShape)
                            } else {
                                Avatar(data = icon.url, size = Sizes.Medium, shape = iconShape)
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            preview.title?.let { title ->
                                Text(text = title, style = MaterialTheme.typography.titleMedium)
                            }
                            preview.subtitle?.let { subtitle ->
                                Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        @Composable
        private fun BatteryOpDialog(
            dialogState: DialogState,
            onDismiss: () -> Unit,
            onIgnore: () -> Unit,
            onOpenSettings: () -> Unit
        ) {
            Dialog(
                dialogState = dialogState,
                onDismiss = onDismiss,
                title = { Text(text = stringResource(id = R.string.title_dialog_oksign_battery_optimization)) },
                content = {
                    Text(text = stringResource(id = R.string.message_dialog_oksign_battery_optimization))
                },
                buttons = { rowScope ->
                    DialogNegativeButton(text = stringResource(id = R.string.button_dont_remind_again), onClick = onIgnore)

                    with(rowScope) { Spacer(modifier = Modifier.weight(1.0f)) }

                    DialogNegativeButton(text = stringResource(id = R.string.button_cancel))

                    DialogPositiveButton(
                        text = stringResource(id = R.string.btn_open_settings),
                        onClick = onOpenSettings
                    )
                }
            )
        }
    }
}
