package com.huanchengfly.tieba.post.ui.page

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraph
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.ComposeNavigatorDestinationBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.createGraph
import androidx.navigation.get
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.huanchengfly.tieba.post.arch.unsafeLazy
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.common.LocalAnimatedVisibilityScope
import com.huanchengfly.tieba.post.ui.common.LocalSharedTransitionScope
import com.huanchengfly.tieba.post.ui.page.Destination.Companion.navTypeOf
import com.huanchengfly.tieba.post.ui.page.dialogs.CopyTextDialogPage
import com.huanchengfly.tieba.post.ui.page.forum.ForumPage
import com.huanchengfly.tieba.post.ui.page.forum.detail.ForumDetailPage
import com.huanchengfly.tieba.post.ui.page.forum.rule.ForumRuleDetailPage
import com.huanchengfly.tieba.post.ui.page.forum.searchpost.ForumSearchPostPage
import com.huanchengfly.tieba.post.ui.page.history.HistoryPage
import com.huanchengfly.tieba.post.ui.page.hottopic.list.HotTopicListPage
import com.huanchengfly.tieba.post.ui.page.login.LoginPage
import com.huanchengfly.tieba.post.ui.page.main.MainPage
import com.huanchengfly.tieba.post.ui.page.main.notifications.NotificationsPage
import com.huanchengfly.tieba.post.ui.page.main.notifications.list.NotificationsType
import com.huanchengfly.tieba.post.ui.page.reply.ReplyPageBottomSheet
import com.huanchengfly.tieba.post.ui.page.search.SearchPage
import com.huanchengfly.tieba.post.ui.page.settings.SettingsDestination
import com.huanchengfly.tieba.post.ui.page.settings.settingsGraph
import com.huanchengfly.tieba.post.ui.page.settings.theme.AppThemePage
import com.huanchengfly.tieba.post.ui.page.subposts.SubPostsSheetPage
import com.huanchengfly.tieba.post.ui.page.thread.ThreadFrom
import com.huanchengfly.tieba.post.ui.page.thread.ThreadPage
import com.huanchengfly.tieba.post.ui.page.thread.ThreadResultKey
import com.huanchengfly.tieba.post.ui.page.thread.ThreadViewModel
import com.huanchengfly.tieba.post.ui.page.threadstore.ThreadStorePage
import com.huanchengfly.tieba.post.ui.page.user.UserProfilePage
import com.huanchengfly.tieba.post.ui.page.webview.WebViewPage
import com.huanchengfly.tieba.post.ui.page.welcome.WelcomeScreen
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

const val TB_LITE_DOMAIN = "tblite"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootNavGraph(
    // bottomSheetNavigator: BottomSheetNavigator,
    navController: NavHostController,
    settingsRepo: SettingsRepository,
    startDestination: Destination = Destination.Main
) {
    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            // ModalBottomSheetLayout(
            //     bottomSheetNavigator = bottomSheetNavigator,
            //     dragHandle = null
            // ) {
                NavHost(
                    navController = navController,
                    graph = remember {
                        buildRootNavGraph(navController, settingsRepo, startDestination)
                    },
                    enterTransition = {
                        scaleIn(
                            animationSpec = tween(delayMillis = 35),
                            initialScale = 1.1F
                        ) + fadeIn(
                            animationSpec = tween(delayMillis = 35)
                        )
                    },
                    exitTransition = { DefaultFadeOut },
                    popEnterTransition = {
                        scaleIn(
                            animationSpec = tween(delayMillis = 35),
                            initialScale = 0.9F
                        ) + fadeIn(
                            animationSpec = tween(delayMillis = 35)
                        )
                    },
                    popExitTransition = { DefaultFadeOut },
                )
            // }
        }
    }
}

