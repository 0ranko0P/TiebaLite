package com.huanchengfly.tieba.post.core.database.di

import com.huanchengfly.tieba.post.core.database.TbLiteDatabase
import com.huanchengfly.tieba.post.core.database.dao.AccountDao
import com.huanchengfly.tieba.post.core.database.dao.BlockDao
import com.huanchengfly.tieba.post.core.database.dao.DraftDao
import com.huanchengfly.tieba.post.core.database.dao.ForumHistoryDao
import com.huanchengfly.tieba.post.core.database.dao.LikedForumDao
import com.huanchengfly.tieba.post.core.database.dao.SearchDao
import com.huanchengfly.tieba.post.core.database.dao.SearchPostDao
import com.huanchengfly.tieba.post.core.database.dao.ThreadHistoryDao
import com.huanchengfly.tieba.post.core.database.dao.TimestampDao
import com.huanchengfly.tieba.post.core.database.dao.TransactionRunner
import com.huanchengfly.tieba.post.core.database.dao.UserProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object DaosModule {

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
