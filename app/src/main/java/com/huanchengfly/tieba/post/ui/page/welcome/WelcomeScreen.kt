package com.huanchengfly.tieba.post.ui.page.welcome

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FormatPaint
import androidx.compose.material.icons.rounded.PsychologyAlt
import androidx.compose.material.icons.rounded.SentimentVerySatisfied
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.findActivity
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.TextPref
import com.huanchengfly.tieba.post.ui.common.theme.compose.LocalExtendedColors
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.settings.CollectSeeLzPreference
import com.huanchengfly.tieba.post.ui.page.settings.DarkThemeModePreference
import com.huanchengfly.tieba.post.ui.page.settings.DarkThemePreference
import com.huanchengfly.tieba.post.ui.page.settings.DefaultSortPreference
import com.huanchengfly.tieba.post.ui.page.settings.ForumListPreference
import com.huanchengfly.tieba.post.ui.page.settings.HideReplyPreference
import com.huanchengfly.tieba.post.ui.page.settings.ImageLoadPreference
import com.huanchengfly.tieba.post.ui.page.welcome.PagerOffset.Companion.LocalPagerOffset
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.NegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.PositiveButton
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils.Companion.KEY_SETUP_FINISHED
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private fun PagerState.nextPage(scope: CoroutineScope) {
    scope.launch {
        animateScrollToPage(currentPage + 1, animationSpec = tween(easing = LinearOutSlowInEasing))
    }
}

private fun PagerState.previousPage(scope: CoroutineScope) {
    scope.launch {
        animateScrollToPage(currentPage - 1, animationSpec = tween(easing = LinearOutSlowInEasing))
    }
}

// Pager offset animation helper class
private class PagerOffset(val state: PagerState, val page: Int) {

    fun offsetX(fraction: Float = 1.0f, easing: Easing = FastOutSlowInEasing): Modifier =
        Modifier.graphicsLayer {
            val offset = easing.transform(abs(calculateCurrentOffsetForPage()))
            translationX = lerp(0f, size.width * fraction, offset)
            if (offset >= 0.9f) {
                alpha = 1 - lerp(1f, 0f, (1 - offset) * 10)
            }
        }

    private fun calculateCurrentOffsetForPage(): Float {
        return (state.currentPage - page) + state.currentPageOffsetFraction
    }

    companion object {
        val LocalPagerOffset = staticCompositionLocalOf<PagerOffset> { error("No PagerOffset provided") }
    }
}

private fun NavController.finishSetup(context: Context, scope: CoroutineScope, login: Boolean) {
    this.navigate(route = if (login) Destination.Login else Destination.Main) {
        popUpTo(graph.id) { inclusive = true }
    }
    scope.launch {
        context.dataStore.edit { it[booleanPreferencesKey(KEY_SETUP_FINISHED)] = true }
    }
}

