package com.huanchengfly.tieba.post.core.data.di

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.huanchengfly.tieba.post.core.common.Dispatcher
import com.huanchengfly.tieba.post.core.common.TbDispatchers.IO
import com.huanchengfly.tieba.post.core.data.repository.AddPostRepository
import com.huanchengfly.tieba.post.core.data.repository.AddPostRepositoryImpl
import com.huanchengfly.tieba.post.core.data.repository.PhotoRepository
import com.huanchengfly.tieba.post.core.data.repository.PhotoRepositoryImpl
import com.huanchengfly.tieba.post.core.data.repository.user.DataStoreSettingsRepository
import com.huanchengfly.tieba.post.core.data.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.core.data.util.ContextSingletonDelegate
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

private const val DATA_STORE_NAME = "app_preferences"

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    internal abstract fun bindAddPostRepository(repo: AddPostRepositoryImpl): AddPostRepository

    @Binds
    internal abstract fun bindPhotoRepository(repo: PhotoRepositoryImpl): PhotoRepository

    @Binds
    internal abstract fun bindSettingsRepository(repo: DataStoreSettingsRepository): SettingsRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    internal fun providesPreferencesDataStore(
        @ApplicationContext context: Context,
        @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler {
                Log.wtf(DATA_STORE_NAME, "onHandleCorruption", it)
                emptyPreferences()
            },
            scope = CoroutineScope(ioDispatcher + SupervisorJob()),
        ) {
            context.preferencesDataStoreFile(DATA_STORE_NAME)
        }
    }
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
