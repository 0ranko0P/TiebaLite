package com.huanchengfly.tieba.post.repository

import android.content.Context
import com.huanchengfly.tieba.post.api.models.FollowBean
import com.huanchengfly.tieba.post.api.models.protos.PostInfoList
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.abstractText
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.repository.source.local.UserProfileLocalDataSource
import com.huanchengfly.tieba.post.repository.source.network.UserProfileNetworkDataSource
import com.huanchengfly.tieba.post.ui.models.Author
import com.huanchengfly.tieba.post.ui.models.Like
import com.huanchengfly.tieba.post.ui.models.LikeZero
import com.huanchengfly.tieba.post.ui.models.SimpleForum
import com.huanchengfly.tieba.post.ui.models.ThreadItem
import com.huanchengfly.tieba.post.ui.models.user.PostContent
import com.huanchengfly.tieba.post.ui.models.user.PostListItem
import com.huanchengfly.tieba.post.ui.models.user.UserProfile
import com.huanchengfly.tieba.post.ui.widgets.compose.buildThreadContent
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileRepository @Inject constructor(
    @ApplicationContext val context: Context,
    private val threadRepo: PbPageRepository,
    private val localDataSource: UserProfileLocalDataSource
){

    private val networkDataSource = UserProfileNetworkDataSource

    private val tbs: String?
        get() = AccountUtil.getInstance().currentAccount.value?.tbs

    /**
     * Load user threads or posts.
     *
     * @param uid user id
     * @param page page number
     * @param cached load from cache
     * @param isThread load threads or post
     *
     * @return list of [PostInfoList] (Thread and Post share the same network model)
     * */
    private suspend fun loadUserThreadPost(uid: Long, page: Int, cached: Boolean, isThread: Boolean): List<PostInfoList> {
        var data: List<PostInfoList>? = null
        if (cached) {
            data = localDataSource.loadUserThreadPost(uid, page, isThread)
        }

        if (data == null) {
            if (page == 1) { // expired or force-refresh, purge all cached threads
                localDataSource.purgeUserThreadPost(uid, isThread)
            }
            data = networkDataSource.loadUserThreadPost(uid, page, isThread)
            localDataSource.saveUserThreadPost(uid, page, data, isThread)
        }
        currentCoroutineContext().ensureActive()
        return data
    }

    suspend fun loadUserPost(uid: Long, page: Int, cached: Boolean): List<PostListItem> {
        return loadUserThreadPost(uid, page, cached, isThread = false).mapUiModelPost(context)
    }

    suspend fun loadUserThread(uid: Long, page: Int, cached: Boolean): List<ThreadItem> {
        return loadUserThreadPost(uid, page, cached, isThread = true).mapUiModelThreads()
    }

    suspend fun loadUserProfile(uid: Long, cached: Boolean): UserProfile {
        var data: User? = null
        if (cached) {
            data = localDataSource.loadUserProfile(uid)
        }

        // no cache or expired, fetch from network
        if (data == null) {
            data = networkDataSource.loadUserProfile(uid)
            localDataSource.saveUserProfile(uid, data)
        }
        currentCoroutineContext().ensureActive()
        return mapToUser(data)
    }

    suspend fun requestFollowUser(profile: UserProfile): FollowBean.Info {
        val result = networkDataSource.requestFollowUser(
            portrait = profile.portrait,
            tbs = tbs ?: throw TiebaNotLoggedInException()
        )
        localDataSource.deleteUserProfile(profile.uid)
        return result
    }

    suspend fun requestUnfollowUser(profile: UserProfile) {
        networkDataSource.requestUnfollowUser(
            portrait = profile.portrait,
            tbs = tbs ?: throw TiebaNotLoggedInException()
        )
        localDataSource.deleteUserProfile(profile.uid)
    }

    suspend fun requestLikeThread(uid: Long, thread: ThreadItem) {
        threadRepo.requestLikeThread(thread)
        // update local data is costly, purge caches instead
        localDataSource.purgeUserThreadPost(uid, isThread = true)
    }

    companion object {

        private suspend fun List<PostInfoList>.mapUiModelPost(context: Context): List<PostListItem> {
            if (isEmpty()) return emptyList()

            val author = with(this[0]) {
                Author(user_id, user_name, avatarUrl = StringUtil.getAvatarUrl(user_portrait))
            }
            return withContext(Dispatchers.Default) {
                map {
                    PostListItem(
                        author = author,
                        contents = it.content.map { p ->
                            PostContent(
                                postId = p.post_id,
                                text = p.post_content.abstractText,
                                timeDesc = DateTimeUtils.getRelativeTimeString(context, p.create_time),
                                isSubPost = p.post_type == 1L
                            )
                        },
                        title = it.title,
                        forumId = it.forum_id,
                        threadId = it.thread_id,
                        deleted = it.is_post_deleted == 1
                    )
                }
            }
        }

        private suspend fun List<PostInfoList>.mapUiModelThreads(): List<ThreadItem> {
            if (isEmpty()) return emptyList()

            val author = with(this[0]) {
                Author(user_id, user_name, avatarUrl = StringUtil.getAvatarUrl(user_portrait))
            }
            return withContext(Dispatchers.Default) {
                map {
                    ThreadItem(
                        id = it.thread_id,
                        firstPostId = it.post_id,
                        author = author,
                        content = buildThreadContent(
                            it.title,
                            it.abstractText,
                            isGood = it.good_types == 1
                        ),
                        title = it.title,
                        isTop = it.top_types == 1,
                        lastTimeMill = DateTimeUtils.fixTimestamp(it.create_time.toLong()),
                        like = it.agree?.let { Like(it) } ?: LikeZero,
                        replyNum = it.reply_num,
                        medias = it.media,
                        video = it.video_info?.wrapImmutable(),
                        originThreadInfo = it.origin_thread_info?.takeIf { _ -> it.is_share_thread == 1 }?.wrapImmutable(),
                        simpleForum = SimpleForum(it.forum_id, it.forum_name, null/* avatar */)
                    )
                }
            }
        }

        private fun mapToUser(user: User): UserProfile = UserProfile(
            uid = user.id,
            portrait = user.portrait,
            name = user.nameShow.trim(),
            userName = user.name.takeUnless { it == user.nameShow || it.length <= 1 }?.trim()?.let { "($it)" },
            tiebaUid = user.tieba_uid,
            intro = user.intro.takeUnless { it.isEmpty() },
            sex = when (user.sex) {
                1 -> "♂"
                2 -> "♀"
                else -> "?"
            },
            tbAge = user.tb_age.toFloatOrNull() ?: 0f,
            address = user.ip_address.takeUnless { it.isEmpty() },
            following = user.has_concerned != 0,
            threadNum = user.thread_num,
            postNum = user.post_num,
            forumNum = user.my_like_num,
            followNum = user.concern_num.getShortNumString(),
            fans = user.fans_num,
            agreeNum = user.total_agree_num.getShortNumString(),
            bazuDesc = user.bazhu_grade?.desc?.takeUnless { it.isEmpty() },
            newGod = user.new_god_data?.takeUnless { it.status <= 0 }?.field_name,
            privateForum = user.privSets?.like != 1,
            isOfficial = user.is_guanfang == 1
        )
    }
}