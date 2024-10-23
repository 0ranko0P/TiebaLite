package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.core.content.ContextCompat
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.LocalWindowSizeClass
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.LocalExtendedColors
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.WindowWidthSizeClass.Companion.Compact
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.LocalAccount
import com.huanchengfly.tieba.post.utils.LocalAllAccounts
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.ThemeUtil

val AppBarHeight: Dp = 56.dp

@Composable
fun accountNavIconIfCompact(): (@Composable () -> Unit)? =
    if (LocalWindowSizeClass.current.widthSizeClass == Compact) (@Composable { AccountNavIcon() })
    else null

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
        Image(
            painter = rememberDrawablePainter(
                drawable = ContextCompat.getDrawable(
                    LocalContext.current,
                    R.drawable.ic_launcher_new_round
                )
            ),
            contentDescription = null,
            modifier = Modifier
                .clip(CircleShape)
                .size(size)
        )
    } else {
        val context = LocalContext.current
        val menuState = rememberMenuState()
        LongClickMenu(
            menuContent = {
                val allAccounts = LocalAllAccounts.current
                allAccounts.fastForEach {
                    DropdownMenuItem(onClick = { AccountUtil.getInstance().switchAccount(it.id) }) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .size(Sizes.Small)
                        ) {
                            Avatar(
                                data = StringUtil.getAvatarUrl(it.portrait),
                                contentDescription = stringResource(id = R.string.title_switch_account_long_press),
                                size = Sizes.Small,
                            )
                            if (currentAccount.id == it.id) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = stringResource(id = R.string.desc_current_account),
                                    tint = Color.White,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color = Color.Black.copy(0.35f))
                                        .padding(8.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = it.nameShow ?: it.name)
                    }
                }
                VerticalDivider(
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                DropdownMenuItem(
                    onClick = {
                        navigator.navigate(Destination.Login)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(id = R.string.title_new_account),
                        tint = ExtendedTheme.colors.onChip,
                        modifier = Modifier
                            .size(Sizes.Small)
                            .clip(CircleShape)
                            .background(color = ExtendedTheme.colors.chip)
                            .padding(8.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = stringResource(id = R.string.title_new_account))
                }
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

@ReadOnlyComposable
@Composable
private fun defaultAppBarElevation(): Dp {
    // No Elevation shadow on TranslucentTheme
    return if (ThemeUtil.isTranslucentTheme(LocalExtendedColors.current)) {
        Dp.Hairline
    } else {
        AppBarDefaults.TopAppBarElevation
    }
}

@Composable
fun TitleCentredToolbar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    elevation: Dp = defaultAppBarElevation(),
    content: (@Composable ColumnScope.() -> Unit)? = null,
) = TitleCentredToolbar(
    title = { Text(text = title) },
    modifier = modifier,
    elevation = elevation,
    navigationIcon = navigationIcon,
    actions = actions,
    content = content
)

@Composable
fun TitleCentredToolbar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = ExtendedTheme.colors.topBar,
    contentColor: Color = ExtendedTheme.colors.onTopBar,
    elevation: Dp = defaultAppBarElevation(),
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    TopAppBarContainer(
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

                ProvideTextStyle(
                    value = MaterialTheme.typography.h6.copy(
                        color = LocalContentColor.current,
                        fontWeight = FontWeight.Bold
                    ),
                    content = title
                )
            }
        },
        modifier = modifier,
        color = color,
        contentColor = contentColor,
        elevation = elevation,
        content = content
    )
}

@Composable
fun Toolbar(
    title: String,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    elevation: Dp = defaultAppBarElevation(),
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Toolbar(
        title = {
            Text(text = title)
        },
        navigationIcon = navigationIcon,
        actions = actions,
        elevation = elevation,
        content = content
    )
}

@Composable
fun Toolbar(
    title: @Composable () -> Unit,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    backgroundColor: Color = ExtendedTheme.colors.topBar,
    contentColor: Color = ExtendedTheme.colors.onTopBar,
    elevation: Dp = defaultAppBarElevation(),
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    TopAppBarContainer(
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

                ProvideTextStyle(
                    value = MaterialTheme.typography.h6.copy(
                        color = LocalContentColor.current,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    Box(Modifier.weight(1.0f)) { title() }
                }

                actions()
            }
        },
        color = backgroundColor,
        contentColor = contentColor,
        elevation = elevation,
        content = content
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopAppBarContainer(
    topBar: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    color: Color = ExtendedTheme.colors.topBar,
    contentColor: Color = ExtendedTheme.colors.onTopBar,
    elevation: Dp = defaultAppBarElevation(),
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = color,
        contentColor = contentColor,
        elevation = elevation
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(AppBarDefaults.ContentPadding)
                    .height(AppBarHeight)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onDoubleClick = {
                            coroutineScope.emitGlobalEvent(GlobalEvent.ScrollToTop)
                        },
                        onClick = {},
                    ),
                content = topBar
            )

            content?.invoke(this)
        }
    }
}