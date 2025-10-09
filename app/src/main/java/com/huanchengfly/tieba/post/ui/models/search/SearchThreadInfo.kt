package com.huanchengfly.tieba.post.ui.models.search

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import com.huanchengfly.tieba.post.api.models.SearchThreadBean
import com.huanchengfly.tieba.post.ui.models.Author


/**
 * UI Model of [SearchThreadBean.ThreadInfoBean]
 *
 * @param tid [SearchThreadBean.ThreadInfoBean.tid]
 * @param pid [SearchThreadBean.ThreadInfoBean.pid]
 * @param cid [SearchThreadBean.ThreadInfoBean.cid]
 * @param author [SearchThreadBean.ThreadInfoBean.user], note that user id is ``-1`` when
 *   this user deregistered
 * @param content [SearchThreadBean.ThreadInfoBean.content] with highlighted search keywords
 * @param pictures list of picture
 * @param video video
 * @param forumInfo [SearchThreadBean.ForumInfo]
 * @param mainPostTitle title content of [SearchThreadBean.MainPost] with highlighted search keywords
 * @param mainPostContent content of [SearchThreadBean.MainPost] with highlighted search keywords
 * @param postInfoContent content of [SearchThreadBean.PostInfo] with highlighted search keywords
 * @param timeDesc formatted time
 * */
@Immutable
/*data */class SearchThreadInfo(
    val tid: Long,
    val pid: Long,
    val cid: Long,
    val author: Author,
    val content: AnnotatedString,
    val pictures: List<SearchMedia.Picture>?,
    val video: SearchMedia.Video?,
    val forumInfo: SearchThreadBean.ForumInfo,
    val mainPostTitle: AnnotatedString? = null,
    val mainPostContent: AnnotatedString? = null,
    val postInfoContent: AnnotatedString? = null,
    val timeDesc: String
)
