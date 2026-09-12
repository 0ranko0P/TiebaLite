package com.huanchengfly.tieba.post.core.network.model

import com.google.gson.annotations.SerializedName
import com.huanchengfly.tieba.post.core.network.model.web.BaseBean

class GetUserBlackInfoBean: BaseBean() {
    @SerializedName("error_code")
    val errorCode: Int = -1

    @SerializedName("error_msg")
    val errorMsg: String? = null

    @SerializedName("perm_list")
    val permList: PermissionListBean? = null

    @SerializedName("is_black_white")
    val isBlackWhite: Int = -1
}