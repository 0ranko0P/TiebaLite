package com.huanchengfly.tieba.post.ui.page.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.BrowseGallery
import androidx.compose.material.icons.outlined.OfflinePin
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.PrefsScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SwitchPref
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.TextPref
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.TimePickerPerf
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.TipPref
import com.huanchengfly.tieba.post.ui.models.settings.SignConfig
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.isIgnoringBatteryOptimizations
import com.huanchengfly.tieba.post.utils.requestIgnoreBatteryOptimizations
import kotlinx.coroutines.launch

@Composable
private fun batteryOptimizeState(): State<Boolean> {
    val context = LocalContext.current
    val batteryOpEnabled = remember { mutableStateOf(!context.isIgnoringBatteryOptimizations()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        batteryOpEnabled.value = !context.isIgnoringBatteryOptimizations()
    }
    return batteryOpEnabled
}

@Composable
fun OKSignSettingsPage(settings: Settings<SignConfig>, onBack: () -> Unit) {
    MyScaffold(
        topBar = {
            TitleCentredToolbar(
                title = stringResource(id = R.string.title_oksign),
                navigationIcon = { BackNavigationIcon(onBackPressed = onBack) }
            )
        },
    ) { paddingValues ->
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val snackbarHostState = LocalSnackbarHostState.current
        val batteryOpEnabled by batteryOptimizeState()

        PrefsScreen(
            settings = settings,
            initialValue = SignConfig(),
            contentPadding = paddingValues
        ) {
            val okSignAvailable by remember {
                derivedStateOf { preferenceState.value.autoSign && !batteryOpEnabled }
            }

            Item { signConfig ->
                SwitchPref(
                    checked = signConfig.autoSign,
                    onCheckedChange = {
                        updatePreference { old -> old.copy(autoSign = it) }
                    },
                    title = R.string.title_auto_sign,
                    summaryOn = R.string.summary_auto_sign_on,
                    summaryOff = R.string.summary_auto_sign,
                    leadingIcon = Icons.Outlined.OfflinePin,
                    enabled = !batteryOpEnabled
                )
            }

            Item { signConfig ->
                TimePickerPerf(
                    time = signConfig.autoSignTime,
                    onTimePicked = {
                        updatePreference { old -> old.copy(autoSignTime = it) }
                    },
                    title = stringResource(id = R.string.title_auto_sign_time),
                    summary = { stringResource(id = R.string.summary_auto_sign_time, it) },
                    dialogTitle = stringResource(id = R.string.title_auto_sign_time),
                    leadingIcon = Icons.Outlined.WatchLater,
                    enabled = okSignAvailable
                )
            }

            Item { signConfig ->
                SwitchPref(
                    checked = signConfig.autoSignSlow,
                    onCheckedChange = {
                        updatePreference { old -> old.copy(autoSignSlow = it) }
                    },
                    leadingIcon = Icons.Outlined.BrowseGallery,
                    title = R.string.title_oksign_slow_mode,
                    summaryOn = R.string.summary_oksign_slow_mode_on,
                    summaryOff = R.string.summary_oksign_slow_mode,
                    enabled = okSignAvailable,
                )
            }

            Item { signConfig ->
                SwitchPref(
                    checked = signConfig.okSignOfficial,
                    onCheckedChange = {
                        updatePreference { old -> old.copy(okSignOfficial = it) }
                    },
                    leadingIcon = Icons.Outlined.Speed,
                    title = R.string.title_oksign_use_official_oksign,
                    summary = R.string.summary_oksign_use_official_oksign,
                    enabled = okSignAvailable
                )
            }

            TextItem {
                TextPref(
                    title = stringResource(id = R.string.title_ignore_battery_optimization),
                    enabled = batteryOpEnabled,
                    summary = if (batteryOpEnabled) {
                        stringResource(id = R.string.summary_ignore_battery_optimization)
                    } else {
                        stringResource(id = R.string.summary_battery_optimization_ignored)
                    },
                    onClick = {
                        if (!context.isIgnoringBatteryOptimizations()) {
                            context.requestIgnoreBatteryOptimizations()
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(context.getString(R.string.summary_battery_optimization_ignored))
                            }
                        }
                    },
                    leadingIcon = Icons.Outlined.BatteryAlert
                )
            }

            TextItem {
                TipPref {
                    val tip = remember {
                        buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(fontWeight = FontWeight.Bold)
                            ) {
                                append(context.getString(R.string.tip_start))
                            }
                            append(context.getString(R.string.tip_auto_sign))
                        }
                    }
                    Text(text = tip, fontSize = 13.sp)
                }
            }
        }
    }
}