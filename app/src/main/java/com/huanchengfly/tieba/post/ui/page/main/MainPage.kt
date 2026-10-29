package com.huanchengfly.tieba.post.ui.page.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationItemColors
import androidx.compose.material3.ShortNavigationBarDefaults
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.material3.adaptive.layout.PaneExpansionAnchor
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteColors
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.lerp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEvent
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.core.common.ktx.unsafeLazy
import com.huanchengfly.tieba.post.core.data.model.settings.NavigationLabel
import com.huanchengfly.tieba.post.core.data.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.core.designsystem.component.IconNavigationItem
import com.huanchengfly.tieba.post.core.designsystem.component.NavigationDrawerItem
import com.huanchengfly.tieba.post.core.designsystem.component.floatingNavigationBarCompactScreenOffset
import com.huanchengfly.tieba.post.core.designsystem.component.navigationsuite.LocalNavSuiteScaffoldState
import com.huanchengfly.tieba.post.core.designsystem.component.navigationsuite.NavigationBarHeight
import com.huanchengfly.tieba.post.core.designsystem.component.navigationsuite.NavigationSuiteScaffoldLayout
import com.huanchengfly.tieba.post.core.designsystem.component.navigationsuite.TallNavigationBarHeight
import com.huanchengfly.tieba.post.core.designsystem.component.navigationsuite.TbNavigationSuite
import com.huanchengfly.tieba.post.core.designsystem.component.navigationsuite.TbNavigationSuiteScaffoldState
import com.huanchengfly.tieba.post.core.designsystem.component.navigationsuite.TbNavigationSuiteScaffoldState.Companion.isVisible
import com.huanchengfly.tieba.post.core.designsystem.component.navigationsuite.TbNavigationSuiteScaffoldState.Companion.rememberNavigationSuiteScaffoldState
import com.huanchengfly.tieba.post.core.designsystem.component.navigationsuite.TbNavigationSuiteType
import com.huanchengfly.tieba.post.core.designsystem.component.navigationsuite.TbNavigationSuiteType.Companion.toNavigationSuiteType
import com.huanchengfly.tieba.post.core.designsystem.component.navigationsuite.calculateNavigationType
import com.huanchengfly.tieba.post.core.designsystem.component.navigationsuite.navigationSuiteScaffoldConsumeWindowInsets
import com.huanchengfly.tieba.post.core.designsystem.component.vibrantFloatingNavigationBarColor
import com.huanchengfly.tieba.post.core.designsystem.component.vibrantFloatingNavigationBarContentColor
import com.huanchengfly.tieba.post.core.designsystem.layout.LocalWindowAdaptiveInfo
import com.huanchengfly.tieba.post.core.designsystem.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.core.designsystem.util.isTranslucent
import com.huanchengfly.tieba.post.core.navigation.NavigationState
import com.huanchengfly.tieba.post.core.navigation.Navigator
import com.huanchengfly.tieba.post.core.navigation.containsType
import com.huanchengfly.tieba.post.core.navigation.defaultContentKey
import com.huanchengfly.tieba.post.core.navigation.toEntries
import com.huanchengfly.tieba.post.core.ui.context.LocalUISettings
import com.huanchengfly.tieba.post.core.ui.modifier.onCase
import com.huanchengfly.tieba.post.core.ui.modifier.withNonNull
import com.huanchengfly.tieba.post.core.ui.scenes.rememberDetailPaneBackHandlerSceneDecoratorStrategy
import com.huanchengfly.tieba.post.core.ui.util.scrollToTop
import com.huanchengfly.tieba.post.ui.common.NavTransitions
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.appEntry
import com.huanchengfly.tieba.post.ui.page.settings.rememberSettingsSceneStrategy
import com.huanchengfly.tieba.post.ui.page.settings.settingsEntry
import com.huanchengfly.tieba.post.ui.scenes.rememberTbThemeSceneDecoratorStrategy
import com.huanchengfly.tieba.post.ui.widgets.compose.AccountNavIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultBackToTopFAB
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TbHazeState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberTbHazeState
import com.huanchengfly.tieba.post.utils.LocalAccount
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * List of [androidx.navigation3.runtime.NavEntry.contentKey]
 * */
private typealias NavEntryContentKeyList = List<Any>

