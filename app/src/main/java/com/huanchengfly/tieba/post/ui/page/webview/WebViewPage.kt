package com.huanchengfly.tieba.post.ui.page.webview

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.WebSettings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.components.TbWebChromeClient
import com.huanchengfly.tieba.post.components.TbWebViewClient
import com.huanchengfly.tieba.post.components.TiebaWebView
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.theme.createTopAppBarColors
import com.huanchengfly.tieba.post.theme.isTranslucent
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadingState
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Toolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.WebView
import com.huanchengfly.tieba.post.ui.widgets.compose.WebViewState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberSaveableWebViewState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberWebViewNavigator
import com.huanchengfly.tieba.post.utils.TiebaUtil

private val topAppBarColorsHighContrast: TopAppBarColors
    @Composable @ReadOnlyComposable get() {
        val colorSchemeExt = TiebaLiteTheme.extendedColorScheme
        // Make TopAppBar non-translucent
        return if (colorSchemeExt.colorScheme.isTranslucent) {
            val colorScheme = colorSchemeExt.colorScheme
            colorScheme.createTopAppBarColors(
                containerColor = colorScheme.surfaceContainerLowest,
                scrolledContainerColor = colorScheme.surfaceContainerLowest
            )
        } else {
            colorSchemeExt.appBarColors
        }
    }

@Composable
fun WebViewProgressIndicator(modifier: Modifier =  Modifier, webViewState: WebViewState) {
    val isLoading by remember {
        derivedStateOf { webViewState.loadingState is LoadingState.Loading }
    }

    if (isLoading) {
        val animatedProgress by animateFloatAsState(
            targetValue = (webViewState.loadingState as? LoadingState.Loading)?.progress ?: 0f,
            label = "progress"
        )

        LinearProgressIndicator(
            progress = { animatedProgress },
            gapSize = Dp.Hairline,
            strokeCap = StrokeCap.Square,
            drawStopIndicator = {},
            modifier = modifier.fillMaxWidth()
        )
    }
}

@Composable
fun WebviewTopAppBar(
    modifier: Modifier = Modifier,
    state: WebViewState,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val currentHost by remember {
        derivedStateOf { state.lastLoadedUrl?.toUri()?.host.orEmpty().lowercase() }
    }
    val isExternalHost by remember {
        derivedStateOf { currentHost.isNotEmpty() && !isInternalHost(currentHost) }
    }

    WebviewTopAppBar(
        modifier = modifier,
        pageTitle = state.pageTitle ?: stringResource(R.string.title_default),
        externalHost = currentHost.takeIf { isExternalHost },
        onBack = onBack,
        actions = actions,
        progressBar = { WebViewProgressIndicator(webViewState = state) }
    )
}

@NonRestartableComposable
@Composable
private fun WebviewTopAppBar(
    modifier: Modifier = Modifier,
    pageTitle: String,
    externalHost: String?,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    progressBar: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Toolbar(
        modifier = modifier,
        title = {
            Column {
                Text(text = pageTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)

                if (externalHost != null) {
                    Text(
                        text = externalHost,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        navigationIcon = { BackNavigationIcon(onBackPressed = onBack) },
        actions = actions,
        colors = topAppBarColorsHighContrast,
        content = progressBar
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewPage(initialUrl: String, navigator: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val webViewState = rememberSaveableWebViewState()
    val webViewNavigator = rememberWebViewNavigator()
    var loaded by rememberSaveable {
        mutableStateOf(false)
    }

    LazyLoad(loaded = loaded) {
        webViewNavigator.loadUrl(initialUrl)
        loaded = true
    }

    MyScaffold(
        topBar = {
            WebviewTopAppBar(state = webViewState, onBack = navigator::navigateUp) {
                val url = webViewState.webView?.url ?: return@WebviewTopAppBar
                ClickMenu(
                    menuContent = {
                        TextMenuItem(text = R.string.title_copy_link) {
                            TiebaUtil.copyText(context, url)
                        }

                        TextMenuItem(text = R.string.title_open_in_browser) {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                            }
                        }

                        TextMenuItem(text = R.string.title_refresh, onClick = webViewNavigator::reload)
                    },
                    triggerShape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(id = R.string.btn_more),
                        modifier = Modifier.minimumInteractiveComponentSize()
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.padding(paddingValues),
        ) {
            WebView(
                state = webViewState,
                modifier = Modifier.fillMaxSize(),
                navigator = webViewNavigator,
                onCreated = {
                    it.settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                    }
                },
                onDispose = TiebaWebView::dispose,
                client = remember {
                    TbWebViewClient(context, coroutineScope) { route ->
                        if ((webViewState.webView as TiebaWebView).canNavigate(route)) {
                            navigator.navigate(route = route)
                        }
                    }
                },
                chromeClient = remember { TbWebChromeClient(context, coroutineScope) },
                factory = {
                    TiebaWebView(it.applicationContext)
                }
            )
        }
    }
}

fun isTiebaHost(host: String): Boolean {
    return host == "wapp.baidu.com" ||
            host.contains("tieba.baidu.com") ||
            host == "tiebac.baidu.com"
}

fun isInternalHost(host: String): Boolean {
    return isTiebaHost(host) ||
            host.contains("wappass.baidu.com") ||
            host.contains("ufosdk.baidu.com") ||
            host.contains("m.help.baidu.com")
}
