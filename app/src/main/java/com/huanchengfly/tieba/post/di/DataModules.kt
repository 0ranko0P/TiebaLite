@file:Suppress("unused")

package com.huanchengfly.tieba.post.di

import android.content.Context
import com.huanchengfly.tieba.post.MacrobenchmarkConstant
import com.huanchengfly.tieba.post.repository.source.local.ExploreAssetsDataSource
import com.huanchengfly.tieba.post.repository.source.local.ExploreLocalDataSource
import com.huanchengfly.tieba.post.repository.source.local.ExploreLocalFileDataSource
import com.huanchengfly.tieba.post.repository.user.OKSignRepository
import com.huanchengfly.tieba.post.repository.user.OKSignRepositoryImp
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

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RepositoryEntryPoint {
    fun okSignRepository(): OKSignRepository
}
