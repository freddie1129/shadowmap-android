package com.example.shadowmap.project

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JsonProjectRepository @Inject constructor(
    @ApplicationContext context: Context
) : ProjectRepository {
    private val directory = File(context.filesDir, PROJECT_DIRECTORY).apply { mkdirs() }

    override fun listProjects(): List<ProjectSummary> = directory.listFiles { file ->
        file.isFile && file.extension == JSON_EXTENSION
    }.orEmpty().mapNotNull { file ->
        runCatching {
            val project = ProjectJsonCodec.decode(file.readText())
            ProjectSummary(project.id, project.name, project.updatedAt, file.name)
        }.getOrNull()
    }.sortedByDescending { it.updatedAt }

    override fun loadProject(id: String): ProjectSnapshot {
        return ProjectJsonCodec.decode(fileFor(id).readText())
    }

    override fun saveProject(project: ProjectSnapshot) {
        val target = fileFor(project.id)
        val temporary = File(directory, "${project.id}.tmp")
        temporary.writeText(ProjectJsonCodec.encode(project))
        check(temporary.renameTo(target)) { "Unable to save project" }
    }

    override fun deleteProject(id: String) {
        check(fileFor(id).delete()) { "Unable to delete project" }
    }

    private fun fileFor(id: String) = File(directory, "$id.$JSON_EXTENSION")

    private companion object {
        const val PROJECT_DIRECTORY = "projects"
        const val JSON_EXTENSION = "json"
    }
}
