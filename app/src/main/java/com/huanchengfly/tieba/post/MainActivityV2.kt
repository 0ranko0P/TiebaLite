package com.huanchengfly.tieba.post

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavOptions
import androidx.navigation.compose.rememberNavController
import com.huanchengfly.tieba.post.MacrobenchmarkConstant.KEY_WELCOME_SETUP
import com.huanchengfly.tieba.post.arch.BaseComposeActivity
import com.huanchengfly.tieba.post.components.ClipBoardLinkDetector
import com.huanchengfly.tieba.post.components.ShortcutInitializer
import com.huanchengfly.tieba.post.components.ShortcutInitializer.Companion.TbShortcut
import com.huanchengfly.tieba.post.theme.ExtendedColorScheme
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.animateBackground
import com.huanchengfly.tieba.post.ui.models.settings.HabitSettings
import com.huanchengfly.tieba.post.ui.models.settings.UISettings
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.RootNavGraph
import com.huanchengfly.tieba.post.ui.page.TB_LITE_DOMAIN
import com.huanchengfly.tieba.post.ui.page.settings.theme.TranslucentThemeBackground
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogPositiveButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.StrongBox
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
import com.huanchengfly.tieba.post.utils.requestIgnoreBatteryOptimizations
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val LocalWindowAdaptiveInfo = staticCompositionLocalOf<WindowAdaptiveInfo> { error("No WindowAdaptiveInfo provided!") }

val LocalHabitSettings = compositionLocalOf<HabitSettings> { error("No HabitSettings provided!") }

val LocalUISettings = compositionLocalOf { UISettings() }

@AndroidEntryPoint
class MainActivityV2 : BaseComposeActivity() {

    private var pendingAppLink by mutableStateOf<Destination?>(null)

    private var pendingDeepLink by mutableStateOf<NavDeepLinkRequest?>(null)

    private val viewModel: MainViewModel by viewModels()

    /**
     * Used in Macrobenchmark to control the initial welcome screen state
     * */
    private var welcomeScreen: Boolean? = null

