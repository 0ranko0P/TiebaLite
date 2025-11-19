package com.huanchengfly.tieba.post.ui.page.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.BrandingWatermark
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.outlined.CalendarViewDay
import androidx.compose.material.icons.outlined.PhotoSizeSelectActual
import androidx.compose.material.icons.outlined.SecurityUpdateWarning
import androidx.compose.material.icons.outlined.SpeakerNotesOff
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.PrefsScope
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.PrefsScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.ListPref
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SwitchPref
import com.huanchengfly.tieba.post.ui.models.settings.ForumFAB
import com.huanchengfly.tieba.post.ui.models.settings.ForumSortType
import com.huanchengfly.tieba.post.ui.models.settings.HabitSettings
import com.huanchengfly.tieba.post.ui.models.settings.WaterType
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.ImageUtil
import kotlinx.collections.immutable.persistentMapOf

@Composable
fun HabitSettingsPage(habitSettings: Settings<HabitSettings>, onBack: () -> Unit) {
    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = stringResource(id = R.string.title_settings_read_habit),
                navigationIcon = { BackNavigationIcon(onBackPressed = onBack) }
            )
        },
    ) { paddingValues ->
        PrefsScreen(
            settings = habitSettings,
            initialValue = HabitSettings(),
            contentPadding = paddingValues
        ) {
            ImageLoadPreference()

            Item { habit ->
                ListPref(
                    value = habit.imageWatermarkType,
                    title = R.string.title_settings_image_watermark,
                    onValueChange = { newType ->
                        updatePreference { old -> old.copy(imageWatermarkType = newType) }
                    },
                    options = persistentMapOf(
                        WaterType.NO to R.string.title_image_watermark_none,
                        WaterType.USER_NAME to R.string.title_image_watermark_user_name,
                        WaterType.FORUM_NAME to R.string.title_image_watermark_forum_name
                    ),
                    leadingIcon = Icons.AutoMirrored.Outlined.BrandingWatermark
                )
            }

            DefaultSortPreference()

            Item { habit ->
                ListPref(
                    value = habit.forumFAB,
                    title = R.string.settings_forum_fab_function,
                    onValueChange = {
                        updatePreference { old -> old.copy(forumFAB = it) }
                    },
                    leadingIcon = Icons.AutoMirrored.Outlined.ExitToApp,
                    options = persistentMapOf(
                        ForumFAB.POST to R.string.btn_post,
                        ForumFAB.REFRESH to R.string.btn_refresh,
                        ForumFAB.BACK_TO_TOP to R.string.btn_back_to_top,
                        ForumFAB.HIDE to R.string.btn_hide
                    )
                )
            }

            Item { habit ->
                SwitchPref(
                    checked = habit.hideMedia,
                    onCheckedChange = {
                        updatePreference { old -> old.copy(hideMedia = it) }
                    },
                    title = R.string.title_hide_media,
                    leadingIcon = Icons.Rounded.UnfoldLess
                )
            }

            CollectSeeLzPreference()

            Item { habit ->
                SwitchPref(
                    checked = habit.collectedDesc,
                    onCheckedChange = {
                        updatePreference { old -> old.copy(collectedDesc = it)}
                    },
                    title = R.string.settings_collect_thread_desc_sort,
                    leadingIcon = Icons.AutoMirrored.Rounded.Sort,
                    summaryOn = R.string.tip_collect_thread_desc_sort_on,
                    summaryOff = R.string.tip_collect_thread_desc_sort
                )
            }

            Item { habit ->
                SwitchPref(
                    checked = habit.showBothName,
                    onCheckedChange = {
                        updatePreference { old -> old.copy(showBothName = it) }
                    },
                    title = R.string.title_show_both_username_and_nickname,
                    leadingIcon = Icons.Outlined.Verified
                )
            }

            Item { habit ->
                SwitchPref(
                    checked = habit.showHistoryInHome,
                    onCheckedChange = {
                        updatePreference { old -> old.copy(showHistoryInHome = it) }
                    },
                    title = R.string.settings_home_page_show_history_forum,
                    leadingIcon = Icons.Outlined.WatchLater
                )
            }

            HideReplyPreference()

            Item { habit ->
                SwitchPref(
                    checked = habit.hideReplyWarning,
                    onCheckedChange = {
                        updatePreference { old -> old.copy(hideReplyWarning = it) }
                    },
                    title = R.string.title_hide_reply_warning,
                    enabled = !habit.hideReply,
                    leadingIcon = Icons.Outlined.SecurityUpdateWarning
                )
            }
        }
    }
}

@Composable
fun PrefsScope<HabitSettings>.ImageLoadPreference(modifier: Modifier = Modifier) = Item { habit ->
    ListPref(
        modifier = modifier,
        value = habit.imageLoadType,
        title = R.string.title_settings_image_load_type,
        onValueChange = { newLoadType ->
            updatePreference { old -> old.copy(imageLoadType = newLoadType) }
        },
        options = persistentMapOf(
            ImageUtil.SETTINGS_SMART_ORIGIN to R.string.title_image_load_type_smart_origin,
            ImageUtil.SETTINGS_SMART_LOAD to R.string.title_image_load_type_smart_load,
            ImageUtil.SETTINGS_ALL_ORIGIN to R.string.title_image_load_type_all_origin,
            ImageUtil.SETTINGS_ALL_NO to R.string.title_image_load_type_all_no
        ),
        leadingIcon = Icons.Outlined.PhotoSizeSelectActual
    )
}

@Composable
fun PrefsScope<HabitSettings>.DefaultSortPreference(modifier: Modifier = Modifier) = Item { habit ->
    ListPref(
        modifier = modifier,
        value = habit.forumSortType,
        title = R.string.title_settings_default_sort_type,
        options = persistentMapOf(
            ForumSortType.BY_REPLY to R.string.title_sort_by_reply,
            ForumSortType.BY_SEND to R.string.title_sort_by_send,
        ),
        onValueChange = {
            updatePreference { old -> old.copy(forumSortType = it)}
        },
        leadingIcon = Icons.Outlined.CalendarViewDay
    )
}

@Composable
fun PrefsScope<HabitSettings>.CollectSeeLzPreference(modifier: Modifier = Modifier) = Item { habit ->
    SwitchPref(
        modifier = modifier,
        checked = habit.collectedDesc,
        onCheckedChange = {
            updatePreference { old -> old.copy(collectedDesc = it) }
        },
        title = R.string.settings_collect_thread_see_lz,
        leadingIcon = Icons.Outlined.StarOutline,
        summaryOn = R.string.tip_collect_thread_see_lz_on,
        summaryOff = R.string.tip_collect_thread_see_lz
    )
}

@Composable
fun PrefsScope<HabitSettings>.HideReplyPreference(modifier: Modifier = Modifier) = Item { habit ->
    SwitchPref(
        modifier = modifier,
        checked = habit.hideReply,
        onCheckedChange = {
            updatePreference { old -> old.copy(hideReply = it) }
        },
        title = R.string.title_hide_reply,
        leadingIcon = Icons.Outlined.SpeakerNotesOff
    )
}
