package com.huanchengfly.tieba.post.ui.page.webview

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.location.LocationManager
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.appcompat.app.AlertDialog
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
import androidx.core.content.getSystemService
import androidx.core.location.LocationManagerCompat
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.arch.unsafeLazy
import com.huanchengfly.tieba.post.components.ClipBoardLinkDetector.isBaidu
import com.huanchengfly.tieba.post.components.TiebaWebView
import com.huanchengfly.tieba.post.components.dialogs.PermissionDialog
import com.huanchengfly.tieba.post.models.PermissionBean
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.widgets.compose.AccompanistWebChromeClient
import com.huanchengfly.tieba.post.ui.widgets.compose.AccompanistWebViewClient
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
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.FileUtil
import com.huanchengfly.tieba.post.utils.PermissionUtils.askPermission
import com.huanchengfly.tieba.post.utils.PermissionUtils.onDenied
import com.huanchengfly.tieba.post.utils.PermissionUtils.onGranted
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.compose.launchActivityForResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

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
                    MyWebViewClient { route ->
                        if ((webViewState.webView as TiebaWebView).canNavigate(route)) {
                            navigator.navigate(route = route)
                        }
                    }
               },
                chromeClient = remember { MyWebChromeClient(context, coroutineScope) },
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

open class MyWebViewClient(
    private val onNavigate: ((Destination) -> Unit)? = null
) : AccompanistWebViewClient() {
    val context: Context
        get() = state.webView?.context ?: App.INSTANCE

    private val clipboardGuardJs by unsafeLazy {
        FileUtil.readAssetFile(context, "ClipboardGuard.js") ?: throw IOException()
    }

    private val cookieManager by unsafeLazy { CookieManager.getInstance() }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        if (url?.startsWith("http") == true) {
            view.evaluateJavascript(clipboardGuardJs, null)
        }
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        return TiebaWebView.interceptRequest(context, request.url, this::onLaunchApp, onNavigate)
    }

    // Show Confirm dialog when web request invokes a DeepLink
    private fun onLaunchApp(uri: Uri, name: String) {
        val scheme = uri.scheme ?: ""
        PermissionDialog(
            context,
            PermissionBean(
                PermissionDialog.CustomPermission.PERMISSION_START_APP,
                "${uri.host}_${scheme}",
                context.getString(R.string.title_start_app_permission, uri.host, name),
                R.drawable.ic_round_exit_to_app
            )
        )
            .setOnGrantedCallback {
                val intent = if (uri.scheme.equals("intent", ignoreCase = true)) {
                    Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
                        .addCategory(Intent.CATEGORY_BROWSABLE)
                } else {
                    Intent(Intent.ACTION_VIEW, uri)
                }
                runCatching {
                    context.startActivity(intent)
                }
            }
            .show()
    }

    open fun injectCookies(url: String) {
        val cookieStr = cookieManager.getCookie(url) ?: ""
        val cookies = AccountUtil.parseCookie(cookieStr)
        val BDUSS = cookies["BDUSS"]
        val currentAccountBDUSS = AccountUtil.getBduss()
        if (currentAccountBDUSS != null && BDUSS != currentAccountBDUSS) {
            cookieManager.setCookie(url, AccountUtil.getBdussCookie(currentAccountBDUSS))
        }
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        if (url?.toUri()?.isBaidu() == true) {
            injectCookies(url = url)
        }
    }
}

class MyWebChromeClient(
    val context: Context,
    coroutineScope: CoroutineScope,
) : AccompanistWebChromeClient() {

    private var uploadMessage: ValueCallback<Array<Uri>>? = null

    val id: String = UUID.randomUUID().toString()

    init {
        coroutineScope.onGlobalEvent<GlobalEvent.ActivityResult>(
            filter = { it.requesterId == id },
        ) {
            uploadMessage?.onReceiveValue(FileChooserParams.parseResult(it.resultCode, it.intent))
            uploadMessage = null
        }
    }

    private fun isEnabledLocationFunction(): Boolean {
        val locationManager = context.getSystemService<LocationManager>()
        return locationManager != null && LocationManagerCompat.isLocationEnabled(locationManager)
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?,
    ) {
        if (origin == null || callback == null) return
        PermissionDialog(
            context,
            PermissionBean(
                PermissionDialog.CustomPermission.PERMISSION_LOCATION,
                origin,
                context.getString(R.string.title_ask_permission_location, origin),
                R.drawable.ic_round_location_on
            )
        )
            .setOnGrantedCallback { isForever: Boolean ->
                MainScope().launch {
                    context.askPermission(
                        desc = R.string.usage_webview_location_permission,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    .onGranted {
                        if (isEnabledLocationFunction()) {
                            callback.invoke(origin, true, isForever)
                        } else {
                            callback.invoke(origin, false, false)
                        }
                    }
                    .onDenied {
                        context.toastShort(R.string.tip_no_permission)
                        callback.invoke(origin, false, false)
                    }
                }
            }
            .setOnDeniedCallback {
                callback.invoke(origin, false, false)
            }
            .show()
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?,
    ): Boolean {
        if (webView == null || filePathCallback == null || fileChooserParams == null) return false
        uploadMessage?.onReceiveValue(null)
        uploadMessage = filePathCallback
        launchActivityForResult(id, fileChooserParams.createIntent())
        return true
    }

    override fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?,
    ): Boolean {
        AlertDialog.Builder(view?.context ?: context)
            .setMessage(message)
            .setPositiveButton(R.string.button_sure_default) { _, _ ->
                result?.confirm()
            }
            .setCancelable(false)
            .create()
            .show()
        return true
    }

    override fun onJsConfirm(
        view: WebView,
        url: String?,
        message: String?,
        result: JsResult,
    ): Boolean {
        if ("ClipboardGuardCopyRequest".equals(message, ignoreCase = true)) {
            val host = Uri.parse(url).host ?: return true
            PermissionDialog(
                context,
                PermissionBean(
                    PermissionDialog.CustomPermission.PERMISSION_CLIPBOARD_COPY,
                    host,
                    context.getString(R.string.title_ask_permission_clipboard_copy, host),
                    R.drawable.ic_round_file_copy
                )
            )
            .setOnGrantedCallback { result.confirm() }
            .setOnDeniedCallback { result.cancel() }
            .show()
        } else {
            AlertDialog.Builder(view.context)
                .setTitle("Confirm")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    result.confirm()
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    result.cancel()
                }
                .create()
                .show()
        }
        return true
    }
}