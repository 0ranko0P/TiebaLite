package com.huanchengfly.tieba.post.ui.page.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.PositiveButton
import com.huanchengfly.tieba.post.utils.TiebaUtil

@Composable
fun CopyTextDialogPage(
    text: String,
    onBack: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(text = stringResource(id = R.string.menu_copy))
                        Text(
                            text = stringResource(id = R.string.tip_copy_text),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = onBack)
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            val context = LocalContext.current

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .navigationBarsPadding()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PositiveButton(
                    modifier = Modifier.fillMaxWidth(),
                    textRes = R.string.btn_copy_all,
                ) {
                    TiebaUtil.copyText(context, text)
                    onBack()
                }

                FilledTonalButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                    content = {
                        Text(text = stringResource(id = R.string.btn_close))
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            SelectionContainer(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = text,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}