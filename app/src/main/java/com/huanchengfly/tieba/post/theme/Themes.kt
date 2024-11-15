package com.huanchengfly.tieba.post.theme

import androidx.compose.ui.graphics.Color
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedColors
import com.huanchengfly.tieba.post.utils.ThemeUtil
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

val TranslucentLight = ExtendedColors(
    theme = ThemeUtil.THEME_TRANSLUCENT_LIGHT,
    isNightMode = false,
    name = R.string.title_theme_translucent,
    primary = TiebaBlue,
    onPrimary = Grey50,
    secondary = LightPurple,
    onSecondary = Grey50,
    topBar = Color.Transparent,
    onTopBar = LightSystemBar,
    bottomBar = Color.Transparent,
    text = Grey100,
    textSecondary = Color.White.copy(alpha = 0.7f),
    background = Color.Transparent,
    chip = Color(0x18F8F8F8),
    onChip = Grey300,
    card = Color.White.copy(alpha = 0.6f),
    floorCard = Color(0x15FFFFFF), //WhiteA8
    divider = Color.White.copy(alpha = 0.4f),
    indicator = Color.White,
    windowBackground = Grey900
)

val TranslucentDark = ExtendedColors(
    theme = ThemeUtil.THEME_TRANSLUCENT_DARK,
    isNightMode = false,
    name = R.string.title_theme_translucent,
    primary = TiebaBlue,
    onPrimary = Grey900,
    secondary = DeepPurple,
    onSecondary = Grey900,
    topBar = Color.Transparent,
    onTopBar = DarkSystemBar,
    bottomBar = Color.Transparent,
    text = Grey900,
    textSecondary = Color.Black.copy(alpha = 0.7f),
    background = Color.Transparent,
    chip = Color(0x2A323232),
    onChip = Grey500,
    card = Color(0x20000000), // BlackA13
    floorCard = Color(0x2A000000), // BlackA16
    divider = Color(0x15000000), // BlackA8
    indicator = Color(0xFF1C1C1C),
    windowBackground = Grey100
)

private val LightColors = ExtendedColors(
    theme = "",
    isNightMode = false,
    name = 0,
    onPrimary = Grey50,
    onSecondary = Grey50,
    topBar = Color.White,
    onTopBar = DarkSystemBar,
    bottomBar = Color.White,
    text = Color.Black,
    textSecondary = Grey700,
    background = Color.White,
    chip = Grey100,
    onChip = Grey600,
    card = Color.White,
    floorCard = Grey100,
    divider = Color(0xFFF5F5F5),
    indicator = Color.White,
    windowBackground = Color.White
)

val DefaultColors: ExtendedColors
    get() = BlueColors

val DefaultDarkColors: ExtendedColors
    get() = DarkBlueColors

val BlueColors = LightColors.copy(
    theme = ThemeUtil.THEME_BLUE,
    name = R.string.theme_blue,
    primary = TiebaBlue,
    secondary = LightPurple
)

val BlackColors = LightColors.copy(
    theme = ThemeUtil.THEME_BLACK,
    name = R.string.theme_black,
    primary = Color.Black,
    secondary = Grey800
)

val PinkColors = LightColors.copy(
    theme = ThemeUtil.THEME_PINK,
    name = R.string.theme_pink,
    primary = Color(0xFFFF9A9E),
    secondary = Color(0xFFFFB3B6)
)

val RedColors = LightColors.copy(
    theme = ThemeUtil.THEME_RED,
    name = R.string.theme_red,
    primary = Color(0xFFC51100),
    secondary = Color(0xFF7B0B00)
)

val PurpleColors = LightColors.copy(
    theme = ThemeUtil.THEME_PURPLE,
    name = R.string.theme_purple,
    primary = Color(0xFF512DA8),
    secondary = Color(0xFF8F2DA8)
)

val CustomColors = DefaultColors.copy(
    theme = ThemeUtil.THEME_CUSTOM
)

private val NightColors = ExtendedColors(
    theme = "",
    isNightMode = true,
    name = 0,
    primary = TiebaBlue,
    onPrimary = Grey50,
    secondary = DeepPurple,
    onSecondary = Grey50,
    onTopBar = LightSystemBar,
    text = Grey200,
    textSecondary = Grey400,
    background = Color.Black
)

val DarkGreyColors = NightColors.copy(
    theme = ThemeUtil.THEME_GREY_DARK,
    name = R.string.theme_grey_dark,
    topBar = Grey900,
    bottomBar = Color(0xFF303030),
    background = Grey900,
    chip = Color(0xFF323232),
    onChip = Grey600,
    card = Color(0xFF2A2A2A),
    floorCard = Color(0xFF1E1E1E),
    divider = Grey900,
    windowBackground = Grey900
)

val DarkAmoledColors = NightColors.copy(
    theme = ThemeUtil.THEME_AMOLED_DARK,
    name = R.string.theme_amoled_dark,
    topBar = Color.Black,
    bottomBar = Color.Black,
    chip = Grey900,
    onChip = Grey600,
    card = Color(0xFF101010),
    floorCard = Color(0xFF151515),
    divider = Color.Black,
    indicator = Color(0xFF1C1C1C),
    windowBackground = Color.Black
)

val DarkBlueColors = NightColors.copy(
    theme = ThemeUtil.THEME_BLUE_DARK,
    name = R.string.theme_blue_dark,
    topBar = BlueDark,
    bottomBar = Color(0xFF1B2733),
    background = Color(0xFF17212B),
    chip = BlueDeepDark,
    onChip = BlueGrey400,
    card = Color(0xFF202B37),
    floorCard = BlueDeepDark,
    divider = BlueDeepDark,
    indicator = Color(0xFF202B37),
    windowBackground = BlueDark
)

val BuiltInThemes: ImmutableList<ExtendedColors> by lazy {
    persistentListOf(
        BlueColors,
        BlackColors,
        PinkColors,
        RedColors,
        PurpleColors,
        DarkBlueColors,
        DarkGreyColors,
        DarkAmoledColors
    )
}