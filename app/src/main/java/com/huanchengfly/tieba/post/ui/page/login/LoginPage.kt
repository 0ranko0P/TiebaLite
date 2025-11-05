package com.huanchengfly.tieba.post.ui.page.login

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.components.TbWebViewClient
import com.huanchengfly.tieba.post.components.TiebaWebView
import com.huanchengfly.tieba.post.components.TiebaWebView.Companion.dispose
import com.huanchengfly.tieba.post.ui.page.webview.WebviewTopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.WebView
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberSaveableWebViewState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberWebViewNavigator
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.AccountUtil.Companion.parseCookie
import com.huanchengfly.tieba.post.utils.ClientUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val LOGIN_URL =
    "https://wappass.baidu.com/passport?login&u=https%3A%2F%2Ftieba.baidu.com%2Findex%2Ftbwise%2Fmine"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginPage(navigator: NavController, onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val webViewState = rememberSaveableWebViewState()
    val webViewNavigator = rememberWebViewNavigator()
    var loaded by rememberSaveable {
        mutableStateOf(false)
    }

    LazyLoad(loaded = loaded) {
        webViewNavigator.loadUrl(LOGIN_URL)
        loaded = true
    }

    MyScaffold(
        topBar = {
            WebviewTopAppBar(state = webViewState, onBack = onBack) {
                ClickMenu(
                    menuContent = {
                        TextMenuItem(text = R.string.title_refresh, onClick = webViewNavigator::reload)
                    },
                    triggerShape = CircleShape
                ) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(id = R.string.btn_more)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box {
            val snackbarHostState = LocalSnackbarHostState.current
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
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                    }
                },
                onDispose = TiebaWebView::dispose,
                client = remember(navigator) {
                    LoginWebViewClient(
                        context,
                        coroutineScope,
                        onToast = { message, duration ->
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(message, duration = duration)
                        },
                        onBack
                    )
                },
            )
        }
    }
}

private class LoginWebViewClient(
    context: Context,
    val coroutineScope: CoroutineScope,
    val onToast: suspend (String, SnackbarDuration) -> Unit,
    val onLoggedIn: () -> Unit
) : TbWebViewClient(context, coroutineScope, onNavigate = null) {
    private var isLoadingAccount = false

    override fun injectCookies(url: String) {}

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        if (url == null) {
            return
        }
        if (isLoadingAccount) {
            return
        }
        val cookieStr = CookieManager.getInstance().getCookie(url) ?: return
        val cookies = parseCookie(cookieStr).mapKeys { it.key.uppercase() }
        val bduss = cookies["BDUSS"]
        val sToken = cookies["STOKEN"]
        val baiduId = cookies["BAIDUID"]
        if (url.startsWith("https://tieba.baidu.com/index/tbwise/") || url.startsWith("https://tiebac.baidu.com/index/tbwise/")) {
            if (bduss == null || sToken == null) {
                return
            }
            if (ClientUtils.baiduId.isNullOrEmpty()) {
                ClientUtils.saveBaiduId(baiduId)
            }
            coroutineScope.launch {
                onToast(context.getString(R.string.text_please_wait), SnackbarDuration.Indefinite)
                val accountUtil = AccountUtil.getInstance()
                try {
                    val account = accountUtil.fetchAccount(bduss, sToken, cookieStr)
                    isLoadingAccount = false
                    accountUtil.saveNewAccount(context, account)
                    onToast(context.getString(R.string.text_login_success), SnackbarDuration.Short)
                    delay(1000)
                    onLoggedIn()
                } catch(e: Throwable) {
                    val error = context.getString(R.string.text_login_failed, e.getErrorMessage())
                    onToast(error, SnackbarDuration.Short)
                    navigator.loadUrl(LOGIN_URL)
                } finally {
                    isLoadingAccount = false
                }
            }
        }
    }
}