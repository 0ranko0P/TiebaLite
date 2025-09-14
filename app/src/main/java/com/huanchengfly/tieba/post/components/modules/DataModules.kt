package com.huanchengfly.tieba.post.components.modules

import com.huanchengfly.tieba.post.repository.user.DataStoreSettingsRepository
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Singleton
    @Binds
    abstract fun bindSettingsRepository(repository: DataStoreSettingsRepository): SettingsRepository
}
