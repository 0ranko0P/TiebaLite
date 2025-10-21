package com.huanchengfly.tieba.post.models.database

import android.content.Context
import androidx.room.Database
import androidx.room.ExperimentalRoomApi
import androidx.room.Room
import androidx.room.RoomDatabase
import com.huanchengfly.tieba.post.models.database.dao.AccountDao
import com.huanchengfly.tieba.post.models.database.dao.BlockDao
import com.huanchengfly.tieba.post.models.database.dao.DraftDao
import com.huanchengfly.tieba.post.models.database.dao.ForumHistoryDao
import com.huanchengfly.tieba.post.models.database.dao.LikedForumDao
import com.huanchengfly.tieba.post.models.database.dao.SearchDao
import com.huanchengfly.tieba.post.models.database.dao.SearchPostDao
import com.huanchengfly.tieba.post.models.database.dao.ThreadHistoryDao
import com.huanchengfly.tieba.post.models.database.dao.TimestampDao
import java.util.concurrent.TimeUnit

@Database(
    entities = [
        Account::class,
        BlockKeyword::class,
        BlockUser::class,
        Draft::class,
        ForumHistory::class,
        LocalLikedForum::class,
        SearchHistory::class,
        SearchPostHistory::class,
        ThreadHistory::class,
        TopForum::class,
        Timestamp::class,
    ],
    version = 1
)
abstract class TbLiteDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao

    abstract fun blockDao(): BlockDao

    abstract fun draftDao(): DraftDao

    abstract fun forumHistoryDao(): ForumHistoryDao

    abstract fun likedForumDao(): LikedForumDao

    abstract fun searchDao(): SearchDao

    abstract fun searchPostDao(): SearchPostDao

    abstract fun threadHistoryDao(): ThreadHistoryDao

    abstract fun timestampDao(): TimestampDao

    companion object {

        @Volatile
        private var INSTANCE: TbLiteDatabase? = null

        // Some old utils can not work with hilt inject, get instance manually for now
        @OptIn(ExperimentalRoomApi::class)
        fun getInstance(context: Context): TbLiteDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room
                    .databaseBuilder(context, TbLiteDatabase::class.java, "tb_lite.db")
                    .setAutoCloseTimeout(15, TimeUnit.MINUTES)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}