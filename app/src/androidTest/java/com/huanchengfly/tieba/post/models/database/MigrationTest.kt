package com.huanchengfly.tieba.post.models.database

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

private const val SchemaV1CreateAccount = """
    CREATE TABLE IF NOT EXISTS `account` (
        `uid` INTEGER NOT NULL,
        `name` TEXT NOT NULL,
        `nickname` TEXT,
        `bduss` TEXT NOT NULL,
        `tbs` TEXT NOT NULL,
        `portrait` TEXT NOT NULL,
        `sToken` TEXT NOT NULL,
        `cookie` TEXT NOT NULL,
        `intro` TEXT,
        `sex` INTEGER NOT NULL,
        `fans` TEXT NOT NULL,
        `posts` TEXT NOT NULL,
        `threads` TEXT NOT NULL,
        `concerned` TEXT NOT NULL,
        `tbAge` REAL NOT NULL,
        `age` INTEGER NOT NULL,
        `birthday_show` INTEGER NOT NULL,
        `birthday_time` INTEGER NOT NULL,
        `constellation` TEXT,
        `tiebaUid` TEXT,
        `zid` TEXT,
        `last_update` INTEGER NOT NULL,
        PRIMARY KEY(`uid`)
    )
"""

private const val SchemaV1CreateThread = """
    CREATE TABLE IF NOT EXISTS `thread_history` (
      `id` INTEGER NOT NULL,
      `avatar` TEXT NOT NULL,
      `name` TEXT NOT NULL,
      `title` TEXT NOT NULL,
      `is_see_lz` INTEGER NOT NULL,
      `pid` INTEGER NOT NULL,
      `timestamp` INTEGER NOT NULL,
      PRIMARY KEY(`id`)
    )
"""

private const val SchemaV1CreateUser = """
    CREATE TABLE IF NOT EXISTS `user` (
        `uid` INTEGER NOT NULL,
        `portrait` TEXT NOT NULL,
        `name` TEXT NOT NULL,
        `nickname` TEXT,
        `tiebaUid` TEXT NOT NULL,
        `intro` TEXT,
        `sex` TEXT NOT NULL,
        `tbAge` TEXT NOT NULL,
        `address` TEXT,
        `following` INTEGER NOT NULL,
        `thread` INTEGER NOT NULL,
        `post` INTEGER NOT NULL,
        `forum` INTEGER NOT NULL,
        `follow` INTEGER NOT NULL,
        `fans` INTEGER NOT NULL,
        `agree` INTEGER NOT NULL,
        `bazuDesc` TEXT,
        `newGod` TEXT,
        `privateForum` INTEGER NOT NULL,
        `isOfficial` INTEGER NOT NULL,
        `last_update` INTEGER NOT NULL,
        `last_visit` INTEGER NOT NULL,
        PRIMARY KEY(`uid`)
    )
"""

@Suppress("PrivatePropertyName")
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val TEST_DB = "migration-test.db"

    // Array of all migrations.
    private val ALL_MIGRATIONS: Array<Migration> = arrayOf(
        // No manual migration for now
    )

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        databaseClass = TbLiteDatabase::class.java,
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2_addForumColumn() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(SchemaV1CreateThread)
            // Insert a history record row using v1 columns
            execSQL("""
                INSERT INTO `thread_history` (id, avatar, name, title, is_see_lz, pid, timestamp)
                VALUES (1, 'avatarUrl', 'Author#0817', 'Test thread #1', 0, 2001, 1769000000000)
            """.trimIndent())
            close()
        }

        // Re-open the database with version 2 and provide
        val db = helper.runMigrationsAndValidate(TEST_DB, version = 2, validateDroppedTables = true)

        // Verify data exists in the new schema
        val cursor = db.query("SELECT * FROM thread_history WHERE id = 1")
        assertTrue("Expected a row for id=1 after migration", cursor.moveToFirst())
        val forumIndex = cursor.getColumnIndex("forum")
        assertTrue("Expected forum column to be NULL for pre-existing rows", cursor.isNull(forumIndex))
        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate2To3_addDaysTofreeColumn() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(SchemaV1CreateAccount)
            execSQL(SchemaV1CreateUser)
            execSQL("""
                INSERT INTO `account` (uid, name, bduss, tbs, portrait, sToken, cookie, sex, fans, posts, threads, concerned, tbAge, age, birthday_show, birthday_time, last_update)
                VALUES (1, 'MyAccount', '', '', '', '', '', 1, '20', '999', '999', '999', '10', 0, 0, 0, 1769000000000)
            """.trimIndent())
            execSQL("""
                INSERT INTO `user` (uid, portrait, name, tiebaUid, sex, tbAge, `following`, thread, post, forum, follow, fans, agree, privateForum, isOfficial, last_update, last_visit)
                Values(114514, '', 'TestUser', 123, '?', 1.5,  0, 0 , 999, 999, 0, 10, 0, 1, 1, 1769000000000, 1769000000000)
            """.trimIndent()
            )
            close()
        }

        // Re-open the database with version 3 and provide
        val db = helper.runMigrationsAndValidate(TEST_DB, version = 3, validateDroppedTables = true)

        // Verify data exists in the new schema
        var cursor = db.query("SELECT * FROM account WHERE uid = 1")
        assertTrue("Expected a row for id=1 after migration", cursor.moveToFirst())
        var blockDaysIndex = cursor.getColumnIndex("days_tofree")
        assertEquals("Expected days_tofree column to be 0 for pre-existing rows", 0, cursor.getInt(blockDaysIndex))
        cursor.close()

        cursor = db.query("SELECT * FROM user WHERE uid = 114514")
        assertTrue("Expected a row for id=114514 after migration", cursor.moveToFirst())
        blockDaysIndex = cursor.getColumnIndex("days_tofree")
        assertEquals("Expected days_tofree column to be 0 for pre-existing rows", 0, cursor.getInt(blockDaysIndex))
        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrateAll() {
        // Create earliest version of the database.
        helper.createDatabase(TEST_DB, 1).apply {
            close()
        }

        // Open latest version of the database. Room validates the schema
        // once all migrations execute.
        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            TbLiteDatabase::class.java,
            TEST_DB
        ).addMigrations(*ALL_MIGRATIONS).build().apply {
            openHelper.writableDatabase.close()
        }
    }
}
