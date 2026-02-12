package com.huanchengfly.tieba.post.ui.page.settings.theme

import android.app.Activity.RESULT_OK
import android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.AnimationConstants.DefaultDurationMillis
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastForEachIndexed
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.color.utilities.Variant
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.TranslucentThemeActivity
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.theme.TiebaBlue
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.theme.colorscheme.BlueColorScheme
import com.huanchengfly.tieba.post.theme.colorscheme.GreenColorScheme
import com.huanchengfly.tieba.post.theme.isDarkScheme
import com.huanchengfly.tieba.post.theme.isTranslucent
import com.huanchengfly.tieba.post.ui.common.theme.compose.PaletteBackground
import com.huanchengfly.tieba.post.ui.common.theme.compose.animateBackground
import com.huanchengfly.tieba.post.ui.common.theme.compose.onCase
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.isWindowHeightCompact
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.isWindowWidthCompact
import com.huanchengfly.tieba.post.ui.models.Like
import com.huanchengfly.tieba.post.ui.models.settings.Theme
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.main.NavigationDrawerItem
import com.huanchengfly.tieba.post.ui.page.main.home.HistoryItem
import com.huanchengfly.tieba.post.ui.page.main.rememberNavigationItems
import com.huanchengfly.tieba.post.ui.page.main.user.StatCard
import com.huanchengfly.tieba.post.ui.page.subposts.PostLikeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.ChipText
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultFabEnterTransition
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultFabExitTransition
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.FloatingTab
import com.huanchengfly.tieba.post.ui.widgets.compose.FloatingTabRow
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.RoundedSlider
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.StrongBox
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.AnyPopDialogProperties
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.ColorPickerDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.DirectionState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.utils.LocalAccount
import com.huanchengfly.tieba.post.utils.StringUtil
import kotlinx.coroutines.launch
import java.io.File

private val ThemePanelShape = RoundedCornerShape(16.dp)
private val ThemeItemMargin = 12.dp

private enum class ThemePage(val nameRes: Int) {
    Featured(nameRes = R.string.theme_tab_featured),
    Custom(nameRes = R.string.theme_tab_custom)
}

private val PagerState.currentTheme: ThemePage
    get() = ThemePage.entries[currentPage]

@Composable
private fun AppThemeSaveDialog(
    state: DialogState,
    isSaving: Boolean,
    onDiscardClicked: () -> Unit,
    onSaveClicked: () -> Unit
) =
    Dialog(
        dialogState = state,
        dialogProperties = AnyPopDialogProperties(
            direction = DirectionState.CENTER,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        title = {
            val title = if (isSaving) R.string.theme_dialog_saving else R.string.theme_dialog_unsave
            Text(text = stringResource(title))
        },
        buttons = {
            AnimatedVisibility(visible = !isSaving) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DialogNegativeButton(
                        text = stringResource(R.string.button_discard),
                        onClick = onDiscardClicked
                    )
                    Spacer(modifier = Modifier.weight(1.0f))

                    DialogNegativeButton(text = stringResource(R.string.button_cancel))

                    Button(onClick = onSaveClicked) {
                        Text(text = stringResource(R.string.button_save_profile), maxLines = 1)
                    }
                }
            }
        }
    ) {
        Text(
            text = if (isSaving) {
                stringResource(id = R.string.dialog_content_wait)
            } else {
                stringResource(id = R.string.theme_dialog_unsave_msg)
            }
        )
    }

