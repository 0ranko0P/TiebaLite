package com.huanchengfly.tieba.post.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import com.huanchengfly.tieba.post.core.database.dao.AccountDao
import com.huanchengfly.tieba.post.core.database.dao.BlockDao
import com.huanchengfly.tieba.post.core.database.dao.DraftDao
import com.huanchengfly.tieba.post.core.database.dao.ForumHistoryDao
import com.huanchengfly.tieba.post.core.database.dao.LikedForumDao
import com.huanchengfly.tieba.post.core.database.dao.SearchDao
import com.huanchengfly.tieba.post.core.database.dao.SearchPostDao
import com.huanchengfly.tieba.post.core.database.dao.ThreadHistoryDao
import com.huanchengfly.tieba.post.core.database.dao.TimestampDao
import com.huanchengfly.tieba.post.core.database.dao.UserProfileDao
import org.junit.After
import org.junit.Before

internal abstract class DatabaseTest {

    private lateinit var db: TbLiteDatabase
    protected lateinit var accountDao: AccountDao
    protected lateinit var blockDao: BlockDao
    protected lateinit var draftDao: DraftDao
    protected lateinit var forumHistoryDao: ForumHistoryDao
    protected lateinit var likedForumDao: LikedForumDao
    protected lateinit var searchDao: SearchDao
    protected lateinit var searchPostDao: SearchPostDao
    protected lateinit var threadHistoryDao: ThreadHistoryDao
    protected lateinit var timestampDao: TimestampDao
    protected lateinit var userProfileDao: UserProfileDao

    @Before
    fun setup() {
        db = run {
            val context = ApplicationProvider.getApplicationContext<Context>()
            Room.inMemoryDatabaseBuilder(context, TbLiteDatabase::class.java)
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                .build()
        }
        accountDao = db.accountDao()
        blockDao = db.blockDao()
        draftDao = db.draftDao()
        forumHistoryDao = db.forumHistoryDao()
        likedForumDao = db.likedForumDao()
        searchDao = db.searchDao()
        searchPostDao = db.searchPostDao()
        threadHistoryDao = db.threadHistoryDao()
        timestampDao = db.timestampDao()
        userProfileDao = db.userProfileDao()
    }

    @After
    fun teardown() = db.close()
}
