package com.huanchengfly.tieba.post.ui.page.welcome

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FormatPaint
import androidx.compose.material.icons.rounded.PsychologyAlt
import androidx.compose.material.icons.rounded.SentimentVerySatisfied
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.isFirstPage
import com.huanchengfly.tieba.post.arch.isLastPage
import com.huanchengfly.tieba.post.findActivity
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.models.settings.HabitSettings
import com.huanchengfly.tieba.post.ui.models.settings.UISettings
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.settings.CollectSeeLzPreference
import com.huanchengfly.tieba.post.ui.page.settings.DarkImagePreference
import com.huanchengfly.tieba.post.ui.page.settings.DarkThemeModePreference
import com.huanchengfly.tieba.post.ui.page.settings.DefaultSortPreference
import com.huanchengfly.tieba.post.ui.page.settings.ForumListPreference
import com.huanchengfly.tieba.post.ui.page.settings.HideReplyPreference
import com.huanchengfly.tieba.post.ui.page.settings.ImageLoadPreference
import com.huanchengfly.tieba.post.ui.page.settings.ReduceEffectPreference
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.NegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.PositiveButton
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.PrefsScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.TextPref
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun PagerState.nextPage(scope: CoroutineScope) {
    scope.launch {
        animateScrollToPage(currentPage + 1, animationSpec = tween())
    }
}

@Composable
fun WelcomeScreen(navController: NavController, viewModel: WelcomeViewModel = hiltViewModel()) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val disclaimerDialog = rememberDialogState()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settingsRepo = viewModel.settingsRepository

    val pages = remember {
        listOf(
            R.string.welcome_intro,
            R.string.welcome_permission,
            R.string.welcome_habit,
            R.string.title_settings_custom,
            R.string.welcome_completed,
        )
    }
    val pagerState = rememberPagerState { pages.size }

    fun finishSetup(login: Boolean) {
        navController.navigate(route = if (login) Destination.Login else Destination.Main) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
        viewModel.onSetupFinished()
    }

    // Setup nullable button click listeners
    val proceedBtnEnabled by remember {
        derivedStateOf { state.essentialGranted || pagerState.currentPage < 1 }
    }

    val onProceedClicked: () -> Unit = {
        if (pagerState.isFirstPage && !state.disclaimerConfirmed) {
            disclaimerDialog.show()
        } else if (pagerState.isLastPage) {
            finishSetup(login = true)
        } else {
            pagerState.nextPage(scope)
        }
    }

    val onBackClicked: () -> Unit = {
        scope.launch {
            pagerState.animateScrollToPage(pagerState.currentPage - 1, animationSpec = tween())
        }
    }
    val onBackClickable by remember { derivedStateOf { !pagerState.isFirstPage } }
    val onFinishClicked: () -> Unit = { finishSetup(login = false) }

    MyScaffold(
        bottomBar = {
            BottomBar(
                modifier = Modifier
                    .fillMaxWidth()
                    // Make buttons visually aligned with dual title
                    .padding(22.dp, Dp.Hairline, 30.dp, 30.dp),
                onBack = onBackClicked.takeIf { onBackClickable },
                onFinish = onFinishClicked.takeIf { pagerState.isLastPage },
                onProceed = onProceedClicked.takeIf { proceedBtnEnabled }
            )
        },
    ) { contentPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            beyondViewportPageCount = 1,
            userScrollEnabled = false,
            overscrollEffect = null,
        ) { i ->
            when (pages[i]) {
                R.string.welcome_intro -> IntroPage()

                R.string.welcome_permission -> PermissionPage(
                    settings = settingsRepo.privacySettings,
                    uiState = state,
                ) { it, granted ->
                    viewModel.onPermissionResult(permission = it)
                    if (!granted) context.toastShort(R.string.tip_no_permission)
                }

                R.string.welcome_habit -> HabitPage(habitSettings = settingsRepo.habitSettings)

                R.string.title_settings_custom -> CustomPage(uiSettings = settingsRepo.uiSettings) {
                    navController.navigate(route = Destination.AppTheme)
                }

                R.string.welcome_completed -> CompletePage()
            }
        }
    }

    BackHandler(enabled = onBackClickable, onBack = onBackClicked)

    if (!disclaimerDialog.show) return
    ConfirmDialog(
        dialogState = disclaimerDialog,
        onConfirm = {
            viewModel.onDisclaimerConfirmed()
            scope.launch {
                // Wait dialog close animation
                delay(AnimationConstants.DefaultDurationMillis.toLong())
                pagerState.nextPage(scope)
            }
        },
        onCancel = {
            context.findActivity()?.finish()
        },
        title = {
            Text(text = stringResource(R.string.title_disclaimer))
        }
    ) {
        Text(text = stringResource(R.string.message_disclaimer))
    }
}

