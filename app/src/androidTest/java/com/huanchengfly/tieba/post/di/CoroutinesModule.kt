package com.huanchengfly.tieba.post.di

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
    replaces = [CoroutinesModule::class]
)
object TestCoroutinesModule {

    val testScope = TestScope()

    val IO: CoroutineDispatcher = StandardTestDispatcher(testScope.testScheduler, "TestIO")

    val Main: CoroutineDispatcher = StandardTestDispatcher(testScope.testScheduler, "TestMain")

    val Default: CoroutineDispatcher
        get() = Main

    @Provides
    @IoDispatcher
    fun providesIODispatcher(): CoroutineDispatcher = IO

    @Provides
    @DefaultDispatcher
    fun providesDefaultDispatcher(): CoroutineDispatcher = Default

    @Provides
    @Singleton
    @ApplicationScope
    fun providesCoroutineScope(): CoroutineScope = testScope
}