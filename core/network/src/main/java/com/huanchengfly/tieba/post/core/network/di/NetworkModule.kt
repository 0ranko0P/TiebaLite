package com.huanchengfly.tieba.post.core.network.di

import android.content.Context
import com.huanchengfly.tieba.post.core.network.retrofit.ITiebaApi
import com.huanchengfly.tieba.post.core.network.retrofit.RetrofitTiebaApi
import com.huanchengfly.tieba.post.core.network.retrofit.impls.MixedTiebaApiImpl
import com.huanchengfly.tieba.post.core.network.session.ClientConfigProvider
import com.huanchengfly.tieba.post.core.network.session.CredentialProvider
import com.huanchengfly.tieba.post.core.network.session.DeviceInfoProvider
import com.huanchengfly.tieba.post.core.network.session.OAIDProvider
import com.huanchengfly.tieba.post.core.network.util.ConnectivityManagerNetworkMonitor
import com.huanchengfly.tieba.post.core.network.util.NetworkMonitor
import com.huanchengfly.tieba.post.core.network.util.UIDManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {

    @Provides
    @Singleton
    fun provideRetrofitTiebaApi(
        clientConfigProvider: ClientConfigProvider,
        credentialProvider: CredentialProvider,
        deviceInfoProvider: DeviceInfoProvider,
        oaidProvider: OAIDProvider,
        uidManager: UIDManager,
        networkMonitor: NetworkMonitor,
    ): RetrofitTiebaApi = RetrofitTiebaApi(
        clientConfigProvider,
        credentialProvider,
        deviceInfoProvider,
        oaidProvider,
        uidManager,
        networkMonitor,
    )

    @Provides
    @Singleton
    fun provideITiebaApi(
        @ApplicationContext context: Context,
        clientConfigProvider: ClientConfigProvider,
        credentialProvider: CredentialProvider,
        deviceInfoProvider: DeviceInfoProvider,
        oaidProvider: OAIDProvider,
        uidManager: UIDManager,
        retrofitTiebaApi: RetrofitTiebaApi
    ): ITiebaApi {
        return MixedTiebaApiImpl(
            clientConfigProvider,
            credentialProvider,
            deviceInfoProvider,
            context.contentResolver,
            oaidProvider,
            uidManager,
            retrofitTiebaApi,
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal interface NetUtilsModule {

    @Binds
    fun bindNetworkMonitor(impl: ConnectivityManagerNetworkMonitor): NetworkMonitor
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NetUtilsEntryPoint {

    fun networkMonitor(): NetworkMonitor
}
