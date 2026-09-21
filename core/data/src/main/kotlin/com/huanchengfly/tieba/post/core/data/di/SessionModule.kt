package com.huanchengfly.tieba.post.core.data.di
import com.huanchengfly.tieba.post.core.data.session.ClientConfigManager
import com.huanchengfly.tieba.post.core.data.session.DefaultOAIDProvider
import com.huanchengfly.tieba.post.core.network.session.ClientConfigProvider
import com.huanchengfly.tieba.post.core.network.session.OAIDProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface SessionModule {

    @Binds
    fun bindClientConfigProvider(impl: ClientConfigManager): ClientConfigProvider

    @Binds
    fun bindOAIDProvider(impl: DefaultOAIDProvider): OAIDProvider
}
