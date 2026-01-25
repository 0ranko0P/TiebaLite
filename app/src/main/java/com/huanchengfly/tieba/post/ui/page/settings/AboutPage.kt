package com.huanchengfly.tieba.post.ui.page.settings

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.components.TiebaWebView
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.page.welcome.UaWebView
import com.huanchengfly.tieba.post.ui.widgets.compose.AlertDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.NegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.StrongBox
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.TextPref
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun AboutPage(
    modifier: Modifier = Modifier,
    onBackClicked: () -> Unit = {},
    onDisclaimerClicked: () -> Unit = {},
    onHomePageClicked: () -> Unit = {},
    onLicenseClicked: () -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val icons = remember {
        listOf(
            R.mipmap.ic_launcher_new_round,
            R.mipmap.ic_launcher_new_invert_round,
            R.mipmap.ic_launcher_round,
        )
    }

    val buildTime = remember {
        val buildDate = Date(BuildConfig.BUILD_TIME * 1000)
        // DateTimeFormatter#ISO_INSTANT
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(buildDate)
    }

    MyScaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.title_about))
                },
                navigationIcon = { BackNavigationIcon(onBackPressed = onBackClicked) },
                colors = TopAppBarDefaults.topAppBarColors(),
                scrollBehavior = scrollBehavior
            )
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(contentPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(36.dp))

            StrongBox {
                var iconIndex by rememberSaveable { mutableIntStateOf(0) }
                GlideImage(
                    model = icons[iconIndex],
                    contentDescription = null,
                    modifier = Modifier
                        .size(96.dp)
                        .clickableNoIndication {
                            iconIndex = (iconIndex + 1).takeIf { it in icons.indices } ?: 0 // Loop icons
                        }
                )
            }

            Image(
                painter = painterResource(R.drawable.ic_splash_text),
                contentDescription = null,
                modifier = Modifier
                    .size(240.dp, 96.dp)
                    .offset(y = (-24).dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
            )

            Text(
                text = stringResource(R.string.welcome_intro_subtitle),
                modifier = Modifier.offset(y = (-40).dp),
                style = MaterialTheme.typography.titleMedium
            )

            ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                Column(
                    modifier = Modifier.offset(y = -(20).dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    Text(text = "${BuildConfig.BUILD_TYPE}#${BuildConfig.BUILD_GIT}")
                    Text(text = buildTime)
                }
            }

            HorizontalDivider()
            Container {
                Column {
                    TextPref(
                        title = stringResource(id = R.string.title_disclaimer),
                        onClick = onDisclaimerClicked
                    )
                    TextPref(
                        title = stringResource(id = R.string.about_source_code),
                        onClick = onHomePageClicked,
                    )
                    TextPref(
                        title = stringResource(id = R.string.about_license),
                        summary = "GNU GENERAL PUBLIC LICENSE Version 3",
                        onClick = onLicenseClicked,
                    )
                }
            }
        }
    }
}

@Composable
fun AboutPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val disclaimerDialogState = rememberDialogState()

    fun launchCustomTab(url: String) {
        TiebaWebView.launchCustomTab(context, Uri.parse(url))
    }

    AboutPage(
        onBackClicked = onBack,
        onDisclaimerClicked = disclaimerDialogState::show,
        onHomePageClicked = { launchCustomTab("https://github.com/0ranko0P/TiebaLite") },
        onLicenseClicked = { launchCustomTab("https://github.com/0ranko0P/TiebaLite/blob/main/LICENSE") },
    )

    AlertDialog(
        dialogState = disclaimerDialogState,
        buttons = {
            NegativeButton(text = stringResource(R.string.btn_close)) {
                disclaimerDialogState.show = false
            }
        }
    ) {
        UaWebView(modifier = Modifier.height(480.dp))
    }
}

@Preview("AboutPage", showBackground = true, backgroundColor = -1L)
@Composable
private fun AboutPagePreview() = TiebaLiteTheme {
    AboutPage()
}