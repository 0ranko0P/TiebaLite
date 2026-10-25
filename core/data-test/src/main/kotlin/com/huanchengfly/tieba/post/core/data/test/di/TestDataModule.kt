package com.huanchengfly.tieba.post.core.data.test.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.huanchengfly.tieba.post.core.data.di.DataStoreModule
import com.huanchengfly.tieba.post.core.data.test.InMemoryDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DataStoreModule::class],
)
internal object TestDataStoreModule {
    @Provides
    @Singleton
    fun providesUserPreferencesDataStore(): DataStore<Preferences> {
        return InMemoryDataStore(initialValue = emptyPreferences())
    }
}
