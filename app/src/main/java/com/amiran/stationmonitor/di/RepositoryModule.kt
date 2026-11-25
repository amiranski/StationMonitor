package com.amiran.stationmonitor.di

import com.amiran.stationmonitor.data.repository.OfflineFirstStationRepository
import com.amiran.stationmonitor.domain.repository.StationRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /** "Whoever asks for StationRepository gets OfflineFirstStationRepository." */
    @Binds
    abstract fun bindStationRepository(impl: OfflineFirstStationRepository): StationRepository

    companion object {
        /** Injected instead of calling System.currentTimeMillis(), so tests can fix the time. */
        @Provides
        fun provideClock(): Clock = Clock.systemUTC()
    }
}
