package com.huanchengfly.tieba.post.ui.page.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.BrandingWatermark
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.outlined.CalendarViewDay
import androidx.compose.material.icons.outlined.PhotoSizeSelectActual
import androidx.compose.material.icons.outlined.SecurityUpdateWarning
import androidx.compose.material.icons.outlined.SpeakerNotesOff
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.runtime.Composable
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.ui.icons.PageHeader
import com.huanchengfly.tieba.post.ui.models.settings.ForumSortType
import com.huanchengfly.tieba.post.ui.models.settings.HabitSettings
import com.huanchengfly.tieba.post.ui.models.settings.WaterType
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SettingsSegmentedPrefsScope
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.preference
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.toggleablePreference
import com.huanchengfly.tieba.post.utils.ImageUtil
import kotlinx.collections.immutable.persistentMapOf

@Composable
fun HabitSettingsPage(
    habitSettings: Settings<HabitSettings>,
    onStickyHeaderClicked: () -> Unit,
    onBack: () -> Unit,
) {
    SettingsScaffold(
        titleRes = R.string.title_settings_read_habit,
        onBack = onBack,
        settings = habitSettings,
        initialValue = HabitSettings()
    ) {
        group(title = R.string.settings_group_media) {
            imageLoadPreference()

            listPref(
                property = HabitSettings::imageWatermarkType,
                title = R.string.title_settings_image_watermark,
                options = persistentMapOf(
                    WaterType.NO to R.string.title_image_watermark_none,
                    WaterType.USER_NAME to R.string.title_image_watermark_user_name,
                    WaterType.FORUM_NAME to R.string.title_image_watermark_forum_name
                ),
                leadingIcon = Icons.AutoMirrored.Outlined.BrandingWatermark
            )

            toggleablePreference(
                property = HabitSettings::hideMedia,
                title = R.string.title_hide_media,
                leadingIcon = Icons.Rounded.UnfoldLess
            )
        }

        group(title = R.string.settings_group_forum) {
            forumSortPreference()
        }

        group(title = R.string.settings_group_thread) {
            toggleablePreference(
                property = HabitSettings::showBothName,
                title = R.string.title_show_both_username_and_nickname,
                leadingIcon = Icons.Outlined.Verified
            )

            preference(
                title = R.string.title_settings_sticky_header,
                summary = R.string.tip_sticky_header,
                onClick = onStickyHeaderClicked,
                leadingIcon = Icons.Rounded.PageHeader,
                trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            )

            collectSeeLzPreference() // 收藏贴自动只看楼主

            toggleablePreference(
                property = HabitSettings::collectedDesc,
                title = R.string.settings_collect_thread_desc_sort,
                leadingIcon = Icons.AutoMirrored.Rounded.Sort,
                summaryOn = R.string.tip_collect_thread_desc_sort_on,
                summaryOff = R.string.tip_collect_thread_desc_sort
            )
        }

        group(title = R.string.settings_group_reply) {
            hideReplyPreference()

            toggleablePreference(
                property = HabitSettings::hideReplyWarning,
                title = R.string.title_hide_reply_warning,
                enabled = !currentPreference.hideReply,
                leadingIcon = Icons.Outlined.SecurityUpdateWarning
            )
        }
    }
}

fun SettingsSegmentedPrefsScope<HabitSettings>.imageLoadPreference() {
    listPref(
        property = HabitSettings::imageLoadType,
        title = R.string.title_settings_image_load_type,
        options = persistentMapOf(
            ImageUtil.SETTINGS_SMART_ORIGIN to R.string.title_image_load_type_smart_origin,
            ImageUtil.SETTINGS_SMART_LOAD to R.string.title_image_load_type_smart_load,
            ImageUtil.SETTINGS_ALL_ORIGIN to R.string.title_image_load_type_all_origin,
        ),
        leadingIcon = Icons.Outlined.PhotoSizeSelectActual
    )
}

fun SettingsSegmentedPrefsScope<HabitSettings>.forumSortPreference() {
    listPref(
        property = HabitSettings::forumSortType,
        title = R.string.title_settings_default_sort_type,
        options = persistentMapOf(
            ForumSortType.BY_REPLY to R.string.title_sort_by_reply,
            ForumSortType.BY_SEND to R.string.title_sort_by_send,
        ),
        leadingIcon = Icons.Outlined.CalendarViewDay
    )
}

fun SettingsSegmentedPrefsScope<HabitSettings>.collectSeeLzPreference() {
    toggleablePreference(
        property = HabitSettings::collectedDesc,
        title = R.string.settings_collect_thread_see_lz,
        leadingIcon = Icons.Outlined.StarOutline,
        summaryOn = R.string.tip_collect_thread_see_lz_on,
        summaryOff = R.string.tip_collect_thread_see_lz
    )
}

fun SettingsSegmentedPrefsScope<HabitSettings>.hideReplyPreference() {
    toggleablePreference(
        property = HabitSettings::hideReply,
        title = R.string.title_hide_reply,
        leadingIcon = Icons.Outlined.SpeakerNotesOff
    )
}
