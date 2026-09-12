package com.huanchengfly.tieba.post.core.network.retrofit.impls

import android.content.ContentResolver
import android.os.Build
import android.text.TextUtils
import com.huanchengfly.tieba.post.core.common.ktx.booleanToString
import com.huanchengfly.tieba.post.core.common.ktx.toJson
import com.huanchengfly.tieba.post.core.common.ktx.urlEncode
import com.huanchengfly.tieba.post.core.network.ClientVersion
import com.huanchengfly.tieba.post.core.network.Param
import com.huanchengfly.tieba.post.core.network.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.core.network.model.AddThreadBean
import com.huanchengfly.tieba.post.core.network.model.AgreeBean
import com.huanchengfly.tieba.post.core.network.model.CheckReportBean
import com.huanchengfly.tieba.post.core.network.model.CollectDataBean
import com.huanchengfly.tieba.post.core.network.model.CommonResponse
import com.huanchengfly.tieba.post.core.network.model.FollowBean
import com.huanchengfly.tieba.post.core.network.model.FollowListBean
import com.huanchengfly.tieba.post.core.network.model.ForumGuideBean
import com.huanchengfly.tieba.post.core.network.model.ForumRecommend
import com.huanchengfly.tieba.post.core.network.model.GetForumListBean
import com.huanchengfly.tieba.post.core.network.model.GetUserBlackInfoBean
import com.huanchengfly.tieba.post.core.network.model.InitNickNameBean
import com.huanchengfly.tieba.post.core.network.model.LikeForumResultBean
import com.huanchengfly.tieba.post.core.network.model.LoginBean
import com.huanchengfly.tieba.post.core.network.model.MSignBean
import com.huanchengfly.tieba.post.core.network.model.MessageListBean
import com.huanchengfly.tieba.post.core.network.model.MsgBean
import com.huanchengfly.tieba.post.core.network.model.NewCollectDataBean
import com.huanchengfly.tieba.post.core.network.model.PermissionListBean
import com.huanchengfly.tieba.post.core.network.model.PersonalizedBean
import com.huanchengfly.tieba.post.core.network.model.PicPageBean
import com.huanchengfly.tieba.post.core.network.model.Profile
import com.huanchengfly.tieba.post.core.network.model.ProfileBean
import com.huanchengfly.tieba.post.core.network.model.SearchForumBean
import com.huanchengfly.tieba.post.core.network.model.SearchPostBean
import com.huanchengfly.tieba.post.core.network.model.SearchThreadBean
import com.huanchengfly.tieba.post.core.network.model.SearchUserBean
import com.huanchengfly.tieba.post.core.network.model.SignResultBean
import com.huanchengfly.tieba.post.core.network.model.SubFloorListBean
import com.huanchengfly.tieba.post.core.network.model.Sync
import com.huanchengfly.tieba.post.core.network.model.ThreadContentBean
import com.huanchengfly.tieba.post.core.network.model.ThreadStoreBean
import com.huanchengfly.tieba.post.core.network.model.TopicDetailBean
import com.huanchengfly.tieba.post.core.network.model.UserLikeForumBean
import com.huanchengfly.tieba.post.core.network.model.UserPostBean
import com.huanchengfly.tieba.post.core.network.model.WebReplyResultBean
import com.huanchengfly.tieba.post.core.network.model.WebUploadPicBean
import com.huanchengfly.tieba.post.core.network.model.protos.GeneralTabList.GeneralTabListRequest
import com.huanchengfly.tieba.post.core.network.model.protos.GeneralTabList.GeneralTabListRequestData
import com.huanchengfly.tieba.post.core.network.model.protos.GeneralTabList.GeneralTabListResponse
import com.huanchengfly.tieba.post.core.network.model.protos.addPollPost.AddPollPostReponse
import com.huanchengfly.tieba.post.core.network.model.protos.addPollPost.AddPollPostRequest
import com.huanchengfly.tieba.post.core.network.model.protos.addPollPost.AddPollPostRequestDate
import com.huanchengfly.tieba.post.core.network.model.protos.addPost.AddPostRequest
import com.huanchengfly.tieba.post.core.network.model.protos.addPost.AddPostRequestData
import com.huanchengfly.tieba.post.core.network.model.protos.addPost.AddPostResponse
import com.huanchengfly.tieba.post.core.network.model.protos.forumGuide.ForumGuideRequest
import com.huanchengfly.tieba.post.core.network.model.protos.forumGuide.ForumGuideRequestData
import com.huanchengfly.tieba.post.core.network.model.protos.forumGuide.ForumGuideResponse
import com.huanchengfly.tieba.post.core.network.model.protos.forumRecommend.ForumRecommendRequest
import com.huanchengfly.tieba.post.core.network.model.protos.forumRecommend.ForumRecommendRequestData
import com.huanchengfly.tieba.post.core.network.model.protos.forumRecommend.ForumRecommendResponse
import com.huanchengfly.tieba.post.core.network.model.protos.forumRuleDetail.ForumRuleDetailRequest
import com.huanchengfly.tieba.post.core.network.model.protos.forumRuleDetail.ForumRuleDetailRequestData
import com.huanchengfly.tieba.post.core.network.model.protos.forumRuleDetail.ForumRuleDetailResponse
import com.huanchengfly.tieba.post.core.network.model.protos.frsPage.FrsPageRequest
import com.huanchengfly.tieba.post.core.network.model.protos.frsPage.FrsPageRequestData
import com.huanchengfly.tieba.post.core.network.model.protos.frsPage.FrsPageResponse
import com.huanchengfly.tieba.post.core.network.model.protos.getBawuInfo.GetBawuInfoRequest
import com.huanchengfly.tieba.post.core.network.model.protos.getBawuInfo.GetBawuInfoRequestData
import com.huanchengfly.tieba.post.core.network.model.protos.getBawuInfo.GetBawuInfoResponse
import com.huanchengfly.tieba.post.core.network.model.protos.getForumDetail.GetForumDetailRequest
import com.huanchengfly.tieba.post.core.network.model.protos.getForumDetail.GetForumDetailRequestData
import com.huanchengfly.tieba.post.core.network.model.protos.getForumDetail.GetForumDetailResponse
import com.huanchengfly.tieba.post.core.network.model.protos.getHistoryForum.GetHistoryForumRequest
import com.huanchengfly.tieba.post.core.network.model.protos.getHistoryForum.GetHistoryForumRequestData
import com.huanchengfly.tieba.post.core.network.model.protos.getHistoryForum.GetHistoryForumResponse
import com.huanchengfly.tieba.post.core.network.model.protos.getLevelInfo.GetLevelInfoRequest
import com.huanchengfly.tieba.post.core.network.model.protos.getLevelInfo.GetLevelInfoRequestData
import com.huanchengfly.tieba.post.core.network.model.protos.getLevelInfo.GetLevelInfoResponse
import com.huanchengfly.tieba.post.core.network.model.protos.getMemberInfo.GetMemberInfoRequest
import com.huanchengfly.tieba.post.core.network.model.protos.getMemberInfo.GetMemberInfoRequestData
import com.huanchengfly.tieba.post.core.network.model.protos.getMemberInfo.GetMemberInfoResponse
import com.huanchengfly.tieba.post.core.network.model.protos.getUserInfo.GetUserInfoRequest
import com.huanchengfly.tieba.post.core.network.model.protos.getUserInfo.GetUserInfoRequestData
import com.huanchengfly.tieba.post.core.network.model.protos.getUserInfo.GetUserInfoResponse
import com.huanchengfly.tieba.post.core.network.model.protos.hotThreadList.HotThreadListRequest
import com.huanchengfly.tieba.post.core.network.model.protos.hotThreadList.HotThreadListRequestData
import com.huanchengfly.tieba.post.core.network.model.protos.hotThreadList.HotThreadListResponse
import com.huanchengfly.tieba.post.core.network.model.protos.pbFloor.PbFloorRequest
import com.huanchengfly.tieba.post.core.network.model.protos.pbFloor.PbFloorRequestData
import com.huanchengfly.tieba.post.core.network.model.protos.pbFloor.PbFloorResponse
import com.huanchengfly.tieba.post.core.network.model.protos.pbPage.PbPageRequest
import com.huanchengfly.tieba.post.core.network.model.protos.pbPage.PbPageRequestData
import com.huanchengfly.tieba.post.core.network.model.protos.pbPage.PbPageResponse
import com.huanchengfly.tieba.post.core.network.model.protos.personalized.PersonalizedRequest
import com.huanchengfly.tieba.post.core.network.model.protos.personalized.PersonalizedRequestData
import com.huanchengfly.tieba.post.core.network.model.protos.personalized.PersonalizedResponse
import com.huanchengfly.tieba.post.core.network.model.protos.profile.ProfileRequest
import com.huanchengfly.tieba.post.core.network.model.protos.profile.ProfileRequestData
import com.huanchengfly.tieba.post.core.network.model.protos.profile.ProfileResponse
import com.huanchengfly.tieba.post.core.network.model.protos.searchSug.SearchSugRequest
import com.huanchengfly.tieba.post.core.network.model.protos.searchSug.SearchSugRequestData
import com.huanchengfly.tieba.post.core.network.model.protos.searchSug.SearchSugResponse
import com.huanchengfly.tieba.post.core.network.model.protos.threadList.AdParam
import com.huanchengfly.tieba.post.core.network.model.protos.threadList.ThreadListRequest
import com.huanchengfly.tieba.post.core.network.model.protos.threadList.ThreadListRequestData
import com.huanchengfly.tieba.post.core.network.model.protos.threadList.ThreadListResponse
import com.huanchengfly.tieba.post.core.network.model.protos.topicList.TopicListRequest
import com.huanchengfly.tieba.post.core.network.model.protos.topicList.TopicListRequestData
import com.huanchengfly.tieba.post.core.network.model.protos.topicList.TopicListResponse
import com.huanchengfly.tieba.post.core.network.model.protos.userLike.UserLikeRequest
import com.huanchengfly.tieba.post.core.network.model.protos.userLike.UserLikeRequestData
import com.huanchengfly.tieba.post.core.network.model.protos.userLike.UserLikeResponse
import com.huanchengfly.tieba.post.core.network.model.protos.userPost.UserPostRequest
import com.huanchengfly.tieba.post.core.network.model.protos.userPost.UserPostRequestData
import com.huanchengfly.tieba.post.core.network.model.protos.userPost.UserPostResponse
import com.huanchengfly.tieba.post.core.network.model.web.DislikeBean
import com.huanchengfly.tieba.post.core.network.model.web.ForumHome
import com.huanchengfly.tieba.post.core.network.model.web.HotMessageListBean
import com.huanchengfly.tieba.post.core.network.model.web.MyInfoBean
import com.huanchengfly.tieba.post.core.network.model.web.PhotoInfoBean
import com.huanchengfly.tieba.post.core.network.retrofit.ApiResult
import com.huanchengfly.tieba.post.core.network.retrofit.ITiebaApi
import com.huanchengfly.tieba.post.core.network.retrofit.RetrofitTiebaApi
import com.huanchengfly.tieba.post.core.network.retrofit.body.MyMultipartBody
import com.huanchengfly.tieba.post.core.network.session.ClientConfigProvider
import com.huanchengfly.tieba.post.core.network.session.CredentialProvider
import com.huanchengfly.tieba.post.core.network.session.DeviceInfoProvider
import com.huanchengfly.tieba.post.core.network.session.OAIDProvider
import com.huanchengfly.tieba.post.core.network.util.CacheUtil.base64Encode
import com.huanchengfly.tieba.post.core.network.util.ImageUtil
import com.huanchengfly.tieba.post.core.network.util.UIDManager
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.URLEncoder

