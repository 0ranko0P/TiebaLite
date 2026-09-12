package com.huanchengfly.tieba.post.core.network.model

import com.google.gson.annotations.SerializedName
import com.huanchengfly.tieba.post.core.network.model.web.BaseBean

class ChangelogBean : BaseBean() {
    @SerializedName("error_code")
    val errorCode = 0

    @SerializedName("error_msg")
    val errorMsg: String? = null

    @SerializedName("success")
    val isSuccess = false
    val result: String? = null

}