@Stable
val MainDestination.titleRes: Int
    @StringRes get() = when(this) {
        MainDestination.Home -> R.string.title_main
        MainDestination.Explore -> R.string.title_explore
        MainDestination.Notification -> R.string.title_notifications
        MainDestination.User -> R.string.title_user
    }

@Stable
val MainDestination.iconRes: Int
    @DrawableRes get() = when(this) {
        MainDestination.Home -> R.drawable.ic_animated_rounded_inventory_2
        MainDestination.Explore -> R.drawable.ic_animated_toy_fans
        MainDestination.Notification -> R.drawable.ic_animated_rounded_notifications
        MainDestination.User -> R.drawable.ic_animated_rounded_person
    }

val bottomNavigationPlaceholder: @Composable () -> Unit = {
    val navigationSuiteType = LocalNavSuiteScaffoldState.current.layoutType
    if (navigationSuiteType.isNavigationBar) {
        Spacer(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(
                    when (navigationSuiteType) {
                        TbNavigationSuiteType.ShortNavigationBarCompact -> NavigationBarHeight
                        TbNavigationSuiteType.FloatingNavigationBar -> {
                            TallNavigationBarHeight + floatingNavigationBarCompactScreenOffset
                        }

                        TbNavigationSuiteType.FloatingNavigationBarCompact -> {
                            NavigationBarHeight + floatingNavigationBarCompactScreenOffset
                        }

                        else -> TallNavigationBarHeight
                    }
                )
        )
    } else {
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
fun MainPage(
    navigator: Navigator,
    settingsRepo: SettingsRepository,
    vm: MainPageViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val uiSettings = LocalUISettings.current
    val navigationState = navigator.state
    val loggedIn = LocalAccount.current != null
    val mainDestinations: List<MainDestination> = remember(loggedIn, uiSettings.hideExplore) {
        listOfNotNull(
            MainDestination.Home,
            MainDestination.Explore.takeUnless { uiSettings.hideExplore },
            MainDestination.Notification.takeIf { loggedIn },
            MainDestination.User,
        )
    }
    val mainDestContentKeys: NavEntryContentKeyList = remember(mainDestinations) {
        mainDestinations.fastMap { it.defaultContentKey }
    }

    val windowAdaptiveInfo = LocalWindowAdaptiveInfo.current
    val directive = remember(windowAdaptiveInfo) {
        calculatePaneScaffoldDirective(windowAdaptiveInfo).copy(horizontalPartitionSpacerSize = 0.dp)
    }
    val isListDetail = directive.maxHorizontalPartitions > 1
    val scaffoldState = rememberNavigationSuiteScaffoldState(
        initialValue = NavigationSuiteScaffoldValue.Hidden,
        floating = uiSettings.bottomNavFloating,
        noLabel = uiSettings.bottomNavLabel == NavigationLabel.NONE,
        windowAdaptiveInfo = windowAdaptiveInfo
    )

    val isTranslucent = MaterialTheme.colorScheme.isTranslucent
    val blurEffect = !uiSettings.reduceEffect && !isTranslucent
    val hazeState = if (blurEffect) rememberTbHazeState() else null

    MainNavigationSuiteScaffold(
        state = scaffoldState,
        hazeState = hazeState,
        navigationItems = {
            val messageCount by vm.messageCountFlow.collectAsStateWithLifecycle()

            MainNavigationItems(
                items = mainDestinations,
                isSelected = { it === navigationState.currentTopLevelKey },
                onSelect = { dest ->
                    navigator.navigate(key = dest)
                    if (dest === MainDestination.Notification) vm.onNavigateNotification()
                },
                onReSelect = { dest ->
                    coroutineScope.emitGlobalEvent(GlobalEvent.ScrollToTop(dest))
                },
                mainNavigationSuiteType = scaffoldState.layoutType,
                bottomNavLabel = uiSettings.bottomNavLabel,
                messageCount = { messageCount },
            )
        },
        navigationSuiteColors = mainNavigationSuiteColors(uiSettings.bottomNavFloating, blurEffect),
        navigationVerticalArrangement = Arrangement.Center,
        primaryActionContent = {
            val onLoginClicked: () -> Unit = { navigator.navigate(Destination.Login) }
            when (scaffoldState.layoutType) {
                TbNavigationSuiteType.WideNavigationRailCollapsed,
                TbNavigationSuiteType.WideNavigationRailExpanded -> {
                    AccountNavIcon(onLoginClicked, modifier = Modifier.padding(start = 32.dp))
                }
                TbNavigationSuiteType.NavigationRail -> {
                    AccountNavIcon(onLoginClicked, modifier = Modifier.padding(top = 10.dp))
                }
                TbNavigationSuiteType.NavigationDrawer -> TbDrawerNavigationAction(onLoginClicked)

                TbNavigationSuiteType.FloatingNavigationBarCompact -> {
                    if (uiSettings.hideExplore) return@MainNavigationSuiteScaffold
                    ExplorePrimaryAction(visible = MainDestination.Explore === navigationState.currentTopLevelKey) {
                        coroutineScope.emitGlobalEvent(GlobalEvent.ScrollToTop(MainDestination.Explore))
                    }
                }

                else -> Unit // NavigationBar or None
            }
        }
    ) {
        SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
            val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(
                directive = directive,
                paneExpansionDragHandle = { state ->
                    val interactionSource = remember { MutableInteractionSource() }
                    VerticalDragHandle(
                        modifier =
                            Modifier.paneExpansionDraggable(
                                state,
                                LocalMinimumInteractiveComponentSize.current,
                                interactionSource,
                            ), interactionSource = interactionSource
                    )
                },
                paneExpansionState = rememberPaneExpansionState(
                    anchors = listOf(
                        PaneExpansionAnchor.Proportion(0.35f),
                        PaneExpansionAnchor.Proportion(0.5f),
                    )
                )
            )

            val onBack: () -> Unit = navigator::navigateUp
            val navTransitions = if (!uiSettings.reduceMotion || isListDetail || isTranslucent) {
                NavTransitions.DefaultTransitions
            } else {
                NavTransitions.SlideTransitions
            }
            LookaheadScope {
                NavDisplay(
                    entries = navigationState.toEntries(
                        entryProvider = entryProvider {
                            mainEntry(navigator, hazeState, scaffoldState)

                            appEntry(isListDetail, navigator)

                            settingsEntry(navigator, settingsRepo)
                        }
                    ),
                    sceneStrategies = listOf(listDetailStrategy, DialogSceneStrategy()),
                    sceneDecoratorStrategies = listOf(
                        rememberDetailPaneBackHandlerSceneDecoratorStrategy(onBack),
                        rememberTbThemeSceneDecoratorStrategy(directive, sharedTransitionScope = this@SharedTransitionLayout),
                        rememberSettingsSceneStrategy(sharedTransitionScope = this@SharedTransitionLayout),
                    ),
                    sharedTransitionScope = this@SharedTransitionLayout,
                    transitionSpec = topTransitionSpec(
                        navSuiteScaffoldState = scaffoldState,
                        contentKeys = mainDestContentKeys,
                        defaultSpect = navTransitions.transitionSpec
                    ),
                    popTransitionSpec = {
                        when {
                            targetState.key in mainDestContentKeys -> {
                                MAIN_FADE_IN_TRANSITION togetherWith navTransitions.popExitTransition
                            }

                            else -> navTransitions.popTransitionSpec
                        }
                    },
                    predictivePopTransitionSpec = predictiveTransition(scaffoldState, navTransitions),
                    onBack = onBack,
                )
            }
        }

        LaunchedBottomNavigationEffect(isListDetail, scaffoldState, navigationState)
    }
}

@Composable
private fun LaunchedBottomNavigationEffect(
    isListDetail: Boolean,
    state: TbNavigationSuiteScaffoldState,
    navigationState: NavigationState,
) {
    val coroutineScope = rememberCoroutineScope()
    val fullScreenNavKey = remember {
        listOf(Destination.Login::class, Destination.AppTheme::class, Destination.UserProfile::class, Destination.Welcome::class)
    }
    LaunchedEffect(navigationState.currentKey, isListDetail) {
        val navState = when {
            navigationState.currentKey is MainDestination -> NavigationSuiteScaffoldValue.Visible

            isListDetail && !fullScreenNavKey.containsType(navigationState.currentKey) -> {
                NavigationSuiteScaffoldValue.Visible
            }

            else -> NavigationSuiteScaffoldValue.Hidden
        }
        if (navState !== state.targetValue) {
            coroutineScope.launch {
                if (state.layoutType.isNavigationBar) state.setState(navState) else state.snapTo(navState)
            }
        }
    }
}

/**
 * [NavigationSuiteScaffold] with Haze blur support
 *
 * @see NavigationSuiteScaffoldLayout
 * @see Modifier.navigationSuiteScaffoldConsumeWindowInsets
 * */
@Composable
private fun MainNavigationSuiteScaffold(
    navigationItems: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: TbHazeState? = null,
    navigationBarAtop: Boolean = true,
    navigationSuiteColors: NavigationSuiteColors = NavigationSuiteDefaults.colors(),
    navigationVerticalArrangement: Arrangement.Vertical = NavigationSuiteDefaults.verticalArrangement,
    state: TbNavigationSuiteScaffoldState = rememberNavigationSuiteScaffoldState(),
    primaryActionContent: @Composable (() -> Unit) = {},
    primaryActionContentHorizontalAlignment: Alignment.Horizontal =
        NavigationSuiteScaffoldDefaults.primaryActionContentAlignment,
    content: @Composable () -> Unit = {},
) {
    val mainNavSuiteType = state.layoutType
    val navigationSuiteType = mainNavSuiteType.toNavigationSuiteType()

    NavigationSuiteScaffoldLayout(
        modifier = modifier,
        navigationSuite = {
            TbNavigationSuite(
                tbNavigationSuiteType = mainNavSuiteType,
                modifier = Modifier
                    .withNonNull(hazeState?.takeIf { mainNavSuiteType.isNavigationBar }) {
                        Modifier.defaultHazeEffect { blurEnabled = state.currentValue.isVisible }
                    },
                colors = navigationSuiteColors,
                verticalArrangement = navigationVerticalArrangement,
                primaryActionContent = primaryActionContent,
                content = navigationItems,
            )
        },
        state = state,
        navigationSuiteType = navigationSuiteType,
        navigationBarAtop = navigationBarAtop,
        primaryActionContent = primaryActionContent,
        primaryActionContentHorizontalAlignment = primaryActionContentHorizontalAlignment,
        content = {
            Box(
                Modifier.navigationSuiteScaffoldConsumeWindowInsets(
                    navigationSuiteType,
                    navigationBarAtop,
                    state,
                ),
            ) {
                content()
            }
        },
    )
}

@Stable
private fun NavigationLabel.visible(selected: Boolean): Boolean {
    return when(this) {
        NavigationLabel.ALWAYS -> true
        NavigationLabel.SELECTED -> selected
        NavigationLabel.NONE -> false
    }
}

@Composable
private fun MainNavigationItems(
    items: List<MainDestination>,
    isSelected: (MainDestination) -> Boolean,
    modifier: Modifier = Modifier,
    onSelect: (MainDestination) -> Unit = {},
    onReSelect: (MainDestination) -> Unit = {},
    mainNavigationSuiteType: TbNavigationSuiteType = calculateMainNavigationSuiteType(),
    bottomNavLabel: NavigationLabel = NavigationLabel.ALWAYS,
    messageCount: () -> String? = { null },
) {
    val isNavigationBar = mainNavigationSuiteType.isNavigationBar
    items.fastForEach { destination ->
        val selected = isSelected(destination)
        MainNavigationSuiteItem(
            selected = selected,
            onClick = {
                if (selected) onReSelect(destination) else onSelect(destination)
            },
            icon = {
                Icon(
                    painter = rememberAnimatedVectorPainter(
                        animatedImageVector = AnimatedImageVector.animatedVectorResource(destination.iconRes),
                        atEnd = selected
                    ),
                    modifier = Modifier.size(Sizes.Tiny),
                    contentDescription = stringResource(destination.titleRes),
                )
            },
            label = if (mainNavigationSuiteType != TbNavigationSuiteType.NavigationRail &&
                (!isNavigationBar || bottomNavLabel.visible(selected))
            ) {
                { Text(stringResource(id = destination.titleRes)) }
            } else {
                null
            },
            modifier = modifier
                .onCase(mainNavigationSuiteType == TbNavigationSuiteType.NavigationDrawer) {
                    padding(horizontal = 16.dp)
                },
            mainNavigationSuiteType = mainNavigationSuiteType,
            badge = if (destination === MainDestination.Notification) {
                {
                    messageCount()?.let { messageCountText ->
                        Badge {
                            Text(
                                text = messageCountText, // 6.sp ~ BadgeTokens.LargeLabelTextFont.fontSize
                                autoSize = TextAutoSize.StepBased(6.sp, LocalTextStyle.current.fontSize),
                                maxLines = 1
                            )
                        }
                    }
                }
            } else null,
        )
    }
}

// Override NavigationDrawer to use our old custom NavigationDrawerItem
@Composable
private fun MainNavigationSuiteItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit)?,
    modifier: Modifier = Modifier,
    mainNavigationSuiteType: TbNavigationSuiteType,
    enabled: Boolean = true,
    badge: @Composable (() -> Unit)? = null,
    colors: NavigationItemColors? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    if (mainNavigationSuiteType == TbNavigationSuiteType.FloatingNavigationBarCompact) {
        IconNavigationItem(
            selected = selected,
            onClick = onClick,
            icon = {
                if (badge != null) {
                    BadgedBox(badge = { badge.invoke() }, content = { icon() })
                } else {
                    icon()
                }
            },
            colors = colors ?: ShortNavigationBarItemDefaults.colors(),
            enabled = enabled,
            modifier = modifier,
            interactionSource = interactionSource,
        )
    } else if (mainNavigationSuiteType != TbNavigationSuiteType.NavigationDrawer) {
        androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem(
            selected = selected,
            onClick = onClick,
            icon = icon,
            label = label,
            modifier = modifier,
            navigationSuiteType = mainNavigationSuiteType.toNavigationSuiteType(),
            enabled = enabled,
            badge = badge,
            colors = colors,
            interactionSource = interactionSource,
        )
    } else {
        val actualColors = if (colors != null) {
            NavigationDrawerItemDefaults.colors(
                selectedIconColor = colors.selectedIconColor,
                selectedTextColor = colors.selectedTextColor,
                unselectedIconColor = colors.unselectedIconColor,
                unselectedTextColor = colors.unselectedTextColor,
                selectedContainerColor = colors.selectedIndicatorColor,
            )
        } else {
            NavigationSuiteDefaults.itemColors().navigationDrawerItemColors
        }

        NavigationDrawerItem(
            modifier = modifier,
            selected = selected,
            onClick = onClick,
            icon = icon,
            badge = badge,
            label = { label?.invoke() ?: Text("") },
            colors = actualColors,
            interactionSource = interactionSource,
        )
    }
}

