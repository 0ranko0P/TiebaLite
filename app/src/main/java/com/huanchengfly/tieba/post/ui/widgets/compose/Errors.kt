package com.huanchengfly.tieba.post.ui.widgets.compose

import android.content.Context
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.retrofit.exception.NoConnectivityException
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaApiException
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorCode
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.LocalWindowSizeClass
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.WindowWidthSizeClass
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreenScope
import java.io.ByteArrayOutputStream
import java.io.PrintStream

@Composable
fun TipScreen(
    title: @Composable (ColumnScope.() -> Unit),
    modifier: Modifier = Modifier,
    image: @Composable (ColumnScope.() -> Unit) = {},
    message: @Composable (ColumnScope.() -> Unit) = {},
    actions: @Composable (ColumnScope.() -> Unit) = {},
    scrollable: Boolean = true,
) {
    val scrollableModifier =
        if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier
    val widthFraction =
        if (LocalWindowSizeClass.current.widthSizeClass == WindowWidthSizeClass.Compact) 0.9f else 0.5f
    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(fraction = widthFraction)
                .padding(16.dp)
                .then(scrollableModifier),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.CenterVertically)
        ) {
            Box(modifier = Modifier.requiredWidthIn(max = 400.dp)) {
                this@Column.image()
            }
            ProvideTextStyle(
                value = MaterialTheme.typography.h6.copy(
                    color = ExtendedTheme.colors.text,
                    fontWeight = FontWeight.Bold
                )
            ) {
                title()
            }
            ProvideTextStyle(
                value = MaterialTheme.typography.body1.copy(
                    color = ExtendedTheme.colors.textSecondary,
                )
            ) {
                message()
            }
            actions()
        }
    }
}

data class ErrorType(
    @StringRes val title: Int,
    val message: String,
    @RawRes val lottieResId: Int,
)

@NonRestartableComposable
@Composable
fun StateScreenScope.ErrorScreen(
    error: Throwable?,
    modifier: Modifier = Modifier,
    showReload: Boolean = true,
    actions: @Composable (ColumnScope.() -> Unit) = {},
) {
    ErrorTipScreen(
        error = error,
        modifier = modifier,
        actions = {
            if (showReload && canReload) {
                Button(onClick = { reload() }) {
                    Text(text = stringResource(id = R.string.btn_reload))
                }
            }
            actions()
        }
    )
}

@Composable
fun ErrorTipScreen(
    modifier: Modifier = Modifier,
    error: Throwable?,
    actions: @Composable (ColumnScope.() -> Unit) = {},
) {
    val context = LocalContext.current
    val errorType = remember { toKnownErrorType(context, error) }
    // Is unknown error, show stack trace
    if (errorType == null) {
        ErrorStackTraceScreen(modifier, error!!, actions)
        return
    }

    TipScreen(
        title = {
            Text(text = stringResource(id = errorType.title))
        },
        modifier = modifier,
        image = {
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(errorType.lottieResId))
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f)
            )
        },
        message = {
            Text(text = errorType.message)
        },
        actions = actions
    )
}

@Composable
fun ErrorStackTraceScreen(
    modifier: Modifier = Modifier,
    throwable: Throwable,
    actions: @Composable (ColumnScope.() -> Unit) = {}
) {
    val stackTrace = remember { runCatching {
        val bos = ByteArrayOutputStream()
        PrintStream(bos).use { out ->
            throwable.printStackTrace(out)
            bos.toString()
        }
    } }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.CenterVertically)
    ) {
        Text(
            text = stringResource(id = R.string.title_unknown_error),
            style = MaterialTheme.typography.h6
        )

        Text(
            text = stackTrace.getOrDefault(stringResource(R.string.message_unknown_error)),
            modifier = Modifier
                .weight(1.0f)
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState()),
            color = ExtendedTheme.colors.textSecondary,
            softWrap = false,
            style = MaterialTheme.typography.body1
        )

        actions()
    }
}

// Returns null if error is unknown type
private fun toKnownErrorType(context: Context, err: Throwable?): ErrorType? {
    return when (err) {
        null -> ErrorType(R.string.title_unknown_error, context.getString(R.string.message_unknown_error), R.raw.lottie_bug_hunting)

        is NoConnectivityException -> ErrorType(
            title = R.string.title_no_internet_connectivity,
            message = context.getString(R.string.message_no_internet_connectivity, err.getErrorMessage()),
            lottieResId = R.raw.lottie_no_internet
        )

        is TiebaApiException -> ErrorType(
            title = R.string.title_api_error,
            message = "${err.getErrorMessage()} Code: ${err.getErrorCode()}",
            lottieResId = R.raw.lottie_error
        )

        is TiebaNotLoggedInException -> ErrorType(
            title = R.string.title_not_logged_in,
            message = context.getString(R.string.message_not_logged_in),
            lottieResId = R.raw.lottie_astronaut
        )

        else -> null // Unknown Type
    }
}