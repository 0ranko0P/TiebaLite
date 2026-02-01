package com.huanchengfly.tieba.post.repository

import android.content.Context
import android.util.Log
import com.huanchengfly.tieba.post.App.Companion.AppBackgroundScope
import com.huanchengfly.tieba.post.api.models.FollowBean
import com.huanchengfly.tieba.post.api.models.PermissionListBean
import com.huanchengfly.tieba.post.api.models.protos.PostInfoList
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.abstractText
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.models.database.UserProfile
import com.huanchengfly.tieba.post.models.database.dao.UserProfileDao
import com.huanchengfly.tieba.post.repository.source.local.UserProfileLocalDataSource
import com.huanchengfly.tieba.post.repository.source.network.UserProfileNetworkDataSource
import com.huanchengfly.tieba.post.ui.models.Author
import com.huanchengfly.tieba.post.ui.models.Like
import com.huanchengfly.tieba.post.ui.models.LikeZero
import com.huanchengfly.tieba.post.ui.models.SimpleForum
import com.huanchengfly.tieba.post.ui.models.ThreadItem
import com.huanchengfly.tieba.post.ui.models.user.PostContent
import com.huanchengfly.tieba.post.ui.models.user.PostListItem
import com.huanchengfly.tieba.post.ui.widgets.compose.buildThreadContent
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.StringUtil.normalized
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileRepository @Inject constructor(
    @ApplicationContext val context: Context,
    private val threadRepo: PbPageRepository,
    private val userProfileDao: UserProfileDao,
    private val localDataSource: UserProfileLocalDataSource
) {

    private val networkDataSource = UserProfileNetworkDataSource

    private val scope = AppBackgroundScope

    init {
        scope.launch { localDataSource.cleanUpExpired() }
    }

    private suspend fun requireTBS(): String {
        return AccountUtil.getInstance().currentAccount.first()?.tbs ?: throw TiebaNotLoggedInException()
    }

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

    fun observeUserProfile(uid: Long): Flow<UserProfile?> = userProfileDao.observeById(uid)

    suspend fun loadUserPost(uid: Long, page: Int, cached: Boolean): List<PostListItem> {
        return loadUserThreadPost(uid, page, cached, isThread = false).mapUiModelPost(context)
    }

    suspend fun loadUserThread(uid: Long, page: Int, cached: Boolean): List<ThreadItem> {
        return loadUserThreadPost(uid, page, cached, isThread = true).mapUiModelThreads()
    }

    /**
     * Refresh and cache user profile, this is non-cancellable.
     * */
    suspend fun refreshUserProfile(uid: Long, forceRefresh: Boolean, recordHistory: Boolean = true) {
        val start = System.currentTimeMillis()
        scope.async {
            // Force refresh or cache expired, load latest user profile from network
            if (forceRefresh || checkUserCacheExpired(uid)) {
                val data: User = networkDataSource.loadUserProfile(uid)
                userProfileDao.upsert(profile = mapToEntity(data))
                localDataSource.purgeByUid(uid)
            } else if (recordHistory) {
                userProfileDao.updateLastVisit(uid, timestamp = System.currentTimeMillis())
            }
            val cost = System.currentTimeMillis() - start
            Log.i(TAG, "onRefreshUserProfile: Done, cost ${cost}ms on $uid")
        }.await()
    }

    suspend fun requestFollowUser(profile: UserProfile): FollowBean.Info {
        val result = networkDataSource.requestFollowUser(portrait = profile.portrait, tbs = requireTBS())
        userProfileDao.updateFollowState(uid = profile.uid, following = true, fans = profile.fans + 1)
        return result
    }

    suspend fun requestUnfollowUser(profile: UserProfile) {
        networkDataSource.requestUnfollowUser(portrait = profile.portrait, tbs = requireTBS())
        userProfileDao.updateFollowState(uid = profile.uid, following = false, fans = profile.fans - 1)
    }

    suspend fun requestLikeThread(uid: Long, thread: ThreadItem) {
        threadRepo.requestLikeThread(thread)
        // update local data is costly, purge caches instead
        localDataSource.purgeUserThreadPost(uid, isThread = true)
    }

    suspend fun getUserBlackInfo(uid: Long): PermissionListBean {
        requireTBS()
        return networkDataSource.getUserBlackInfo(uid)
    }

    suspend fun setUserBlack(uid: Long, permList: PermissionListBean) {
        networkDataSource.setUserBlack(uid, tbs = requireTBS(), permList)
    }

    private suspend fun checkUserCacheExpired(uid: Long): Boolean {
        val lastUpdate = userProfileDao.getLastUpdate(uid) ?: return true
        return lastUpdate + PROFILE_EXPIRE_MILL < System.currentTimeMillis()
    }

    companion object {

        private const val TAG = "UserProfileRepository"

        private const val PROFILE_EXPIRE_MILL = 0x240C8400 // 7 days

        private fun PostInfoList.getAuthor(): Author {
            return Author(
                id = user_id,
                name = name_show.takeUnless { it.isEmpty() || it == user_name } ?: user_name,
                avatarUrl = StringUtil.getAvatarUrl(user_portrait)
            )
        }

        private suspend fun List<PostInfoList>.mapUiModelPost(context: Context): List<PostListItem> {
            if (isEmpty()) return emptyList()

            val author = this.first().getAuthor()
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

            val author = this.first().getAuthor()
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

        private fun mapToEntity(user: User): UserProfile {
            val nickname = user.nameShow.trim().normalized().takeUnless { it.isEmpty() || it.isBlank() }
            val name = user.name.trim()
                .normalized()
                .takeUnless { it == "-"/* Server Bug? */ || it.isEmpty() || it.isBlank() }
                ?: nickname

            return UserProfile(
                uid = user.id,
                portrait = user.portrait,
                name = name.orEmpty(),
                nickname = nickname?.takeUnless { it == name },
                tiebaUid = user.tieba_uid,
                intro = user.intro.takeUnless { it.isEmpty() },
                sex = when (user.sex) {
                    1 -> "♂"
                    2 -> "♀"
                    else -> "?"
                },
                tbAge = user.tb_age,
                address = user.ip_address.takeUnless { it.isEmpty() },
                following = user.has_concerned != 0,
                thread = user.thread_num,
                post = user.post_num,
                forum = user.my_like_num,
                follow = user.concern_num,
                fans = user.fans_num,
                agree = user.total_agree_num,
                bazuDesc = user.bazhu_grade?.desc?.takeUnless { it.isEmpty() },
                newGod = user.new_god_data?.takeUnless { it.status <= 0 }?.field_name,
                privateForum = user.privSets?.like != 1,
                isOfficial = user.is_guanfang == 1
            )
        }
    }
}