package com.huanchengfly.tieba.post.di

import com.huanchengfly.tieba.post.components.SessionManager
import com.huanchengfly.tieba.post.components.SessionManagerImpl
import com.huanchengfly.tieba.post.core.network.session.CredentialProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class SessionModule {

    @Binds
    internal abstract fun bindCredentialProvider(impl: SessionManagerImpl): CredentialProvider

    @Binds
    internal abstract fun bindSessionManager(impl: SessionManagerImpl): SessionManager
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SessionModuleEntryPoint {

    fun SessionManager(): SessionManager
}