private fun buildRootNavGraph(
    navController: NavHostController,
    settingsRepo: SettingsRepository,
    startDestination: Destination
): NavGraph {
    return navController.createGraph(startDestination) {
        animatedComposable<Destination.Main> {
            MainPage(navController)
        }

        composable<Destination.AppTheme> {
            AppThemePage(navController)
        }

        animatedComposable<Destination.History>(
            deepLinks = listOf(navDeepLink<Destination.History>(basePath = "$TB_LITE_DOMAIN://history"))
        ) {
            HistoryPage(navController)
        }

        composable<Destination.Notification>(
            deepLinks = listOf(navDeepLink<Destination.Notification>(basePath = "$TB_LITE_DOMAIN://notifications"))
        ) { backStackEntry ->
            val type = backStackEntry.toRoute<Destination.Notification>().type
            NotificationsPage(initialPage = NotificationsType.entries[type], navigator = navController)
        }

        animatedComposable<Destination.Forum>(
            deepLinks = listOf(navDeepLink<Destination.Forum>(basePath = "$TB_LITE_DOMAIN://forum"))
        ) { backStackEntry ->
            backStackEntry.toRoute<Destination.Forum>().run {
                ForumPage(forumName, avatarUrl = avatar, transitionKey, navigator = navController)
            }
        }

        composable<Destination.ForumDetail> { backStackEntry ->
            ForumDetailPage(
                onBack = navController::navigateUp,
                onManagerClicked = { navController.navigate(Destination.UserProfile(uid = it)) }
            )
        }

        animatedComposable<Destination.ForumRuleDetail> { backStackEntry ->
            ForumRuleDetailPage(navController)
        }

        animatedComposable<Destination.ForumSearchPost> { backStackEntry ->
            val params = backStackEntry.toRoute<Destination.ForumSearchPost>()
            ForumSearchPostPage(params.forumName, navController)
        }

        animatedComposable<Destination.Thread>(
            typeMap = mapOf(typeOf<ThreadFrom?>() to navTypeOf<ThreadFrom?>(isNullableAllowed = true))
        ) { backStackEntry ->
            with(backStackEntry.toRoute<Destination.Thread>()) {
                val vm: ThreadViewModel = hiltViewModel()
                ThreadPage(threadId, postId, from, navController, vm) { result ->
                    if (result != null) {
                        navController.navigateBackWithResult(ThreadResultKey, result)
                    } else {
                        navController.navigateUp()
                    }
                }
            }
        }

        animatedComposable<Destination.ThreadStore>(
            deepLinks = listOf(navDeepLink<Destination.ThreadStore>(basePath = "$TB_LITE_DOMAIN://favorite"))
        ) {
            ThreadStorePage(navController)
        }

        animatedComposable<Destination.SubPosts> { backStackEntry ->
            val params = backStackEntry.toRoute<Destination.SubPosts>()
            SubPostsSheetPage(params, navController)
        }

        composable<Destination.HotTopicList> {
            HotTopicListPage(navigator = navController)
        }

        composable<Destination.Login> {
            LoginPage(navController) {
                if (navController.previousBackStackEntry == null) {
                    navController.navigate(Destination.Main) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                } else {
                    navController.navigateUp()
                }
            }
        }

        animatedComposable<Destination.Search>(
            deepLinks = listOf(navDeepLink<Destination.Search>(basePath = "$TB_LITE_DOMAIN://search"))
        ) {
            SearchPage(navController)
        }

        animatedComposable<Destination.UserProfile> { backStackEntry ->
            backStackEntry.toRoute<Destination.UserProfile>().run {
                UserProfilePage(uid, avatar, nickname, username, transitionKey, navController)
            }
        }

        composable<Destination.WebView> { backStackEntry ->
            val params = backStackEntry.toRoute<Destination.WebView>()
            WebViewPage(params.initialUrl, navController)
        }

        navigation<Destination.Settings>(startDestination = SettingsDestination.Settings) {
            settingsGraph(navController, settingsRepo)
        }

        composable<Destination.CopyText> { backStackEntry ->
            val params = backStackEntry.toRoute<Destination.CopyText>()
            CopyTextDialogPage(text = params.text, onBack = navController::navigateUp)
        }

        // Bug: new MD3 ModalBottomSheet breaks our reply panel animation
        // bottomSheet<Destination.Reply> { backStackEntry ->
        dialog<Destination.Reply>(
            dialogProperties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) { backStackEntry ->
            val params = backStackEntry.toRoute<Destination.Reply>()
            ReplyPageBottomSheet(params, navController::navigateUp)
        }

        composable<Destination.Welcome> {
            WelcomeScreen(navController)
        }
    }
}

private val DefaultFadeOut: ExitTransition by unsafeLazy {
    fadeOut(animationSpec = tween(100))
}

/**
 * Add the [Composable] to the [NavGraphBuilder]
 *
 * @param T route from a [KClass] for the destination
 * @param typeMap map of destination arguments' kotlin type [KType] to its respective custom
 *   [NavType]. May be empty if [T] does not use custom NavTypes.
 * @param deepLinks list of deep links to associate with the destinations
 * @param enterTransition callback to determine the destination's enter transition
 * @param exitTransition callback to determine the destination's exit transition
 * @param popEnterTransition callback to determine the destination's popEnter transition
 * @param popExitTransition callback to determine the destination's popExit transition
 * @param sizeTransform callback to determine the destination's sizeTransform.
 * @param content composable for the destination
 */
private inline fun <reified T : Any> NavGraphBuilder.animatedComposable(
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    deepLinks: List<NavDeepLink> = emptyList(),
    noinline enterTransition:
        (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
            EnterTransition?)? =
        null,
    noinline exitTransition:
        (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
            ExitTransition?)? =
        null,
    noinline popEnterTransition:
        (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
            EnterTransition?)? =
        enterTransition,
    noinline popExitTransition:
        (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
            ExitTransition?)? =
        exitTransition,
    noinline sizeTransform:
        (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
            SizeTransform?)? =
        null,
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    destination(
        ComposeNavigatorDestinationBuilder(
            provider[ComposeNavigator::class],
            T::class,
            typeMap
        ) {
            CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                content(it)
            }
        }
        .apply {
            deepLinks.forEach { deepLink -> deepLink(deepLink) }
            this.enterTransition = enterTransition
            this.exitTransition = exitTransition
            this.popEnterTransition = popEnterTransition
            this.popExitTransition = popExitTransition
            this.sizeTransform = sizeTransform
        }
    )
}
