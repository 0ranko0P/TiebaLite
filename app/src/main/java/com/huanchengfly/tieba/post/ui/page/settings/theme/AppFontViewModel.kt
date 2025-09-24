package com.huanchengfly.tieba.post.ui.page.settings.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.set
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class AppFontViewModel @Inject constructor(settingsRepo: SettingsRepository) : ViewModel() {

    private val fontScaleSettings = settingsRepo.fontScale

    private val _fontScale = MutableStateFlow(-1.0f)
    val fontScale = _fontScale.asStateFlow()

    val fontScaleChanged = combine(
        flow = fontScaleSettings.flow,
        flow2 = _fontScale,
        transform = { old, new -> abs(old - new) >= 0.01f }
    )
    .distinctUntilChanged()
    .stateIn(viewModelScope, started = SharingStarted.WhileSubscribed(5_000), false)

    init {
        viewModelScope.launch { onFontScaleChanged(fontScaleSettings.flow.first()) }
    }

    fun onFontScaleChanged(fontScale: Float) = _fontScale.set { fontScale }

    fun onSave() = fontScaleSettings.set(fontScale.value)
}