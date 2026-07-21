package com.gooludou.shadowplanner.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.ZoneId

@Module
@InstallIn(SingletonComponent::class)
object TimeModule {
    @Provides
    fun provideClock(): Clock = Clock.systemUTC()

    @Provides
    fun provideZoneId(): ZoneId = ZoneId.systemDefault()
}
