package com.huanchengfly.tieba.post.models.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.ExperimentalRoomApi
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.SQLiteConnection
import com.huanchengfly.tieba.post.models.database.dao.AccountDao
import com.huanchengfly.tieba.post.models.database.dao.BlockDao
import com.huanchengfly.tieba.post.models.database.dao.DraftDao
import com.huanchengfly.tieba.post.models.database.dao.ForumHistoryDao
import com.huanchengfly.tieba.post.models.database.dao.LikedForumDao
import com.huanchengfly.tieba.post.models.database.dao.SearchDao
import com.huanchengfly.tieba.post.models.database.dao.SearchPostDao
import com.huanchengfly.tieba.post.models.database.dao.ThreadHistoryDao
import com.huanchengfly.tieba.post.models.database.dao.TimestampDao
import com.huanchengfly.tieba.post.models.database.dao.UserProfileDao
import com.huanchengfly.tieba.post.models.database.TbLiteDatabase.Companion.Migrations
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
        UserProfile::class,
    ],
    version = 2,
    autoMigrations = [
        AutoMigration(from = 1, to = 2, spec = Migrations.Migration_1_2::class)
    ]
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

    abstract fun userProfileDao(): UserProfileDao

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

        @Suppress("ClassName")
        object Migrations {

            /**
             * [ThreadHistory] add forum column
             *
             * @since 4.0.0-beta.4
             */
            class Migration_1_2 : AutoMigrationSpec {
                override fun onPostMigrate(connection: SQLiteConnection) {
                }
            }
        }
    }
}