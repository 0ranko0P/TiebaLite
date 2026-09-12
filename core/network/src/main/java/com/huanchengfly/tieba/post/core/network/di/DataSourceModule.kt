package com.huanchengfly.tieba.post.core.network.di

import com.huanchengfly.tieba.post.core.network.source.AuthNetworkDataSource
import com.huanchengfly.tieba.post.core.network.source.ExploreNetworkDataSource
import com.huanchengfly.tieba.post.core.network.source.ForumNetworkDataSource
import com.huanchengfly.tieba.post.core.network.source.HomeNetworkDataSource
import com.huanchengfly.tieba.post.core.network.source.HomeNetworkDataSourceImpl
import com.huanchengfly.tieba.post.core.network.source.HotTopicNetworkDataSource
import com.huanchengfly.tieba.post.core.network.source.HotTopicNetworkDataSourceImpl
import com.huanchengfly.tieba.post.core.network.source.OKSignNetworkDataSource
import com.huanchengfly.tieba.post.core.network.source.OKSignNetworkDataSourceImpl
import com.huanchengfly.tieba.post.core.network.source.ReplyNetworkDataSource
import com.huanchengfly.tieba.post.core.network.source.RetrofitAuthNetworkDataSource
import com.huanchengfly.tieba.post.core.network.source.RetrofitExploreDataSource
import com.huanchengfly.tieba.post.core.network.source.RetrofitForumDataSource
import com.huanchengfly.tieba.post.core.network.source.RetrofitReplyNetworkDataSource
import com.huanchengfly.tieba.post.core.network.source.RetrofitSearchNetworkDataSource
import com.huanchengfly.tieba.post.core.network.source.RetrofitSofireDataSource
import com.huanchengfly.tieba.post.core.network.source.RetrofitThreadNetworkDataSource
import com.huanchengfly.tieba.post.core.network.source.RetrofitThreadPictureDataSource
import com.huanchengfly.tieba.post.core.network.source.RetrofitThreadStoreNetworkDataSource
import com.huanchengfly.tieba.post.core.network.source.SearchNetworkDataSource
import com.huanchengfly.tieba.post.core.network.source.SofireDataSource
import com.huanchengfly.tieba.post.core.network.source.ThreadNetworkDataSource
import com.huanchengfly.tieba.post.core.network.source.ThreadPictureDataSource
import com.huanchengfly.tieba.post.core.network.source.ThreadStoreNetworkDataSource
import com.huanchengfly.tieba.post.core.network.source.UserProfileNetworkDataSource
import com.huanchengfly.tieba.post.core.network.source.UserProfileNetworkDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface DataSourceModule {

    @Binds
    fun bindAuthNetworkDataSource(impl: RetrofitAuthNetworkDataSource): AuthNetworkDataSource

    @Binds
    fun bindExploreNetworkDataSource(impl: RetrofitExploreDataSource): ExploreNetworkDataSource

    @Binds
    fun bindForumNetworkDataSource(impl: RetrofitForumDataSource): ForumNetworkDataSource

    @Binds
    fun bindHomeNetworkDataSource(impl: HomeNetworkDataSourceImpl): HomeNetworkDataSource

    @Binds
    fun bindHotTopicNetworkDataSource(impl: HotTopicNetworkDataSourceImpl): HotTopicNetworkDataSource

    @Binds
    fun bindOKSignNetworkDataSource(impl: OKSignNetworkDataSourceImpl): OKSignNetworkDataSource

    @Binds
    fun bindReplyNetworkDataSource(impl: RetrofitReplyNetworkDataSource): ReplyNetworkDataSource

    @Binds
    fun bindSearchNetworkDataSource(impl: RetrofitSearchNetworkDataSource): SearchNetworkDataSource

    @Binds
    fun bindSofireDataSource(impl: RetrofitSofireDataSource): SofireDataSource

    @Binds
    fun bindThreadNetworkDataSource(impl: RetrofitThreadNetworkDataSource): ThreadNetworkDataSource

    @Binds
    fun bindThreadPictureDataSource(impl: RetrofitThreadPictureDataSource): ThreadPictureDataSource

    @Binds
    fun bindThreadStoreNetworkDataSource(impl: RetrofitThreadStoreNetworkDataSource): ThreadStoreNetworkDataSource

    @Binds
    fun bindUserProfileNetworkDataSource(impl: UserProfileNetworkDataSourceImpl): UserProfileNetworkDataSource
}