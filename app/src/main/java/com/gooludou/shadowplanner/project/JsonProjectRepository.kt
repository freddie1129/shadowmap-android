package com.gooludou.shadowplanner.project

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

@Singleton
class JsonProjectRepository @Inject constructor(@ApplicationContext context: Context) :
    ProjectRepository {
    private val directory = File(context.filesDir, PROJECT_DIRECTORY).apply { mkdirs() }
    private val changes = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 1)

    override fun listProjects(): List<ProjectSummary> = directory.listFiles { file ->
        file.isFile && file.extension == JSON_EXTENSION
    }.orEmpty().mapNotNull { file ->
        runCatching {
            val project = ProjectJsonCodec.decode(file.readText())
            ProjectSummary(
                id = project.id,
                name = project.name,
                locationLabel = project.selectedLocationLabel,
                updatedAt = project.updatedAt,
                fileName = file.name
            )
        }.getOrNull()
    }.sortedByDescending { it.updatedAt }

    override fun projectChanges(): Flow<Unit> = changes

    override fun loadProject(id: String): ProjectSnapshot =
        ProjectJsonCodec.decode(fileFor(id).readText())

    override fun saveProject(project: ProjectSnapshot) {
        val target = fileFor(project.id)
        val temporary = File(directory, "${project.id}.tmp")
        temporary.writeText(ProjectJsonCodec.encode(project))
        check(temporary.renameTo(target)) { "Unable to save project" }
        changes.tryEmit(Unit)
    }

    override fun deleteProject(id: String) {
        check(fileFor(id).delete()) { "Unable to delete project" }
        changes.tryEmit(Unit)
    }

    private fun fileFor(id: String) = File(directory, "$id.$JSON_EXTENSION")

    private companion object {
        const val PROJECT_DIRECTORY = "projects"
        const val JSON_EXTENSION = "json"
    }
}