    private suspend fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && AccountUtil.isLoggedIn()) {
            askPermission(R.string.desc_permission_post_notifications, Manifest.permission.POST_NOTIFICATIONS, noRationale = true)
        }
    }

    @Suppress("KotlinConstantConditions")
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            ClientUtils.refreshActiveTimestamp()
            delay(2000L)
            requestNotificationPermission()
        }

        intent?.run {
            if (BuildConfig.BUILD_TYPE != "release") {
                welcomeScreen = extras?.getBoolean(KEY_WELCOME_SETUP, false)
            }
            ShortcutInitializer.getTbShortcut(this)?.also { onNewShortcut(it) }
            data?.normalizeScheme()?.let { pendingAppLink = appLinkToNavRoute(uri = it) }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        // Due to the privacy changes in Android 10, check Clipboard only when focused
        if (hasFocus) {
            viewModel.onCheckClipBoard()
        }
    }

    private fun onNewShortcut(shortcut: TbShortcut) {
        ShortcutManagerCompat.reportShortcutUsed(applicationContext, shortcut.id)
    }

    override fun onNewIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            ShortcutInitializer.getTbShortcut(intent)?.also { onNewShortcut(it) }
            val uri = intent.data?.normalizeScheme() ?: return
            // Is TbLite DeepLink
            if (uri.scheme == TB_LITE_DOMAIN) {
                pendingDeepLink = NavDeepLinkRequest.Builder.fromUri(uri).build()
            } else {
                pendingAppLink = appLinkToNavRoute(uri)
            }
        } else {
            super.onNewIntent(intent)
        }
    }

    @Composable
    override fun Content() {
        // val bottomSheetNavigator = rememberBottomSheetNavigator(skipPartiallyExpanded = true)
        val navController = rememberNavController(/* bottomSheetNavigator */)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        TiebaExtendedTheme(colorsExt = uiState.themeColor) {
            TiebaLiteLocalProvider(
                habit = uiState.habitSettings ?: return@TiebaExtendedTheme, // Initializing ...
                uiSettings = uiState.uiSettings ?: return@TiebaExtendedTheme
            ) {
                val setupFinished = if (welcomeScreen == null) {
                    uiState.uiSettings!!.setupFinished
                } else {
                    welcomeScreen == false // Override by Macrobenchmark
                }

                if (setupFinished) {
                    LaunchedDeepLinkEffect(navController)

                    StrongBox {
                        val preview by viewModel.previewInfoFlow.collectAsStateWithLifecycle()
                        ClipBoardDetectDialog(preview, viewModel::onClipBoardDetectDialogDismiss) {
                            val route: Destination = it.clipBoardLink.toRoute(avatarUrl = it.icon?.url)
                            navController.navigate(route = route)
                        }

                        if (uiState.autoSignRestricted) {
                            BatteryOpDialog(onOpenSettings = ::requestIgnoreBatteryOptimizations)
                        }
                    }
                } else {
                    intent.data = null
                }

                RootNavGraph(
                    // bottomSheetNavigator = bottomSheetNavigator,
                    navController = navController,
                    settingsRepo = viewModel.settingsRepository,
                    startDestination = if (setupFinished) Destination.Main else Destination.Welcome
                )
            }
        }
    }

    @Composable
    private fun TiebaExtendedTheme(colorsExt: ExtendedColorScheme, content: @Composable () -> Unit) {
        val backgroundImage by viewModel.translucentThemeBackground.collectAsStateWithLifecycle()

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

    @Composable
    private fun LaunchedDeepLinkEffect(navController: NavController) {
        LaunchedEffect(pendingDeepLink) {
            pendingDeepLink?.let {
                val navOptions = NavOptions.Builder().setLaunchSingleTop(true).build()
                runCatching {
                    navController.navigate(request = it, navOptions = navOptions)
                }
                .onFailure { e -> e.printStackTrace() }
                pendingDeepLink = null
            }
        }

        LaunchedEffect(pendingAppLink) {
            pendingAppLink?.let {
                navController.navigate(route = it)
                pendingAppLink = null
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EmoticonManager.clear()
    }

    @NonRestartableComposable
    @Composable
    private fun TiebaLiteLocalProvider(
        habit: HabitSettings,
        uiSettings: UISettings,
        content: @Composable () -> Unit
    ) {
        val currentAccount by viewModel.account.collectAsStateWithLifecycle(initialValue = null)
        CompositionLocalProvider(
            LocalAccount provides currentAccount,
            LocalHabitSettings provides habit,
            LocalUISettings provides uiSettings,
            content = content
        )
    }

    companion object {

        private fun appLinkToNavRoute(uri: Uri): Destination? {
            return ClipBoardLinkDetector.parseDeepLink(uri)?.toRoute()
        }

        @Composable
        private fun ClipBoardDetectDialog(
            preview: PreviewInfo?,
            onDismiss: () -> Unit,
            onOpen: (PreviewInfo) -> Unit
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
                    DialogPositiveButton(text = stringResource(id = R.string.button_open)) {
                        onOpen(preview)
                    }
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
            dialogState: DialogState = rememberDialogState(),
            onOpenSettings: () -> Unit
        ) {
            // Show dialog only once
            var dismissed by rememberSaveable { mutableStateOf(false) }
            if (dismissed) return

            LaunchedEffect(Unit) {
                delay(2000L)
                dialogState.show()
            }
            if (!dialogState.show) return

            Dialog(
                dialogState = dialogState,
                onDismiss = {
                    dismissed = true
                },
                title = { Text(text = stringResource(id = R.string.title_ignore_battery_optimization)) },
                content = {
                    Text(text = stringResource(id = R.string.tip_auto_sign))
                },
                buttons = {
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
