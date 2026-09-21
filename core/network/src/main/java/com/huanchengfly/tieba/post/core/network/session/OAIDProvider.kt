package com.huanchengfly.tieba.post.core.network.session

interface OAIDProvider {

    val appFirstInstallTime: Long

    val appLastUpdateTime: Long

    val encodedOAID: String

    val isOAIDSupported: Boolean

    val isTrackLimited: Boolean

    val statusCode: Int

    val userAgent: String?
}