package com.huanchengfly.tieba.post.ui.page.settings.theme

import android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.DoneOutline
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.PhotoSizeSelectActual
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.navigation.NavController
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.TranslucentThemeActivity
import com.huanchengfly.tieba.post.goToActivity
import com.huanchengfly.tieba.post.rememberPreferenceAsMutableState
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.theme.BuiltInThemes
import com.huanchengfly.tieba.post.theme.Grey200
import com.huanchengfly.tieba.post.theme.TiebaBlue
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedColors
import com.huanchengfly.tieba.post.ui.common.theme.compose.LocalExtendedColors
import com.huanchengfly.tieba.post.ui.common.theme.compose.PaletteBackground
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.ColorPickerDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import java.io.File

private val ThemeButtonHeight = 56.dp

private val MediumRoundedShape by lazy { RoundedCornerShape(6.dp) }

@Composable
fun AppThemePage(navigator: NavController = LocalNavController.current) {
    val context = LocalContext.current
    val currentTheme = LocalExtendedColors.current

    val customPrimaryColorDialogState = rememberDialogState()

    val customPrimaryColorInt by rememberPreferenceAsState(
        key = intPreferencesKey(ThemeUtil.KEY_CUSTOM_PRIMARY_COLOR),
        defaultValue = TiebaBlue.toArgb()
    )
    val customPrimaryColor = Color(customPrimaryColorInt)

    var customToolbarPrimaryColor by rememberPreferenceAsMutableState(
        key = booleanPreferencesKey(ThemeUtil.KEY_TINT_TOOLBAR),
        defaultValue = false
    )
    ColorPickerDialog(
        state = customPrimaryColorDialogState,
        title = R.string.title_custom_theme,
        initial = customPrimaryColor,
        onColorChanged = { newColor ->
            ThemeUtil.switchCustomTheme(newColor, context)
        }
    ) {
        CheckableButton(
            modifier = Modifier.padding(start = 10.dp),
            checked = customToolbarPrimaryColor,
            text = R.string.tip_toolbar_primary_color,
            onCheckedChange = {
                customToolbarPrimaryColor = it
            }
        )
    }

    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = stringResource(id = R.string.title_theme),
                navigationIcon = { BackNavigationIcon(onBackPressed = navigator::navigateUp) }
            )
        },
        bottomBar = {
            // Use Transparent navigation bar here
            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    DynamicThemeButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        selected = currentTheme.theme == ThemeUtil.THEME_DYNAMIC,
                        onClick = { ThemeUtil.switchTheme(ThemeUtil.THEME_DYNAMIC, context) }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CustomThemeButton(
                        modifier = Modifier
                            .weight(1f)
                            .background(color = customPrimaryColor, shape = MediumRoundedShape),
                        selected = currentTheme.theme == ThemeUtil.THEME_CUSTOM,
                        onClick = customPrimaryColorDialogState::show
                    )

                    TranslucentThemeButton(
                        modifier = Modifier.weight(1f),
                        selected = currentTheme.theme == ThemeUtil.THEME_TRANSLUCENT_LIGHT || currentTheme.theme == ThemeUtil.THEME_TRANSLUCENT_DARK,
                        onClick = {
                            // Trim to 50% of cache size
                            Glide.get(context).trimMemory(TRIM_MEMORY_UI_HIDDEN)
                            context.goToActivity<TranslucentThemeActivity>()
                        }
                    )
                }
            }

            items(items = BuiltInThemes, key = { item -> item.theme }) { item ->
                ThemeItem(
                    themeColor = item,
                    name = stringResource(id = item.name),
                    onClick = {
                        ThemeUtil.switchTheme(item.theme, context)
                    }
                )
            }
        }
    }
}

@Composable
private fun ThemeItem(themeColor: ExtendedColors, name: String, onClick: () -> Unit) {
    val selected = LocalExtendedColors.current == themeColor
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(onClickLabel = name, onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val background = if (themeColor.isNightMode) themeColor.windowBackground else themeColor.primary
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color = background, shape = CircleShape)
                .padding(9.dp),
        ) {
            if (themeColor.isNightMode) {
                Icon(
                    imageVector = Icons.Rounded.NightsStay,
                    contentDescription = stringResource(id = R.string.desc_night_theme),
                    tint = themeColor.onPrimary
                )
            }
        }
        Text(text = name, modifier = Modifier.weight(1f))

        AnimatedVisibility(selected) {
            Icon(
                imageVector = Icons.Rounded.DoneOutline,
                contentDescription = stringResource(id = R.string.desc_checked),
                tint = if (themeColor.isNightMode) themeColor.onPrimary else themeColor.primary
            )
        }
    }
}

@Composable
private fun CheckableButton(
    modifier: Modifier = Modifier,
    checked: Boolean,
    @StringRes text: Int,
    onCheckedChange: (Boolean) -> Unit
) = Row(
    modifier = modifier.clickable(onClick = { onCheckedChange(!checked) }),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    Checkbox(checked = checked, onCheckedChange = onCheckedChange)

    Text(text = stringResource(id = text))
}


@Composable
@RequiresApi(31)
private fun DynamicThemeButton(modifier: Modifier = Modifier, selected: Boolean, onClick: () -> Unit) {
    PaletteBackground(
        modifier = modifier
            .height(ThemeButtonHeight)
            .clickable(onClick = onClick),
        shape = MediumRoundedShape
    ) {
        SelectableIconTheme(
            modifier = Modifier.align(Alignment.Center),
            title = R.string.title_dynamic_theme,
            icon = Icons.Rounded.Colorize,
            selected = selected
        )
    }
}

@Composable
private fun CustomThemeButton(modifier: Modifier = Modifier, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(ThemeButtonHeight)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        SelectableIconTheme(
            title = R.string.title_custom_color,
            icon = Icons.Rounded.ColorLens,
            selected = selected
        )
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun TranslucentThemeButton(modifier: Modifier = Modifier, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(ThemeButtonHeight)
            .clip(MediumRoundedShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val preference = LocalContext.current.appPreferences
        val background: File? by preference.translucentThemeBackgroundFile.collectAsState(null)
        GlideImage(
            model = background ?: R.drawable.user_header,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            failure = placeholder(R.drawable.user_header),
            requestBuilderTransform = { it.diskCacheStrategy(DiskCacheStrategy.NONE) }
        )
        SelectableIconTheme(
            title = R.string.title_theme_translucent,
            icon = Icons.Rounded.PhotoSizeSelectActual,
            selected = selected
        )
    }
}

@Composable
private fun SelectableIconTheme(
    modifier: Modifier = Modifier,
    @StringRes title: Int,
    icon: ImageVector,
    selected: Boolean
) = Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(16.dp)
) {
    Icon(
        imageVector = if (selected) Icons.Rounded.Check else icon,
        contentDescription = null,
        tint = Grey200
    )
    Text(
        text = stringResource(id = title),
        fontWeight = FontWeight.Bold,
        color = Grey200
    )
}