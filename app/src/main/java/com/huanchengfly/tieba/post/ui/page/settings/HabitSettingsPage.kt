package com.huanchengfly.tieba.post.ui.page.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.BrandingWatermark
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.outlined.CalendarViewDay
import androidx.compose.material.icons.outlined.NightsStay
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
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.components.ImageUploader
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsScreen
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.ListPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.SwitchPref
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils.Companion.ForumFabFunction
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils.Companion.ForumSortType
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils.Companion.KEY_FORUM_FAB_FUNCTION
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils.Companion.KEY_FORUM_SORT_DEFAULT
import com.huanchengfly.tieba.post.utils.ImageUtil
import kotlinx.collections.immutable.persistentMapOf

@Composable
fun HabitSettingsPage(onBack: () -> Unit) = MyScaffold(
    backgroundColor = Color.Transparent,
    topBar = {
        TitleCentredToolbar(
            title = stringResource(id = R.string.title_settings_read_habit),
            navigationIcon = { BackNavigationIcon(onBackPressed = onBack) }
        )
    },
) { paddingValues ->
    PrefsScreen(
        contentPadding = paddingValues
    ) {
        prefsItem {
            ImageLoadPreference()
        }
        prefsItem {
            ListPref(
                key = intPreferencesKey(ImageUploader.KEY_PIC_WATERMARK_TYPE),
                title = R.string.title_settings_image_watermark,
                options = persistentMapOf(
                    ImageUploader.PIC_WATER_TYPE_NO to R.string.title_image_watermark_none,
                    ImageUploader.PIC_WATER_TYPE_USER_NAME to R.string.title_image_watermark_user_name,
                    ImageUploader.PIC_WATER_TYPE_FORUM_NAME to R.string.title_image_watermark_forum_name
                ),
                useSelectedAsSummary = true,
                defaultValue = ImageUploader.PIC_WATER_TYPE_FORUM_NAME,
                leadingIcon = Icons.AutoMirrored.Outlined.BrandingWatermark
            )
        }
        prefsItem {
            SwitchPref(
                key = AppPreferencesUtils.KEY_DARKEN_IMAGE_WHEN_NIGHT_MODE,
                title = R.string.settings_image_darken_when_night_mode,
                defaultChecked = true,
                leadingIcon = Icons.Outlined.NightsStay
            )
        }
        prefsItem {
            ListPref(
                key = intPreferencesKey(KEY_FORUM_SORT_DEFAULT),
                title = R.string.title_settings_default_sort_type,
                options = persistentMapOf(
                    ForumSortType.BY_REPLY to R.string.title_sort_by_reply,
                    ForumSortType.BY_SEND to R.string.title_sort_by_send,
                ),
                useSelectedAsSummary = true,
                defaultValue = ForumSortType.BY_REPLY,
                leadingIcon = Icons.Outlined.CalendarViewDay
            )
        }
        prefsItem {
            ListPref(
                key = stringPreferencesKey(KEY_FORUM_FAB_FUNCTION),
                title = R.string.settings_forum_fab_function,
                defaultValue = ForumFabFunction.BACK_TO_TOP,
                useSelectedAsSummary = true,
                leadingIcon = Icons.AutoMirrored.Outlined.ExitToApp,
                options = persistentMapOf(
                    ForumFabFunction.POST to R.string.btn_post,
                    ForumFabFunction.REFRESH to R.string.btn_refresh,
                    ForumFabFunction.BACK_TO_TOP to R.string.btn_back_to_top,
                    ForumFabFunction.HIDE to R.string.btn_hide
                )
            )
        }
        prefsItem {
            SwitchPref(
                key = AppPreferencesUtils.KEY_POST_HIDE_MEDIA,
                title = R.string.title_hide_media,
                defaultChecked = false,
                leadingIcon = Icons.Rounded.UnfoldLess
            )
        }
        prefsItem {
            CollectSeeLzPreference()
        }
        prefsItem {
            SwitchPref(
                key = AppPreferencesUtils.KEY_COLLECTED_DESC,
                title = R.string.settings_collect_thread_desc_sort,
                defaultChecked = false,
                leadingIcon = Icons.AutoMirrored.Rounded.Sort,
                summaryOn = R.string.tip_collect_thread_desc_sort_on,
                summaryOff = R.string.tip_collect_thread_desc_sort
            )
        }
        prefsItem {
            SwitchPref(
                key = AppPreferencesUtils.KEY_SHOW_NICKNAME,
                title = R.string.title_show_both_username_and_nickname,
                defaultChecked = false,
                leadingIcon = Icons.Outlined.Verified
            )
        }
        prefsItem {
            SwitchPref(
                key = AppPreferencesUtils.KEY_HOME_PAGE_SHOW_HISTORY,
                title = R.string.settings_home_page_show_history_forum,
                defaultChecked = true,
                leadingIcon = Icons.Outlined.WatchLater
            )
        }
        prefsItem {
            SwitchPref(
                key = AppPreferencesUtils.KEY_REPLY_WARNING,
                title = R.string.title_post_or_reply_warning,
                defaultChecked = true,
                leadingIcon = Icons.Outlined.SecurityUpdateWarning
            )
        }
        prefsItem {
            HideReplyPreference()
        }
    }
}

@Composable
fun ImageLoadPreference(modifier: Modifier = Modifier) {
    ListPref(
        key = intPreferencesKey(ImageUtil.KEY_IMAGE_LOAD_TYPE),
        title = R.string.title_settings_image_load_type,
        modifier = modifier,
        options = persistentMapOf(
            ImageUtil.SETTINGS_SMART_ORIGIN to R.string.title_image_load_type_smart_origin,
            ImageUtil.SETTINGS_SMART_LOAD to R.string.title_image_load_type_smart_load,
            ImageUtil.SETTINGS_ALL_ORIGIN to R.string.title_image_load_type_all_origin,
            ImageUtil.SETTINGS_ALL_NO to R.string.title_image_load_type_all_no
        ),
        useSelectedAsSummary = true,
        defaultValue = ImageUtil.SETTINGS_SMART_ORIGIN,
        leadingIcon = Icons.Outlined.PhotoSizeSelectActual
    )
}

@Composable
fun DefaultSortPreference(modifier: Modifier = Modifier) {
    ListPref(
        key = intPreferencesKey(KEY_FORUM_SORT_DEFAULT),
        title = R.string.title_settings_default_sort_type,
        modifier = modifier,
        options = persistentMapOf(
            ForumSortType.BY_REPLY to R.string.title_sort_by_reply,
            ForumSortType.BY_SEND to R.string.title_sort_by_send,
        ),
        useSelectedAsSummary = true,
        defaultValue = ForumSortType.BY_REPLY,
        leadingIcon = Icons.Outlined.CalendarViewDay
    )
}

@Composable
fun CollectSeeLzPreference(modifier: Modifier = Modifier) {
    SwitchPref(
        key = AppPreferencesUtils.KEY_COLLECTED_SEE_LZ,
        title = R.string.settings_collect_thread_see_lz,
        modifier = modifier,
        defaultChecked = true,
        leadingIcon = Icons.Outlined.StarOutline,
        summaryOn = R.string.tip_collect_thread_see_lz_on,
        summaryOff = R.string.tip_collect_thread_see_lz
    )
}

@Composable
fun HideReplyPreference(modifier: Modifier = Modifier) {
    SwitchPref(
        key = AppPreferencesUtils.KEY_REPLY_HIDE,
        title = R.string.title_hide_reply,
        modifier = modifier,
        defaultChecked = false,
        leadingIcon = Icons.Outlined.SpeakerNotesOff
    )
}
