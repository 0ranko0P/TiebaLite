package com.huanchengfly.tieba.post

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.activities.TranslucentThemeViewModel.Companion.translucentBackground
import com.huanchengfly.tieba.post.arch.shareInBackground
import com.huanchengfly.tieba.post.components.ClipBoardLinkDetector
import com.huanchengfly.tieba.post.repository.ForumRepository
import com.huanchengfly.tieba.post.repository.PbPageRepository
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.theme.ExtendedColorScheme
import com.huanchengfly.tieba.post.ui.models.settings.Theme
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.set
import com.huanchengfly.tieba.post.utils.QuickPreviewUtil.PreviewInfo
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.isIgnoringBatteryOptimizations
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class MainUiState(
    val setupFinished: Boolean? = null,
    val ignoreBatteryOpDialogVisible: Boolean = false,
    val preview: PreviewInfo? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    private val settingsRepository: SettingsRepository,
    private val forumRepo: ForumRepository,
    private val threadRepo: PbPageRepository
) : ViewModel() {

    private val _uiState: MutableStateFlow<MainUiState> = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = combine(
        _uiState,
        ClipBoardLinkDetector.previewInfoStateFlow,
        { ui, preview -> ui.copy(preview = preview) }
    )
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    /**
     * Cropped wallpaper file of [Theme.TRANSLUCENT], **null** when current theme is not translucent.
     * */
    val translucentThemeBackground: Flow<File?> = settingsRepository.themeSettings.flow
        .map {
            if (it.theme == Theme.TRANSLUCENT && it.transBackground != null) {
                context.translucentBackground(it.transBackground)
            } else {
                null
            }
        }
        .distinctUntilChanged()
        .shareInBackground()

    val extendedColorScheme: StateFlow<ExtendedColorScheme> =
        ThemeUtil.getExtendedColorFlow(settingsRepository, context)
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ThemeUtil.getRawTheme()
            )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val uiSettings = settingsRepository.uiSettings.flow.first()
            val signConfig = settingsRepository.signConfig.flow.first()

            _uiState.set {
                MainUiState(
                    setupFinished = uiSettings.setupFinished,
                    ignoreBatteryOpDialogVisible = !signConfig.ignoreBatteryOp && signConfig.autoSign
                            && !context.isIgnoringBatteryOptimizations()
                )
            }
        }
    }

    fun onDismissBatteryOpDialog() = _uiState.set { copy(ignoreBatteryOpDialogVisible = false) }

    fun onIgnoreBatteryOpDialog() {
        settingsRepository.signConfig.save { it.copy(ignoreBatteryOp = true) }
        _uiState.update { it.copy(ignoreBatteryOpDialogVisible = false) }
    }

    fun onCheckClipBoard() {
        viewModelScope.launch {
            ClipBoardLinkDetector.checkClipBoard(context, forumRepo, threadRepo)
        }
    }

    fun onClipBoardDetectDialogDismiss() = ClipBoardLinkDetector.clear()
}