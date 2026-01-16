package com.huanchengfly.tieba.post.ui.page.welcome

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Immutable
import androidx.compose.ui.util.fastFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.components.ConfigInitializer
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.set
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for the Welcome Page
 *
 * @param uaAccepted is User Agreement accepted
 * @param permissionEssential essential permissions
 * @param permissionOptional optional permissions
 */
@Immutable
data class WelcomeState(
    val uaAccepted: Boolean = false,
    val permissionEssential: List<String>? = null,
    val permissionOptional: List<String>? = null,
) {
    val essentialGranted = permissionEssential.isNullOrEmpty()
}

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    private val configInitializer: ConfigInitializer,
    val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WelcomeState())
    val uiState: StateFlow<WelcomeState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            // init essential permissions
            val permissionEssential = listOfNotNull(
                Manifest.permission.READ_PHONE_STATE.takeIf { Build.VERSION.SDK_INT < Build.VERSION_CODES.Q },
            )
            // init optional permissions
            val permissionOptional = listOfNotNull(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.POST_NOTIFICATIONS
                } else {
                    null
                }
            )
            _uiState.update {
                it.copy(
                    permissionEssential = permissionEssential.filterGranted(),
                    permissionOptional = permissionOptional.filterGranted(),
                )
            }
        }
    }

    fun onUaAcceptStateChanged(state: Boolean) = _uiState.set { copy(uaAccepted = state) }

    fun onPermissionResult(permission: String) {
        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            when (permission) {
                Manifest.permission.READ_PHONE_STATE -> configInitializer.init(reload = true)

                else -> {}
            }

            // remove granted permission
            _uiState.update {
                it.copy(
                    permissionEssential = it.permissionEssential?.fastFilter { p -> p != permission },
                    permissionOptional = it.permissionOptional?.fastFilter { p -> p != permission }
                )
            }
        }
    }

    // implement
    // fun onPermissionsResult(permissions: List<String>, result: List<Boolean>)

    fun onSetupFinished() = viewModelScope.launch {
        settingsRepository.uiSettings.save { it.copy(setupFinished = true) }
    }

    private fun List<String>.filterGranted(): List<String>? {
        return if (isNotEmpty()) {
            filter { p ->
                ContextCompat.checkSelfPermission(context, p) != PackageManager.PERMISSION_GRANTED
            }
        } else {
            null
        }
    }
}