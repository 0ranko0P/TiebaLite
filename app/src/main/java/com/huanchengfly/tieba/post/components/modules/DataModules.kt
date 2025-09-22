@file:Suppress("unused")

package com.huanchengfly.tieba.post.components.modules

import com.huanchengfly.tieba.post.repository.user.DataStoreSettingsRepository
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.EntryPoint
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

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SettingsRepositoryEntryPoint {
    fun settingsRepository(): SettingsRepository
}
