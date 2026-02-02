package com.huanchengfly.tieba.post.api.models

import com.google.gson.annotations.SerializedName
import com.huanchengfly.tieba.post.models.BaseBean

data class ForumGuideBean(
    var time: Long = 0,
    @SerializedName("msign_text") var msignText: String = "",
    @SerializedName("msign_level") var msignLevel: Int = 0,
    @SerializedName("max_top_forum_num") var maxTopForumNum: Int = 0,
    @SerializedName("fold_display_num") var foldDisplayNum: Int = 0,
    @SerializedName("like_forum_has_more") var likeForumHasMore: Boolean = false,
    @SerializedName("is_login") var isLogin: Int = 0,
    @SerializedName("msign_valid") var msignValid: Int = 0,
    @SerializedName("error_code") var errorCode: Int = 0,
    @SerializedName("error_msg") var errorMsg: String = "",
    @SerializedName("like_forum") var likeForum: List<LikeForum> = emptyList(),
    @SerializedName("like_priv_sets") var likePrivSets: Int = 0
) : BaseBean() {
    data class LikeForum(
        @SerializedName("forum_name") var forumName: String = "",
        @SerializedName("hot_num") var hotNum: Int = 0,
        @SerializedName("member_count") var memberCount: Int = 0,
        @SerializedName("thread_num") var threadNum: Int = 0,
        @SerializedName("level_name") var levelName: String = "",
        @SerializedName("forum_id") var forumId: Long = 0,
        @SerializedName("day_thread_num") var dayThreadNum: Int = 0,
        var avatar: String = "",
        @SerializedName("is_sign") var isSign: Int = -1,
        @SerializedName("top_sort_value") var topSortValue: Int = 0,
        @SerializedName("level_id") var levelId: Int = 0,
        @SerializedName("is_forbidden") var isForbidden: Int = 0,
        @SerializedName("is_official_forum") var isOfficialForum: Int? = null
    )

}
