package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.isWindowWidthCompact
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.LocalAccount
import com.huanchengfly.tieba.post.utils.StringUtil

val AppBarHeight: Dp = 56.dp

val accountNavIconIfCompact: @Composable () -> Unit = {
    if (isWindowWidthCompact()) {
        AccountNavIcon()
    }
}

@Composable
private fun AccountDropdownMenuItem(
    onClick: () -> Unit,
    account: Account,
    currentAccount: Account,
    modifier: Modifier = Modifier,
) {
    DropdownMenuItem(
        text = { Text(text = account.nickname ?: account.name) },
        onClick = onClick,
        modifier = modifier.semantics(mergeDescendants = true) {
            role = Role.DropdownList
            selected = currentAccount.uid == account.uid
            contentDescription = account.nickname ?: account.name
        },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .size(Sizes.Small)
            ) {
                Avatar(
                    data = remember { StringUtil.getAvatarUrl(account.portrait) },
                    size = Sizes.Small
                )
                if (currentAccount.uid == account.uid) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color = Color.Black.copy(0.35f))
                            .padding(8.dp)
                    )
                }
            }
        }
    )
}

@Composable
fun AccountNavIcon(
    onClick: (() -> Unit)? = null,
    spacer: Boolean = true,
    size: Dp = Sizes.Small
) {
    val navigator = LocalNavController.current
    val currentAccount = LocalAccount.current

    if (spacer) Spacer(modifier = Modifier.width(12.dp))
    if (currentAccount == null) {
        Avatar(data = R.drawable.ic_launcher_new_round, size = size)
    } else {
        val menuState = rememberMenuState()
        val addTitleText = stringResource(id = R.string.title_new_account)

        LongClickMenu(
            menuContent = {
                val accountUtil = remember { AccountUtil.getInstance() }
                val accounts by accountUtil.allAccounts.collectAsStateWithLifecycle(emptyList())

                accounts.fastForEach {
                    AccountDropdownMenuItem(
                        onClick = { accountUtil.switchAccount(uid = it.uid) },
                        account = it,
                        currentAccount = currentAccount,
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                DropdownMenuItem(
                    text = { Text(text = addTitleText) },
                    onClick = {
                        navigator.navigate(Destination.Login)
                    },
                    modifier = Modifier.semantics(mergeDescendants = true) {
                        role = Role.DropdownList
                        contentDescription = addTitleText
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.inverseOnSurface,
                            modifier = Modifier
                                .size(Sizes.Small)
                                .background(color = LocalContentColor.current, shape = CircleShape)
                                .padding(8.dp),
                        )
                    }
                )
            },
            menuState = menuState,
            onClick = onClick,
            shape = CircleShape
        ) {
            Avatar(
                data = StringUtil.getAvatarUrl(currentAccount.portrait),
                size = size,
                contentDescription = stringResource(id = R.string.title_switch_account_long_press)
            )
        }
    }
}

@Composable
fun ActionItem(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}

@Composable
fun BackNavigationIcon(onBackPressed: () -> Unit) {
    IconButton(onClick = onBackPressed) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(id = R.string.button_back)
        )
    }
}

@NonRestartableComposable
@Composable
fun TitleCentredToolbar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: (@Composable ColumnScope.() -> Unit)? = null,
) =
    TitleCentredToolbar(
        title = { Text(text = title) },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        content = content
    )

@Composable
fun TitleCentredToolbar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    colors: TopAppBarColors = TiebaLiteTheme.extendedColorScheme.appBarColors,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    TopAppBarContainer(
        modifier = modifier,
        topBar = {
            Box(contentAlignment = Alignment.Center) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    navigationIcon?.invoke()

                    Spacer(modifier = Modifier.weight(1f))

                    actions()
                }

                ProvideTextStyle(MaterialTheme.typography.titleLarge, content = title)
            }
        },
        colors = colors,
        content = content
    )
}

@Composable
fun Toolbar(
    modifier: Modifier = Modifier,
    title: String,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Toolbar(
        modifier = modifier,
        title = {
            Text(text = title)
        },
        navigationIcon = navigationIcon,
        actions = actions,
        content = content
    )
}

@Composable
fun Toolbar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TiebaLiteTheme.extendedColorScheme.appBarColors,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    TopAppBarContainer(
        modifier = modifier,
        topBar = {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.minimumInteractiveComponentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    navigationIcon?.invoke()
                }

                ProvideTextStyle(MaterialTheme.typography.titleLarge) {
                    Box(Modifier.weight(1.0f)) { title() }
                }

                actions()
            }
        },
        colors = colors,
        content = content
    )
}

@Composable
fun TopAppBarContainer(
    modifier: Modifier = Modifier,
    topBar: @Composable BoxScope.() -> Unit,
    colors: TopAppBarColors = TiebaLiteTheme.extendedColorScheme.appBarColors,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.containerColor,
        contentColor = colors.titleContentColor,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = TopAppBarHorizontalPadding)
                    .height(AppBarHeight),
                content = topBar
            )

            content?.invoke(this)
        }
    }
}