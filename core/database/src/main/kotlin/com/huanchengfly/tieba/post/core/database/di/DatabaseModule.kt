package com.huanchengfly.tieba.post.core.database.di

import android.content.Context
import androidx.room.ExperimentalRoomApi
import androidx.room.Room
import com.huanchengfly.tieba.post.core.database.TbLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    @OptIn(ExperimentalRoomApi::class)
    @Provides
    @Singleton
    fun provideDataBase(@ApplicationContext context: Context): TbLiteDatabase {
        return Room.databaseBuilder(context, TbLiteDatabase::class.java, "tb_lite.db")
            .setAutoCloseTimeout(15, TimeUnit.MINUTES)
            .build()
    }
}
