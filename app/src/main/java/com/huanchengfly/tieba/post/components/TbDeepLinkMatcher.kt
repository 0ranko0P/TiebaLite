package com.huanchengfly.tieba.post.components

import android.util.Log
import androidx.core.net.toUri
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.deeplink.DeepLinkMatcher
import androidx.navigation3.runtime.deeplink.DeepLinkMatcher.MatchResult
import androidx.navigation3.runtime.deeplink.DeepLinkRequest
import androidx.navigation3.runtime.deeplink.DeepLinkUri
import androidx.navigation3.runtime.deeplink.StaticKeyDeepLinkMatcher
import androidx.navigation3.runtime.deeplink.UriDeepLinkMatcher
import com.huanchengfly.tieba.post.core.common.ktx.unsafeLazy
import com.huanchengfly.tieba.post.core.network.exception.getErrorMessage
import com.huanchengfly.tieba.post.ui.page.Destination

object TbDeepLinkMatcher {

    const val TB_LITE_DOMAIN = "tblite"

    const val URL_FAVORITE = "$TB_LITE_DOMAIN://favorite"

    const val URL_FORUM_BASE = "$TB_LITE_DOMAIN://forum"
    const val URL_FORUM = "$URL_FORUM_BASE/{forumName}?avatar={avatar}&transitionKey={transitionKey}"

    const val URL_SEARCH = "$TB_LITE_DOMAIN://search"

    const val URL_NOTIFICATION_BASE = "$TB_LITE_DOMAIN://notifications"

    private val deepLinkMatchers: List<DeepLinkMatcher<NavKey, MatchResult<NavKey>>> by unsafeLazy {
        listOf(
            // "tblite://favorite"
            StaticKeyDeepLinkMatcher(
                key = Destination.ThreadStore,
                filters = listOf(DeepLinkMatcher.baseUriFilter(URL_FAVORITE)),
            ),
            // "tblite://forum/{forumName}?{avatar}&{transitionKey}"
            UriDeepLinkMatcher(
                uriPattern = URL_FORUM.toUri(),
                serializer = Destination.Forum.serializer()
            ),
            // "tblite://notifications?{type}"
            UriDeepLinkMatcher(
                uriPattern = "$URL_NOTIFICATION_BASE?type={type}".toUri(),
                serializer = Destination.Notification.serializer(),
                filters = listOf(DeepLinkMatcher.baseUriFilter(URL_NOTIFICATION_BASE)),
            ),
            // "tblite://search"
            StaticKeyDeepLinkMatcher(
                key = Destination.Search,
                filters = listOf(DeepLinkMatcher.baseUriFilter(URL_SEARCH))
            ),
        )
    }

    /**
     * Creates a [DeepLinkMatcher.Filter] that filters a [DeepLinkRequest] with the [baseUri].
     * Matching is case-sensitive.
     *
     * @param baseUri the base uri the filter by
     * @return true if the [DeepLinkRequest]'s base URI exactly matches the [baseUri].
     */
    fun DeepLinkMatcher.Companion.baseUriFilter(baseUri: String): DeepLinkMatcher.Filter = { request ->
        request.uri?.toString()?.startsWith(baseUri) ?: false
    }

    fun DeepLinkUri.isTbLiteDeepLink(): Boolean {
        return this.scheme == TB_LITE_DOMAIN
    }

    fun resolveDeepLink(uri: DeepLinkUri): NavKey? {
        val request = DeepLinkRequest(uri)
        val matches = try {
            deepLinkMatchers.mapNotNull { it.match(request) }
        } catch (e: Throwable) {
            Log.e(this::class.simpleName, "onResolveDeepLink: ${e.getErrorMessage()} on $uri")
            return null
        }
        return matches.maxOrNull()?.key
    }
}