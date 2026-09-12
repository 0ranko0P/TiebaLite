package com.huanchengfly.tieba.post.core.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.ExperimentalRoomApi
import androidx.room.Room
import androidx.room.RoomDatabase
import com.huanchengfly.tieba.post.core.database.dao.AccountDao
import com.huanchengfly.tieba.post.core.database.dao.BlockDao
import com.huanchengfly.tieba.post.core.database.dao.DraftDao
import com.huanchengfly.tieba.post.core.database.dao.ForumHistoryDao
import com.huanchengfly.tieba.post.core.database.dao.LikedForumDao
import com.huanchengfly.tieba.post.core.database.dao.SearchDao
import com.huanchengfly.tieba.post.core.database.dao.SearchPostDao
import com.huanchengfly.tieba.post.core.database.dao.ThreadHistoryDao
import com.huanchengfly.tieba.post.core.database.dao.TimestampDao
import com.huanchengfly.tieba.post.core.database.dao.TransactionRunnerDao
import com.huanchengfly.tieba.post.core.database.dao.UserProfileDao
import com.huanchengfly.tieba.post.core.database.model.Account
import com.huanchengfly.tieba.post.core.database.model.BlockForum
import com.huanchengfly.tieba.post.core.database.model.BlockKeyword
import com.huanchengfly.tieba.post.core.database.model.BlockUser
import com.huanchengfly.tieba.post.core.database.model.Draft
import com.huanchengfly.tieba.post.core.database.model.ForumHistory
import com.huanchengfly.tieba.post.core.database.model.LocalLikedForum
import com.huanchengfly.tieba.post.core.database.model.SearchHistory
import com.huanchengfly.tieba.post.core.database.model.SearchPostHistory
import com.huanchengfly.tieba.post.core.database.model.ThreadHistory
import com.huanchengfly.tieba.post.core.database.model.Timestamp
import com.huanchengfly.tieba.post.core.database.model.TopForum
import com.huanchengfly.tieba.post.core.database.model.UserProfile
import java.util.concurrent.TimeUnit

@Database(
    entities = [
        Account::class,
        BlockForum::class,
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
    version = 4,
    autoMigrations = [
        AutoMigration(
            from = 1,
            to = 2,
            spec = DatabaseMigrations.Migration_1_2::class
        ),
        AutoMigration(
            from = 2,
            to = 3,
            spec = DatabaseMigrations.Migration_2_3::class
        ),
        AutoMigration(
            from = 3,
            to = 4,
            spec = DatabaseMigrations.Migration_3_4::class
        ),
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

    abstract fun transactionRunnerDao(): TransactionRunnerDao

    abstract fun userProfileDao(): UserProfileDao
}