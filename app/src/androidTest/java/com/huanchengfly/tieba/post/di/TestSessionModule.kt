package com.huanchengfly.tieba.post.di

import com.huanchengfly.tieba.post.components.SessionManager
import com.huanchengfly.tieba.post.core.network.session.CredentialProvider
import com.huanchengfly.tieba.post.session.FakeSessionManager
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [SessionModule::class]
)
abstract class TestSessionModule {

    @Binds
    abstract fun bindCredentialProvider(impl: FakeSessionManager): CredentialProvider

    @Binds
    abstract fun bindSessionManager(impl: FakeSessionManager): SessionManager
}