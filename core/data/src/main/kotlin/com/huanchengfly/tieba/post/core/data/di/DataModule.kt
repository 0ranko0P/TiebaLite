package com.huanchengfly.tieba.post.core.data.di

import com.huanchengfly.tieba.post.core.data.repository.AddPostRepository
import com.huanchengfly.tieba.post.core.data.repository.AddPostRepositoryImpl
import com.huanchengfly.tieba.post.core.data.repository.user.DataStoreSettingsRepository
import com.huanchengfly.tieba.post.core.data.repository.user.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface DataModule {

    @Binds
    fun bindAddPostRepository(repository: AddPostRepositoryImpl): AddPostRepository

    @Binds
    fun bindSettingsRepository(repository: DataStoreSettingsRepository): SettingsRepository
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SettingsEntryPoint {
    fun settingsRepository(): SettingsRepository
}
