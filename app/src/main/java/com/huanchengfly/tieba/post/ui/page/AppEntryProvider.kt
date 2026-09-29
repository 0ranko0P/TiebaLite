package com.huanchengfly.tieba.post.ui.page

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.scene.DialogSceneStrategy
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.core.data.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.core.designsystem.component.PainterDetailPlaceholder
import com.huanchengfly.tieba.post.core.designsystem.component.navigationsuite.TbNavigationSuiteScaffoldState
import com.huanchengfly.tieba.post.core.navigation.LocalNavigator
import com.huanchengfly.tieba.post.core.navigation.Navigator
import com.huanchengfly.tieba.post.core.navigation.containsScene
import com.huanchengfly.tieba.post.core.navigation.popUpNavigate
import com.huanchengfly.tieba.post.core.ui.scenes.DetailPaneBackHandlerSceneDecoratorStrategy
import com.huanchengfly.tieba.post.ui.page.dialogs.CopyTextDialogPage
import com.huanchengfly.tieba.post.ui.page.forum.ForumPage
import com.huanchengfly.tieba.post.ui.page.forum.detail.ForumDetailPage
import com.huanchengfly.tieba.post.ui.page.forum.rule.ForumRuleDetailPage
import com.huanchengfly.tieba.post.ui.page.forum.searchpost.ForumSearchPostPage
import com.huanchengfly.tieba.post.ui.page.history.HistoryPage
import com.huanchengfly.tieba.post.ui.page.hottopic.detail.TopicDetailPage
import com.huanchengfly.tieba.post.ui.page.hottopic.list.HotTopicListPage
import com.huanchengfly.tieba.post.ui.page.login.LoginPage
import com.huanchengfly.tieba.post.ui.page.main.MainDestination
import com.huanchengfly.tieba.post.ui.page.main.explore.createThreadClickListeners
import com.huanchengfly.tieba.post.ui.page.main.mainEntry
import com.huanchengfly.tieba.post.ui.page.main.notifications.NotificationsPage
import com.huanchengfly.tieba.post.ui.page.main.notifications.list.NotificationsType
import com.huanchengfly.tieba.post.ui.page.reply.ReplyPageBottomSheet
import com.huanchengfly.tieba.post.ui.page.report.ReportPage
import com.huanchengfly.tieba.post.ui.page.search.SearchPage
import com.huanchengfly.tieba.post.ui.page.settings.settingsEntry
import com.huanchengfly.tieba.post.ui.page.settings.theme.AppThemePage
import com.huanchengfly.tieba.post.ui.page.subposts.SubPostsPage
import com.huanchengfly.tieba.post.ui.page.thread.ThreadPage
import com.huanchengfly.tieba.post.ui.page.threadstore.ThreadStorePage
import com.huanchengfly.tieba.post.ui.page.user.UserProfilePage
import com.huanchengfly.tieba.post.ui.page.user.followlist.FollowListPage
import com.huanchengfly.tieba.post.ui.page.webview.WebViewPage
import com.huanchengfly.tieba.post.ui.page.welcome.WelcomeScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.TbHazeState
import com.huanchengfly.tieba.post.ui.widgets.compose.video.LocalVideoPreviewState

