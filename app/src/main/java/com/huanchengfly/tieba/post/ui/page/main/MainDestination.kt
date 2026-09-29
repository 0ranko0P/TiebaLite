package com.huanchengfly.tieba.post.ui.page.main

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface MainDestination: NavKey {

    @Serializable
    data object Home: MainDestination

    @Serializable
    data object Explore: MainDestination

    @Serializable
    data object Notification: MainDestination

    @Serializable
    data object User: MainDestination
}