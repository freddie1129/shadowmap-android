package com.gooludou.shadowplanner.project

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface ProjectRepository {
    fun listProjects(): List<ProjectSummary>
    fun projectChanges(): Flow<Unit> = emptyFlow()
    fun loadProject(id: String): ProjectSnapshot
    fun saveProject(project: ProjectSnapshot)
    fun deleteProject(id: String)
}
