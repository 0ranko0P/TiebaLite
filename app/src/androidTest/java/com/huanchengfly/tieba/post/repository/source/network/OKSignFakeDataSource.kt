package com.huanchengfly.tieba.post.repository.source.network

import com.huanchengfly.tieba.post.api.booleanToString
import com.huanchengfly.tieba.post.api.models.GetForumListBean
import com.huanchengfly.tieba.post.api.models.GetForumListBean.ForumInfo
import com.huanchengfly.tieba.post.api.models.MSignBean.Info
import com.huanchengfly.tieba.post.api.models.SignResultBean.UserInfo
import com.huanchengfly.tieba.post.repository.source.TestData

object OKSignFakeDataSource : OKSignNetworkDataSource {

    private val mSignFailForums = hashSetOf<String>()
    private var mSignError: Throwable? = null

    private val signFailForums = hashSetOf<String>()
    private var signError: Throwable? = null

    fun setSignFailForums(vararg forums: String) {
        signFailForums.addAll(forums)
    }

    fun setOfficialSignFailForums(vararg forums: String) {
        mSignFailForums.addAll(forums)
    }

    /**
     * Throws given exception [e] on next sign, parse ``null`` to clear it.
     * */
    fun setNextSignThrow(e: Throwable?) {
        signError = e
    }

    /**
     * Throws given exception [e] on next official sign, parse ``null`` to clear it.
     * */
    fun setNextOfficialSignThrow(e: Throwable?) {
        mSignError = e
    }

    override suspend fun getForumList(): GetForumListBean = TestData.DummyGetForumListBean

    override suspend fun requestOfficialSign(forums: List<ForumInfo>, tbs: String): List<Info> {
        mSignError?.let { mSignError = null; throw it }

        val noError = Info.Error("0", "", "")
        val curScore = 8.toString()
        return forums.map {
            val isSigned = !mSignFailForums.contains(it.forumName)
            val error = if (isSigned) noError else Info.Error("9", "err", "抱歉,根据相关法律法规和政策,${it.forumName}暂不开放")
            Info(curScore, error, it.forumId, it.forumName, "0", "1", signDayCount = "1", signed = isSigned.booleanToString())
        }
    }

    override suspend fun requestSign(forumId: Long, forumName: String, tbs: String): UserInfo {
        signError?.let { signError = null; throw it }

        val isSigned = !signFailForums.contains(forumName)
        return UserInfo(
            isSignIn = isSigned.booleanToString().toInt(),
            contSignNum = 10,
            signTime = (System.currentTimeMillis() / 1000).toString(),
            signBonusPoint = if (isSigned) 25 else 0
        )
    }
}