@Composable
fun AppThemePage(
    navigator: NavController = LocalNavController.current,
    viewModel: AppThemeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val pagerState = rememberPagerState { ThemePage.entries.size }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val customPrimaryColorDialogState = rememberDialogState()
    if (customPrimaryColorDialogState.show) {
        ColorPickerDialog(
            state = customPrimaryColorDialogState,
            title = R.string.theme_title_color_picker,
            initial = uiState.pickedVariant?.color ?: TiebaBlue,
            onColorChanged = viewModel::onCustomColorPicked
        )
    }

    val translucentThemeActivityLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK && pagerState.currentTheme == ThemePage.Featured) {
            viewModel.onTranslucentThemeChanged()
        }
    }

    val saveThemeDialogState = rememberDialogState()

    val onSaveThemeClicked: () -> Unit = {
        saveThemeDialogState.show()
        viewModel
            .onSaveClicked(isFeatured = pagerState.currentTheme == ThemePage.Featured)
            .invokeOnCompletion { navigator.navigateUp() }
    }

    if (saveThemeDialogState.show) {
        AppThemeSaveDialog(
            state = saveThemeDialogState,
            isSaving = uiState.savingTheme,
            onDiscardClicked = navigator::navigateUp,
            onSaveClicked = onSaveThemeClicked
        )
    }

    val currentScheme = MaterialTheme.colorScheme

    var themeWidgetLayoutData by remember { mutableStateOf<Pair<IntSize, Offset>?>(null) }
    if (currentScheme.isTranslucent && !isWindowHeightCompact()) {
        themeWidgetLayoutData?.let { (size, pos) ->
            TranslucentThemeOverlay(widgetPanelSize = size, positionInWindow = pos)
        }
    }

    MyScaffold(
        topBar = {
            TitleCentredToolbar(
                title = stringResource(id = R.string.title_theme),
                navigationIcon = { BackNavigationIcon(onBackPressed = navigator::navigateUp) },
                actions = {
                    val themeChanged by when (pagerState.currentTheme) {
                        ThemePage.Featured -> viewModel.isBuiltInThemeChanged.collectAsStateWithLifecycle()
                        ThemePage.Custom -> viewModel.isCustomThemeChanged.collectAsStateWithLifecycle()
                    }

                    AnimatedVisibility(
                        visible = themeChanged,
                        enter = DefaultFabEnterTransition,
                        exit = DefaultFabExitTransition
                    ) {
                        ActionItem(
                            icon = Icons.Rounded.Save,
                            contentDescription = R.string.button_save_profile,
                            onClick = onSaveThemeClicked
                        )
                    }

                    BackHandler(enabled = themeChanged, onBack = saveThemeDialogState::show)
                }
            )
        },
        bottomBar = { // Use Transparent navigation bar here
            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(ThemeItemMargin),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val defaultTheme = remember { BuiltInTheme(Theme.BLUE, BlueColorScheme) }
            val initialized by remember { derivedStateOf { uiState.builtInThemes.isNotEmpty() } }
            var isDarkMode by remember { mutableStateOf(currentScheme.isDarkScheme) }

            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .verticalScroll(rememberScrollState())
            ) {
                if (!isWindowHeightCompact()) {
                    val currentTheme = when (pagerState.currentTheme) {
                        ThemePage.Featured -> uiState.pickedBuiltInTheme
                        ThemePage.Custom -> uiState.pickedVariant ?: uiState.pickedBuiltInTheme
                    }

                    ThemedWidgetPanel(
                        modifier = Modifier.onCase(currentScheme.isTranslucent) {
                            // Track layout coordinates on translucent theme
                            onPlaced {
                                if (currentTheme is TranslucentTheme) {
                                    themeWidgetLayoutData = it.size to it.positionInWindow()
                                }
                            }
                        },
                        theme = currentTheme ?: defaultTheme,
                        isDarkMode = { isDarkMode },
                        onDarkModeChanged = { isDarkMode = !isDarkMode }
                    )
                } else {
                    Spacer(modifier = Modifier.matchParentSize())
                }
            }

            AnimatedVisibility(
                visible = initialized,
                enter = fadeIn() + slideInVertically { it }
            ) {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.height(120.dp),
                    beyondViewportPageCount = 1,
                    userScrollEnabled = false
                ) {
                    when(ThemePage.entries[it]) {
                        ThemePage.Featured -> GeneralThemePickerRow(
                            builtInThemes = uiState.builtInThemes,
                            selected = { uiState.pickedBuiltInTheme },
                            isDarkMode = { isDarkMode },
                            onThemePicked = { newTheme ->
                                if (newTheme.theme == Theme.TRANSLUCENT) {
                                    // Trim to 50% of cache size
                                    Glide.get(context).trimMemory(TRIM_MEMORY_UI_HIDDEN)
                                    translucentThemeActivityLauncher.launch(
                                        Intent(context, TranslucentThemeActivity::class.java)
                                    )
                                    // Wait activity result
                                } else {
                                    viewModel.onBuiltInThemePicked(newTheme)
                                }
                            }
                        )

                        ThemePage.Custom  -> VariantThemePickerRow(
                            variantList = uiState.variantThemes,
                            selected = { uiState.pickedVariant },
                            isDarkMode = { isDarkMode },
                            onThemePicked = viewModel::onCustomVariantPicked,
                            onColorPickerClicked = customPrimaryColorDialogState::show
                        )
                    }
                }
            }

            FloatingTabRow {
                ThemePage.entries.forEachIndexed { i, page ->
                    FloatingTab(
                        selected = pagerState.currentPage == i,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(page = i) }
                        },
                        icon = {
                            val icon = if (page == ThemePage.Featured) Icons.Rounded.Favorite else Icons.Rounded.Palette
                            AnimatedVisibility(visible = pagerState.currentPage == i) {
                                Icon(imageVector = icon, contentDescription = null)
                            }
                        },
                        content = { Text(text = stringResource(id = page.nameRes)) }
                    )
                }
            }
        }
    }
}

