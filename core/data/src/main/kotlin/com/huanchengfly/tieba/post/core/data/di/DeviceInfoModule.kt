package com.huanchengfly.tieba.post.core.data.di

import android.content.Context
import com.huanchengfly.tieba.post.core.data.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.core.data.session.DefaultDeviceInfoProvider
import com.huanchengfly.tieba.post.core.network.session.DeviceInfoProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DeviceInfoModule {

    @Singleton
    @Provides
    fun provideDeviceInfoProvider(
        @ApplicationContext application: Context,
        settingsRepo: SettingsRepository,
    ): DeviceInfoProvider {
        return DefaultDeviceInfoProvider.instance(application, settingsRepo)
    }
}