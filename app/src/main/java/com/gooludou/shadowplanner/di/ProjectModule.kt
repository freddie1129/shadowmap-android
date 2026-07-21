package com.gooludou.shadowplanner.di

import com.gooludou.shadowplanner.project.JsonProjectRepository
import com.gooludou.shadowplanner.project.ProjectRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ProjectModule {
    @Binds
    abstract fun bindProjectRepository(repository: JsonProjectRepository): ProjectRepository
}