// UserStatsCard and DarkMode status Chip
@Composable
private fun UserStatWidgetRow(
    modifier: Modifier = Modifier,
    posts: String?,
    fans: String?,
    concerned: String?,
    isDarkMode: () -> Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space = ThemeItemMargin),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val primaryContainerAni by animateColorAsState(colorScheme.primaryContainer)
            val secondaryColorAni by animateColorAsState(colorScheme.secondary)

            FloatingActionButton(
                onClick = {},
                modifier = Modifier.size(size = Sizes.Medium),
                shape = CircleShape,
                containerColor = primaryContainerAni,
                contentColor = colorScheme.onPrimaryContainer,
                content = { Icon(Icons.Rounded.Add, contentDescription = null) }
            )

            ChipText(
                text = stringResource(if (isDarkMode()) R.string.dark_color else R.string.light_color),
                containerColor = secondaryColorAni,
                contentColor = colorScheme.onSecondary,
            )
        }

        StrongBox(
            modifier = Modifier.weight(1.0f)
        ) {
            val secondaryContainerAni by animateColorAsState(
                targetValue = colorScheme.secondaryContainer,
                animationSpec = TweenSpec(delay = 100, durationMillis = DefaultDurationMillis * 2)
            )

            Surface(
                shape = MaterialTheme.shapes.small,
                color = secondaryContainerAni,
                contentColor = colorScheme.onSecondaryContainer
            ) {
                StatCard(posts, fans, concerned)
            }
        }
    }
}

// Sliders and some tiny Widgets
@Composable
private fun SlidersWidgetRow(
    modifier: Modifier = Modifier,
    isDarkMode: () -> Boolean,
    onDarkModeChanged: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier.height(IntrinsicSize.Min),
        // Do this manually to make Sliders 'visually aligned'
        // horizontalArrangement = Arrangement.spacedBy(space = ThemeItemMargin),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HistoryItem(
            modifier = Modifier.padding(top = 8.dp, end = ThemeItemMargin),
            title = stringResource(R.string.title_fuzzy_match),
            avatar = { Avatar(R.drawable.ic_launcher_new_round, size = Sizes.Tiny) },
            color = colorScheme.surfaceContainerLow,
            contentColor = colorScheme.onSurface
        )

        StrongBox(
            modifier = Modifier.minimumInteractiveComponentSize()
        ) {
            val tertiaryAni by animateColorAsState(colorScheme.tertiary)
            FloatingActionButton(
                onClick = onDarkModeChanged,
                containerColor = tertiaryAni,
                contentColor = colorScheme.onTertiary,
                content = {
                    Icon(if (isDarkMode()) Icons.Filled.DarkMode else Icons.Rounded.WbSunny, null)
                }
            )
        }

        Spacer(modifier = Modifier.width(ThemeItemMargin))

        Switch(
            checked = isDarkMode(),
            onCheckedChange = { onDarkModeChanged() },
            thumbContent = {
                if (isDarkMode()) Icon(Icons.Filled.DarkMode, contentDescription = null)
            }
        )

        Box(modifier = Modifier.fillMaxHeight()) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, start = 22.dp),
                gapSize = Dp.Hairline
            )

            StrongBox(
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                var slider by remember { mutableFloatStateOf(0.3f) }
                RoundedSlider(
                    value = slider,
                    onValueChange = { slider = it },
                    modifier = Modifier.graphicsLayer {
                        translationX = ThemeItemMargin.toPx()
                        translationY = 12.dp.toPx()
                    }
                )
            }
        }
    }
}

