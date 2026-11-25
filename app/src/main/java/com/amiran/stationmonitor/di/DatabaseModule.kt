package com.amiran.stationmonitor.di

import android.content.Context
import androidx.room.Room
import com.amiran.stationmonitor.data.local.StationMonitorDatabase
import com.amiran.stationmonitor.data.local.dao.DeviceDao
import com.amiran.stationmonitor.data.local.dao.StationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): StationMonitorDatabase =
        Room.databaseBuilder(context, StationMonitorDatabase::class.java, StationMonitorDatabase.NAME)
            // The database is only a cache of server data: if a future version changes the
            // schema without a migration, dropping it and downloading again is acceptable.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideStationDao(database: StationMonitorDatabase): StationDao = database.stationDao()

    @Provides
    fun provideDeviceDao(database: StationMonitorDatabase): DeviceDao = database.deviceDao()
}
