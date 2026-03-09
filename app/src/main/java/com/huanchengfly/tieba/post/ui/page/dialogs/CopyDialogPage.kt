package com.huanchengfly.tieba.post.ui.page.dialogs

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CopyAll
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarExitDirection
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.theme.compose.onNotNull
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.ExtendedFabHeight
import com.huanchengfly.tieba.post.ui.widgets.compose.PlainTooltipBox
import com.huanchengfly.tieba.post.ui.widgets.compose.ToolbarToFabGap
import com.huanchengfly.tieba.post.utils.TiebaUtil

@Composable
fun CopyTextDialogPage(
    text: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val toolbarScrollBehavior = FloatingToolbarDefaults.exitAlwaysScrollBehavior(
        exitDirection = FloatingToolbarExitDirection.Bottom
    )

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
            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            val colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors()
            val fabElevation = FloatingActionButtonDefaults.loweredElevation()

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = -FloatingToolbarDefaults.ScreenOffset / 2)
                    .zIndex(1f)
                    .onNotNull(toolbarScrollBehavior) {
                        with(it) { floatingScrollBehavior() }
                    },
                horizontalArrangement = Arrangement.spacedBy(ToolbarToFabGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ExtendedFloatingActionButton(
                    text = {
                        Text(text = stringResource(R.string.btn_copy_all))
                    },
                    icon = { Icon(imageVector = Icons.Rounded.CopyAll, contentDescription = null) },
                    onClick = {
                        TiebaUtil.copyText(context, text)
                        onBack()
                    },
                    containerColor = colors.toolbarContainerColor,
                    contentColor = LocalContentColor.current,
                    elevation = fabElevation
                )

                val shareContentDescription = stringResource(R.string.title_share)
                PlainTooltipBox(
                    positionProvider = rememberTooltipPositionProvider(positioning = TooltipAnchorPosition.Above),
                    contentDescription = shareContentDescription,
                ) {
                    FloatingActionButton(
                        modifier = Modifier.size(ExtendedFabHeight),
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, text)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(intent, shareContentDescription)
                            runCatching {
                                context.startActivity(shareIntent)
                            }
                            .onFailure { context.toastShort(text = it.getErrorMessage()) }
                        },
                        containerColor = colors.fabContainerColor,
                        contentColor = colors.fabContentColor,
                        elevation = fabElevation,
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = shareContentDescription)
                    }
                }
            }

            SelectionContainer(
                modifier = Modifier
                    .nestedScroll(connection = scrollBehavior.nestedScrollConnection)
                    .nestedScroll(connection = toolbarScrollBehavior)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    text = text,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}