fun appEntries(
    navigator: Navigator,
    settingsRepo: SettingsRepository,
    hazeState: TbHazeState? = null,
    navSuiteScaffoldState: TbNavigationSuiteScaffoldState,
    sharedTransitionScope: SharedTransitionScope? = null,
): (NavKey) -> NavEntry<NavKey> = entryProvider {
    val onBack: () -> Unit = navigator::navigateUp
    val detailPaneMetadata = ListDetailSceneStrategy.detailPane() +
            DetailPaneBackHandlerSceneDecoratorStrategy.backHandler()

    mainEntry(navigator, hazeState, navSuiteScaffoldState, sharedTransitionScope)

    entry<Destination.AppTheme> {
        AppThemePage(navigator::navigateUp)
    }

    entry<Destination.CopyText> { navKey ->
        CopyTextDialogPage(text = navKey.text, onBack = onBack)
    }

    entry<Destination.Forum>(
        metadata = if (navigator.containsScene<Destination.Forum, Destination.Thread>()) {
            ListDetailSceneStrategy.listPane()
        } else {
            detailPaneMetadata
        },
    ) { navKey ->
        with(navKey) {
            ForumPage(forumName, avatarUrl = avatar, transitionKey, navigator)
        }
    }

    entry<Destination.ForumDetail> { navKey ->
        ForumDetailPage(
            forumName = navKey.forumName,
            onManagerClicked = { navigator.navigate(Destination.UserProfile(uid = it)) },
            onBack = onBack,
        )
    }

    entry<Destination.ForumRuleDetail>(
        metadata = ListDetailSceneStrategy.detailPane(),
    ) { navKey ->
        ForumRuleDetailPage(
            forumId = navKey.forumId,
            onBack = onBack,
            onNavigateUser = { navigator.navigate(Destination.UserProfile(user = it)) }
        )
    }

    entry<Destination.ForumSearchPost> { navKey ->
        ForumSearchPostPage(
            forumName = navKey.forumName,
            forumId = navKey.forumId,
            navigator = navigator,
        )
    }

    entry<Destination.History>(metadata = ListDetailSceneStrategy.listPane()) {
        HistoryPage(
            onBack = onBack,
            onNavigateHistory = { navigator.popUpNavigate<Destination.History>(it) },
        )
    }

    entry<Destination.HotTopicList>() {
        HotTopicListPage(onBack, onNavigateHotTopic = navigator::navigate)
    }

    entry<Destination.HotTopicDetail>(
        metadata = ListDetailSceneStrategy.listPane()
    ) { navKey ->
        TopicDetailPage(
            topicId = navKey.topicId,
            topicName = navKey.topicName,
            onBack = onBack,
            threadClickListeners = remember(navigator) {
                createThreadClickListeners(onNavigate = navigator::navigate)
            },
        )
    }

    entry<Destination.Login> {
        LoginPage(onBack = onBack)
    }

    entry<Destination.Notification>(
        metadata = ListDetailSceneStrategy.listPane(
            detailPlaceholder = {
                PainterDetailPlaceholder(painter = painterResource(R.drawable.empty_mailbox))
            }
        )
    ) { navKey ->
        val notificationsType = NotificationsType.entries[navKey.type]
        NotificationsPage(
            initialPage = notificationsType,
            fromHome = false,
            onBack = onBack,
            onNavigate = navigator::navigate
        )
    }

    entry<Destination.Reply>(
        metadata = DialogSceneStrategy.dialog(
            DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ),
    ) { navKey ->
        ReplyPageBottomSheet(navKey, onBack = navigator::navigateUp)
    }

    entry<Destination.Report> { navKey ->
        ReportPage(postId = navKey.postId, onBack, onNavigate = navigator::navigate)
    }

    entry<Destination.Search>(
        metadata = ListDetailSceneStrategy.listPane(),
    ) {
        SearchPage(navigator)
    }

    entry<Destination.SubPosts>(
        metadata = detailPaneMetadata,
    ) { navKey ->
        SubPostsPage(navKey, navigator)
    }

    entry<Destination.Thread>(
        metadata = if (navigator.containsScene<Destination.Thread, Destination.SubPosts>()) {
            ListDetailSceneStrategy.listPane()
        } else {
            detailPaneMetadata
        },
    ) { navKey ->
        ThreadPage(navKey, navigator)
    }

    entry<Destination.ThreadStore> {
        ThreadStorePage(navigator)
    }

    entry<Destination.UserFollowList> { navKey ->
        FollowListPage(
            uid = navKey.uid,
            onBack = onBack,
            onUserClicked = { navigator.navigate(Destination.UserProfile(uid = it)) }
        )
    }

    entry<Destination.UserProfile>() { navKey ->
        CompositionLocalProvider(
            LocalVideoPreviewState provides null,
            LocalNavigator provides navigator,
        ) {
            UserProfilePage(params = navKey, navigator)
        }
    }

    entry<Destination.WebView> { navKey ->
        WebViewPage(
            initialUrl = navKey.initialUrl,
            customClient = navKey.customClient,
            onBack = onBack,
            onNavigate = navigator::navigate,
        )
    }

    entry<Destination.Welcome> {
        WelcomeScreen(
            onNavigateAppTheme = { navigator.navigate(Destination.AppTheme) },
            onNavigateLogin = {
                navigator.state.currentSubStack.clear()
                navigator.navigate(MainDestination.Home)
                navigator.navigate(Destination.Login)
            },
            onNavigateMain = { navigator.navigate(MainDestination.Home) }
        )
    }

    settingsEntry(navigator, settingsRepo)
}