internal class MixedTiebaApiImpl(
    internal val clientConfigProvider: ClientConfigProvider,
    internal val credentialProvider: CredentialProvider,
    internal val deviceInfoProvider: DeviceInfoProvider,
    internal val contentResolver: ContentResolver,
    internal val oaidProvider: OAIDProvider,
    internal val uidManager: UIDManager,
    internal val retrofitTiebaApi: RetrofitTiebaApi,
) : ITiebaApi {
    override fun personalized(loadType: Int, page: Int): Call<PersonalizedBean> =
        retrofitTiebaApi.MINI_TIEBA_API.personalized(
            loadType,
            page,
            client_user_token = credentialProvider.getUid(),
            scr_dip = deviceInfoProvider.density.toString(),
            scr_h = deviceInfoProvider.screenHeight.toString(),
            scr_w = deviceInfoProvider.screenWidth.toString(),
        )

    override fun personalizedAsync(
        loadType: Int,
        page: Int
    ): Deferred<ApiResult<PersonalizedBean>> =
        retrofitTiebaApi.MINI_TIEBA_API.personalizedAsync(
            loadType,
            page,
            client_user_token = credentialProvider.getUid(),
            scr_dip = deviceInfoProvider.density.toString(),
            scr_h = deviceInfoProvider.screenHeight.toString(),
            scr_w = deviceInfoProvider.screenWidth.toString(),
        )

    override fun personalizedFlow(loadType: Int, page: Int): Flow<PersonalizedBean> {
        return retrofitTiebaApi.OFFICIAL_TIEBA_API.personalizedFlow(
            loadType,
            page,
            client_user_token = credentialProvider.getUid(),
            scr_dip = deviceInfoProvider.density.toString(),
            scr_h = deviceInfoProvider.screenHeight.toString(),
            scr_w = deviceInfoProvider.screenWidth.toString()
        )
    }

    override fun personalizedProtoFlow(loadType: Int, page: Int): Flow<PersonalizedResponse> {
        return retrofitTiebaApi.OFFICIAL_PROTOBUF_TIEBA_V12_API.personalizedFlow(
            buildProtobufRequestBody(
                data = PersonalizedRequest(
                    PersonalizedRequestData(
                        app_pos = buildAppPosInfo(),
                        common = buildCommonRequest(clientVersion = ClientVersion.TIEBA_V12),
                        load_type = loadType,
                        pn = page,
                        need_tags = 0,
                        page_thread_count = 11,
                        pre_ad_thread_count = 0,
                        sug_count = 0,
                        tag_code = 0,
                        q_type = 1,
                        need_forumlist = 0,
                        new_net_type = 1,
                        new_install = 0,
                        request_times = 0,
                        invoke_source = "",
                        scr_dip = deviceInfoProvider.density.toDouble(),
                        scr_h = deviceInfoProvider.screenHeight,
                        scr_w = deviceInfoProvider.screenWidth
                    )
                ),
                clientVersion = ClientVersion.TIEBA_V12
            )
        )
    }

    override fun myProfileAsync(): Deferred<ApiResult<com.huanchengfly.tieba.post.core.network.model.web.Profile>> =
        retrofitTiebaApi.WEB_TIEBA_API.myProfileAsync("json", "", "")

    override fun opAgree(
        threadId: String,
        postId: String,
        opType: Int
    ): Call<AgreeBean> =
        retrofitTiebaApi.MINI_TIEBA_API.agree(
            postId,
            threadId,
            client_user_token = credentialProvider.getUid(),
            op_type = opType,
            tbs = credentialProvider.getTbs(),
            stoken = credentialProvider.getSToken(),
        )

    override fun disagree(
        threadId: String,
        postId: String,
        opType: Int
    ): Call<AgreeBean> =
        retrofitTiebaApi.MINI_TIEBA_API.disagree(
            postId,
            threadId,
            client_user_token = credentialProvider.getUid(),
            op_type = opType,
            tbs = credentialProvider.getTbs()!!,
            stoken = credentialProvider.getSToken()!!,
        )

    override fun opAgreeFlow(
        threadId: String,
        postId: String,
        opType: Int,
        objType: Int,
        agreeType: Int,
    ): Flow<AgreeBean> =
        retrofitTiebaApi.MINI_TIEBA_API.opAgreeFlow(
            threadId,
            postId,
            opType = opType,
            clientUserToken = credentialProvider.getUid(),
            objType = objType,
            agreeType = agreeType,
            tbs = credentialProvider.getTbs(),
            stoken = credentialProvider.getSToken(),
        )

    override fun disagreeFlow(
        threadId: String,
        postId: String,
        opType: Int
    ): Flow<AgreeBean> =
        retrofitTiebaApi.MINI_TIEBA_API.disagreeFlow(
            postId,
            threadId,
            op_type = opType,
            client_user_token = credentialProvider.getUid(),
            tbs = credentialProvider.getTbs() ?: throw TiebaNotLoggedInException(),
            stoken = credentialProvider.getSToken() ?: throw TiebaNotLoggedInException(),
        )

    override fun forumRecommend(): Call<ForumRecommend> =
        retrofitTiebaApi.MINI_TIEBA_API.forumRecommend()

    override fun forumRecommendAsync(): Deferred<ApiResult<ForumRecommend>> =
        retrofitTiebaApi.MINI_TIEBA_API.forumRecommendAsync()

    override fun forumRecommendFlow(): Flow<ForumRecommend> =
        retrofitTiebaApi.MINI_TIEBA_API.forumRecommendFlow()

    override fun floor(
        threadId: String, page: Int, postId: String?, subPostId: String?
    ): Call<SubFloorListBean> =
        retrofitTiebaApi.OFFICIAL_TIEBA_API.floor(threadId, page, postId, subPostId)

    override fun forumHomeAsync(sortType: Int, page: Int): Deferred<ApiResult<ForumHome>> {
        return retrofitTiebaApi.WEB_TIEBA_API.getForumHomeAsync(
            sortType,
            page,
            20,
            "",
            ""
        )
    }

    override fun userLikeForum(
        uid: String, page: Int
    ): Call<UserLikeForumBean> {
        val myUid = credentialProvider.getUid()
        return retrofitTiebaApi.MINI_TIEBA_API.userLikeForum(
            page = page,
            uid = myUid,
            friendUid = if (!TextUtils.equals(uid, myUid)) uid else null,
            is_guest = if (!TextUtils.equals(uid, myUid)) "1" else null
        )
    }

    override fun userPost(
        uid: String, page: Int, isThread: Boolean
    ): Call<UserPostBean> =
        retrofitTiebaApi.MINI_TIEBA_API.userPost(uid, page, if (isThread) 1 else 0)

    override fun picPage(
        forumId: String,
        forumName: String,
        threadId: String,
        seeLz: Boolean,
        picId: String,
        picIndex: String,
        objType: String,
        prev: Boolean
    ): Call<PicPageBean> = retrofitTiebaApi.MINI_TIEBA_API.picPage(
        forumId,
        forumName,
        threadId,
        picId,
        picIndex,
        objType,
        next = if (prev) 0 else 10,
        myUid = credentialProvider.getUid(),
        scr_h = deviceInfoProvider.screenHeight.toString(),
        scr_w = deviceInfoProvider.screenWidth.toString(),
        prev = if (prev) 10 else 0,
        not_see_lz = if (seeLz) 0 else 1
    )

    override fun picPageFlow(
        forumId: String,
        forumName: String,
        threadId: String,
        seeLz: Boolean,
        picId: String,
        picIndex: String,
        objType: String,
        prev: Boolean
    ): Flow<PicPageBean> = retrofitTiebaApi.MINI_TIEBA_API.picPageFlow(
        forumId,
        forumName,
        threadId,
        picId,
        picIndex,
        objType,
        myUid = credentialProvider.getUid(),
        scr_h = deviceInfoProvider.screenHeight.toString(),
        scr_w = deviceInfoProvider.screenWidth.toString(),
        prev = if (prev) 10 else 0,
        next = if (prev) 0 else 10,
        not_see_lz = if (seeLz) 0 else 1
    )

    override fun profile(uid: String): Call<ProfileBean> =
        retrofitTiebaApi.MINI_TIEBA_API.profile(uid)

    override fun profileFlow(uid: String): Flow<Profile> =
        retrofitTiebaApi.OFFICIAL_TIEBA_API.profileFlow(
            sToken  = credentialProvider.getSToken()!!,
            tbs  = credentialProvider.getTbs()!!,
            userId = uid,
        )

    override fun unlikeForum(
        forumId: String,
        forumName: String,
        tbs: String
    ): Call<CommonResponse> = retrofitTiebaApi.MINI_TIEBA_API.unlikeForum(forumId, forumName, tbs)

    override fun unlikeForumFlow(
        forumId: String,
        forumName: String,
        tbs: String
    ): Flow<CommonResponse> =
        retrofitTiebaApi.OFFICIAL_TIEBA_API.unfavolike(
            forumId,
            forumName,
            tbs,
            client_user_token = credentialProvider.getUid()!!,
            stoken = credentialProvider.getSToken(),
        )

    override fun likeForum(
        forumId: String, forumName: String, tbs: String
    ): Call<LikeForumResultBean> =
        retrofitTiebaApi.MINI_TIEBA_API.likeForum(forumId, forumName, tbs)

    override fun likeForumFlow(
        forumId: String,
        forumName: String,
        tbs: String
    ): Flow<LikeForumResultBean> =
        retrofitTiebaApi.MINI_TIEBA_API.likeForumFlow(forumId, forumName, tbs)

    override fun signAsync(forumName: String, tbs: String): Deferred<ApiResult<SignResultBean>> =
        retrofitTiebaApi.MINI_TIEBA_API.signAsync(forumName, tbs)

    override fun signFlow(forumId: String, forumName: String, tbs: String): Flow<SignResultBean> =
        retrofitTiebaApi.OFFICIAL_TIEBA_API.signFlow(forumId, forumName, tbs, client_user_token = credentialProvider.getUid()!!)

    override fun delThread(
        forumId: String,
        forumName: String,
        threadId: String,
        tbs: String
    ): Call<CommonResponse> =
        retrofitTiebaApi.MINI_TIEBA_API.delThread(forumId, forumName, threadId, tbs)

    override fun delThreadFlow(
        forumId: Long,
        forumName: String,
        threadId: Long,
        tbs: String?,
        delMyThread: Boolean,
        isHide: Boolean,
    ): Flow<CommonResponse> =
        retrofitTiebaApi.OFFICIAL_TIEBA_API
            .delThreadFlow(
                forumId,
                forumName,
                threadId,
                tbs,
                deleteMyThread = if (delMyThread) 1 else 0,
                isFrsMask = if (isHide) 1 else 0
            )

    override fun delPost(
        forumId: String,
        forumName: String,
        threadId: String,
        postId: String,
        tbs: String,
        isFloor: Boolean,
        delMyPost: Boolean
    ): Call<CommonResponse> =
        retrofitTiebaApi.MINI_TIEBA_API.delPost(
            forumId,
            forumName,
            threadId,
            postId,
            tbs,
            is_floor = if (isFloor) 1 else 0,
            src = if (isFloor) 3 else 1,
            is_vip_del = if (delMyPost) 0 else 1,
            delete_my_post = if (delMyPost) 1 else 0
        )

    override fun delPostFlow(
        forumId: Long,
        forumName: String,
        threadId: Long,
        postId: Long,
        tbs: String?,
        isFloor: Boolean,
        delMyPost: Boolean
    ): Flow<CommonResponse> =
        retrofitTiebaApi.OFFICIAL_TIEBA_API
            .delPostFlow(
                forumId,
                forumName,
                threadId,
                postId,
                isFloor = if (isFloor) 1 else 0,
                src = if (isFloor) 3 else 1,
                isVipDel = if (delMyPost) 0 else 1,
                deleteMyPost = if (delMyPost) 1 else 0,
                tbs = tbs
            )

    override fun searchPost(
        keyword: String,
        forumName: String,
        onlyThread: Boolean,
        sortMode: Int,
        page: Int,
        pageSize: Int
    ): Call<SearchPostBean> = retrofitTiebaApi.MINI_TIEBA_API.searchPost(
        keyword,
        forumName,
        page,
        pageSize,
        only_thread = if (onlyThread) 1 else 0,
        sortMode = sortMode
    )

    override fun searchPostAsync(
        keyword: String,
        forumName: String,
        onlyThread: Boolean,
        sortMode: Int,
        page: Int,
        pageSize: Int
    ): Deferred<ApiResult<SearchPostBean>> = retrofitTiebaApi.MINI_TIEBA_API.searchPostAsync(
        keyword,
        forumName,
        page,
        pageSize,
        only_thread = if (onlyThread) 1 else 0,
        sortMode = sortMode
    )

    override fun searchUser(keyword: String): Call<SearchUserBean> =
        retrofitTiebaApi.MINI_TIEBA_API.searchUser(keyword, client_user_token = credentialProvider.getUid())

    override fun searchUserFlow(keyword: String): Flow<SearchUserBean> =
        retrofitTiebaApi.HYBRID_TIEBA_API.searchUserFlow(keyword)

    override fun msg(): Call<MsgBean> = retrofitTiebaApi.NEW_TIEBA_API.msg()

    override fun msgFlow(): Flow<MsgBean> = retrofitTiebaApi.NEW_TIEBA_API.msgFlow()

    override fun threadStore(page: Int, pageSize: Int): Call<ThreadStoreBean> =
        retrofitTiebaApi.NEW_TIEBA_API.threadStore(
            pageSize,
            pageSize * page,
            credentialProvider.getUid()
        )

    override fun threadStoreFlow(page: Int, pageSize: Int): Flow<ThreadStoreBean> =
        retrofitTiebaApi.OFFICIAL_TIEBA_API.threadStoreFlow(
            pageSize,
            pageSize * page,
            client_user_token = credentialProvider.getUid()!!,
            stoken = credentialProvider.getSToken(),
            user_id = credentialProvider.getUid()!!,
        )

    override fun removeStore(threadId: String, tbs: String): Call<CommonResponse> =
        retrofitTiebaApi.NEW_TIEBA_API.removeStore(threadId, tbs)

    override fun removeStoreFlow(
        threadId: Long,
        forumId: Long?,
        tbs: String
    ): Flow<CommonResponse> =
        retrofitTiebaApi.OFFICIAL_TIEBA_API.removeStoreFlow(
            threadId = threadId.toString(),
            forumId = forumId?.toString() ?: "null",
            tbs = tbs,
            stoken = credentialProvider.getSToken()!!,
            user_id = credentialProvider.getUid()!!,
        )

    override fun removeStoreFlow(threadId: String): Flow<CommonResponse> =
        retrofitTiebaApi.OFFICIAL_TIEBA_API.removeStoreFlow(
            threadId,
            tbs = credentialProvider.getTbs()!!,
            stoken = credentialProvider.getSToken()!!,
            user_id = credentialProvider.getUid()!!,
        )

    override fun addStore(threadId: String, postId: String, tbs: String): Call<CommonResponse> =
        retrofitTiebaApi.NEW_TIEBA_API.addStore(
            listOf(
                CollectDataBean(
                    threadId,
                    postId,
                    "0",
                    "0"
                )
            ).toJson(),
            tbs
        )

    override fun addStoreAsync(threadId: Long, postId: Long): Deferred<ApiResult<CommonResponse>> =
        retrofitTiebaApi.OFFICIAL_TIEBA_API.addStoreAsync(
            listOf(
                NewCollectDataBean(
                    threadId.toString(),
                    postId.toString(),
                    status = 1
                )
            ).toJson(),
            stoken = credentialProvider.getSToken()!!,
            clientUserToken = credentialProvider.getUid()!!,
        )

    override fun addStoreFlow(threadId: Long, postId: Long): Flow<CommonResponse> =
        retrofitTiebaApi.OFFICIAL_TIEBA_API.addStoreFlow(
            listOf(
                NewCollectDataBean(
                    threadId.toString(),
                    postId.toString(),
                    status = 1
                )
            ).toJson(),
            stoken = credentialProvider.getSToken()!!,
            client_user_token = credentialProvider.getUid()!!,
        )


    override fun replyMe(page: Int): Call<MessageListBean> =
        retrofitTiebaApi.NEW_TIEBA_API.replyMe(page)

    override fun replyMeAsync(page: Int): Deferred<ApiResult<MessageListBean>> =
        retrofitTiebaApi.NEW_TIEBA_API.replyMeAsync(page)

    override fun replyMeFlow(page: Int): Flow<MessageListBean> =
        retrofitTiebaApi.NEW_TIEBA_API.replyMeFlow(page)

    override fun atMe(page: Int): Call<MessageListBean> = retrofitTiebaApi.NEW_TIEBA_API.atMe(page)

    override fun atMeAsync(page: Int): Deferred<ApiResult<MessageListBean>> =
        retrofitTiebaApi.NEW_TIEBA_API.atMeAsync(page)

    override fun atMeFlow(page: Int): Flow<MessageListBean> =
        retrofitTiebaApi.NEW_TIEBA_API.atMeFlow(page)

    override fun agreeMe(page: Int): Call<MessageListBean> =
        retrofitTiebaApi.NEW_TIEBA_API.agreeMe(page)

    override fun threadContent(
        threadId: String, page: Int, seeLz: Boolean, reverse: Boolean
    ): Call<ThreadContentBean> = retrofitTiebaApi.OFFICIAL_TIEBA_API.threadContent(
        threadId,
        page,
        last = if (reverse) "1" else null,
        r = if (reverse) "1" else null,
        lz = if (seeLz) 1 else 0,
        scr_dip = deviceInfoProvider.density.toString(),
        scr_h = deviceInfoProvider.screenHeight.toString(),
        scr_w = deviceInfoProvider.screenWidth.toString(),
    )

    override fun threadContent(
        threadId: String, postId: String?, seeLz: Boolean, reverse: Boolean
    ): Call<ThreadContentBean> = retrofitTiebaApi.OFFICIAL_TIEBA_API.threadContent(
        threadId,
        postId,
        last = if (reverse) "1" else null,
        r = if (reverse) "1" else null,
        lz = if (seeLz) 1 else 0,
        scr_dip = deviceInfoProvider.density.toString(),
        scr_h = deviceInfoProvider.screenHeight.toString(),
        scr_w = deviceInfoProvider.screenWidth.toString(),
    )

    override fun threadContentAsync(
        threadId: String,
        page: Int,
        seeLz: Boolean,
        reverse: Boolean
    ): Deferred<ApiResult<ThreadContentBean>> =
        retrofitTiebaApi.OFFICIAL_TIEBA_API.threadContentAsync(
            threadId,
            page,
            last = if (reverse) "1" else null,
            r = if (reverse) "1" else null,
            lz = if (seeLz) 1 else 0,
            scr_dip = deviceInfoProvider.density.toString(),
            scr_h = deviceInfoProvider.screenHeight.toString(),
            scr_w = deviceInfoProvider.screenWidth.toString(),
        )

    override fun threadContentAsync(
        threadId: String,
        postId: String?,
        seeLz: Boolean,
        reverse: Boolean
    ): Deferred<ApiResult<ThreadContentBean>> =
        retrofitTiebaApi.OFFICIAL_TIEBA_API.threadContentAsync(
            threadId,
            postId,
            last = if (reverse) "1" else null,
            r = if (reverse) "1" else null,
            lz = if (seeLz) 1 else 0,
            scr_dip = deviceInfoProvider.density.toString(),
            scr_h = deviceInfoProvider.screenHeight.toString(),
            scr_w = deviceInfoProvider.screenWidth.toString(),
        )

    override fun submitDislike(
        dislikeBean: DislikeBean,
        stoken: String
    ): Call<CommonResponse> =
        retrofitTiebaApi.OFFICIAL_TIEBA_API.submitDislike(
            listOf(dislikeBean).toJson(),
            stoken = stoken,
        )

    override fun submitDislikeFlow(dislikeBean: DislikeBean): Flow<CommonResponse> =
        retrofitTiebaApi.OFFICIAL_TIEBA_API.submitDislikeFlow(
            listOf(dislikeBean).toJson(),
            stoken = credentialProvider.getSToken()!!
        )

    override fun follow(
        portrait: String, tbs: String
    ): Call<CommonResponse> = retrofitTiebaApi.WEB_TIEBA_API.follow(
        "https://tieba.baidu.com/i/?portrait=${
            URLEncoder.encode(
                portrait,
                "UTF-8"
            )
        }&cuid=&auth=&uid=&ssid=&from=&uid=&pu=&bd_page_type=2&auth=&originid=&mo_device=1&tbs=${tbs}&action=follow&op=follow"
    )

    override fun unfollow(
        portrait: String,
        tbs: String
    ): Call<CommonResponse> = retrofitTiebaApi.WEB_TIEBA_API.follow(
        "https://tieba.baidu.com/i/?portrait=${
            URLEncoder.encode(
                portrait,
                "UTF-8"
            )
        }&cuid=&auth=&uid=&ssid=&from=&uid=&pu=&bd_page_type=2&auth=&originid=&mo_device=1&tbs=${tbs}&action=follow&op=unfollow"
    )

    override fun followFlow(
        portrait: String,
        tbs: String
    ): Flow<FollowBean> = retrofitTiebaApi.OFFICIAL_TIEBA_API.followFlow(
        portrait,
        tbs,
        stoken = credentialProvider.getSToken() ?: throw TiebaNotLoggedInException(),
    )

    override fun unfollowFlow(
        portrait: String,
        tbs: String
    ): Flow<CommonResponse> = retrofitTiebaApi.OFFICIAL_TIEBA_API.unfollowFlow(
        portrait,
        tbs,
        stoken = credentialProvider.getSToken() ?: throw TiebaNotLoggedInException(),
    )

    override fun followListFlow(page: Int, uid: Long?): Flow<FollowListBean> =
        retrofitTiebaApi.OFFICIAL_TIEBA_API.followListFlow(page, uid)

    override fun getAllFollowFlow(uid: Long?): Flow<FollowListBean> = flow {
        var currentPage = 1
        var hasMore = true
        var finalBean: FollowListBean? = null
        val allUsers = mutableListOf<FollowListBean.FollowUserBean>()

        while (hasMore) {
            val response = followListFlow(currentPage, uid).first()
            if (finalBean == null) {
                finalBean = response
            }
            allUsers.addAll(response.followList)
            hasMore = response.hasMore == 1
            currentPage++
        }

        finalBean?.apply {
            this.followList = allUsers
        }?.let {
            emit(it)
        }
    }.flowOn(Dispatchers.IO)

    override fun hotMessageList(): Call<HotMessageListBean> =
        retrofitTiebaApi.WEB_TIEBA_API.hotMessageList()

    override fun myInfo(cookie: String): Call<MyInfoBean> =
        retrofitTiebaApi.WEB_TIEBA_API.myInfo(cookie)

    override fun myInfoAsync(cookie: String): Deferred<ApiResult<MyInfoBean>> =
        retrofitTiebaApi.WEB_TIEBA_API.myInfoAsync(cookie)

    override fun myInfoFlow(cookie: String): Flow<MyInfoBean> =
        retrofitTiebaApi.WEB_TIEBA_API.myInfoFlow(cookie)

    override fun searchForum(keyword: String): Call<SearchForumBean> =
        retrofitTiebaApi.WEB_TIEBA_API.searchForum(keyword)

    override fun searchForumFlow(keyword: String): Flow<SearchForumBean> =
        retrofitTiebaApi.HYBRID_TIEBA_API.searchForumFlow(keyword)

    override fun searchThreadFlow(
        keyword: String, page: Int, sort: Int,
    ): Flow<SearchThreadBean> =
        retrofitTiebaApi.HYBRID_TIEBA_API.searchThreadFlow(
            keyword,
            page,
            sort
        )

    override fun topicDetailFlow(
        topicId: String,
        topicName: String,
        isNew: Int,
        isShare: Int,
        page: Int,
        pageSize: Int,
        offset: Int,
        lastId: String
    ): Flow<TopicDetailBean> =
        retrofitTiebaApi.HYBRID_TIEBA_API.topicDetailFlow(
            topicId,
            topicName,
            isNew,
            isShare,
            page,
            pageSize,
            offset,
            lastId
        )

    override fun searchPostFlow(
        keyword: String,
        forumName: String,
        forumId: Long,
        sortType: Int,
        filterType: Int,
        page: Int,
        pageSize: Int,
    ): Flow<SearchThreadBean> =
        retrofitTiebaApi.HYBRID_TIEBA_API.searchThreadFlow(
            keyword,
            page,
            sortType,
            filterType,
            pageSize,
            forumName,
            ct = 2,
            isUseZonghe = null,
            clientVersion = ClientVersion.TIEBA_V12.version,
            referer = "https://tieba.baidu.com/mo/q/hybrid-usergrow-search/searchGlobal?entryPage=frs&loadingSignal=1&forumName=${forumName.urlEncode()}&forumId=$forumId&customfullscreen=1&nonavigationbar=1&cuid=${uidManager.newCUID}&cuid_galaxy2=${uidManager.newCUID}&cuid_gid=&timestamp=${System.currentTimeMillis()}&_client_version=${ClientVersion.TIEBA_V12.version}&_client_type=2"
        )

    override fun webUploadPic(photoInfoBean: PhotoInfoBean): Call<WebUploadPicBean> {
        var base64: String? = null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            base64 = ImageUtil.imageToBase64(photoInfoBean.file)
        } else {
            try {
                contentResolver.openAssetFileDescriptor(
                    photoInfoBean.fileUri,
                    "r"
                )?.use { afd ->
                    base64 =
                        ImageUtil.imageToBase64(FileInputStream(afd.parcelFileDescriptor.fileDescriptor))
                }
            } catch (e: IOException) {
                e.printStackTrace()
                base64 = null
            }
        }
        return retrofitTiebaApi.WEB_TIEBA_API.webUploadPic(base64)
    }

    override fun webReply(
        forumId: String,
        forumName: String,
        threadId: String,
        tbs: String,
        content: String,
        imgInfo: String?,
        nickName: String,
        pn: String,
        bsk: String
    ): Call<WebReplyResultBean> =
        retrofitTiebaApi.WEB_TIEBA_API.webReply(
            content = content,
            imgInfo = imgInfo ?: "",
            forumId = forumId,
            forumName = forumName,
            tbs = tbs,
            threadId = threadId,
            nickName = nickName,
            bsk = bsk,
            referer = "https://tieba.baidu.com/p/$threadId?lp=5028&mo_device=1&is_jingpost=0&pn=$pn&"
        )

    override fun webReply(
        forumId: String,
        forumName: String,
        threadId: String,
        tbs: String,
        content: String,
        imgInfo: String?,
        nickName: String,
        postId: String,
        floor: String,
        pn: String,
        bsk: String
    ): Call<WebReplyResultBean> =
        retrofitTiebaApi.WEB_TIEBA_API.webReply(
            content = content,
            imgInfo = imgInfo ?: "",
            forumId = forumId,
            forumName = forumName,
            tbs = tbs,
            threadId = threadId,
            nickName = nickName,
            postId = postId,
            floor = floor,
            bsk = bsk,
            referer = "https://tieba.baidu.com/p/$threadId?lp=5028&mo_device=1&is_jingpost=0&pn=$pn&"
        )

    override fun webReply(
        forumId: String,
        forumName: String,
        threadId: String,
        tbs: String,
        content: String,
        imgInfo: String?,
        nickName: String,
        postId: String,
        replyPostId: String,
        floor: String,
        pn: String,
        bsk: String
    ): Call<WebReplyResultBean> =
        retrofitTiebaApi.WEB_TIEBA_API.webReply(
            content = content,
            imgInfo = imgInfo ?: "",
            forumId = forumId,
            forumName = forumName,
            tbs = tbs,
            threadId = threadId,
            nickName = nickName,
            postId = postId,
            replyPostId = replyPostId,
            floor = floor,
            bsk = bsk,
            referer = "https://tieba.baidu.com/p/$threadId?lp=5028&mo_device=1&is_jingpost=0&pn=$pn&"
        )

    override fun webReplyAsync(
        forumId: String,
        forumName: String,
        threadId: String,
        tbs: String,
        content: String,
        imgInfo: String?,
        nickName: String,
        pn: String,
        bsk: String
    ): Deferred<ApiResult<WebReplyResultBean>> =
        retrofitTiebaApi.WEB_TIEBA_API.webReplyAsync(
            content = content,
            imgInfo = imgInfo ?: "",
            forumId = forumId,
            forumName = forumName,
            tbs = tbs,
            threadId = threadId,
            nickName = nickName,
            bsk = bsk,
            referer = "https://tieba.baidu.com/p/$threadId?lp=5028&mo_device=1&is_jingpost=0&pn=$pn&"
        )

    override fun webReplyAsync(
        forumId: String,
        forumName: String,
        threadId: String,
        tbs: String,
        content: String,
        imgInfo: String?,
        nickName: String,
        postId: String,
        floor: String,
        pn: String,
        bsk: String
    ): Deferred<ApiResult<WebReplyResultBean>> =
        retrofitTiebaApi.WEB_TIEBA_API.webReplyAsync(
            content = content,
            imgInfo = imgInfo ?: "",
            forumId = forumId,
            forumName = forumName,
            tbs = tbs,
            threadId = threadId,
            nickName = nickName,
            postId = postId,
            floor = floor,
            bsk = bsk,
            referer = "https://tieba.baidu.com/p/$threadId?lp=5028&mo_device=1&is_jingpost=0&pn=$pn&"
        )

    override fun webReplyAsync(
        forumId: String,
        forumName: String,
        threadId: String,
        tbs: String,
        content: String,
        imgInfo: String?,
        nickName: String,
        postId: String,
        replyPostId: String,
        floor: String,
        pn: String,
        bsk: String
    ): Deferred<ApiResult<WebReplyResultBean>> =
        retrofitTiebaApi.WEB_TIEBA_API.webReplyAsync(
            content = content,
            imgInfo = imgInfo ?: "",
            forumId = forumId,
            forumName = forumName,
            tbs = tbs,
            threadId = threadId,
            nickName = nickName,
            postId = postId,
            replyPostId = replyPostId,
            floor = floor,
            bsk = bsk,
            referer = "https://tieba.baidu.com/p/$threadId?lp=5028&mo_device=1&is_jingpost=0&pn=$pn&"
        )

    override suspend fun checkReportPost(postId: String): CheckReportBean =
        retrofitTiebaApi.OFFICIAL_TIEBA_API.checkReport(
            category = "1",
            reportParam = mapOf(
                "pid" to postId
            ),
            stoken = credentialProvider.getSToken(),
        )

    override fun initNickNameFlow(): Flow<InitNickNameBean> =
        retrofitTiebaApi.OFFICIAL_TIEBA_API.initNickNameFlow(
            bduss = credentialProvider.getBduss()!!,
            sToken = credentialProvider.getSToken()!!,
        )

    override fun initNickNameFlow(bduss: String, sToken: String): Flow<InitNickNameBean> =
        retrofitTiebaApi.OFFICIAL_TIEBA_API.initNickNameFlow(bduss, sToken)

    override fun loginFlow(): Flow<LoginBean> =
        retrofitTiebaApi.OFFICIAL_TIEBA_API.loginFlow(
            bdusstoken = "${credentialProvider.getBduss()!!}|null",
            sToken  = credentialProvider.getSToken()!!,
            userId = credentialProvider.getUid(),
        )

    override fun loginFlow(bduss: String, sToken: String): Flow<LoginBean> =
        retrofitTiebaApi.OFFICIAL_TIEBA_API.loginFlow("$bduss|", sToken, null)

    override fun profileModifyFlow(
        birthdayShowStatus: Boolean,
        birthdayTime: String,
        intro: String,
        sex: String,
        nickName: String,
    ): Flow<CommonResponse> =
        retrofitTiebaApi.OFFICIAL_TIEBA_API.profileModify(
            birthdayShowStatus.booleanToString(),
            birthdayTime,
            intro,
            sex,
            nickName,
            sToken  = credentialProvider.getSToken()!!,
        )

    override fun imgPortrait(file: File): Flow<CommonResponse> {
        return retrofitTiebaApi.OFFICIAL_TIEBA_API.imgPortrait(
            MyMultipartBody.Builder("--------7da3d81520810*").apply {
                setType(MyMultipartBody.FORM)
                addFormDataPart(Param.CLIENT_VERSION, ClientVersion.TIEBA_V12.version)
                addFormDataPart("pic", "file", file.asRequestBody())
            }.build()
        )
    }

    override fun getForumListFlow(): Flow<GetForumListBean> =
        retrofitTiebaApi.OFFICIAL_TIEBA_API.getForumListFlow(
            bduss = credentialProvider.getBduss()!!,
            stoken = credentialProvider.getSToken()!!,
            userId = credentialProvider.getUid()!!
        )

    override fun mSign(
        forumIds: String,
        tbs: String
    ): Flow<MSignBean> =
        retrofitTiebaApi.OFFICIAL_TIEBA_API.mSignFlow(
            forumIds,
            tbs,
            stoken = credentialProvider.getSToken()!!,
            userId = credentialProvider.getUid()!!
        )

    override fun userLikeFlow(
        pageTag: String,
        lastRequestUnix: Long,
        loadType: Int
    ): Flow<UserLikeResponse> {
        return retrofitTiebaApi.OFFICIAL_PROTOBUF_TIEBA_API.userLikeFlow(
            buildProtobufRequestBody(
                UserLikeRequest(
                    UserLikeRequestData(
                        common = buildCommonRequest(),
                        pageTag = pageTag,
                        lastRequestUnix = lastRequestUnix,
                        followType = 1,
                        loadType = loadType
                    )
                )
            )
        )
    }

    override fun hotThreadListFlow(tabCode: String): Flow<HotThreadListResponse> {
        return retrofitTiebaApi.OFFICIAL_PROTOBUF_TIEBA_API.hotThreadListFlow(
            buildProtobufRequestBody(
                HotThreadListRequest(
                    HotThreadListRequestData(
                        common = buildCommonRequest(),
                        tabCode = tabCode,
                        tabId = "1"
                    )
                )
            )
        )
    }

    override fun topicListFlow(): Flow<TopicListResponse> {
        return retrofitTiebaApi.OFFICIAL_PROTOBUF_TIEBA_API.topicListFlow(
            buildProtobufRequestBody(
                TopicListRequest(
                    TopicListRequestData(
                        common = buildCommonRequest(),
                        call_from = "newbang",
                        list_type = "all",
                        need_tab_list = "0",
                        fid = 0L
                    )
                )
            )
        )
    }

    override fun forumRecommendNewFlow(
        sortType: Int
    ): Flow<ForumRecommendResponse> {
        return retrofitTiebaApi.OFFICIAL_PROTOBUF_TIEBA_API.forumRecommendFlow(
            buildProtobufRequestBody(
                ForumRecommendRequest(
                    ForumRecommendRequestData(
                        common = buildCommonRequest(),
                        like_forum = 1,
                        recommend = 1,
                        sort_type = sortType,
                        topic = 0
                    )
                )
            )
        )
    }

    override fun forumGuideNewFlow(
        sortType: Int,
    ): Flow<ForumGuideResponse> {
        return retrofitTiebaApi.OFFICIAL_PROTOBUF_TIEBA_API.forumGuideFlow(
            buildProtobufRequestBody(
                ForumGuideRequest(
                    ForumGuideRequestData(
                        sort_type = sortType,
                        call_from = 0
                    )
                ),
                clientVersion = ClientVersion.TIEBA_V12
            ),
        )
    }

    override fun frsPage(
        forumName: String,
        page: Int,
        loadType: Int,
        sortType: Int,
        goodClassifyId: Int?
    ): Flow<FrsPageResponse> {
        return retrofitTiebaApi.OFFICIAL_PROTOBUF_TIEBA_V12_API.frsPageFlow(
            buildProtobufRequestBody(
                FrsPageRequest(
                    FrsPageRequestData(
                        ad_param = buildAdParam(),
                        app_pos = buildAppPosInfo(),
                        call_from = 0,
                        category_id = 0,
                        cid = goodClassifyId ?: 0,
                        common = buildCommonRequest(clientVersion = ClientVersion.TIEBA_V12),
                        ctime = 0,
                        data_size = 0,
                        hot_thread_id = 0,
                        is_default_navtab = 0,
                        is_good = if (goodClassifyId != null) 1 else 0,
                        is_selection = 0,
                        kw = forumName.urlEncode(),
                        last_click_tid = 0,
                        load_type = loadType,
                        net_error = 0,
                        pn = page,
                        q_type = 2,
                        rn = 90,
                        rn_need = 30,
                        scr_dip = deviceInfoProvider.density.toDouble(),
                        scr_h = deviceInfoProvider.screenHeight,
                        scr_w = deviceInfoProvider.screenWidth,
                        sort_type = sortType,
                        st_param = 0,
                        st_type = "recom_flist",
                        up_schema = "",
                        with_group = 1,
                        yuelaou_locate = ""
                    )
                ),
                clientVersion = ClientVersion.TIEBA_V12
            ),
            forumName = forumName.urlEncode()
        )
    }

    override fun threadList(
        forumId: Long,
        forumName: String,
        page: Int,
        sortType: Int,
        threadIds: String
    ): Flow<ThreadListResponse> {
        return retrofitTiebaApi.OFFICIAL_PROTOBUF_TIEBA_V12_API.threadListFlow(
            buildProtobufRequestBody(
                ThreadListRequest(
                    ThreadListRequestData(
                        ad_param = AdParam(3, 0, null),
                        app_pos = buildAppPosInfo(),
                        common = buildCommonRequest(clientVersion = ClientVersion.TIEBA_V12),
                        scr_dip = deviceInfoProvider.density.toDouble(),
                        scr_h = deviceInfoProvider.screenHeight,
                        scr_w = deviceInfoProvider.screenWidth,
                        forum_id = forumId,
                        forum_name = forumName,
                        pn = page,
                        q_type = 2,
                        user_id = credentialProvider.getUid()?.toLongOrNull(),
                        thread_ids = threadIds,
                        sort_type = sortType,
                        need_abstract = 0,
                        st_type = 0,
                        last_click_tid = 0
                    )
                ),
                clientVersion = ClientVersion.TIEBA_V12
            )
        )
    }

    override fun generalTabList(
        forumId: Long,
        forumName: String,
        tabId: Int,
        tabType: Int,
        tabName: String,
        isGeneralTab: Int,
        pn: Int,
        sortType: Int,
        lastThreadId: Long,
        isDefaultNavTab: Int,
    ): Flow<GeneralTabListResponse> {
        return retrofitTiebaApi.OFFICIAL_PROTOBUF_TIEBA_POST_API.generalTabListFlow(
            buildProtobufRequestBody(
                GeneralTabListRequest(
                    GeneralTabListRequestData(
                        common = buildCommonRequest(clientVersion = ClientVersion.TIEBA_V12),
                        tab_id = tabId,
                        forum_id = forumId,
                        pn = pn,
                        rn = 30,
                        scr_w = deviceInfoProvider.screenWidth,
                        scr_h = deviceInfoProvider.screenHeight,
                        scr_dip = deviceInfoProvider.density.toInt(),
                        last_thread_id = lastThreadId,
                        is_default_navtab = isDefaultNavTab,
                        tab_name = tabName,
                        is_general_tab = isGeneralTab,
                        sort_type = sortType,
                        tab_type = tabType,
                        ad_ext_params = "",
                        ad_bear_context = "",
                        has_ad_bear = 0,
                        ad_bear_sid = "",
                        ad_bear_sid_price = 0.0,
                        request_times = 0,
                        frs_common_info = "",
                        is_newfrs = 1,
                        is_video_doublerow = 0,
                    )
                ),
                clientVersion = ClientVersion.TIEBA_V12
            )
        )
    }

    override fun syncFlow(clientId: String?): Flow<Sync> =
        retrofitTiebaApi.OFFICIAL_TIEBA_API.sync(
            clientId,
            phoneScreen = "${deviceInfoProvider.screenWidth},${deviceInfoProvider.screenHeight}",
            androidIdR = base64Encode(uidManager.getAndroidId("000")),
            imeiR = base64Encode(deviceInfoProvider.imei),
            scr_dip = deviceInfoProvider.density.toString(),
            scr_h = deviceInfoProvider.screenHeight.toString(),
            scr_w = deviceInfoProvider.screenWidth.toString(),
            sToken = credentialProvider.getSToken(),
            cookie = clientConfigProvider.getBaiduId()?.let { "ka=open;BAIDUID=$it" } ?: "ka=open"
        )

    override fun addPostFlow(
        content: String,
        forumId: String,
        forumName: String,
        threadId: String,
        tbs: String?,
        nameShow: String?,
        postId: String?,
        subPostId: String?,
        replyUserId: String?
    ): Flow<AddPostResponse> {
        return retrofitTiebaApi.OFFICIAL_PROTOBUF_TIEBA_POST_API
            .addPostFlow(
                buildProtobufRequestBody(
                    AddPostRequest(
                        AddPostRequestData(
                            anonymous = "1",
                            barrage_time = "0".takeIf { postId.isNullOrEmpty() },
                            can_no_forum = "0",
                            common = buildCommonRequest(
                                clientVersion = ClientVersion.TIEBA_V12_POST,
                                tbs = tbs ?: credentialProvider.getTbs()
                            ),
                            content = content,
                            entrance_type = "0",
                            fid = forumId,
                            floor_num = "0",
                            kw = forumName,
                            is_ad = "0",
                            is_addition = "0",
                            is_barrage = "0",
                            is_feedback = "0",
                            is_giftpost = "0",
                            is_pictxt = "0",
                            is_show_bless = 0,
                            is_twzhibo_thread = "0",
                            name_show = nameShow ?: credentialProvider.getNickname().orEmpty(),
                            new_vcode = "1",
                            post_from = if (postId.isNullOrEmpty() && subPostId.isNullOrEmpty()) "13" else if (subPostId.isNullOrEmpty()) "0" else null,
                            quote_id = postId,
                            reply_uid = replyUserId.takeIf { !postId.isNullOrEmpty() },
                            repostid = postId,
                            sub_post_id = subPostId,
                            show_custom_figure = 0,
                            takephoto_num = "0",
                            tid = threadId,
                            v_fid = "".takeIf { postId.isNullOrEmpty() },
                            v_fname = "".takeIf { postId.isNullOrEmpty() },
                            vcode_tag = "12",
                        )
                    ),
                    clientVersion = ClientVersion.TIEBA_V12_POST
                )
            )
    }

    override fun userProfileFlow(uid: Long): Flow<ProfileResponse> {
        val selfUid = credentialProvider.getUid()?.toLongOrNull()
        val isSelf = selfUid == uid
        return retrofitTiebaApi.OFFICIAL_PROTOBUF_TIEBA_V12_API.profileFlow(
            buildProtobufRequestBody(
                ProfileRequest(
                    ProfileRequestData(
                        common = buildCommonRequest(clientVersion = ClientVersion.TIEBA_V12),
                        friend_uid = uid.takeIf { !isSelf },
                        friend_uid_portrait = "",
                        has_plist = 1,
                        is_from_usercenter = 1,
                        is_guest = if (isSelf) 0 else 1,
                        need_post_count = 1,
                        page = 1,
                        pn = 1,
                        q_type = 0,
                        rn = 20,
                        scr_dip = deviceInfoProvider.density.toDouble(),
                        scr_h = deviceInfoProvider.screenHeight,
                        scr_w = deviceInfoProvider.screenWidth,
                        uid = selfUid,
                    )
                ),
                clientVersion = ClientVersion.TIEBA_V12
            )
        )
    }

    override fun pbPageFlow(
        threadId: Long,
        page: Int,
        postId: Long,
        seeLz: Boolean,
        back: Boolean,
        sortType: Int,
        forumId: Long?,
        stType: String,
        mark: Int,
        lastPostId: Long?,
    ): Flow<PbPageResponse> {
        return retrofitTiebaApi.OFFICIAL_PROTOBUF_TIEBA_V12_API.pbPageFlow(
            buildProtobufRequestBody(
                PbPageRequest(
                    PbPageRequestData(
                        common = buildCommonRequest(clientVersion = ClientVersion.TIEBA_V12),
                        kz = threadId,
                        pid = postId,
                        pn = page,
                        r = sortType,
                        lz = if (seeLz) 1 else 0,
                        forum_id = forumId ?: 0,
                        ad_param = com.huanchengfly.tieba.post.core.network.model.protos.pbPage.AdParam(
                            load_count = 0,
                            refresh_count = 1,
                            is_req_ad = 1
                        ),
                        mark = mark,
                        last_pid = lastPostId ?: 0,
                        app_pos = buildAppPosInfo(),
                        back = if (back) 1 else 0,
                        banner = 0,
                        broadcast_id = 0,
                        floor_rn = 4,
                        floor_sort_type = 1,
                        from_push = 0,
                        from_smart_frs = 0,
                        immersion_video_comment_source = 0,
                        is_comm_reverse = 0,
                        is_fold_comment_req = 0,
                        is_jumpfloor = 0,
                        jumpfloor_num = 0,
                        need_repost_recommend_forum = 0,
                        obj_locate = "",
                        obj_param1 = "10",
                        obj_source = "",
                        ori_ugc_type = 0,
                        pb_rn = 0,
                        q_type = 2,
                        request_times = 0,
                        rn = 15,
                        s_model = 0,
                        scr_dip = deviceInfoProvider.density.toDouble(),
                        scr_h = deviceInfoProvider.screenHeight,
                        scr_w = deviceInfoProvider.screenWidth,
                        similar_from = 0,
                        source_type = 2,
                        st_type = stType,
                        thread_type = 0,
                        weipost = 0,
                        with_floor = 1
                    )
                ),
                clientVersion = ClientVersion.TIEBA_V12
            )
        )
    }

    override fun pbFloorFlow(
        threadId: Long,
        postId: Long,
        forumId: Long,
        page: Int,
        subPostId: Long
    ): Flow<PbFloorResponse> {
        return retrofitTiebaApi.OFFICIAL_PROTOBUF_TIEBA_V12_API.pbFloorFlow(
            buildProtobufRequestBody(
                PbFloorRequest(
                    PbFloorRequestData(
                        common = buildCommonRequest(clientVersion = ClientVersion.TIEBA_V12),
                        forum_id = forumId,
                        kz = threadId,
                        pid = postId,
                        pn = page,
                        spid = subPostId,
                        scr_dip = deviceInfoProvider.density.toDouble(),
                        scr_h = deviceInfoProvider.screenHeight,
                        scr_w = deviceInfoProvider.screenWidth,
                        is_comm_reverse = 0,
                        ori_ugc_type = 0
                    )
                ),
                clientVersion = ClientVersion.TIEBA_V12,
                needSToken = false
            )
        )
    }

    override fun searchSuggestionsFlow(keyword: String, isForum: Boolean): Flow<SearchSugResponse> {
        return retrofitTiebaApi.OFFICIAL_PROTOBUF_TIEBA_V12_API.searchSugFlow(
            buildProtobufRequestBody(
                SearchSugRequest(
                    SearchSugRequestData(
                        common = buildCommonRequest(clientVersion = ClientVersion.TIEBA_V12),
                        word = keyword,
                        isforum = isForum.booleanToString()
                    )
                ),
                clientVersion = ClientVersion.TIEBA_V12,
                needSToken = true
            )
        )
    }

    override fun getForumDetailFlow(forumId: Long): Flow<GetForumDetailResponse> {
        return retrofitTiebaApi.OFFICIAL_PROTOBUF_TIEBA_V12_API.getForumDetailFlow(
            buildProtobufRequestBody(
                GetForumDetailRequest(
                    GetForumDetailRequestData(
                        common = buildCommonRequest(clientVersion = ClientVersion.TIEBA_V12),
                        forum_id = forumId,
                    )
                ),
                clientVersion = ClientVersion.TIEBA_V12,
                needSToken = true
            )
        )
    }

    override fun getBawuInfoFlow(forumId: Long): Flow<GetBawuInfoResponse> {
        return retrofitTiebaApi.OFFICIAL_PROTOBUF_TIEBA_V12_API.getBawuInfoFlow(
            buildProtobufRequestBody(
                GetBawuInfoRequest(
                    GetBawuInfoRequestData(
                        common = buildCommonRequest(clientVersion = ClientVersion.TIEBA_V12),
                        forum_id = forumId,
                    )
                ),
                clientVersion = ClientVersion.TIEBA_V12,
                needSToken = true
            )
        )
    }

    override fun getLevelInfoFlow(forumId: Long): Flow<GetLevelInfoResponse> {
        return retrofitTiebaApi.OFFICIAL_PROTOBUF_TIEBA_V12_API.getLevelInfoFlow(
            buildProtobufRequestBody(
                GetLevelInfoRequest(
                    GetLevelInfoRequestData(
                        common = buildCommonRequest(clientVersion = ClientVersion.TIEBA_V12),
                        forum_id = forumId,
                    )
                ),
                clientVersion = ClientVersion.TIEBA_V12,
                needSToken = true
            )
        )
    }

    override fun getMemberInfoFlow(forumId: Long): Flow<GetMemberInfoResponse> {
        return retrofitTiebaApi.OFFICIAL_PROTOBUF_TIEBA_V12_API.getMemberInfoFlow(
            buildProtobufRequestBody(
                GetMemberInfoRequest(
                    GetMemberInfoRequestData(
                        common = buildCommonRequest(clientVersion = ClientVersion.TIEBA_V12),
                        forum_id = forumId,
                    )
                ),
                clientVersion = ClientVersion.TIEBA_V12,
                needSToken = true
            )
        )
    }

    override fun forumRuleDetailFlow(forumId: Long): Flow<ForumRuleDetailResponse> {
        return retrofitTiebaApi.OFFICIAL_PROTOBUF_TIEBA_V12_API.forumRuleDetailFlow(
            buildProtobufRequestBody(
                ForumRuleDetailRequest(
                    ForumRuleDetailRequestData(
                        common = buildCommonRequest(clientVersion = ClientVersion.TIEBA_V12),
                        forum_id = forumId,
                    )
                ),
                clientVersion = ClientVersion.TIEBA_V12,
                needSToken = true
            )
        )
    }

    override fun userPostFlow(uid: Long, page: Int, isThread: Boolean): Flow<UserPostResponse> {
        return retrofitTiebaApi.OFFICIAL_PROTOBUF_TIEBA_V12_API.userPostFlow(
            buildProtobufRequestBody(
                UserPostRequest(
                    UserPostRequestData(
                        uid = uid,
                        rn = 20,
                        is_thread = if (isThread) 1 else 0,
                        need_content = 1,
                        pn = page,
                        common = buildCommonRequest(clientVersion = ClientVersion.TIEBA_V12),
                        scr_w = deviceInfoProvider.screenWidth,
                        scr_h = deviceInfoProvider.screenHeight,
                        scr_dip = deviceInfoProvider.density.toDouble(),
                        q_type = 1,
                        is_view_card = if (isThread) 1 else 0,
                        subtype = 0.takeUnless { isThread },
                    )
                ),
                clientVersion = ClientVersion.TIEBA_V12,
                needSToken = true
            )
        )
    }

    override fun userLikeForumFlow(uid: String, page: Int): Flow<UserLikeForumBean> {
        val myUid = credentialProvider.getUid()
        return retrofitTiebaApi.OFFICIAL_TIEBA_API.userLikeForumFlow(
            page = page,
            uid = myUid,
            friendUid = if (!TextUtils.equals(uid, myUid)) uid else null,
            is_guest = if (!TextUtils.equals(uid, myUid)) "1" else null
        )
    }

    override fun getUserInfoFlow(): Flow<GetUserInfoResponse> {
        return getUserInfoFlow(credentialProvider.getUid()!!.toLong(), null, null)
    }

    override fun getUserInfoFlow(
        uid: Long,
        bduss: String?,
        sToken: String?,
    ): Flow<GetUserInfoResponse> {
        return retrofitTiebaApi.OFFICIAL_PROTOBUF_TIEBA_V12_API.getUserInfoFlow(
            buildProtobufRequestBody(
                GetUserInfoRequest(
                    GetUserInfoRequestData(
                        common = buildCommonRequest(
                            clientVersion = ClientVersion.TIEBA_V12,
                            bduss = bduss,
                            stoken = sToken
                        ),
                        uid = uid,
                        scr_w = deviceInfoProvider.screenWidth
                    )
                ),
                clientVersion = ClientVersion.TIEBA_V12,
                needSToken = true
            )
        )
    }

    override fun getHistoryForumFlow(history: String): Flow<GetHistoryForumResponse> {
        return retrofitTiebaApi.OFFICIAL_PROTOBUF_TIEBA_V12_API.getHistoryForumFlow(
            buildProtobufRequestBody(
                GetHistoryForumRequest(
                    GetHistoryForumRequestData(
                        common = buildCommonRequest(clientVersion = ClientVersion.TIEBA_V12),
                        history = history,
                    )
                ),
                clientVersion = ClientVersion.TIEBA_V12,
                needSToken = true
            )
        )
    }

    override fun addThreadFlow(
        threadContent: String,
        kw: String,
        fid: String,
        title: String,
        isHide: Int,
        isTitle: Int
    ): Flow<AddThreadBean> =
    retrofitTiebaApi.MINI_TIEBA_API.addThreadFlow(
        threadContent,
        kw,
        fid,
        title,
        isHide,
        isTitle,
        z_id = credentialProvider.getZid() ?: throw TiebaNotLoggedInException(),
        nameShow = credentialProvider.getNickname(),
        clientUserToken = credentialProvider.getUid() ?: throw TiebaNotLoggedInException(),
        tbs = credentialProvider.getTbs(),
        stoken = credentialProvider.getSToken(),
    )

    override fun setUserBlackFlow(
        blackUid: Long,
        tbs: String,
        permList: PermissionListBean
    ): Flow<CommonResponse> =
        retrofitTiebaApi.OFFICIAL_TIEBA_API.setUserBlackFlow(
            blackUid,
            tbs,
            permList.toJson(),
            stoken = credentialProvider.getSToken()!!,
        )

    override fun getUserBlackInfoFlow(
        blackUid: Long
    ): Flow<GetUserBlackInfoBean> =
        retrofitTiebaApi.OFFICIAL_TIEBA_API.getUserBlackFlow(
            blackUid,
            stoken = credentialProvider.getSToken()!!,
        )

    override fun forumGuideFlow(
        sortType: Int?,
        callFrom: Int?,
        pageNo: Int,
        resNum: Int,
        topForumNum: Int?,
    ): Flow<ForumGuideBean> =
        retrofitTiebaApi.OFFICIAL_TIEBA_API.forumGuideFlow(
            sortType,
            callFrom,
            pageNo,
            resNum,
            topForumNum,
            tbs = credentialProvider.getTbs(),
            stoken = credentialProvider.getSToken()!!,
        )

    /**
     * 关注吧列表
     * @param sortType 排序方式
     * @param callFrom 1来自主页?(包含热搜数据),3 来自签到页?
     */
    override fun allForumGuideFlow(
        sortType: Int?,
        callFrom: Int?,
    ): Flow<ForumGuideBean> = flow {
        var currentPage = 1
        var hasMore = true
        var finalBean: ForumGuideBean? = null
        val allLikeForums = mutableListOf<ForumGuideBean.LikeForum>()

        while (hasMore) {
            val response = forumGuideFlow(
                sortType = sortType,
                callFrom = callFrom,
                pageNo = currentPage,
                resNum = 50,
                topForumNum = 0
            ).first()
            if (finalBean == null) {
                finalBean = response
            }
            response.likeForum.let { allLikeForums.addAll(it) }
            hasMore = response.likeForumHasMore == true
            currentPage++
        }

        finalBean!!.likeForum = allLikeForums
        emit(finalBean)
    }.flowOn(Dispatchers.IO)

    override fun addPollPost(forumId: Long?, threadId: Long, option: String): Flow<CommonResponse> =
        retrofitTiebaApi.HYBRID_TIEBA_API.addPollPost(
            forumId,
            threadId,
            option
        )

    override fun addPollPostProtobuf(
        forumId: Long?,
        threadId: Long,
        option: String
    ): Flow<AddPollPostReponse> =
        retrofitTiebaApi.OFFICIAL_PROTOBUF_TIEBA_POST_API.addPollPostProtobuf(
            buildProtobufRequestBody(
                AddPollPostRequest(
                    AddPollPostRequestDate(
                        forum_id = forumId ?: 0L,
                        thread_id = threadId,
                        options = option,
                    )
                )
            )
        )
}