@Composable
private fun UserPostCardWidget(modifier: Modifier = Modifier, account: Account?, postText: Int) {
    val colorScheme = MaterialTheme.colorScheme

    CompositionLocalProvider(LocalContentColor provides colorScheme.onSurface) {
        Column(
            modifier = modifier
                .animateBackground(color = colorScheme.surface, shape = MaterialTheme.shapes.small)
                .padding(all = ThemeItemMargin)
                .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow))
        ) {
            UserHeader(
                avatar = {
                    if (account?.portrait.isNullOrEmpty()) {
                        Avatar(R.drawable.ic_launcher_new_round, size = Sizes.Small)
                    } else {
                        Avatar(remember { StringUtil.getAvatarUrl(account.portrait) }, size = Sizes.Small)
                    }
                },
                name = {
                    Text(text = account?.name ?: stringResource(R.string.app_name))
                },
                desc = {
                    Text(text = stringResource(R.string.relative_date_minute, 1), maxLines = 1)
               },
            ) {
                var like by remember { mutableStateOf(Like(true, 99999)) }
                PostLikeButton(like, onClick = { like = !like })
            }

            Text(
                text = stringResource(id = postText),
                modifier = Modifier.padding(start = 44.dp, top = 8.dp)
            )
        }
    }
}

@Composable
private fun CompactNavigationDrawer(modifier: Modifier = Modifier) {
    var selected by remember { mutableIntStateOf(0) }
    val navItems = rememberNavigationItems()

    Column(
        modifier = modifier
            .animateBackground(color = MaterialTheme.colorScheme.surface, shape = ThemePanelShape)
            .padding(16.dp)
    ) {
        navItems.fastForEachIndexed { index, navigationItem ->
            NavigationDrawerItem(
                selected = index == selected,
                onClick = { if (index != selected) { selected = index } },
                label = { Text(text = stringResource(navigationItem.title)) },
                icon = {
                    Icon(
                        painter = rememberAnimatedVectorPainter(
                            animatedImageVector = navigationItem.icon(),
                            atEnd = index == selected
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(Sizes.Tiny)
                    )
                }
            )
        }
    }
}

@Composable
private fun TranslucentThemeOverlay(widgetPanelSize: IntSize, positionInWindow: Offset) {
    val currentScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                val size = widgetPanelSize.toSize()
                val widgetCornerSize = ThemePanelShape.topStart.toPx(size, this)
                val cornerRadius = CornerRadius(widgetCornerSize)

                // simi-transparent background overlay
                drawRect(currentScheme.background.copy(0.76f))

                // erase background overlay with panel rect
                drawRoundRect(
                    Color.Black,
                    positionInWindow,
                    size,
                    cornerRadius,
                    blendMode = BlendMode.Clear
                )
            }
    )
}

