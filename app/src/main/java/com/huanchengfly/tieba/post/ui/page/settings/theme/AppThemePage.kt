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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.TranslucentThemeActivity
import com.huanchengfly.tieba.post.goToActivity
import com.huanchengfly.tieba.post.rememberPreferenceAsMutableState
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.PaletteBackground
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.ColorPickerDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import java.io.File

private val ThemeButtonHeight = 56.dp

private val MediumRoundedShape by lazy { RoundedCornerShape(6.dp) }

@Destination
@Composable
fun AppThemePage(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val themeValues = stringArrayResource(id = R.array.theme_values)
    val themeNames = stringArrayResource(id = R.array.themeNames)
    val currentTheme by ThemeUtil.themeState
    val isDynamicTheme by rememberPreferenceAsState(
        key = booleanPreferencesKey(ThemeUtil.KEY_USE_DYNAMIC_THEME),
        defaultValue = false
    )
    val customPrimaryColorDialogState = rememberDialogState()

    var customPrimaryColorInt by rememberPreferenceAsMutableState(
        key = intPreferencesKey(ThemeUtil.KEY_CUSTOM_PRIMARY_COLOR),
        defaultValue = 0x4477E0 // TiebaBlue
    )
    val customPrimaryColor by remember { derivedStateOf { Color(customPrimaryColorInt) } }

    var customToolbarPrimaryColor by rememberPreferenceAsMutableState(
        key = booleanPreferencesKey(ThemeUtil.KEY_CUSTOM_TOOLBAR_PRIMARY_COLOR),
        defaultValue = false
    )
    var customStatusBarFontDark by rememberPreferenceAsMutableState(
        key = booleanPreferencesKey(ThemeUtil.KEY_CUSTOM_STATUS_BAR_FONT_DARK),
        defaultValue = false
    )

    ColorPickerDialog(
        state = customPrimaryColorDialogState,
        title = R.string.title_custom_theme,
        initial = customPrimaryColor,
        onColorChanged = { newColor ->
            customPrimaryColorInt = newColor.toArgb()
            customStatusBarFontDark = customStatusBarFontDark || !customToolbarPrimaryColor
            ThemeUtil.setUseDynamicTheme(false)
            ThemeUtil.switchTheme(ThemeUtil.THEME_CUSTOM)
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
        if (customToolbarPrimaryColor) {
            Spacer(Modifier.height(8.dp))
            CheckableButton(
                modifier = Modifier.padding(start = 10.dp),
                checked = customStatusBarFontDark,
                text = R.string.tip_status_bar_font,
                onCheckedChange = {
                    customStatusBarFontDark = it
                }
            )
        }
    }

    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = stringResource(id = R.string.title_theme),
                navigationIcon = { BackNavigationIcon(onBackPressed = navigator::navigateUp) }
            )
        },
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
                        selected = isDynamicTheme,
                        onClick = { ThemeUtil.setUseDynamicTheme(true) }
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
                        selected = !isDynamicTheme && currentTheme == ThemeUtil.THEME_CUSTOM,
                        onClick = customPrimaryColorDialogState::show
                    )

                    TranslucentThemeButton(
                        modifier = Modifier.weight(1f),
                        selected = ThemeUtil.isTranslucentTheme(currentTheme),
                        onClick = {
                            // Trim to 50% of cache size
                            Glide.get(context).trimMemory(TRIM_MEMORY_UI_HIDDEN)
                            context.goToActivity<TranslucentThemeActivity>()
                        }
                    )
                }
            }

            itemsIndexed(items = themeValues, key = { _, item -> item }) { index, item ->
                val name = themeNames[index]
                val backgroundColor = remember {
                    Color(
                        App.ThemeDelegate.getColorByAttr(
                            context,
                            R.attr.colorBackground,
                            item
                        )
                    )
                }
                val primaryColor = remember {
                    Color(
                        App.ThemeDelegate.getColorByAttr(
                            context,
                            R.attr.colorNewPrimary,
                            item
                        )
                    )
                }
                val accentColor = remember {
                    Color(
                        App.ThemeDelegate.getColorByAttr(
                            context,
                            R.attr.colorAccent,
                            item
                        )
                    )
                }
                val onAccentColor = remember {
                    Color(
                        App.ThemeDelegate.getColorByAttr(
                            context,
                            R.attr.colorOnAccent,
                            item
                        )
                    )
                }
                val onBackgroundColor = remember {
                    Color(
                        App.ThemeDelegate.getColorByAttr(
                            context,
                            R.attr.colorText,
                            item
                        )
                    )
                }

                if (ThemeUtil.isNightMode(item)) {
                    ThemeItem(
                        themeName = name,
                        themeValue = item,
                        primaryColor = backgroundColor,
                        accentColor = backgroundColor,
                        contentColor = onBackgroundColor,
                        selected = !isDynamicTheme && currentTheme == item,
                        onClick = {
                            ThemeUtil.switchTheme(item)
                            ThemeUtil.setUseDynamicTheme(false)
                        }
                    )
                } else {
                    ThemeItem(
                        themeName = name,
                        themeValue = item,
                        primaryColor = primaryColor,
                        accentColor = accentColor,
                        contentColor = onAccentColor,
                        selected = !isDynamicTheme && currentTheme == item,
                        onClick = {
                            ThemeUtil.switchTheme(item)
                            ThemeUtil.setUseDynamicTheme(false)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeItem(
    themeName: String,
    themeValue: String,
    primaryColor: Color,
    accentColor: Color,
    contentColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(
                onClickLabel = themeName,
                onClick = onClick
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val isNightTheme = ThemeUtil.isNightMode(themeValue)
        val background = if (primaryColor == accentColor) {
            Modifier.background(color = primaryColor, shape = CircleShape)
        } else {
            Modifier.background(
                brush = Brush.radialGradient(listOf(primaryColor, accentColor)),
                shape = CircleShape
            )
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .then(background)
                .padding(9.dp),
        ) {
            if (isNightTheme) {
                Icon(
                    imageVector = Icons.Rounded.NightsStay,
                    contentDescription = stringResource(id = R.string.desc_night_theme),
                    tint = contentColor
                )
            }
        }
        Text(text = themeName, modifier = Modifier.weight(1f))

        AnimatedVisibility(selected) {
            Icon(
                imageVector = Icons.Rounded.DoneOutline,
                contentDescription = stringResource(id = R.string.desc_checked),
                tint = if (isNightTheme) contentColor else primaryColor
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
        tint = ExtendedTheme.colors.windowBackground
    )
    Text(
        text = stringResource(id = title),
        fontWeight = FontWeight.Bold,
        color = ExtendedTheme.colors.windowBackground
    )
}