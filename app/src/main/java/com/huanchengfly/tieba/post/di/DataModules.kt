@file:Suppress("unused")

package com.huanchengfly.tieba.post.di

import android.content.Context
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
import com.huanchengfly.tieba.post.models.database.dao.UserProfileDao
import com.huanchengfly.tieba.post.repository.source.network.HomeNetworkDataSource
import com.huanchengfly.tieba.post.repository.source.network.HomeNetworkDataSourceImpl
import com.huanchengfly.tieba.post.repository.source.network.HotTopicNetworkDataSource
import com.huanchengfly.tieba.post.repository.source.network.HotTopicNetworkDataSourceImpl
import com.huanchengfly.tieba.post.repository.source.network.OKSignNetworkDataSource
import com.huanchengfly.tieba.post.repository.source.network.OKSignNetworkDataSourceImpl
import com.huanchengfly.tieba.post.repository.user.DataStoreSettingsRepository
import com.huanchengfly.tieba.post.repository.user.OKSignRepository
import com.huanchengfly.tieba.post.repository.user.OKSignRepositoryImp
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Singleton
    @Binds
    abstract fun bindSettingsRepository(repository: DataStoreSettingsRepository): SettingsRepository

    @Singleton
    @Binds
    abstract fun bindOKSignRepository(repository: OKSignRepositoryImp): OKSignRepository
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {

    @Singleton
    @Binds
    abstract fun bindHomeNetworkDataSource(dataSource: HomeNetworkDataSourceImpl): HomeNetworkDataSource

    @Singleton
    @Binds
    abstract fun bindOKSignNetworkDataSource(dataSource: OKSignNetworkDataSourceImpl): OKSignNetworkDataSource

    @Singleton
    @Binds
    abstract fun bindHotTopicNetworkDataSource(dataSource: HotTopicNetworkDataSourceImpl): HotTopicNetworkDataSource
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideDataBase(@ApplicationContext context: Context): TbLiteDatabase {
        return TbLiteDatabase.getInstance(context)
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

    @Provides
    fun provideUserProfileDao(database: TbLiteDatabase): UserProfileDao = database.userProfileDao()
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RepositoryEntryPoint {
    fun settingsRepository(): SettingsRepository

    fun okSignRepository(): OKSignRepository
}
