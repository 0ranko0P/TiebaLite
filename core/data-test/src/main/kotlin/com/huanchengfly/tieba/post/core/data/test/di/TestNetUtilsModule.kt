package com.huanchengfly.tieba.post.core.data.test.di

import com.huanchengfly.tieba.post.core.data.test.util.AlwaysOnlineNetworkMonitor
import com.huanchengfly.tieba.post.core.network.di.NetUtilsModule
import com.huanchengfly.tieba.post.core.network.util.NetworkMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [NetUtilsModule::class]
)
abstract class TestNetUtilsModule {

    @Binds
    internal abstract fun NetworkMonitor(impl: AlwaysOnlineNetworkMonitor): NetworkMonitor
}
