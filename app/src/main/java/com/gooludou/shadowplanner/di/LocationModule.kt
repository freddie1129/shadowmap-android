package com.gooludou.shadowplanner.di

import com.gooludou.shadowplanner.location.CurrentLocationResolver
import com.gooludou.shadowplanner.location.MapboxCurrentLocationResolver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {
    @Binds
    abstract fun bindCurrentLocationResolver(
        resolver: MapboxCurrentLocationResolver
    ): CurrentLocationResolver
}
