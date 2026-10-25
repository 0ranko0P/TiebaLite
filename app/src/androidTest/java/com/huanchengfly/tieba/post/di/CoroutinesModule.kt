package com.huanchengfly.tieba.post.di

import com.huanchengfly.tieba.post.core.common.Dispatcher
import com.huanchengfly.tieba.post.core.common.TbDispatchers
import com.huanchengfly.tieba.post.core.common.di.ApplicationScope
import com.huanchengfly.tieba.post.core.common.di.CoroutineScopesModule
import com.huanchengfly.tieba.post.core.common.di.DispatchersModule
import com.huanchengfly.tieba.post.di.TestCoroutinesModule.testScope
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [CoroutineScopesModule::class]
)
object TestCoroutinesModule {

    val testScope = TestScope()

    @Provides
    @Singleton
    @ApplicationScope
    fun providesCoroutineScope(): CoroutineScope = testScope
}

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DispatchersModule::class]
)
object TestDispatchersModule {

    val IO: CoroutineDispatcher = StandardTestDispatcher(testScope.testScheduler, "TestIO")

    val Default: CoroutineDispatcher = StandardTestDispatcher(testScope.testScheduler, "TestDefault")

    @Provides
    @Dispatcher(TbDispatchers.IO)
    fun providesIODispatcher(): CoroutineDispatcher = this.IO

    @Provides
    @Dispatcher(TbDispatchers.Default)
    fun providesDefaultDispatcher(): CoroutineDispatcher = this.Default
}