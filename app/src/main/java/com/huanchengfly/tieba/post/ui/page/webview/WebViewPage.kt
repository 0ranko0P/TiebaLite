package com.huanchengfly.tieba.post.ui.page.webview

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.WebSettings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
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
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadingState
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Toolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.WebView
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberSaveableWebViewState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberWebViewNavigator
import com.huanchengfly.tieba.post.utils.TiebaUtil
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

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
    var pageTitle by rememberSaveable {
        mutableStateOf("")
    }
    val displayPageTitle by remember {
        derivedStateOf {
            pageTitle.ifEmpty {
                context.getString(R.string.title_default)
            }
        }
    }
    val currentHost by remember {
        derivedStateOf {
            webViewState.lastLoadedUrl?.toUri()?.host.orEmpty().lowercase()
        }
    }
    val isExternalHost by remember {
        derivedStateOf {
            currentHost.isNotEmpty() && !isInternalHost(currentHost)
        }
    }

    DisposableEffect(Unit) {
        val job = coroutineScope.launch {
            snapshotFlow { webViewState.pageTitle }
                .filterNotNull()
                .filter { it.isNotEmpty() }
                .cancellable()
                .collect {
                    pageTitle = it
                }
        }
        onDispose {
            job.cancel()
        }
    }

    LazyLoad(loaded = loaded) {
        webViewNavigator.loadUrl(initialUrl)
        loaded = true
    }

    val isLoading by remember {
        derivedStateOf {
            webViewState.loadingState is LoadingState.Loading
        }
    }

    val progress by remember {
        derivedStateOf {
            webViewState.loadingState.let { if (it is LoadingState.Loading) it.progress else 0f }
        }
    }

    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    MyScaffold(
        topBar = {
            Toolbar(
                title = {
                    Column {
                        Text(
                            text = displayPageTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isExternalHost) {
                            Text(
                                text = currentHost,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = ExtendedTheme.colors.onTopBar.copy(ContentAlpha.medium),
                                style = MaterialTheme.typography.caption
                            )
                        }
                    }
                },
                navigationIcon = { BackNavigationIcon(onBackPressed = navigator::navigateUp) },
                actions = {
                    val menuState = rememberMenuState()
                    ClickMenu(
                        menuContent = {
                            TextMenuItem(text = R.string.title_copy_link) {
                                webViewState.webView?.url?.let { TiebaUtil.copyText(context, it) }
                            }

                            TextMenuItem(text = R.string.title_open_in_browser) {
                                webViewState.webView?.url?.toUri()?.let {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, it))
                                    }
                                }
                            }

                            TextMenuItem(text = R.string.title_refresh, onClick = webViewNavigator::reload)
                        },
                        menuState = menuState,
                        triggerShape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(id = R.string.btn_more),
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                    }
                },
                elevation = Dp.Hairline
            )
        }
    ) { paddingValues ->
        Box {
            WebView(
                state = webViewState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
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

            if (isLoading) {
                LinearProgressIndicator(
                    progress = animatedProgress,
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
