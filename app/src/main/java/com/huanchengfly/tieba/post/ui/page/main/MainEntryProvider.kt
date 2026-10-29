package com.huanchengfly.tieba.post.ui.page.main

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.res.painterResource
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.core.designsystem.component.PainterDetailPlaceholder
import com.huanchengfly.tieba.post.core.designsystem.component.navigationsuite.LocalNavSuiteScaffoldState
import com.huanchengfly.tieba.post.core.designsystem.component.navigationsuite.TbNavigationSuiteScaffoldState
import com.huanchengfly.tieba.post.core.navigation.Navigator
import com.huanchengfly.tieba.post.core.navigation.defaultContentKey
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.main.explore.ExplorePage
import com.huanchengfly.tieba.post.ui.page.main.home.HomePage
import com.huanchengfly.tieba.post.ui.page.main.notifications.NotificationsPage
import com.huanchengfly.tieba.post.ui.page.main.user.UserPage
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalHazeState
import com.huanchengfly.tieba.post.ui.widgets.compose.TbHazeState
import com.huanchengfly.tieba.post.utils.LocalAccount

fun EntryProviderScope<NavKey>.mainEntry(
    navigator: Navigator,
    hazeState: TbHazeState? = null,
    navSuiteScaffoldState: TbNavigationSuiteScaffoldState,
) {
    animatedEntry<MainDestination.Home>(
        hazeState = hazeState,
        navSuiteScaffoldState = navSuiteScaffoldState,
    ) {
        HomePage(
            onExploreClicked = {
                navigator.navigate(key = MainDestination.Explore)
            },
            onLoginClicked = {
                navigator.navigate(key = Destination.Login)
            },
            onSearchClicked = {
                navigator.navigate(key = Destination.Search)
            },
            onForumClicked = {
                navigator.navigate(key = Destination.Forum(forumName = it.name, avatar = it.avatar))
            },
            onHistoryClicked = {
                navigator.navigate(key = Destination.Forum(forumName = it.name))
            }
        )
    }

    animatedEntry<MainDestination.Explore>(
        hazeState = hazeState,
        navSuiteScaffoldState = navSuiteScaffoldState,
    ) {
        val loggedIn = LocalAccount.current != null
        key(loggedIn) { // Force recreate
            ExplorePage(loggedIn, navigator)
        }
    }

    animatedEntry<MainDestination.Notification>(
        metadata = ListDetailSceneStrategy.listPane(
            detailPlaceholder = {
                PainterDetailPlaceholder(painter = painterResource(R.drawable.empty_mailbox))
            }
        ),
        hazeState = hazeState,
        navSuiteScaffoldState = navSuiteScaffoldState,
    ) {
        NotificationsPage(fromHome = true, onNavigate = navigator::navigate)
    }

    animatedEntry<MainDestination.User>(
        hazeState = null,
        navSuiteScaffoldState = navSuiteScaffoldState,
    ) {
        UserPage(navigator = navigator)
    }
}

private inline fun <reified K : NavKey> EntryProviderScope<NavKey>.animatedEntry(
    noinline clazzContentKey: (key: @JvmSuppressWildcards K) -> Any = { it.defaultContentKey },
    metadata: Map<String, Any> = emptyMap(),
    hazeState: TbHazeState?,
    navSuiteScaffoldState: TbNavigationSuiteScaffoldState,
    noinline content: @Composable AnimatedContentScope.(K) -> Unit,
) {
    addEntryProvider(K::class, clazzContentKey, { metadata }) {
        CompositionLocalProvider(
            LocalHazeState provides hazeState,
            LocalNavSuiteScaffoldState provides navSuiteScaffoldState,
        ) {
            LocalNavAnimatedContentScope.current.content(it)
        }
    }
}
