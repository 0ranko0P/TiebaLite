package com.huanchengfly.tieba.post.core.network.model

import com.google.gson.annotations.SerializedName

data class Sync(
    val client: Client,
    @SerializedName("wl_config")
    val wlConfig: WlConfig
) {
    data class Client(
        @SerializedName("client_id")
        val clientId: String
    )

    data class WlConfig(
        @SerializedName("sample_id")
        val sampleId: String
    )
}