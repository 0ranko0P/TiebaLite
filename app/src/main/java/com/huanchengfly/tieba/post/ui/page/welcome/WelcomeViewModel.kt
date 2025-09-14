package com.huanchengfly.tieba.post.ui.page.welcome

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.util.fastFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.components.ConfigInitializer
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.set
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WelcomeState(
    val disclaimerConfirmed: Boolean = false,
    val permissionRequest: List<String>
) {
    val permissionGranted = permissionRequest.isEmpty()
}

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    private val configInitializer: ConfigInitializer,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val permissionList = listOfNotNull(
        Manifest.permission.READ_PHONE_STATE.takeIf { Build.VERSION.SDK_INT < Build.VERSION_CODES.Q },
    )

    private val _uiState = MutableStateFlow(WelcomeState(permissionRequest = permissionList))
    val uiState: StateFlow<WelcomeState> = _uiState.asStateFlow()

    init {
        if (permissionList.isNotEmpty()) {
            _uiState.set {
                val permissions = permissionList.filter { p ->
                    ContextCompat.checkSelfPermission(context, p) != PackageManager.PERMISSION_GRANTED
                }
                copy(permissionRequest = permissions)
            }
        }
    }

    fun onDisclaimerConfirmed() = _uiState.set { copy(disclaimerConfirmed = true) }

    fun onPermissionResult(permission: String) {
        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        when(permission) {
            Manifest.permission.READ_PHONE_STATE -> {
                configInitializer.init(reload = true)
            }

            else -> throw RuntimeException()
        }

        _uiState.update {
            // remove granted permission
            it.copy(permissionRequest = it.permissionRequest.fastFilter { p -> p != permission })
        }
    }

    // implement
    // fun onPermissionsResult(permissions: List<String>, result: List<Boolean>)

    fun onSetupFinished() = viewModelScope.launch {
        settingsRepository.uiSettings.save { it.copy(setupFinished = true) }
    }
}