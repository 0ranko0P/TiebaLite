@file:Suppress("unused")

package com.huanchengfly.tieba.post.di

import android.content.Context
import com.huanchengfly.tieba.post.MacrobenchmarkConstant
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
import com.huanchengfly.tieba.post.models.database.dao.TransactionRunner
import com.huanchengfly.tieba.post.models.database.dao.UserProfileDao
import com.huanchengfly.tieba.post.repository.AddPostRepository
import com.huanchengfly.tieba.post.repository.AddPostRepositoryImpl
import com.huanchengfly.tieba.post.repository.source.local.ExploreAssetsDataSource
import com.huanchengfly.tieba.post.repository.source.local.ExploreLocalDataSource
import com.huanchengfly.tieba.post.repository.source.local.ExploreLocalFileDataSource
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
interface RepositoryModule {

    @Binds
    fun bindAddPostRepository(repository: AddPostRepositoryImpl): AddPostRepository

    @Binds
    fun bindSettingsRepository(repository: DataStoreSettingsRepository): SettingsRepository

    @Binds
    fun bindOKSignRepository(repository: OKSignRepositoryImp): OKSignRepository
}

// TODO: Gradle Modularization
@Module
@InstallIn(SingletonComponent::class)
object ExploreLocalCacheModule {

    @Singleton
    @Provides
    fun provideLocalDataSource(@ApplicationContext context: Context): ExploreLocalDataSource {
        return if (!MacrobenchmarkConstant.TRACE_ENABLED) {
            ExploreLocalFileDataSource(context)
        } else {
            ExploreAssetsDataSource(context)
        }
    }
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
    fun provideTransactionRunner(database: TbLiteDatabase): TransactionRunner = database.transactionRunnerDao()

    @Provides
    fun provideUserProfileDao(database: TbLiteDatabase): UserProfileDao = database.userProfileDao()
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RepositoryEntryPoint {
    fun settingsRepository(): SettingsRepository

    fun okSignRepository(): OKSignRepository
}
