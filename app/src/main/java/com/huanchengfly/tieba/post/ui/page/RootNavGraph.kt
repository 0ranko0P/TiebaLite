package com.huanchengfly.tieba.post.ui.page

import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.material.navigation.BottomSheetNavigator
import androidx.compose.material.navigation.ModalBottomSheetLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.huanchengfly.tieba.post.ui.common.LocalAnimatedVisibilityScope
import com.huanchengfly.tieba.post.ui.common.LocalSharedTransitionScope
import com.huanchengfly.tieba.post.ui.page.Destination.Companion.ForumDetailParams
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
import com.huanchengfly.tieba.post.ui.page.reply.ReplyPage
import com.huanchengfly.tieba.post.ui.page.search.SearchPage
import com.huanchengfly.tieba.post.ui.page.settings.SettingsDestination
import com.huanchengfly.tieba.post.ui.page.settings.settingsNestedGraphBuilder
import com.huanchengfly.tieba.post.ui.page.settings.theme.AppThemePage
import com.huanchengfly.tieba.post.ui.page.subposts.SubPostsSheetPage
import com.huanchengfly.tieba.post.ui.page.thread.ThreadFrom
import com.huanchengfly.tieba.post.ui.page.thread.ThreadPage
import com.huanchengfly.tieba.post.ui.page.threadstore.ThreadStorePage
import com.huanchengfly.tieba.post.ui.page.user.UserProfilePage
import com.huanchengfly.tieba.post.ui.page.webview.WebViewPage
import kotlin.reflect.typeOf

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun RootNavGraph(
    bottomSheetNavigator: BottomSheetNavigator,
    navController: NavHostController,
    startDestination: Destination = Destination.Main
) {
    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            ModalBottomSheetLayout(bottomSheetNavigator) {
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
            }
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
            deepLinks = listOf(navDeepLink<Destination.History>(basePath = "tblite://history"))
        ) {
            HistoryPage(navController)
        }

        composable<Destination.Notification>(
            deepLinks = listOf(navDeepLink<Destination.Notification>(basePath = "tblite://notifications"))
        ) {
            NotificationsPage(navigator = navController)
        }

        composable<Destination.Forum>(
            deepLinks = listOf(navDeepLink<Destination.Forum>(basePath = "tblite://forum"))
        ) { backStackEntry ->
            val params = backStackEntry.toRoute<Destination.Forum>()
            ForumPage(params.forumName, navController)
        }

        composable<Destination.ForumDetail>(
            typeMap = mapOf(typeOf<ForumDetailParams>() to navTypeOf<ForumDetailParams>())
        ) { backStackEntry ->
            val params = backStackEntry.toRoute<Destination.ForumDetail>().params
            params.run {
                ForumDetailPage(forumId, avatar, name, slogan, memberCount, threadCount, postCount, managers, navController::navigateUp) { user ->
                    navController.navigate(Destination.UserProfile(user.id))
                }
            }
        }

        composable<Destination.ForumRuleDetail> { backStackEntry ->
            val params = backStackEntry.toRoute<Destination.ForumRuleDetail>()
            ForumRuleDetailPage(params.forumId, navController)
        }

        composable<Destination.ForumSearchPost> { backStackEntry ->
            val params = backStackEntry.toRoute<Destination.ForumSearchPost>()
            ForumSearchPostPage(params.forumName, params.forumId, navController)
        }

        val threadTypeMap = mapOf(typeOf<ThreadFrom?>() to navTypeOf<ThreadFrom?>(isNullableAllowed = true))
        composable<Destination.Thread>(
            deepLinks = listOf(navDeepLink<Destination.Thread>(basePath = "tblite://thread", typeMap = threadTypeMap)),
            typeMap = threadTypeMap
        ) { backStackEntry ->
            with(backStackEntry.toRoute<Destination.Thread>()) {
                ThreadPage(threadId, postId, from, scrollToReply, navController)
            }
        }

        composable<Destination.ThreadStore>(
            deepLinks = listOf(navDeepLink<Destination.ThreadStore>(basePath = "tblite://favorite"))
        ) {
            ThreadStorePage(navController)
        }

        // Use Type-Safe BottomSheet: b351858980
        composable<Destination.SubPosts> { backStackEntry ->
            val params = backStackEntry.toRoute<Destination.SubPosts>()
            SubPostsSheetPage(params, navController)
        }

        composable<Destination.HotTopicList> {
            HotTopicListPage(navigator = navController)
        }

        composable<Destination.Login> {
            LoginPage(navController)
        }

        composable<Destination.Search>(
            deepLinks = listOf(navDeepLink<Destination.Search>(basePath = "tblite://search"))
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
            CopyTextDialogPage(params.text, navController)
        }

        // Use Type-Safe BottomSheet: b351858980
        composable<Destination.Reply> { backStackEntry ->
            val params = backStackEntry.toRoute<Destination.Reply>()
            ReplyPage(params, navController::navigateUp)
        }
    }
}

val DefaultFadeOut: ExitTransition by lazy {
    fadeOut(animationSpec = tween(100))
}