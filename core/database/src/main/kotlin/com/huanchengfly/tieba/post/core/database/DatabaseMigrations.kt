package com.huanchengfly.tieba.post.core.database

import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.SQLiteConnection
import com.huanchengfly.tieba.post.core.database.model.BlockForum
import com.huanchengfly.tieba.post.core.database.model.ThreadHistory
import com.huanchengfly.tieba.post.core.database.model.UserProfile

@Suppress("ClassName")
internal object DatabaseMigrations {

    /**
     * [ThreadHistory] add forum column
     *
     * @since 4.0.0-beta.4
     */
    class Migration_1_2 : AutoMigrationSpec {
        override fun onPostMigrate(connection: SQLiteConnection) {
        }
    }

    /**
     * [UserProfile] add days_tofree column
     * [com.huanchengfly.tieba.post.core.database.model.Account] add days_tofree column
     *
     * @since 4.0.0-beta.4.3
     */
    class Migration_2_3 : AutoMigrationSpec {
        override fun onPostMigrate(connection: SQLiteConnection) {
        }
    }

    /**
     * [BlockForum] new Entity
     *
     * @since 4.0.0-beta.4.4
     */
    class Migration_3_4 : AutoMigrationSpec {
        override fun onPostMigrate(connection: SQLiteConnection) {
        }
    }
}