@Composable
private fun ThemedWidgetPanel(
    modifier: Modifier = Modifier,
    theme: AppTheme,
    isDarkMode: () -> Boolean,
    onDarkModeChanged: () -> Unit
) {
    val account = LocalAccount.current
    val colorScheme = theme.getColorScheme(isDarkMode())
    val isTranslucentTheme = theme is TranslucentTheme
    val isCompactWidth = isWindowWidthCompact()

    Box(
        modifier = modifier
            .onCase(condition = !isTranslucentTheme) {
                animateBackground(
                    color = colorScheme.surfaceColorAtElevation(4.dp),
                    shape = ThemePanelShape,
                    animationSpec = spring(stiffness = Spring.StiffnessVeryLow)
                )
            }
            .padding(all = ThemeItemMargin)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center
        ) {
            MaterialTheme(colorScheme = colorScheme) {
                Column(
                    modifier = Modifier.fillMaxWidth(fraction = if (isCompactWidth) 1.0f else 0.65f)
                ) {
                    UserStatWidgetRow(
                        posts = account?.posts ?: "2.6K",
                        fans = account?.fans ?: "10",
                        concerned = account?.concerned ?: "10W",
                        isDarkMode = isDarkMode
                    )

                    SlidersWidgetRow(isDarkMode = isDarkMode, onDarkModeChanged = onDarkModeChanged)

                    Spacer(modifier = Modifier.height(height = ThemeItemMargin))

                    UserPostCardWidget(account = account, postText = theme.description)
                }

                if (!isCompactWidth) {
                    Spacer(modifier = Modifier.width(36.dp))

                    CompactNavigationDrawer(modifier = Modifier.width(160.dp))
                }
            }
        }
    }
}