@Composable
private fun BottomBar(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)?,
    onFinish: (() -> Unit)?,
    onProceed: (() -> Unit)?
) = Row(
    modifier = modifier.windowInsetsPadding(WindowInsets.navigationBars)
) {
    val none: () -> Unit = { }

    // Back button invisible on fist page
    AnimatedVisibility(visible = onBack != null, enter = fadeIn(), exit = fadeOut()) {
        NegativeButton(text = stringResource(R.string.button_previous), onClick = onBack ?: none)
    }

    Spacer(modifier = Modifier.weight(1.0f))

    // Finish button only visible on last page
    AnimatedVisibility(visible = onFinish != null, enter = fadeIn(), exit = fadeOut()) {
        NegativeButton(text = stringResource(R.string.button_no_login), onClick = onFinish ?: none)
    }

    Spacer(modifier = Modifier.width(8.dp))

    // Proceed button always visible
    PositiveButton(
        text = stringResource(if (onFinish == null) R.string.button_next else R.string.button_login),
        enabled = onProceed != null,
        onClick = onProceed ?: none
    )
}

@Composable
fun DualTitleContent(
    modifier: Modifier = Modifier,
    icon: @Composable BoxScope.() -> Unit,
    @StringRes title: Int,
    @StringRes subtitle: Int,
    content: (@Composable BoxScope.() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.padding(30.dp)
        ) {
            Box(modifier = Modifier.size(size = Sizes.Small), content = icon)

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(subtitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (content != null) {
            Spacer(modifier = Modifier.weight(0.75f))
            Box(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .padding(12.dp, Dp.Hairline, 18.dp, Dp.Hairline), // Visually aligned
                content = content
            )
            Spacer(modifier = Modifier.weight(0.25f))
        }
    }
}

@NonRestartableComposable
@Composable
fun DualTitleContent(
    modifier: Modifier = Modifier,
    icon: Painter,
    @StringRes title: Int,
    @StringRes subtitle: Int,
    content: (@Composable BoxScope.() -> Unit)? = null
) = DualTitleContent(
    modifier = modifier,
    icon = {
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            tint = MaterialTheme.colorScheme.primary
        )
    },
    title = title,
    subtitle = subtitle,
    content = content
)

@NonRestartableComposable
@Composable
private fun IntroPage(modifier: Modifier = Modifier) {
    DualTitleContent(
        modifier = modifier,
        icon = {
            Image(painterResource(R.drawable.ic_splash), null, Modifier.matchParentSize())
        },
        title = R.string.welcome_intro,
        subtitle = R.string.welcome_intro_subtitle
    )
}

@Composable
private fun HabitPage(modifier: Modifier = Modifier, habitSettings: Settings<HabitSettings>) {
    DualTitleContent(
        modifier = modifier,
        icon = rememberVectorPainter(Icons.Rounded.PsychologyAlt),
        title = R.string.welcome_habit,
        subtitle = R.string.welcome_habit_subtitle,
    ) {
        PrefsScreen(
            settings = habitSettings,
            initialValue = HabitSettings(),
        ) {
            DefaultSortPreference()
            CollectSeeLzPreference()
            HideReplyPreference()
            ImageLoadPreference()
        }
    }
}

@Composable
private fun CustomPage(
    modifier: Modifier = Modifier,
    uiSettings: Settings<UISettings>,
    onThemeClicked: () -> Unit
) {
    DualTitleContent(
        icon = rememberVectorPainter(Icons.Rounded.FormatPaint),
        title = R.string.title_settings_custom,
        modifier = modifier,
        subtitle = R.string.welcome_custom_subtitle,
    ) {
        PrefsScreen(
            settings = uiSettings,
            initialValue = UISettings(),
        ) {
            TextPref(
                title = stringResource(id = R.string.title_theme),
                onClick = onThemeClicked,
                leadingIcon = ImageVector.vectorResource(id = R.drawable.ic_brush_24)
            )
            DarkThemeModePreference()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ReduceEffectPreference()
            } else {
                DarkImagePreference()
            }
            ForumListPreference()
        }
    }
}

@Composable
private fun CompletePage(modifier: Modifier = Modifier) {
    DualTitleContent(
        modifier = modifier,
        icon = rememberVectorPainter(Icons.Rounded.SentimentVerySatisfied),
        title = R.string.welcome_completed,
        subtitle = R.string.welcome_completed_subtitle,
    )
}