@Composable
fun WelcomeScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val disclaimerDialog = rememberDialogState()
    var disclaimerConfirmed by rememberSaveable { mutableStateOf(false) }

    val pages = remember {
        listOfNotNull(
            R.string.welcome_intro,
            R.string.welcome_permission.takeIf { shouldRequestPermissions(context) },
            R.string.welcome_habit,
            R.string.title_settings_custom,
            R.string.welcome_completed,
        ).toImmutableList()
    }
    val pagerState = rememberPagerState { pages.size }
    val isFirstPage by remember { derivedStateOf { pagerState.currentPage == 0 } }
    val isLastPage by remember { derivedStateOf { pagerState.currentPage == pagerState.pageCount - 1 } }

    // Setup button click listeners
    var proceedBtnEnabled by remember { mutableStateOf(true) }
    val onProceedClicked: () -> Unit = remember { {
        if (isFirstPage && !disclaimerConfirmed) {
            disclaimerDialog.show()
        } else if (isLastPage) {
            navController.finishSetup(context, scope, login = true)
        } else {
            pagerState.nextPage(scope)
        }
    } }

    val onBackClicked: () -> Unit = remember { {
        proceedBtnEnabled = true
        pagerState.previousPage(scope)
    } }

    val onFinishClicked: () -> Unit = remember { {
        navController.finishSetup(context, scope, login = false)
    } }

    Scaffold (
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        bottomBar = {
            BottomBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 30.dp, top = 8.dp, end = 30.dp, bottom = 30.dp),
                onBack = onBackClicked.takeUnless { isFirstPage },
                onFinish = onFinishClicked.takeIf { isLastPage },
                onProceed = onProceedClicked.takeIf { proceedBtnEnabled }
            )
        }
    ) { contentPadding ->
        HorizontalPager(
            modifier = Modifier.fillMaxWidth(),
            state = pagerState,
            contentPadding = contentPadding,
            userScrollEnabled = false
        ) { i ->
            CompositionLocalProvider(
                LocalPagerOffset provides PagerOffset(state = pagerState, page = i)
            ) {
                Box(modifier = Modifier.padding(30.dp)) {
                    when (pages[i]) {
                        R.string.welcome_intro -> IntroPage()

                        R.string.welcome_permission -> PermissionPage { enabled ->
                            proceedBtnEnabled = enabled
                        }

                        R.string.welcome_habit -> HabitPage()

                        R.string.title_settings_custom -> CustomPage {
                            navController.navigate(route = Destination.AppTheme)
                        }

                        R.string.welcome_completed -> CompletePage()
                    }
                }
            }
        }
    }

    ConfirmDialog(
        dialogState = disclaimerDialog,
        onConfirm = {
            scope.launch {
                disclaimerConfirmed = true
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
    icon: Painter? = null,
    @StringRes title: Int,
    @StringRes subtitle: Int,
    content: (@Composable ColumnScope.() -> Unit)? = null
) = Column(
    modifier = modifier.fillMaxSize(),
) {
    val pagerOffset = LocalPagerOffset.current
    val colors = LocalExtendedColors.current

    if (icon != null) {
        Icon(icon, null, modifier = pagerOffset.offsetX().size(36.dp), tint = colors.primary)
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text(text = stringResource(title), fontSize = 24.sp, fontWeight = FontWeight.Bold)

    Spacer(modifier = Modifier.height(4.dp))
    Text(text = stringResource(subtitle), color = colors.textSecondary, fontSize = 16.sp)

    if (content != null) {
        Spacer(modifier = Modifier.height(16.dp))

        Column (modifier = pagerOffset.offsetX(0.2f, easing = LinearOutSlowInEasing)) {
            content()
        }
    }
}

@NonRestartableComposable
@Composable
private fun IntroPage(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Image(painterResource(R.drawable.ic_splash), null, modifier = Modifier.size(36.dp))

        DualTitleContent(
            icon = null,
            title = R.string.welcome_intro,
            subtitle = R.string.welcome_intro_subtitle
        )
    }
}

@NonRestartableComposable
@Composable
private fun HabitPage(modifier: Modifier = Modifier) {
    DualTitleContent(
        modifier = modifier,
        icon = rememberVectorPainter(Icons.Rounded.PsychologyAlt),
        title = R.string.welcome_habit,
        subtitle = R.string.welcome_habit_subtitle,
    ) {
        HideReplyPreference()
        ImageLoadPreference()
        DefaultSortPreference()
        CollectSeeLzPreference()
    }
}

@NonRestartableComposable
@Composable
private fun CustomPage(modifier: Modifier = Modifier, onThemeClicked: () -> Unit) {
    DualTitleContent(
        icon = rememberVectorPainter(Icons.Rounded.FormatPaint),
        title = R.string.title_settings_custom,
        modifier = modifier,
        subtitle = R.string.welcome_custom_subtitle,
    ) {
        TextPref(
            title = stringResource(id = R.string.title_theme),
            onClick = onThemeClicked,
            leadingIcon = ImageVector.vectorResource(id = R.drawable.ic_brush_24)
        )

        DarkThemeModePreference()
        DarkThemePreference()
        ForumListPreference()
    }
}

@NonRestartableComposable
@Composable
fun CompletePage(modifier: Modifier = Modifier) {
    DualTitleContent(
        modifier = modifier,
        icon = rememberVectorPainter(Icons.Rounded.SentimentVerySatisfied),
        title = R.string.welcome_completed,
        subtitle = R.string.welcome_completed_subtitle,
    )
}
