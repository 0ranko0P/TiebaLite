package com.huanchengfly.tieba.post.ui.page.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.BrowseGallery
import androidx.compose.material.icons.outlined.OfflinePin
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsScreen
import com.huanchengfly.tieba.post.ui.common.prefs.depend
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.SwitchPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.TextPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.TimePickerPerf
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.TipPref
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils
import com.huanchengfly.tieba.post.utils.appPreferences
import com.huanchengfly.tieba.post.utils.isIgnoringBatteryOptimizations
import com.huanchengfly.tieba.post.utils.requestIgnoreBatteryOptimizations
import kotlinx.coroutines.launch

@Composable
fun OKSignSettingsPage(onBack: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    MyScaffold(
        topBar = {
            TitleCentredToolbar(
                title = stringResource(id = R.string.title_oksign),
                navigationIcon = { BackNavigationIcon(onBackPressed = onBack) }
            )
        },
    ) { paddingValues ->
        val context = LocalContext.current
        val snackbarHostState = LocalSnackbarHostState.current

        PrefsScreen(
            contentPadding = paddingValues
        ) {
            prefsItem {
                SwitchPref(
                    leadingIcon = Icons.Outlined.BrowseGallery,
                    key = AppPreferencesUtils.KEY_OKSIGN_SLOW,
                    title = R.string.title_oksign_slow_mode,
                    defaultChecked = true,
                    summaryOn = R.string.summary_oksign_slow_mode_on,
                    summaryOff = R.string.summary_oksign_slow_mode,
                )
            }
            prefsItem {
                SwitchPref(
                    leadingIcon = Icons.Outlined.Speed,
                    key = AppPreferencesUtils.KEY_OKSIGN_OFFICIAL,
                    title = R.string.title_oksign_use_official_oksign,
                    defaultChecked = true,
                    summary = { R.string.summary_oksign_use_official_oksign },
                )
            }
            prefsItem {
                SwitchPref(
                    key = AppPreferencesUtils.KEY_OKSIGN_AUTO,
                    title = R.string.title_auto_sign,
                    defaultChecked = false,
                    summaryOn = R.string.summary_auto_sign_on,
                    summaryOff = R.string.summary_auto_sign,
                    leadingIcon = Icons.Outlined.OfflinePin,
                )
            }
            prefsItem {
                TimePickerPerf(
                    key = AppPreferencesUtils.KEY_OKSIGN_AUTO_TIME,
                    title = stringResource(id = R.string.title_auto_sign_time),
                    defaultValue = context.appPreferences.autoSignTime,
                    summary = { stringResource(id = R.string.summary_auto_sign_time, it) },
                    dialogTitle = stringResource(id = R.string.title_auto_sign_time),
                    leadingIcon = Icons.Outlined.WatchLater,
                    enabled = depend(key = AppPreferencesUtils.KEY_OKSIGN_AUTO)
                )
            }
                prefsItem {
                    TextPref(
                        title = stringResource(id = R.string.title_ignore_battery_optimization),
                        enabled = !context.isIgnoringBatteryOptimizations(),
                        summary = if (context.isIgnoringBatteryOptimizations()) {
                            stringResource(id = R.string.summary_battery_optimization_ignored)
                        } else {
                            stringResource(id = R.string.summary_ignore_battery_optimization)
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

            prefsItem {
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
                    Text(text = tip, fontSize = 12.sp)
                }
            }
        }
    }
}