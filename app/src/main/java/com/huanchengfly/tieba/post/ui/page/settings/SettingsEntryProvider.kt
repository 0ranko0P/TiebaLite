package com.huanchengfly.tieba.post.ui.page.settings

import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.ui.res.painterResource
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.core.data.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.core.designsystem.component.PainterDetailPlaceholder
import com.huanchengfly.tieba.post.core.ui.scenes.DetailPaneBackHandlerSceneDecoratorStrategy
import com.huanchengfly.tieba.post.core.navigation.Navigator
import com.huanchengfly.tieba.post.ui.page.settings.blocklist.ForumBlockListPage
import com.huanchengfly.tieba.post.ui.page.settings.blocklist.KeywordBlockListPage
import com.huanchengfly.tieba.post.ui.page.settings.blocklist.UserBlockListPage
import com.huanchengfly.tieba.post.ui.page.settings.theme.AppFontPage

fun EntryProviderScope<NavKey>.settingsEntry(
    navigator: Navigator,
    settingsRepo: SettingsRepository,
) {
    val onBack: () -> Unit = navigator::navigateUp
    val settingsDetailPane = ListDetailSceneStrategy.detailPane() + DetailPaneBackHandlerSceneDecoratorStrategy.backHandler()
    val settingsMetadata: (title: Int) -> Map<String, Any> = { title ->
        settingsDetailPane + SettingsSceneDecoratorStrategy.decorate(title)
    }

    entry<SettingsDestination.Settings>(
        metadata = ListDetailSceneStrategy.listPane(
            detailPlaceholder = {
                PainterDetailPlaceholder(painter = painterResource(R.drawable.advanced_customization))
            }
        ) + SettingsSceneDecoratorStrategy.decorate(R.string.title_settings, alwaysVisible = true),
    ) {
        SettingsPage(navigator)
    }

    entry<SettingsDestination.About> {
        AboutPage(onBack = onBack)
    }

    entry<SettingsDestination.AccountManage>(
        metadata = settingsMetadata(R.string.title_account_manage),
    ) {
        AccountManagePage(myLittleTailSettings = settingsRepo.myLittleTail, navigator)
    }

    entry<SettingsDestination.AppFont>(metadata = settingsMetadata(R.string.title_custom_font_size)) {
        AppFontPage(onBack = onBack)
    }

    entry<SettingsDestination.BlockSettings>(metadata = settingsMetadata(R.string.title_block_settings)) {
        BlockSettingsPage(
            settings = settingsRepo.blockSettings,
            onBack = onBack,
            onNavigateForumBlock = {
                navigator.navigate(SettingsDestination.ForumBlockList)
            },
            onNavigateKeywordBlock = {
                navigator.navigate(SettingsDestination.KeywordBlockList)
            },
            onNavigateUserBlock = {
                navigator.navigate(SettingsDestination.UserBlockList)
            },
        )
    }

    entry<SettingsDestination.ForumBlockList>(metadata = settingsMetadata(R.string.settings_block_forum)) {
        ForumBlockListPage(onBack = onBack)
    }

    entry<SettingsDestination.Habit>(metadata = settingsMetadata(R.string.title_settings_read_habit)) {
        HabitSettingsPage(
            habitSettings = settingsRepo.habitSettings,
            onStickyHeaderClicked = {
                navigator.navigate(key = SettingsDestination.StickyHeader)
            },
            onBack = onBack
        )
    }

    entry<SettingsDestination.KeywordBlockList>(metadata = settingsMetadata(R.string.settings_block_keyword)) {
        KeywordBlockListPage(onBack = onBack)
    }

    entry<SettingsDestination.More>(metadata = settingsMetadata(R.string.title_settings_more)) {
        MoreSettingsPage(
            onBack = onBack,
            onNavigateWorkInfo = { navigator.navigate(key = SettingsDestination.WorkInfo) }
        )
    }

    entry<SettingsDestination.OKSign>(metadata = settingsMetadata(R.string.title_oksign)) {
        OKSignSettingsPage(settings = settingsRepo.signConfig, onBack = onBack)
    }

    entry<SettingsDestination.Privacy>(metadata = settingsMetadata(R.string.title_settings_privacy)) {
        PrivacySettingsPage(settingsRepo.privacySettings, onBack = onBack)
    }

    entry<SettingsDestination.StickyHeader>(metadata = settingsMetadata(R.string.title_settings_sticky_header)) {
        StickyHeaderSettingsPage(settingsRepo.habitSettings, onBack = onBack)
    }

    entry<SettingsDestination.UI>(metadata = settingsMetadata(R.string.title_settings_custom)) {
        UISettingsPage(
            settings = settingsRepo.uiSettings,
            onBack = onBack,
            onNavigateAppFont = {
                navigator.navigate(key = SettingsDestination.AppFont)
            },
        )
    }

    entry<SettingsDestination.UserBlockList>(metadata = settingsMetadata(R.string.settings_block_user)) {
        UserBlockListPage(onBack = onBack)
    }

    entry<SettingsDestination.WorkInfo> {
        WorkInfoPage(onBack = onBack)
    }
}