/**
 * @return [NavigationSuiteColors] for background blurring
 *  */
@Composable
private fun mainNavigationSuiteColors(floatingNavBar: Boolean, blur: Boolean): NavigationSuiteColors {
    return TiebaLiteTheme.extendedColorScheme.run {
        NavigationSuiteDefaults.colors(
            shortNavigationBarContainerColor = if (floatingNavBar) {
                colorScheme.vibrantFloatingNavigationBarColor.copy(
                    alpha = when {
                        colorScheme.isTranslucent -> 0.65f
                        blur -> if (darkTheme) 0.86f else 0.74f
                        else -> 1f
                    }
                )
            } else {
                navigationContainer
            },
            shortNavigationBarContentColor = if (floatingNavBar) {
                colorScheme.vibrantFloatingNavigationBarContentColor
            } else {
                ShortNavigationBarDefaults.contentColor
            },
            navigationBarContainerColor = navigationContainer
        )
    }
}

@Composable
private fun ExplorePrimaryAction(modifier: Modifier = Modifier, visible: Boolean, onClick: () -> Unit) {
    val screenOffset = floatingNavigationBarCompactScreenOffset
    val visibilityAnimation by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
    )
    DefaultBackToTopFAB(
        modifier = modifier
            .graphicsLayer { // SlideIn vertically
                translationY = lerp(size.height * 2, -screenOffset.toPx(), visibilityAnimation)
            },
        visible = visible,
        size = NavigationBarHeight,
        onClick = onClick,
    )
}

