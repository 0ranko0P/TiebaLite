package com.huanchengfly.tieba.post.core.network.session

interface OAIDProvider {

    fun isOAIDSupported(): Boolean

    fun isTrackLimited(): Boolean

    fun getEncodedOAID(): String

    fun getStatusCode(): Int

    val appFirstInstallTime: Long

    val appLastUpdateTime: Long

    val userAgent: String?
}