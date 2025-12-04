package com.huanchengfly.tieba.post.ui.page.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.page.settings.blocklist.KeywordBlockListPage
import com.huanchengfly.tieba.post.ui.page.settings.blocklist.UserBlockListPage
import com.huanchengfly.tieba.post.ui.page.settings.theme.AppFontPage
import kotlinx.serialization.Serializable

sealed interface SettingsDestination {

    @Serializable
    object Settings: SettingsDestination

    @Serializable
    object About: SettingsDestination

    @Serializable
    object AccountManage: SettingsDestination

    @Serializable
    object AppFont: SettingsDestination

    @Serializable
    object BlockSettings: SettingsDestination

    /**
     * Destination of block list page
     *
     * @param isUser is user or keyword blocklist
     * */
    @Serializable
    data class BlockList(val isUser: Boolean): SettingsDestination

    @Serializable
    object UI: SettingsDestination

    @Serializable
    object Habit: SettingsDestination

    @Serializable
    object Privacy: SettingsDestination

    @Serializable
    object More: SettingsDestination

    @Serializable
    object OKSign: SettingsDestination

    @Serializable
    object WorkInfo: SettingsDestination
}

fun settingsNestedGraphBuilder(
    navController: NavController,
    settingsRepo: SettingsRepository = App.INSTANCE.settingRepository
): NavGraphBuilder.() -> Unit = {
    composable<SettingsDestination.Settings> {
        SettingsPage(navController)
    }

    composable<SettingsDestination.About> {
        AboutPage(navController::navigateUp)
    }

    composable<SettingsDestination.AccountManage> {
        AccountManagePage(myLittleTailSettings = settingsRepo.myLittleTail, navController)
    }

    composable<SettingsDestination.AppFont> {
        AppFontPage(navController::navigateUp)
    }

    composable<SettingsDestination.BlockSettings> {
        BlockSettingsPage(settings = settingsRepo.blockSettings, navController)
    }

    composable<SettingsDestination.BlockList> { backStackEntry ->
        val params = backStackEntry.toRoute<SettingsDestination.BlockList>()
        if (params.isUser) {
            UserBlockListPage(onBack = navController::navigateUp)
        } else {
            KeywordBlockListPage(onBack = navController::navigateUp)
        }
    }

    composable<SettingsDestination.UI> {
        UISettingsPage(settings = settingsRepo.uiSettings, navController)
    }

    composable<SettingsDestination.Habit> {
        HabitSettingsPage(settingsRepo.habitSettings, onBack = navController::navigateUp)
    }

    composable<SettingsDestination.Privacy> {
        PrivacySettingsPage(settingsRepo.privacySettings, onBack = navController::navigateUp)
    }

    composable<SettingsDestination.More> {
        MoreSettingsPage(navController)
    }

    composable<SettingsDestination.OKSign> {
        OKSignSettingsPage(settings = settingsRepo.signConfig, onBack = navController::navigateUp)
    }

    composable<SettingsDestination.WorkInfo> {
        WorkInfoPage(onBack = navController::navigateUp)
    }
}