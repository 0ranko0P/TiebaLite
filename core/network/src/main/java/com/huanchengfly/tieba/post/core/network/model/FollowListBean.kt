package com.huanchengfly.tieba.post.core.network.model

import com.google.gson.annotations.SerializedName
import com.huanchengfly.tieba.post.core.network.model.web.BaseBean

data class FollowListBean(
    @SerializedName("error_code")
    val errorCode: Int = 0,
    @SerializedName("error_msg")
    val errorMsg: String? = null,
    val time: Long = 0,
    val logid: String? = null,
    @SerializedName("pn")
    val pageNum: Int = 1,
    @SerializedName("has_more")
    val hasMore: Int = 0,
    @SerializedName("total_follow_num")
    val totalFollowNum: Int = 0,
    @SerializedName("tips_text")
    val tipsText: String? = null,
    @SerializedName("follow_list_switch")
    val followListSwitch: Int = 0,
    @SerializedName("ctime")
    val ctime: String? = null,
    @SerializedName("server_time")
    val serverTime: Int = 0,
    @SerializedName("priv_sets")
    val privSets: Map<String, Int>? = null,
    @SerializedName("follow_list")
    var followList: List<FollowUserBean> = emptyList(),
) : BaseBean() {

    data class FollowUserBean(
        @SerializedName("business_account_info")
        val businessAccountInfo: BusinessAccountInfoBean? = null,
        @SerializedName("bazhu_grade")
        val bazhuGrade: BazhuGradeBean? = null,
        val intro: String? = null,
        @SerializedName("has_concerned")
        val hasConcerned: Int = 0,
        @SerializedName("ala_info")
        val alaInfo: AlaInfoBean? = null,
        @SerializedName("follow_from")
        val followFrom: String? = null,
        val id: Long = 0,
        val name: String? = "",
        @SerializedName("name_show")
        val nameShow: String? = "",
        @SerializedName("priv_sets")
        val privSets: Map<String, Int>? = null,
        val portrait: String? = null,
        val portraith: String? = null,
        @SerializedName("display_auth_type")
        val displayAuthType: Int = 0,
        @SerializedName("work_creator_info")
        val workCreatorInfo: WorkCreatorInfoBean? = null,
    )

    data class BusinessAccountInfoBean(
        @SerializedName("is_business_account")
        val isBusinessAccount: Int = 0,
        @SerializedName("is_forum_business_account")
        val isForumBusinessAccount: Int = 0,
    )

    data class AlaInfoBean(
        val location: String? = null,
        val lng: Double = 0.0,
        val lat: Double = 0.0,
    )

    data class WorkCreatorInfoBean(
        @SerializedName("auth_desc")
        val authDesc: String? = null,
    )

    data class BazhuGradeBean(
        val desc: String? = null,
        val level: String? = null,
    )
}
