package com.huanchengfly.tieba.post.ui.page.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.huanchengfly.tieba.post.ui.page.settings.blocklist.BlockListPage
import com.huanchengfly.tieba.post.ui.page.settings.theme.AppFontPage
import kotlinx.serialization.Serializable

sealed interface SettingsDestination {

    @Serializable
    data object Settings: SettingsDestination

    @Serializable
    data object About: SettingsDestination

    @Serializable
    data object AccountManage: SettingsDestination

    @Serializable
    data object AppFont: SettingsDestination

    @Serializable
    data object BlockSettings: SettingsDestination

    @Serializable
    data object BlockList: SettingsDestination

    @Serializable
    data object Custom: SettingsDestination

    @Serializable
    data object Habit: SettingsDestination

    @Serializable
    data object More: SettingsDestination

    @Serializable
    data object OKSign: SettingsDestination

}

fun settingsNestedGraphBuilder(navController: NavController): NavGraphBuilder.() -> Unit = {
    composable<SettingsDestination.Settings> {
        SettingsPage(navController)
    }

    composable<SettingsDestination.About> {
        AboutPage(navController::navigateUp)
    }

    composable<SettingsDestination.AccountManage> {
        AccountManagePage(navController)
    }

    composable<SettingsDestination.AppFont> {
        AppFontPage(navController::navigateUp)
    }

    composable<SettingsDestination.BlockSettings> {
        BlockSettingsPage(navController)
    }

    composable<SettingsDestination.BlockList> {
        BlockListPage(onBack = navController::navigateUp)
    }

    composable<SettingsDestination.Custom> {
        CustomSettingsPage(navController)
    }

    composable<SettingsDestination.Habit> {
        HabitSettingsPage(navController::navigateUp)
    }

    composable<SettingsDestination.More> {
        MoreSettingsPage(navController)
    }

    composable<SettingsDestination.OKSign> {
        OKSignSettingsPage(navController::navigateUp)
    }
}