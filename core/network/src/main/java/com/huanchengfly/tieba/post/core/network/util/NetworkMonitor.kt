package com.huanchengfly.tieba.post.core.network.util

import kotlinx.coroutines.flow.StateFlow

/**
 * Utility for reporting app connectivity status
 */
interface NetworkMonitor {

    val isValidated: StateFlow<Boolean>

    val isUnmetered: StateFlow<Boolean>

    /**
     * @return true if the currently active network is unmetered.
     */
    fun isCurrentlyNotMetered(): Boolean

    /**
     * @return true if the currently active network was successfully validated.
     */
    fun isCurrentlyValidated(): Boolean
}
