package com.huanchengfly.tieba.post

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.activities.TranslucentThemeViewModel.Companion.translucentBackground
import com.huanchengfly.tieba.post.components.ClipBoardLinkDetector
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.repository.ForumRepository
import com.huanchengfly.tieba.post.repository.PbPageRepository
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.theme.ExtendedColorScheme
import com.huanchengfly.tieba.post.ui.models.settings.HabitSettings
import com.huanchengfly.tieba.post.ui.models.settings.PrivacySettings
import com.huanchengfly.tieba.post.ui.models.settings.Theme
import com.huanchengfly.tieba.post.ui.models.settings.UISettings
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.isIgnoringBatteryOptimizations
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@Immutable
data class MainUiState(
    val habitSettings: HabitSettings? = null,
    val uiSettings: UISettings? = null,
    val autoSignRestricted: Boolean = false,
    val themeColor: ExtendedColorScheme = ThemeUtil.getRawTheme(),
)

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    settingsRepository: SettingsRepository,
    private val forumRepo: ForumRepository,
    private val threadRepo: PbPageRepository
) : ViewModel() {

    val account: SharedFlow<Account?> = AccountUtil.getInstance().currentAccount

    val previewInfoFlow = ClipBoardLinkDetector.previewInfoStateFlow

    val uiState: StateFlow<MainUiState> = combine(
        settingsRepository.habitSettings,
        settingsRepository.uiSettings,
        settingsRepository.signConfig.map { it.autoSign },
        ThemeUtil.getExtendedColorFlow(settingsRepository, context),
    ) { habitSettings, uiSettings, autoSign, themeColor ->
        // Show warning dialog when background activity is restricted
        val autoSignRestricted = autoSign && !context.isIgnoringBatteryOptimizations()
        MainUiState(habitSettings, uiSettings, autoSignRestricted, themeColor)
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    /**
     * Cropped wallpaper file of [Theme.TRANSLUCENT], **null** when current theme is not translucent.
     * */
    val translucentThemeBackground: StateFlow<File?> = settingsRepository.themeSettings
        .map {
            if (it.theme == Theme.TRANSLUCENT && it.transBackground != null) {
                context.translucentBackground(it.transBackground)
            } else {
                null
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val privacySettings: Settings<PrivacySettings> = settingsRepository.privacySettings

    fun onCheckClipBoard() {
        viewModelScope.launch {
            if (privacySettings.snapshot().readClipBoardLink) {
                ClipBoardLinkDetector.checkClipBoard(context, forumRepo, threadRepo)
            }
        }
    }

    fun onClipBoardDetectDialogDismiss() = ClipBoardLinkDetector.clear()
}