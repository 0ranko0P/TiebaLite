package com.huanchengfly.tieba.post.api.models

import com.google.gson.annotations.SerializedName
import com.huanchengfly.tieba.post.models.BaseBean

class AddThreadBean : BaseBean(){
    @SerializedName("error_code")
    var errorCode: String? = null

    @SerializedName("msg", alternate = ["errmsg", "error", "error_msg"])
    var errorMsg: String? = null

    var info: InfoBean? = null
    var pid: String? = null
    var tid: String? = null

    inner class InfoBean {
        @SerializedName("need_vcode")
        val needVcode: String? = null

        @SerializedName("vcode_md5")
        val vcodeMD5: String? = null

        @SerializedName("vcode_pic_url")
        val vcodePicUrl: String? = null

        @SerializedName("pass_token")
        val passToken: String? = null

    }
}