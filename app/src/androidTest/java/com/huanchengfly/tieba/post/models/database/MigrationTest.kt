package com.huanchengfly.tieba.post.models.database

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

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
