package com.huanchengfly.tieba.post.core.network.source

import android.text.TextUtils
import com.huanchengfly.tieba.post.core.network.exception.TiebaApiException
import com.huanchengfly.tieba.post.core.network.exception.TiebaException
import com.huanchengfly.tieba.post.core.network.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.core.network.model.CommonResponse
import com.huanchengfly.tieba.post.core.network.model.FollowBean
import com.huanchengfly.tieba.post.core.network.model.FollowListBean
import com.huanchengfly.tieba.post.core.network.model.PermissionListBean
import com.huanchengfly.tieba.post.core.network.model.UserLikeForumBean.ForumBean
import com.huanchengfly.tieba.post.core.network.model.protos.Anti
import com.huanchengfly.tieba.post.core.network.model.protos.PostInfoList
import com.huanchengfly.tieba.post.core.network.model.protos.User
import com.huanchengfly.tieba.post.core.network.retrofit.ITiebaApi
import com.huanchengfly.tieba.post.core.network.retrofit.commonResponse
import com.huanchengfly.tieba.post.core.network.retrofit.firstOrThrow
import java.io.File
import javax.inject.Inject

/**
 * Main entry point for accessing user profile data from the network.
 */
interface UserProfileNetworkDataSource {

    suspend fun loadUserLikeForum(uid: Long, page: Int): Pair<List<ForumBean>, Boolean>

    suspend fun loadUserThreadPost(uid: Long, page: Int, isThread: Boolean): List<PostInfoList>

    suspend fun loadUserProfile(uid: Long): Pair<User, Anti?>

    suspend fun loadUserFollowList(uid: Long, page: Int): FollowListBean

    suspend fun requestFollowUser(portrait: String, tbs: String): FollowBean.Info

    suspend fun requestUnfollowUser(portrait: String, tbs: String)

    /**
     * 修改个人资料
     *
     * @param birthdayShowStatus 是否仅显示星座
     * @param birthdayTime 生日时间戳 / 1000
     * @param intro 个人简介（最多 500 字）
     * @param sex 性别（1 = 男，2 = 女）
     * @param nickName 昵称
     */
    suspend fun requestProfileUpdate(
        birthdayShowStatus: Boolean,
        birthdayTime: String,
        intro: String,
        sex: String,
        nickName: String,
    )

    /**
     * 修改个人头像
     *
     * @param file 图片 File 对象
     * */
    suspend fun uploadPortrait(file: File): String

    /**
     * 查询单个用户的拉黑信息
     * @param uid 用户 id
     */
    suspend fun getUserBlackInfo(uid: Long): PermissionListBean

    /**
     * 禁止用户互动（转、评、赞踩、@）
     * @param uid 用户 id
     * @param tbs tbs（长）
     * @param permList 参数列表：关注，互动，私信。(0,允许 1,禁止)
     */
    suspend fun setUserBlack(uid: Long, tbs: String, permList: PermissionListBean)
}

internal class UserProfileNetworkDataSourceImpl @Inject constructor(
    private val tiebaApi: ITiebaApi,
): UserProfileNetworkDataSource {

    override suspend fun loadUserLikeForum(uid: Long, page: Int): Pair<List<ForumBean>, Boolean> {
        require(uid > 0) { "Invalid user ID: $uid." }
        require(page >= 1) { "Invalid page number: $page." }

        return tiebaApi.userLikeForumFlow(uid.toString(), page = page)
            .firstOrThrow()
            .run {
                val code = errorCode.toIntOrNull() ?: 0
                if (code != 0) throw TiebaApiException(CommonResponse(code, errorMsg))
                Pair(forumList.forumList, hasMore == "1")
            }
    }

    override suspend fun loadUserThreadPost(uid: Long, page: Int, isThread: Boolean): List<PostInfoList> {
        require(uid > 0) { "Invalid user ID: $uid." }
        require(page >= 1) { "Invalid page number: $page." }

        return tiebaApi
            .userPostFlow(uid, page, isThread)
            .firstOrThrow()
            .run {
                data_?.post_list ?: throw TiebaApiException(commonResponse = error.commonResponse)
            }
    }

    override suspend fun loadUserProfile(uid: Long): Pair<User, Anti?> {
        require(uid > 0) { "Invalid user ID: $uid." }

        return tiebaApi
            .userProfileFlow(uid)
            .firstOrThrow()
            .run {
                if (data_?.user == null) throw TiebaException("Null user data")
                data_.user to data_.anti_stat
            }
    }

    override suspend fun loadUserFollowList(uid: Long, page: Int): FollowListBean {
        require(uid > 0) { "Invalid user ID: $uid." }
        require(page > 0) { "Invalid page: $page" }

        return tiebaApi
            .followListFlow(page, uid)
            .firstOrThrow()
            .apply {
                if (errorCode != 0) throw TiebaApiException(CommonResponse(errorCode, errorMsg.orEmpty()))
            }
    }

    override suspend fun requestFollowUser(portrait: String, tbs: String): FollowBean.Info {
        if (TextUtils.isEmpty(tbs)) throw TiebaNotLoggedInException()
        if (TextUtils.isEmpty(portrait)) throw IllegalArgumentException("Invalid user portrait")

        return tiebaApi
            .followFlow(portrait, tbs)
            .firstOrThrow()
            .info
    }

    override suspend fun requestUnfollowUser(portrait: String, tbs: String) {
        if (TextUtils.isEmpty(tbs)) throw TiebaNotLoggedInException()
        if (TextUtils.isEmpty(portrait)) throw IllegalArgumentException("Invalid user portrait")

        tiebaApi.unfollowFlow(portrait, tbs)
            .firstOrThrow()
    }

    override suspend fun requestProfileUpdate(
        birthdayShowStatus: Boolean,
        birthdayTime: String,
        intro: String,
        sex: String,
        nickName: String
    ) {
        tiebaApi.profileModifyFlow(birthdayShowStatus, birthdayTime, intro, sex, nickName)
            .firstOrThrow()
    }

    override suspend fun uploadPortrait(file: File): String {
        return tiebaApi.imgPortrait(file)
            .firstOrThrow()
            .run {
                if (errorCode != 0 && errorCode != 300003) {
                    throw TiebaApiException(CommonResponse(errorCode, errorMsg))
                } else {
                    errorMsg
                }
            }
    }

    /**
     * 查询单个用户的拉黑信息
     * @param uid 用户 id
     */
    override suspend fun getUserBlackInfo(uid: Long): PermissionListBean {
        return tiebaApi
            .getUserBlackInfoFlow(uid)
            .firstOrThrow()
            .run {
                if (errorCode != 0 || permList == null) {
                    throw TiebaApiException(CommonResponse(errorCode, errorMsg ?: "Null"))
                } else {
                    this.permList
                }
            }
    }

    /**
     * 禁止用户互动（转、评、赞踩、@）
     * @param uid 用户 id
     * @param tbs tbs（长）
     * @param permList 参数列表：关注，互动，私信。(0,允许 1,禁止)
     */
    override suspend fun setUserBlack(uid: Long, tbs: String, permList: PermissionListBean) {
        tiebaApi.setUserBlackFlow(uid, tbs, permList)
            .firstOrThrow()
    }
}