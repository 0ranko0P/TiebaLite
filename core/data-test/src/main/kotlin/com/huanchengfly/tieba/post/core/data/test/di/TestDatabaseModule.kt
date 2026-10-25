package com.huanchengfly.tieba.post.core.data.test.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.huanchengfly.tieba.post.core.database.TbLiteDatabase
import com.huanchengfly.tieba.post.core.database.di.DatabaseModule
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DatabaseModule::class]
)
internal object TestDatabaseModule {

    @Singleton
    @Provides
    fun provideDataBase(@ApplicationContext context: Context): TbLiteDatabase {
        return Room.inMemoryDatabaseBuilder(context, TbLiteDatabase::class.java)
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .build()
    }
}
