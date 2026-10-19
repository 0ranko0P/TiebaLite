package com.huanchengfly.tieba.post.core.network.model

import com.google.gson.annotations.SerializedName
import com.huanchengfly.tieba.post.core.network.model.web.BaseBean

data class FanListBean(
    val page: PageBean = PageBean(),
    @SerializedName("user_list")
    val userList: List<FanUserBean> = emptyList(),
    @SerializedName("error_code")
    val errorCode: Int = 0,
    @SerializedName("error_msg")
    val errorMsg: String? = null,
    @SerializedName("tips_text")
    val tipsText: String? = null,
    val time: Long = 0,
) : BaseBean() {

    data class PageBean(
        @SerializedName("current_page")
        val currentPage: Int = 1,
        @SerializedName("total_count")
        val totalCount: Int = 0,
        @SerializedName("total_page")
        val totalPage: Int = 1,
        @SerializedName("has_more")
        val hasMore: Int = 0,
    )

    data class FanUserBean(
        val id: Long = 0,
        val name: String? = null,
        @SerializedName("name_show")
        val nameShow: String? = null,
        val intro: String? = null,
        val portrait: String? = null,
        @SerializedName("follow_time")
        val followTime: Int = 0,
        @SerializedName("follow_from")
        val followFrom: String? = null,
        @SerializedName("has_concerned")
        val hasConcerned: Int = 0,
    )
}
