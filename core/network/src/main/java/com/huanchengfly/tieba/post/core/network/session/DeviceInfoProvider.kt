package com.huanchengfly.tieba.post.core.network.session

interface DeviceInfoProvider {

    val density: Float

    val deviceScore: String

    val imei: String

    val screenHeight: Int

    val screenWidth: Int

    val uuid: String
}