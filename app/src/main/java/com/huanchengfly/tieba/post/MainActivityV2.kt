package com.huanchengfly.tieba.post

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.util.Consumer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation3.runtime.NavKey
import com.huanchengfly.tieba.post.MacrobenchmarkConstant.EXTRA_REDUCE_EFFECT
import com.huanchengfly.tieba.post.MacrobenchmarkConstant.EXTRA_WELCOME_SETUP
import com.huanchengfly.tieba.post.arch.BaseComposeActivity
import com.huanchengfly.tieba.post.components.ClipBoardLinkDetector
import com.huanchengfly.tieba.post.components.ClipBoardLinkDetector.isHttp
import com.huanchengfly.tieba.post.components.ShortcutInitializer
import com.huanchengfly.tieba.post.components.TbDeepLinkMatcher
import com.huanchengfly.tieba.post.components.TbDeepLinkMatcher.isTbLiteDeepLink
import com.huanchengfly.tieba.post.core.data.model.settings.HabitSettings
import com.huanchengfly.tieba.post.core.data.model.settings.UISettings
import com.huanchengfly.tieba.post.core.data.session.ClientConfigManager
import com.huanchengfly.tieba.post.core.navigation.LocalNavigator
import com.huanchengfly.tieba.post.core.navigation.Navigator
import com.huanchengfly.tieba.post.core.navigation.rememberNavigator
import com.huanchengfly.tieba.post.core.network.exception.getErrorMessage
import com.huanchengfly.tieba.post.theme.LocalExtendedColorScheme
import com.huanchengfly.tieba.post.ui.common.LocalPbInlineContentCache
import com.huanchengfly.tieba.post.ui.common.PbInlineContentCache.Companion.rememberPbInlineContentCache
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.main.MainDestination
import com.huanchengfly.tieba.post.ui.page.main.MainPage
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
import com.huanchengfly.tieba.post.ui.widgets.compose.video.LocalVideoPreviewState
import com.huanchengfly.tieba.post.ui.widgets.compose.video.rememberVideoPreviewState
import com.huanchengfly.tieba.post.utils.LocalAccount
import com.huanchengfly.tieba.post.utils.PermissionUtils.askPermission
import com.huanchengfly.tieba.post.utils.QuickPreviewUtil
import com.huanchengfly.tieba.post.utils.QuickPreviewUtil.PreviewInfo
import com.huanchengfly.tieba.post.utils.requestIgnoreBatteryOptimizations
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

val LocalWindowAdaptiveInfo = staticCompositionLocalOf<WindowAdaptiveInfo> { error("No WindowAdaptiveInfo provided!") }

val LocalHabitSettings = compositionLocalOf<HabitSettings> { error("No HabitSettings provided!") }

val LocalUISettings = compositionLocalOf { UISettings() }

@AndroidEntryPoint
class MainActivityV2 : BaseComposeActivity() {

    private var launchDeepLink: NavKey? = null

    private val viewModel: MainViewModel by viewModels()

    @Inject lateinit var clientConfigManagerProvider: Provider<ClientConfigManager>

    /** Used to control the initial welcome screen state in Macrobenchmark */
    private var welcomeScreen: Boolean? = null

    private var reduceEffect: Boolean? = null

