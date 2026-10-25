package com.huanchengfly.tieba.post.core.data.test.util

import com.huanchengfly.tieba.post.core.network.util.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class AlwaysOnlineNetworkMonitor @Inject constructor() : NetworkMonitor {

    override val isValidated: StateFlow<Boolean> = MutableStateFlow(true)

    override val isUnmetered: StateFlow<Boolean> = MutableStateFlow(true)

    override fun isCurrentlyNotMetered(): Boolean = true

    override fun isCurrentlyValidated(): Boolean = true
}
