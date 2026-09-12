package com.huanchengfly.tieba.post.di

import com.huanchengfly.tieba.post.components.ClientConfigManager
import com.huanchengfly.tieba.post.components.ConfigInitializer
import com.huanchengfly.tieba.post.components.DefaultDeviceInfoProvider
import com.huanchengfly.tieba.post.components.SessionManager
import com.huanchengfly.tieba.post.components.SessionManagerImpl
import com.huanchengfly.tieba.post.core.network.session.ClientConfigProvider
import com.huanchengfly.tieba.post.core.network.session.CredentialProvider
import com.huanchengfly.tieba.post.core.network.session.DeviceInfoProvider
import com.huanchengfly.tieba.post.core.network.session.OAIDProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface SessionModule {

    @Binds
    fun bindClientConfigProvider(impl: ClientConfigManager): ClientConfigProvider

    @Binds
    fun bindCredentialProvider(impl: SessionManagerImpl): CredentialProvider

    @Binds
    fun bindDeviceInfoProvider(impl: DefaultDeviceInfoProvider): DeviceInfoProvider

    @Binds
    fun bindOAIDProvider(impl: ConfigInitializer): OAIDProvider

    @Binds
    fun bindSessionManager(impl: SessionManagerImpl): SessionManager
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SessionModuleEntryPoint {

    fun SessionManager(): SessionManager
}
