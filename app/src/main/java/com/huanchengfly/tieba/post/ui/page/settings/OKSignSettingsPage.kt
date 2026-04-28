package com.huanchengfly.tieba.post.ui.page.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.BrowseGallery
import androidx.compose.material.icons.outlined.OfflinePin
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.ui.models.settings.SignConfig
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SegmentedPreference
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SegmentedPrefsScope
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.toggleablePreference
import com.huanchengfly.tieba.post.utils.isIgnoringBatteryOptimizations
import com.huanchengfly.tieba.post.utils.requestIgnoreBatteryOptimizations
import kotlinx.coroutines.launch

@Composable
private fun batteryOptimizeState(): State<Boolean> {
    val context = LocalContext.current
    val batteryOpEnabled = remember { mutableStateOf(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        batteryOpEnabled.value = !context.isIgnoringBatteryOptimizations()
    }
    return batteryOpEnabled
}

private fun SegmentedPrefsScope.batteryOpPreference(modifier: Modifier = Modifier, enabled: Boolean) {
    customPreference { shapes ->
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val snackbarHostState = LocalSnackbarHostState.current

        Column(
            modifier = modifier.animateItem(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(id = R.string.tip_auto_sign),
                modifier = Modifier.padding(horizontal = 8.dp),
                style = MaterialTheme.typography.bodyMedium, // ListTokens.ItemSupportingTextFont
            )

            SegmentedPreference(
                title = R.string.title_ignore_battery_optimization,
                shapes = shapes,
                summary = if (enabled) {
                    R.string.summary_ignore_battery_optimization
                } else {
                    R.string.summary_battery_optimization_ignored
                },
                onClick = {
                    if (!context.isIgnoringBatteryOptimizations()) {
                        context.requestIgnoreBatteryOptimizations()
                    } else {
                        val msg = context.getString(R.string.summary_battery_optimization_ignored)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(msg)
                        }
                    }
                },
                enabled = enabled,
                leadingIcon = Icons.Outlined.BatteryAlert
            )
        }
    }
}

@Composable
fun OKSignSettingsPage(settings: Settings<SignConfig>, onBack: () -> Unit) {
    val batteryOpEnabled by batteryOptimizeState()

    SettingsScaffold(
        titleRes = R.string.title_oksign,
        onBack = onBack,
        settings = settings,
        initialValue = SignConfig(),
    ) {
        val okSignAvailable = currentPreference.autoSign && !batteryOpEnabled

        group(title = R.string.settings_group_battery_op, titleVerticalPadding = Dp.Hairline) {
            batteryOpPreference(
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                enabled = batteryOpEnabled
            )
        }

        group(title = R.string.settings_group_oksign) {
            toggleablePreference(
                property = SignConfig::autoSign,
                title = R.string.title_auto_sign,
                summaryOn = R.string.summary_auto_sign_on,
                summaryOff = R.string.summary_auto_sign,
                leadingIcon = Icons.Outlined.OfflinePin,
                enabled = !batteryOpEnabled
            )

            timePreference(
                time = currentPreference.autoSignTime,
                onTimeChange = { newTime ->
                    settings.save { it.copy(autoSignTime = newTime)  }
                },
                title = { Text(text = stringResource(id = R.string.title_auto_sign_time)) },
                summary = {
                    val time = currentPreference.autoSignTime
                    Text(text = stringResource(id = R.string.summary_auto_sign_time, time))
                },
                dialogTitle = {
                    Text(text = stringResource(id = R.string.title_auto_sign_time))
                },
                leadingIcon = Icons.Outlined.WatchLater,
                enabled = okSignAvailable
            )

            toggleablePreference(
                property = SignConfig::autoSignSlow,
                title = R.string.title_oksign_slow_mode,
                summaryOn = R.string.summary_oksign_slow_mode_on,
                summaryOff = R.string.summary_oksign_slow_mode,
                leadingIcon = Icons.Outlined.BrowseGallery,
                enabled = okSignAvailable,
            )

            toggleablePreference(
                property = SignConfig::okSignOfficial,
                title = R.string.title_oksign_use_official_oksign,
                summary = R.string.summary_oksign_use_official_oksign,
                leadingIcon = Icons.Outlined.Speed,
                enabled = okSignAvailable
            )
        }
    }
}