// Common ScrollToTop event listener for main destinations
@Composable
inline fun <reified T: MainDestination> OnMainNavigationScrollTopEvent(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    topAppBarState: TopAppBarState? = null,
    crossinline scrollableState: () -> ScrollableState?,
) {
    val haptic = LocalHapticFeedback.current
    onGlobalEvent<GlobalEvent.ScrollToTop>(coroutineScope, filter = { it.tag is T }) {
        val state = scrollableState()
        if (state?.canScrollBackward == true) {
            haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)
            coroutineScope.launch {
                state.scrollToTop()
            }
        }
        // Reset TopAppBarState
        topAppBarState?.contentOffset = 0f
        topAppBarState?.heightOffset = 0f
    }
}

/**
 * Creates a list of [TopAppBarState] that is remembered across compositions.
 * */
@Composable
fun rememberTopAppBarScrollBehaviors(
    size: Int,
    init: @Composable (TopAppBarState) -> TopAppBarScrollBehavior
): List<TopAppBarScrollBehavior> {
    var result: List<TopAppBarScrollBehavior> by remember(size) { mutableStateOf(emptyList()) }

    val stateList = rememberSaveable(size, saver = Saver) {
        val states = mutableListOf<TopAppBarState>()
        repeat(size) {
            states.add(TopAppBarState(-Float.MAX_VALUE, 0f, 0f))
        }
        states.toImmutableList()
    }

    if (result.isEmpty()) {
        result = stateList.fastMap { init(it) }
    }
    return result
}

