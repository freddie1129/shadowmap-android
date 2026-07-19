package com.gooludou.shadowplanner.project

interface ProjectRepository {
    fun listProjects(): List<ProjectSummary>
    fun loadProject(id: String): ProjectSnapshot
    fun saveProject(project: ProjectSnapshot)
    fun deleteProject(id: String)
}
