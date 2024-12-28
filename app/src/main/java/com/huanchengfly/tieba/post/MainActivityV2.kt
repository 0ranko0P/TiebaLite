package com.huanchengfly.tieba.post

import android.Manifest
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.arch.BaseComposeActivity
import com.huanchengfly.tieba.post.components.ClipBoardLinkDetector
import com.huanchengfly.tieba.post.services.NotifyJobService
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.RootNavGraph
import com.huanchengfly.tieba.post.ui.page.rememberBottomSheetNavigator
import com.huanchengfly.tieba.post.ui.utils.DevicePosture
import com.huanchengfly.tieba.post.ui.utils.isBookPosture
import com.huanchengfly.tieba.post.ui.utils.isSeparating
import com.huanchengfly.tieba.post.ui.widgets.compose.AlertDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.AvatarIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogPositiveButton
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils.Companion.KEY_IGNORE_BATTERY_OPTIMIZATION
import com.huanchengfly.tieba.post.utils.ClientUtils
import com.huanchengfly.tieba.post.utils.JobServiceUtil
import com.huanchengfly.tieba.post.utils.PermissionUtils.askPermission
import com.huanchengfly.tieba.post.utils.QuickPreviewUtil
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.isIgnoringBatteryOptimizations
import com.huanchengfly.tieba.post.utils.requestIgnoreBatteryOptimizations
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

val LocalNotificationCountFlow =
    staticCompositionLocalOf<Flow<Int>> { throw IllegalStateException("not allowed here!") }
val LocalDevicePosture =
    staticCompositionLocalOf<State<DevicePosture>> { throw IllegalStateException("not allowed here!") }

@AndroidEntryPoint
class MainActivityV2 : BaseComposeActivity() {

    private var pendingRoute: Destination? by mutableStateOf(null)

    private val notificationCountFlow: MutableSharedFlow<Int> =
        MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val devicePostureFlow: StateFlow<DevicePosture> by lazy {
        WindowInfoTracker.getOrCreate(this)
            .windowLayoutInfo(this)
            .flowWithLifecycle(lifecycle)
            .map { layoutInfo ->
                val foldingFeature =
                    layoutInfo.displayFeatures
                        .filterIsInstance<FoldingFeature>()
                        .firstOrNull()
                when {
                    isBookPosture(foldingFeature) ->
                        DevicePosture.BookPosture(foldingFeature.bounds)

                    isSeparating(foldingFeature) ->
                        DevicePosture.Separating(foldingFeature.bounds, foldingFeature.orientation)

                    else -> DevicePosture.NormalPosture
                }
            }
            .stateIn(
                scope = lifecycleScope,
                started = SharingStarted.Eagerly,
                initialValue = DevicePosture.NormalPosture
            )
    }

    // Convert Deep Link intent to destination route if it's valid
    private fun handelDeepLinks(intent: Intent): Destination? = intent.data?.let { uri ->
        ClipBoardLinkDetector.parseDeepLink(uri)?.toRoute()
    }

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
        runCatching {
            AccountUtil.getInstance().currentAccount.value ?: throw TiebaNotLoggedInException()
            val intentFilter = IntentFilter(NotifyJobService.ACTION_NEW_MESSAGE)
            ContextCompat.registerReceiver(this, NewMessageReceiver(), intentFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
            startService(Intent(this, NotifyJobService::class.java))
            val builder = JobInfo.Builder(
                JobServiceUtil.getJobId(this),
                ComponentName(this, NotifyJobService::class.java)
            )
                .setPersisted(true)
                .setPeriodic(30 * 60 * 1000L)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.schedule(builder.build())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundColor(0)
        window.setBackgroundDrawable(ColorDrawable(0))
        lifecycleScope.launch {
            ClientUtils.setActiveTimestamp(applicationContext)
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
            ClipBoardLinkDetector.checkClipBoard(owner = this, context = this)
        }
    }

    @Composable
    override fun Content() {
        val bottomSheetNavigator = rememberBottomSheetNavigator(skipHalfExpanded = true)
        val navController = rememberNavController(bottomSheetNavigator)
        val entryRoute = if (appPreferences.setupFinished) Destination.Main else Destination.Welcome

        BatteryOpDialog(this, appPreferences)
        ClipBoardDetectDialog(navController)

        TiebaLiteLocalProvider {
            TranslucentThemeBackground {
                RootNavGraph(bottomSheetNavigator, navController, entryRoute)
            }
        }

        pendingRoute?.let {
            LaunchedEffect(it) {
                navController.navigate(route = it)
                pendingRoute = null
            }
        }
    }

    @OptIn(ExperimentalGlideComposeApi::class)
    @Composable
    private fun TranslucentThemeBackground(
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit,
    ) {
        val backgroundColor by animateColorAsState(
            targetValue = MaterialTheme.colors.background,
            animationSpec = TweenSpec(durationMillis = AnimationConstants.DefaultDurationMillis),
            label = "BackgroundColorAnimation"
        )
        Surface(
            color = backgroundColor,
            contentColor = MaterialTheme.colors.onBackground,
            modifier = modifier
        ) {
            val isTranslucentTheme by remember {
                derivedStateOf { ThemeUtil.isTranslucentTheme(ThemeUtil.themeState.value) }
            }
            if (isTranslucentTheme) {
                val background by appPreferences.translucentThemeBackgroundFile.collectAsState(null)
                if (background != null) {
                    GlideImage(
                        model = background,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        requestBuilderTransform = { it.diskCacheStrategy(DiskCacheStrategy.NONE) }
                    )
                }
            }
            content()
        }
    }

    @Composable
    fun TiebaLiteLocalProvider(content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalNotificationCountFlow provides notificationCountFlow,
            LocalDevicePosture provides devicePostureFlow.collectAsState(),
        ) {
            content()
        }
    }