/** The default [Saver] implementation for list of [TopAppBarState]. */
@Suppress("UNCHECKED_CAST")
private val Saver: Saver<List<TopAppBarState>, *> = listSaver(
    save = {
        it.mapIndexed { i, it ->
            mutableListOf(it.heightOffsetLimit, it.heightOffset, it.contentOffset)
        }.reduce { rec, list ->
            rec.apply { addAll(list) }
        }
    },
    restore = {
        assert(it.isNotEmpty())
        val states = mutableListOf<TopAppBarState>()
        var trimList = it
        val saver = (TopAppBarState.Saver as Saver<TopAppBarState, List<Float>>)
        repeat(it.size / 3) { i ->
            trimList = it.subList(i * 3, it.size)
            val state = saver.restore(trimList)!!
            states.add(state)
        }
        states.toImmutableList()
    }
)

// Pager style transition
private fun <T : Any> topTransitionSpec(
    navSuiteScaffoldState: TbNavigationSuiteScaffoldState,
    contentKeys: NavEntryContentKeyList,
    defaultSpect: ContentTransform,
): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    val navigationSuiteType = navSuiteScaffoldState.layoutType
    var from = -1
    var to = -1
    val initialKey = initialState.entries.lastOrNull()?.contentKey
    val targetKey = targetState.entries.lastOrNull()?.contentKey
    contentKeys.fastForEachIndexed { i, dest ->
        if (from == -1 && initialKey == dest) {
            from = i
        } else if (to == -1 && targetKey == dest) {
            to = i
        }
    }
    when {
        from == -1 || to == -1 -> defaultSpect

        navigationSuiteType.isNavigationBar -> if (from > to) SlideRtl else SlideLtr

        navigationSuiteType === TbNavigationSuiteType.None -> defaultSpect

        else -> if (from > to) SlideBtt else SlideTtb
    }
}