    private suspend fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && viewModel.account.first() != null/* isLoggedIn */) {
            askPermission(R.string.desc_permission_post_notifications, Manifest.permission.POST_NOTIFICATIONS, noRationale = true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            clientConfigManagerProvider.get().refreshActiveTimestamp()
            delay(2000L)
            runCatching {
                requestNotificationPermission()
            }
        }

        intent?.takeIf { savedInstanceState == null }?.run {
            if (MacrobenchmarkConstant.TRACE_ENABLED) {
                reduceEffect = extras?.getBoolean(EXTRA_REDUCE_EFFECT, false)
                welcomeScreen = extras?.getBoolean(EXTRA_WELCOME_SETUP, false)
            }
            launchDeepLink = handleIntentAction(intent = this)
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
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val habitSettings = uiState.habitSettings ?: return // Initializing ...
        val uiSettings = uiState.uiSettings ?: return
        val videoPreviewState = if (!habitSettings.hideMedia && habitSettings.videoAutoplay) {
            rememberVideoPreviewState(viewModel.playerPool)
        } else {
            null
        }

        val currentAccount by viewModel.account.collectAsStateWithLifecycle(initialValue = null)
        val setupFinished = welcomeScreen?.not() ?: uiSettings.setupFinished
        val navigator = rememberNavigator(
            startKey = if (setupFinished) MainDestination.Home else Destination.Welcome,
            topLevelKeys = rememberTopNavKeys()
        )
        CompositionLocalProvider(
            LocalExtendedColorScheme provides uiState.themeColor,
            LocalWindowAdaptiveInfo provides currentWindowAdaptiveInfoV2(),
            LocalAccount provides currentAccount,
            LocalNavigator provides navigator,
            LocalHabitSettings provides habitSettings,
            LocalUISettings provides uiSettings,
            LocalPbInlineContentCache provides rememberPbInlineContentCache(),
            LocalVideoPreviewState provides videoPreviewState,
        ) {
            MaterialExpressiveTheme(
                colorScheme = uiState.themeColor.colorScheme,
                motionScheme = if (!uiSettings.reduceMotion) MotionScheme.expressive() else MotionScheme.standard(),
            ) {
                TiebaBackground()
                MainPage(navigator, settingsRepo = viewModel.settingsRepository)
            }
        }

        if (setupFinished) {
            LaunchedDeepLinkEffect(navigator)

            StrongBox {
                val preview by viewModel.previewInfoFlow.collectAsStateWithLifecycle()
                ClipBoardDetectDialog(preview, viewModel::onClipBoardDetectDialogDismiss) {
                    val navKey: Destination = it.clipBoardLink.toRoute(avatarUrl = it.icon?.url)
                    navigator.navigate(navKey)
                }

                if (uiState.autoSignRestricted) {
                    BatteryOpDialog(onOpenSettings = ::requestIgnoreBatteryOptimizations)
                }
            }
        }
    }

    @Composable
    private fun TiebaBackground() {
        val backgroundImage by viewModel.translucentThemeBackground.collectAsStateWithLifecycle()
        if (backgroundImage != null) {
            TranslucentThemeBackground(modifier = Modifier.fillMaxSize(), file = backgroundImage)
        } else {
            val background = MaterialTheme.colorScheme.background
            Spacer(modifier = Modifier.fillMaxSize().background(color = background))
        }
    }

    private fun handleIntentAction(intent: Intent): NavKey? {
        if (intent.action != Intent.ACTION_VIEW) {
            Log.w(TAG, "onHandleIntentAction: Unsupported action: ${intent.action}")
            return null
        }
        ShortcutInitializer.getTbShortcut(intent)?.let { shortcut ->
            ShortcutManagerCompat.reportShortcutUsed(applicationContext, shortcut.id)
        }
        val uri = intent.data?.normalizeScheme() ?: return null
        return when {
            uri.isTbLiteDeepLink() -> TbDeepLinkMatcher.resolveDeepLink(uri)

            uri.isHttp() -> appLinkToNavKey(uri) ?: uri.toString().let {
                Destination.WebView(it, customClient = ClipBoardLinkDetector.isTieba(it))
            }

            else -> null
        }
    }

    @Composable
    private fun LaunchedDeepLinkEffect(navigator: Navigator) {
        LaunchedEffect(Unit) {
            callbackFlow {
                val consumer = Consumer<Intent> { trySend(it) }
                addOnNewIntentListener(consumer)
                awaitClose { removeOnNewIntentListener(consumer) }
            }
            .collectLatest { newIntent ->
                val navKey = handleIntentAction(intent = newIntent) ?: return@collectLatest
                navigator.navigate(navKey)
            }
        }

        launchDeepLink?.let {
            LaunchedEffect(Unit) {
                launchDeepLink = null
                navigator.navigate(key = it)
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"

        private fun Context.appLinkToNavKey(uri: Uri): Destination? {
            return ClipBoardLinkDetector.parseDeepLink(uri)
                .onFailure {
                    toastShort(it.getErrorMessage())
                }
                .getOrNull()
                ?.toRoute()
        }

        @Composable
        private fun rememberTopNavKeys(): List<NavKey> = remember {
            listOf(
                MainDestination.Home,
                MainDestination.Explore,
                MainDestination.Notification,
                MainDestination.User,
                Destination.Welcome,
            )
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
