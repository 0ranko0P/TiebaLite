package com.huanchengfly.tieba.post.di

import android.content.Context
import com.huanchengfly.tieba.post.components.SessionManager
import com.huanchengfly.tieba.post.core.common.di.ApplicationScope
import com.huanchengfly.tieba.post.core.data.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.core.database.TbLiteDatabase
import com.huanchengfly.tieba.post.core.network.session.CredentialProvider
import com.huanchengfly.tieba.post.repository.HomeRepository
import com.huanchengfly.tieba.post.repository.HomeRepositoryImpl
import com.huanchengfly.tieba.post.repository.source.network.ForumFakeNetworkDataSource
import com.huanchengfly.tieba.post.repository.source.network.HomeNetworkFakeDataSource
import com.huanchengfly.tieba.post.repository.source.network.OKSignFakeDataSource
import com.huanchengfly.tieba.post.repository.user.OKSignRepository
import com.huanchengfly.tieba.post.repository.user.OKSignRepositoryImp
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.CoroutineScope

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RepositoryModule::class]
)
object TestRepositoryModule {

    @Provides
    fun provideHomeRepository(
        @ApplicationScope scope: CoroutineScope,
        tbLiteDatabase: TbLiteDatabase,
        settingsRepo: SettingsRepository,
        credentialProvider: CredentialProvider,
    ): HomeRepository =
        HomeRepositoryImpl(
            networkDataSource = HomeNetworkFakeDataSource,
            forumNetworkDataSource = ForumFakeNetworkDataSource,
            localDataSource = tbLiteDatabase.likedForumDao(),
            timestampDao = tbLiteDatabase.timestampDao(),
            settingsRepo = settingsRepo,
            credentialProvider = credentialProvider,
            scope = scope,
        )

    @Provides
    fun provideOKSignRepository(
        @ApplicationScope scope: CoroutineScope,
        @ApplicationContext context: Context,
        homeRepo: HomeRepository,
        settingsRepo: SettingsRepository,
        sessionManager: SessionManager,
    ): OKSignRepository =
        OKSignRepositoryImp(
            scope = scope,
            context = context,
            homeRepo = homeRepo,
            settingsRepo = settingsRepo,
            networkDataSource = OKSignFakeDataSource,
            homeDataSource = HomeNetworkFakeDataSource,
            sessionManager = sessionManager,
        )
}