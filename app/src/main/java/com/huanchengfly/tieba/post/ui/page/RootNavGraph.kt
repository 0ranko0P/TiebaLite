package com.huanchengfly.tieba.post.ui.page

import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
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
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.createGraph
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import androidx.navigation.toRoute
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
import com.huanchengfly.tieba.post.ui.page.settings.settingsNestedGraphBuilder
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
import kotlin.reflect.typeOf

const val TB_LITE_DOMAIN = "tblite"

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RootNavGraph(
    // bottomSheetNavigator: BottomSheetNavigator,
    navController: NavHostController,
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
                    graph = remember(startDestination) {
                        buildRootNavGraph(navController, startDestination)
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

private fun buildRootNavGraph(navController: NavHostController, startDestination: Destination): NavGraph {
    return navController.createGraph(startDestination) {
        composable<Destination.Main> {
            CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                MainPage(navController)
            }
        }

        composable<Destination.AppTheme> {
            AppThemePage(navController)
        }

        composable<Destination.History>(
            deepLinks = listOf(navDeepLink<Destination.History>(basePath = "$TB_LITE_DOMAIN://history"))
        ) {
            CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                HistoryPage(navController)
            }
        }

        composable<Destination.Notification>(
            deepLinks = listOf(navDeepLink<Destination.Notification>(basePath = "$TB_LITE_DOMAIN://notifications"))
        ) { backStackEntry ->
            val type = backStackEntry.toRoute<Destination.Notification>().type
            NotificationsPage(initialPage = NotificationsType.entries[type], navigator = navController)
        }

        composable<Destination.Forum>(
            deepLinks = listOf(navDeepLink<Destination.Forum>(basePath = "$TB_LITE_DOMAIN://forum"))
        ) { backStackEntry ->
            val params = backStackEntry.toRoute<Destination.Forum>()
            CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                ForumPage(params.forumName, params.avatar, params.transitionKey, navController)
            }
        }

        composable<Destination.ForumDetail> { backStackEntry ->
            ForumDetailPage(
                onBack = navController::navigateUp,
                onManagerClicked = { navController.navigate(Destination.UserProfile(uid = it)) }
            )
        }

        composable<Destination.ForumRuleDetail> { backStackEntry ->
            ForumRuleDetailPage(navController)
        }

        composable<Destination.ForumSearchPost> { backStackEntry ->
            val params = backStackEntry.toRoute<Destination.ForumSearchPost>()
            ForumSearchPostPage(params.forumName, navController)
        }

        val threadTypeMap = mapOf(typeOf<ThreadFrom?>() to navTypeOf<ThreadFrom?>(isNullableAllowed = true))
        composable<Destination.Thread>(
            typeMap = threadTypeMap
        ) { backStackEntry ->
            with(backStackEntry.toRoute<Destination.Thread>()) {
                val vm: ThreadViewModel = hiltViewModel()
                ThreadPage(threadId, postId, from, scrollToReply, navController, vm) { result ->
                    if (result != null) {
                        navController.navigateBackWithResult(ThreadResultKey, result)
                    } else {
                        navController.navigateUp()
                    }
                }
            }
        }

        composable<Destination.ThreadStore>(
            deepLinks = listOf(navDeepLink<Destination.ThreadStore>(basePath = "$TB_LITE_DOMAIN://favorite"))
        ) {
            ThreadStorePage(navController)
        }

        composable<Destination.SubPosts> { backStackEntry ->
            val params = backStackEntry.toRoute<Destination.SubPosts>()
            SubPostsSheetPage(params, navController)
        }

        composable<Destination.HotTopicList> {
            HotTopicListPage(navigator = navController)
        }

        composable<Destination.Login> {
            LoginPage(navController) {
                if (navController.isLastOrEmptyRoute()) {
                    navController.navigate(Destination.Main) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                } else {
                    navController.navigateUp()
                }
            }
        }

        composable<Destination.Search>(
            deepLinks = listOf(navDeepLink<Destination.Search>(basePath = "$TB_LITE_DOMAIN://search"))
        ) {
            CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                SearchPage(navController)
            }
        }

        composable<Destination.UserProfile> { backStackEntry ->
            val params = backStackEntry.toRoute<Destination.UserProfile>()
            UserProfilePage(params.uid, navController)
        }

        composable<Destination.WebView> { backStackEntry ->
            val params = backStackEntry.toRoute<Destination.WebView>()
            WebViewPage(params.initialUrl, navController)
        }

        navigation<Destination.Settings>(
            startDestination = SettingsDestination.Settings,
            builder = settingsNestedGraphBuilder(navController)
        )

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

val DefaultFadeOut: ExitTransition by lazy {
    fadeOut(animationSpec = tween(100))
}

private fun NavController.isLastOrEmptyRoute(): Boolean = visibleEntries.value.size <= 1