    private inner class NewMessageReceiver : BroadcastReceiver(), DefaultLifecycleObserver {

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

        override fun onDestroy(owner: LifecycleOwner) {
            unregisterReceiver(this)
        }
    }

    companion object {

        @Composable
        private fun ClipBoardDetectDialog(navController: NavController) {
            val dialogState = rememberDialogState()

            val previewInfo by ClipBoardLinkDetector.previewInfoStateFlow.collectAsStateWithLifecycle()
            LaunchedEffect(previewInfo) {
                if (previewInfo != null) dialogState.show()
            }

            Dialog(
                dialogState = dialogState,
                onDismiss = ClipBoardLinkDetector::clear,
                title = {
                    Text(text = stringResource(id = R.string.title_dialog_clip_board_tieba_url))
                },
                cancelableOnTouchOutside = false,
                buttons = {
                    DialogPositiveButton(text = stringResource(id = R.string.button_open)) {
                        previewInfo?.let {
                            val route = it.clipBoardLink.toRoute(it.icon?.url)
                            navController.navigate(route = route)
                        }
                    }
                    DialogNegativeButton(text = stringResource(id = R.string.btn_close))
                },
            ) {
                previewInfo?.let {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        border = BorderStroke(1.dp, ExtendedTheme.colors.divider),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            it.icon?.let { icon ->
                                if (icon.type == QuickPreviewUtil.Icon.TYPE_DRAWABLE_RES) {
                                    AvatarIcon(resId = icon.res, size = Sizes.Medium)
                                } else {
                                    Avatar(data = icon.url, size = Sizes.Medium)
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                it.title?.let { title ->
                                    Text(text = title, style = MaterialTheme.typography.subtitle1)
                                }
                                it.subtitle?.let { subtitle ->
                                    Text(text = subtitle, style = MaterialTheme.typography.body2)
                                }
                            }
                        }
                    }
                }
            }
        }

        @Composable
        private fun BatteryOpDialog(context: Context, prefUtil: AppPreferencesUtils) {
            val ignoreBatteryOp by rememberPreferenceAsState(
                key = booleanPreferencesKey(KEY_IGNORE_BATTERY_OPTIMIZATION),
                defaultValue = false
            )

            val okSignAlertDialogState = rememberDialogState()
            AlertDialog(
                dialogState = okSignAlertDialogState,
                title = { Text(text = stringResource(id = R.string.title_dialog_oksign_battery_optimization)) },
                content = { Text(text = stringResource(id = R.string.message_dialog_oksign_battery_optimization)) },
                buttons = {
                    DialogPositiveButton(
                        text = stringResource(id = R.string.button_go_to_ignore_battery_optimization),
                        onClick = context::requestIgnoreBatteryOptimizations
                    )

                    DialogNegativeButton(text = stringResource(id = R.string.button_cancel))

                    DialogNegativeButton(text = stringResource(id = R.string.button_dont_remind_again)) {
                        context.dataStore.putBoolean(KEY_IGNORE_BATTERY_OPTIMIZATION, true)
                    }
                }
            )

            LaunchedEffect(Unit) {
                delay(2000L)
                if (!ignoreBatteryOp && prefUtil.autoSign && !context.isIgnoringBatteryOptimizations()) {
                    okSignAlertDialogState.show()
                }
            }
        }
    }
}