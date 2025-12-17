package com.huanchengfly.tieba.post.ui.widgets.compose.states

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.safeContent
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.onCase
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.TipScreen

val DefaultLoadingScreen: @Composable StateScreenScope.() -> Unit = {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_loading_paperplane))
    Box(
        modifier = Modifier.requiredWidthIn(max = 500.dp)
    ) {
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f)
        )
    }
}

@Composable
fun StateScreenScope.DefaultEmptyScreen(modifier: Modifier = Modifier, scrollable: Boolean = false) {
    TipScreen(
        title = { Text(text = stringResource(id = R.string.title_empty)) },
        scrollable = scrollable,
        image = {
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_empty_box))
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f)
            )
        },
        actions = {
            if (canReload) {
                FilledTonalButton(
                    onClick = ::reload,
                    content = { Text(text = stringResource(R.string.btn_refresh)) }
                )
            }
        },
        modifier = modifier.fillMaxWidth(),
    )
}

val DefaultErrorScreen: @Composable StateScreenScope.() -> Unit = {
    Text(
        text = stringResource(id = R.string.error_tip),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun StateScreen(
    modifier: Modifier = Modifier,
    isEmpty: Boolean = false,
    isError: Boolean,
    isLoading: Boolean,
    onReload: (() -> Unit)? = null,
    emptyScreen: @Composable StateScreenScope.() -> Unit = { DefaultEmptyScreen() },
    errorScreen: @Composable StateScreenScope.() -> Unit = DefaultErrorScreen,
    loadingScreen: @Composable StateScreenScope.() -> Unit = DefaultLoadingScreen,
    screenPadding: PaddingValues = WindowInsets.safeContent.asPaddingValues(),
    content: @Composable StateScreenScope.() -> Unit,
) {
    val stateScreenScope = remember(key1 = onReload) { StateScreenScope(onReload) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .onCase(isError || isLoading || isEmpty) { padding(screenPadding) },
        contentAlignment = Alignment.Center
    ) {
        if (isError) {
            stateScreenScope.errorScreen()
        } else if (isLoading) {
            stateScreenScope.loadingScreen()
        } else if (isEmpty) {
            stateScreenScope.emptyScreen()
        } else {
            stateScreenScope.content()
        }
    }
}

@NonRestartableComposable
@Composable
fun StateScreen(
    modifier: Modifier = Modifier,
    isEmpty: Boolean = false,
    isLoading: Boolean,
    error: Throwable?,
    onReload: (() -> Unit)? = null,
    emptyScreen: @Composable StateScreenScope.() -> Unit = { DefaultEmptyScreen() },
    errorScreen: @Composable StateScreenScope.() -> Unit = { ErrorScreen(error) },
    loadingScreen: @Composable StateScreenScope.() -> Unit = DefaultLoadingScreen,
    screenPadding: PaddingValues = WindowInsets.safeContent.asPaddingValues(),
    content: @Composable StateScreenScope.() -> Unit,
) =
    StateScreen(
        modifier,
        isEmpty,
        isError = error != null,
        isLoading = isLoading,
        onReload = onReload,
        emptyScreen = emptyScreen,
        errorScreen = errorScreen,
        loadingScreen = loadingScreen,
        screenPadding = screenPadding,
        content = content
    )

class StateScreenScope(
    private val onReload: (() -> Unit)? = null
) {
    val canReload: Boolean
        get() = onReload != null

    fun reload() {
        onReload?.invoke()
    }
}