package com.example.shadowmap.di

import com.example.shadowmap.project.JsonProjectRepository
import com.example.shadowmap.project.ProjectRepository
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
