package com.huanchengfly.tieba.post.core.data.di

import android.content.Context
import com.huanchengfly.tieba.post.core.data.repository.AddPostRepository
import com.huanchengfly.tieba.post.core.data.repository.AddPostRepositoryImpl
import com.huanchengfly.tieba.post.core.data.repository.PhotoRepository
import com.huanchengfly.tieba.post.core.data.repository.PhotoRepositoryImpl
import com.huanchengfly.tieba.post.core.data.repository.user.DataStoreSettingsRepository
import com.huanchengfly.tieba.post.core.data.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.core.data.util.ContextSingletonDelegate
import dagger.Binds
import dagger.Module
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface DataModule {

    @Binds
    fun bindAddPostRepository(repository: AddPostRepositoryImpl): AddPostRepository

    @Binds
    fun bindPhotoRepository(repository: PhotoRepositoryImpl): PhotoRepository

    @Binds
    fun bindSettingsRepository(repository: DataStoreSettingsRepository): SettingsRepository
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SettingsEntryPoint {
    fun settingsRepository(): SettingsRepository
}

// For components without Hilt inject support
val Context.settingRepository: SettingsRepository by ContextSingletonDelegate {
    EntryPointAccessors
        .fromApplication<SettingsEntryPoint>(this)
        .settingsRepository()
}
