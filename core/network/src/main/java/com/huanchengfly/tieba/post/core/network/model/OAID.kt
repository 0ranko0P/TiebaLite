package com.huanchengfly.tieba.post.core.network.model

import com.google.gson.annotations.SerializedName
import com.huanchengfly.tieba.post.core.network.session.OAIDProvider

data class OAID(
    @SerializedName("v")
    val encodedOAID: String,
    @SerializedName("sc")
    val statusCode: Int,
    @SerializedName("sup")
    val support: Int,
    val isTrackLimited: Int,
) {
    constructor(oaidProvider: OAIDProvider): this(
        encodedOAID = oaidProvider.encodedOAID,
        statusCode = oaidProvider.statusCode,
        support = if (oaidProvider.isOAIDSupported) 1 else 0,
        isTrackLimited = if (oaidProvider.isTrackLimited) 1 else 0,
    )
}
