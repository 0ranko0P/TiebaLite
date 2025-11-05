@file:Suppress("unused")

package com.huanchengfly.tieba.post.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.huanchengfly.tieba.post.models.database.TbLiteDatabase
import com.huanchengfly.tieba.post.models.database.dao.AccountDao
import com.huanchengfly.tieba.post.models.database.dao.BlockDao
import com.huanchengfly.tieba.post.models.database.dao.DraftDao
import com.huanchengfly.tieba.post.models.database.dao.ForumHistoryDao
import com.huanchengfly.tieba.post.models.database.dao.LikedForumDao
import com.huanchengfly.tieba.post.models.database.dao.SearchDao
import com.huanchengfly.tieba.post.models.database.dao.SearchPostDao
import com.huanchengfly.tieba.post.models.database.dao.ThreadHistoryDao
import com.huanchengfly.tieba.post.models.database.dao.TimestampDao
import com.huanchengfly.tieba.post.repository.source.network.HomeNetworkDataSource
import com.huanchengfly.tieba.post.repository.source.network.HomeNetworkFakeDataSource
import com.huanchengfly.tieba.post.repository.user.FakeSettingsRepository
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RepositoryModule::class]
)
abstract class FakeRepositoryModule {

    @Singleton
    @Binds
    abstract fun bindSettingsRepository(repository: FakeSettingsRepository): SettingsRepository
}

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DataSourceModule::class]
)
object FakeDataSourceModule {

    @Provides
    fun provideHomeNetworkDataSource(): HomeNetworkDataSource = HomeNetworkFakeDataSource

    @Provides
    fun provideHomeFakeNetworkDataSource(): HomeNetworkFakeDataSource = HomeNetworkFakeDataSource
}

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DatabaseModule::class]
)
object FakeDatabaseModule {

    @Singleton
    @Provides
    fun provideDataBase(@ApplicationContext context: Context): TbLiteDatabase {
        return Room.inMemoryDatabaseBuilder(context, TbLiteDatabase::class.java)
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .build()
    }

    @Provides
    fun provideAccountDao(database: TbLiteDatabase): AccountDao = database.accountDao()

    @Provides
    fun provideBlockDao(database: TbLiteDatabase): BlockDao = database.blockDao()

    @Provides
    fun provideDraftDao(database: TbLiteDatabase): DraftDao = database.draftDao()

    @Provides
    fun provideForumHistoryDao(database: TbLiteDatabase): ForumHistoryDao = database.forumHistoryDao()

    @Provides
    fun likedForumDao(database: TbLiteDatabase): LikedForumDao = database.likedForumDao()

    @Provides
    fun searchDao(database: TbLiteDatabase): SearchDao = database.searchDao()

    @Provides
    fun searchPostDao(database: TbLiteDatabase): SearchPostDao = database.searchPostDao()

    @Provides
    fun provideThreadHistoryDao(database: TbLiteDatabase): ThreadHistoryDao = database.threadHistoryDao()

    @Provides
    fun provideTimestampDao(database: TbLiteDatabase): TimestampDao = database.timestampDao()
}
