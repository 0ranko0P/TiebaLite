package com.huanchengfly.tieba.post.ui.page.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.BottomNavigationDefaults
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.threadBottomBar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.Button
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.TiebaUtil

@Composable
fun CopyTextDialogPage(
    text: String,
    navigator: NavController,
) {
    val context = LocalContext.current

    CopyTextPageContent(
        text = text,
        onCopy = {
            TiebaUtil.copyText(context, it)
        },
        onCancel = navigator::navigateUp
    )
}

@Composable
private fun CopyTextPageContent(
    text: String,
    onCopy: (String) -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier.background(color = ExtendedTheme.colors.windowBackground),
    ) {
        TitleCentredToolbar(
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(id = R.string.menu_copy),
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(id = R.string.tip_copy_text),
                        style = MaterialTheme.typography.caption
                    )
                }
            },
            navigationIcon = {
                BackNavigationIcon(onBackPressed = onCancel)
            }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            SelectionContainer {
                Text(
                    text = text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp), // Content padding
                    style = MaterialTheme.typography.body1
                )
            }
        }

        Surface(
            color = ExtendedTheme.colors.threadBottomBar,
            elevation = BottomNavigationDefaults.Elevation,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onCopy(text)
                        onCancel()
                    }
                ) {
                    Text(text = stringResource(id = R.string.btn_copy_all))
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = ExtendedTheme.colors.text.copy(alpha = 0.1f),
                        contentColor = ExtendedTheme.colors.text
                    )
                ) {
                    Text(text = stringResource(id = R.string.btn_close))
                }
            }
        }
    }
}