private fun predictiveTransition(
    navSuiteScaffoldState: TbNavigationSuiteScaffoldState,
    navTransitions: NavTransitions,
): AnimatedContentTransitionScope<Scene<NavKey>>.(@NavigationEvent.SwipeEdge Int) -> ContentTransform = { event ->
    // Use scale predictive transition if isReduceMotion, list-detail or isTranslucent is true
    if (navTransitions === NavTransitions.DefaultTransitions || !navSuiteScaffoldState.layoutType.isNavigationBar) {
        NavTransitions.DefaultTransitions.popTransitionSpec
    } else {
        val edgeEnterTransition = slideInHorizontally(
            initialOffsetX = { if (event == NavigationEvent.EDGE_LEFT) -it / 8 else it / 8 }
        ) + scaleIn(
            initialScale = 0.9f
        )
        val edgeExitTransition = slideOutHorizontally(
            targetOffsetX = { if (event == NavigationEvent.EDGE_LEFT) it else -it }
        ) + fadeOut(
            animationSpec = tween(delayMillis = 100)
        )
        edgeEnterTransition togetherWith edgeExitTransition
    }
}

private val MAIN_TRANSITION_SPEC: FiniteAnimationSpec<IntOffset> =
    spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset.VisibilityThreshold)

private val MAIN_FADE_IN_TRANSITION: EnterTransition =
    fadeIn(tween(durationMillis = 300, easing = FastOutSlowInEasing))

private val MAIN_FADE_OUT_TRANSITION: ExitTransition =
    fadeOut(tween(durationMillis = 300, easing = FastOutSlowInEasing))

