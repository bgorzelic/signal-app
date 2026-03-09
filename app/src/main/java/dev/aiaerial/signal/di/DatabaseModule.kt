package dev.aiaerial.signal.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.aiaerial.signal.data.local.NetworkEventDao
import dev.aiaerial.signal.data.local.SignalDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SignalDatabase {
        return Room.databaseBuilder(
            context,
            SignalDatabase::class.java,
            "signal_database",
        ).build()
    }

    @Provides
    fun provideNetworkEventDao(database: SignalDatabase): NetworkEventDao {
        return database.networkEventDao()
    }
}
