package com.huanchengfly.tieba.post.repository.source.local

import com.huanchengfly.tieba.post.models.database.TopForum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.litepal.LitePal
import org.litepal.extension.deleteAll

object TopForumDao {

    suspend fun getTopForums(): List<TopForum> = withContext(Dispatchers.IO) {
        LitePal.findAll(TopForum::class.java)
    }

    suspend fun add(forumId: Long): Boolean = withContext(Dispatchers.IO) {
        TopForum(forumId).saveOrUpdate("forumId = ?", forumId.toString())
    }

    suspend fun delete(forumId: Long): Boolean = withContext(Dispatchers.IO) {
        LitePal.deleteAll<TopForum>("forumId = ?", forumId.toString()) > 0
    }
}