package com.huanchengfly.tieba.post.ui.page.settings

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun AboutPage(
    modifier: Modifier = Modifier,
    onBackClicked: () -> Unit = {},
    onHomePageClicked: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier.windowInsetsPadding(WindowInsets.navigationBars),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TitleCentredToolbar(
            title = {
                Text(text = stringResource(id = R.string.title_about))
            },
            navigationIcon = { BackNavigationIcon(onBackPressed = onBackClicked) },
            colors = TopAppBarDefaults.topAppBarColors()
        )

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            GlideImage(
                model = R.mipmap.ic_launcher_new,
                contentDescription = null,
                modifier = Modifier.size(100.dp)
            )
        }

        TextButton(
            shape = CircleShape,
            colors = ButtonDefaults.textButtonColors(
                containerColor = colorScheme.primaryContainer.copy(alpha = 0.45f),
                contentColor = colorScheme.onPrimaryContainer
            ),
            onClick = onHomePageClicked,
            modifier = Modifier.fillMaxWidth(0.5f)
        ) {
            Text(stringResource(id = R.string.source_code))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = R.string.tip_about, BuildConfig.VERSION_NAME, BuildConfig.BUILD_TYPE),
            style = MaterialTheme.typography.labelMedium,
            color = colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = BuildConfig.BUILD_GIT,
            style = MaterialTheme.typography.labelMedium,
            color = colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun AboutPage(onBack: () -> Unit) {
    val context = LocalContext.current

    AboutPage(
        onBackClicked = onBack,
        onHomePageClicked = {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/HuanCheng65/TiebaLite"))
            )
        }
    )
}

@Preview("AboutPage", showBackground = true, backgroundColor = Color.WHITE.toLong())
@Composable
private fun AboutPagePreview() = TiebaLiteTheme {
    AboutPage()
}