@Composable
private fun ThemeItem(
    modifier: Modifier = Modifier,
    selected: Boolean,
    name: String,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Column(
        modifier = modifier
            .width(IntrinsicSize.Min)
            .height(IntrinsicSize.Min)
            .semantics(mergeDescendants = true) {
                role = Role.Checkbox
                contentDescription = name
                this.selected = selected
            },
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val shape = MaterialTheme.shapes.large
        val colorScheme = MaterialTheme.colorScheme

        Box(
            modifier = Modifier
                .size(Sizes.Large)
                .onCase(condition = selected) {
                    border(width = 1.dp, color = colorScheme.onSurfaceVariant, shape)
                        .padding(1.dp)
                        .border(
                            width = 1.5.dp,
                            color = colorScheme.surfaceColorAtElevation(4.dp),
                            shape = shape
                        )
                }
                .clip(shape = shape)
                .background(color = colorScheme.surfaceBright)
                .clickable(onClick = onClick)
                .padding(all = 8.dp)
                .clip(shape = CircleShape),
            content = content
        )

        Text(
            text = name,
            autoSize = TextAutoSize.StepBased(10.sp, MaterialTheme.typography.titleMedium.fontSize),
            maxLines = 1,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@NonRestartableComposable
@Composable
private fun ColorSchemeItem(
    themeColor: ColorScheme,
    selected: Boolean,
    name: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) =
    ThemeItem(
        modifier = modifier,
        selected = selected,
        name = stringResource(id = name),
        onClick = onClick
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRect(color = themeColor.primary, size = size.copy(height = size.height / 2))

            val quarterSize = size * 0.5f
            drawRect(
                color = themeColor.secondary,
                topLeft = Offset(x = 0f, y = quarterSize.height),
                size = quarterSize
            )
            drawRect(
                color = themeColor.tertiary,
                topLeft = Offset(x = quarterSize.width, y = quarterSize.height),
                size = quarterSize
            )
        }
    }

@Composable
fun TranslucentThemeBackground(modifier: Modifier = Modifier, file: File?) {
    GlideImage(
        model = file ?: R.drawable.user_header,
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        failure = placeholder(R.drawable.user_header),
        requestBuilderTransform = { it.diskCacheStrategy(DiskCacheStrategy.NONE) }
    )
}

@Composable
private fun TranslucentItem(
    modifier: Modifier = Modifier,
    background: File?,
    selected: Boolean,
    onClick: () -> Unit
) {
    ThemeItem(
        modifier = modifier,
        selected = selected,
        name = stringResource(id = R.string.theme_translucent),
        onClick = onClick,
        content = { TranslucentThemeBackground(Modifier.matchParentSize(), file = background) }
    )
}

@NonRestartableComposable
@Composable
private fun DynamicItem(
    colorScheme: ColorScheme,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) =
    ThemeItem(modifier, selected, stringResource(id = R.string.theme_dynamic), onClick) {
        PaletteBackground(
            modifier = Modifier.matchParentSize(),
            colors = remember {
                with(colorScheme) {
                    listOf(primaryContainer, secondaryContainer, Color.LightGray, tertiary, primary)
                }
            }
        )
    }

@Composable
private fun GeneralThemePickerRow(
    modifier: Modifier = Modifier,
    builtInThemes: List<BuiltInTheme>,
    selected: () -> BuiltInTheme?,
    isDarkMode: () -> Boolean,
    onThemePicked: (BuiltInTheme) -> Unit
) {
    Surface(
        modifier = modifier,
        shape = ThemePanelShape,
        tonalElevation = 4.dp,
    ) {
        LazyRow(
            modifier = Modifier.padding(all = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(items = builtInThemes, key = { _, item -> item.name }) { index, theme ->
                val selected = selected()?.theme == theme.theme
                val colorScheme = theme.getColorScheme(isDarkMode())
                val onClick = { onThemePicked(theme) }

                when(theme.theme) {
                    Theme.TRANSLUCENT -> TranslucentItem(
                        background = (theme as TranslucentTheme).background,
                        selected = selected,
                        onClick = onClick
                    )

                    Theme.DYNAMIC -> DynamicItem(colorScheme, selected, onClick = onClick)

                    else -> {
                        ColorSchemeItem(colorScheme, selected, theme.name, onClick = onClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun VariantThemePickerRow(
    modifier: Modifier = Modifier,
    variantList: List<VariantTheme>,
    selected: () -> VariantTheme?,
    isDarkMode: () -> Boolean,
    onThemePicked: (VariantTheme) -> Unit,
    onColorPickerClicked: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val background = colorScheme.surfaceColorAtElevation(4.dp)

    Surface(
        modifier = modifier,
        shape = ThemePanelShape,
        color = background,
        contentColor = colorScheme.onSurface
    ) {
        Row(modifier = Modifier.padding(all = 16.dp)) {
            // Do not use StickyHeader
            ThemeItem(
                modifier = Modifier.padding(end = 8.dp),
                selected = false,
                name = stringResource(id = R.string.theme_btn_color_picker),
                onClick = onColorPickerClicked
            ) {
                Icon(
                    imageVector = Icons.Rounded.Colorize,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    tint = selected()?.color ?: colorScheme.onSurface
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(items = variantList, key = { _, it -> it.name }) { index, theme ->
                    ColorSchemeItem(
                        themeColor = theme.getColorScheme(isDarkMode()),
                        selected = selected()?.variant == theme.variant,
                        name = theme.name,
                        onClick = { onThemePicked(theme) }
                    )
                }
            }
        }
    }
}

@Preview("ThemedWidgetPanel", device = Devices.PIXEL_TABLET)
@Composable
private fun ThemedWidgetPanelPreview() = TiebaLiteTheme {
    CompositionLocalProvider(LocalAccount provides null) {
        val isDark = isSystemInDarkTheme()
        ThemedWidgetPanel(
            theme = BuiltInTheme(Theme.GREEN, GreenColorScheme),
            isDarkMode = { isDark },
            onDarkModeChanged = {}
        )
    }
}

@Preview("ColorSchemeItem", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun ColorSchemeItemPreview() = TiebaLiteTheme {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ColorSchemeItem(
            modifier = Modifier.padding(12.dp),
            themeColor = BlueColorScheme.lightColor,
            selected = true,
            name = R.string.theme_blue,
            onClick = {}
        )
        ColorSchemeItem(
            modifier = Modifier.padding(12.dp),
            themeColor = BlueColorScheme.darkColor,
            selected = false,
            name = R.string.theme_blue,
            onClick = {}
        )
    }
}

@Preview("VariantThemePickerRow")
@Composable
private fun VariantThemePickerRowPreview() = TiebaLiteTheme {
    val variantThemes = remember { Variant.entries.map { VariantTheme(TiebaBlue, it) } }
    VariantThemePickerRow(
        variantList = variantThemes,
        selected = { variantThemes[2] },
        isDarkMode = { false },
        onThemePicked = {},
        onColorPickerClicked = {}
    )
}