private val SlideRtl: ContentTransform by unsafeLazy {
    ContentTransform(
        slideInHorizontally(MAIN_TRANSITION_SPEC, initialOffsetX = { -it }) + MAIN_FADE_IN_TRANSITION,
        slideOutHorizontally(MAIN_TRANSITION_SPEC, targetOffsetX = { it }) + MAIN_FADE_OUT_TRANSITION,
    )
}

private val SlideLtr: ContentTransform by unsafeLazy {
    ContentTransform(
        slideInHorizontally(MAIN_TRANSITION_SPEC, initialOffsetX = { it }) + MAIN_FADE_IN_TRANSITION,
        slideOutHorizontally(MAIN_TRANSITION_SPEC, targetOffsetX = { -it }) + MAIN_FADE_OUT_TRANSITION
    )
}

private val SlideTtb: ContentTransform by unsafeLazy {
    ContentTransform(
        slideInVertically(MAIN_TRANSITION_SPEC, initialOffsetY = { it }) + MAIN_FADE_IN_TRANSITION,
        slideOutVertically(MAIN_TRANSITION_SPEC, targetOffsetY = { -it }) + MAIN_FADE_OUT_TRANSITION
    )
}

private val SlideBtt: ContentTransform by unsafeLazy {
    ContentTransform(
        slideInVertically(MAIN_TRANSITION_SPEC, initialOffsetY = { -it }) + MAIN_FADE_IN_TRANSITION,
        slideOutVertically(MAIN_TRANSITION_SPEC, targetOffsetY = { it }) + MAIN_FADE_OUT_TRANSITION
    )
}

@ReadOnlyComposable
@Composable
private fun calculateMainNavigationSuiteType(): TbNavigationSuiteType {
    val uiSettings = LocalUISettings.current
    return TbNavigationSuiteType.fromNavigationSuiteType(
        type = calculateNavigationType(LocalWindowAdaptiveInfo.current),
        floating = uiSettings.bottomNavFloating,
        noLabel = uiSettings.bottomNavLabel == NavigationLabel.NONE
    )
}

private val TbNavigationSuiteType.isNavigationBar
    get() = when (this) {
        TbNavigationSuiteType.FloatingNavigationBar,
        TbNavigationSuiteType.FloatingNavigationBarCompact,
        TbNavigationSuiteType.ShortNavigationBarCompact,
        TbNavigationSuiteType.ShortNavigationBarMedium,
        TbNavigationSuiteType.NavigationBar -> true
        else -> false
    }

@Preview("MainNavigationItems", device = Devices.PIXEL_TABLET)
@Composable
private fun MainNavigationItemsPreview() = TiebaLiteTheme {
    val destinations = listOf(MainDestination.Home, MainDestination.Explore, MainDestination.Notification, MainDestination.User)
    val isSelected: (MainDestination) -> Boolean = { it == MainDestination.Home }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            TbNavigationSuiteType.WideNavigationRailCollapsed,
            TbNavigationSuiteType.WideNavigationRailExpanded,
            TbNavigationSuiteType.NavigationRail,
            TbNavigationSuiteType.NavigationDrawer,
        )
        .forEach { type ->
            TbNavigationSuite(type, verticalArrangement = Arrangement.Center) {
                MainNavigationItems(destinations, isSelected, mainNavigationSuiteType = type)
            }
        }
    }
}

@Preview("MainBottomNavigationItems", device = Devices.PIXEL_9)
@Composable
private fun MainBottomNavigationItemsPreview() = TiebaLiteTheme {
    val destinations = listOf(MainDestination.Home, MainDestination.Explore, MainDestination.Notification, MainDestination.User)
    val isSelected: (MainDestination) -> Boolean = { it == MainDestination.Home }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            TbNavigationSuiteType.FloatingNavigationBar,
            TbNavigationSuiteType.FloatingNavigationBarCompact,
            TbNavigationSuiteType.ShortNavigationBarCompact,
            TbNavigationSuiteType.ShortNavigationBarMedium,
            TbNavigationSuiteType.NavigationBar,
        )
        .forEach { type ->
            TbNavigationSuite(type, verticalArrangement = Arrangement.Center) {
                MainNavigationItems(destinations, isSelected, mainNavigationSuiteType = type)
            }
        }
    }
}
