package com.gooludou.shadowplanner.project

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.gooludou.shadowplanner.domain.BuildingSource
import com.gooludou.shadowplanner.domain.ShadowAppearance

/** JSON codec for the versioned project file format. */
object ProjectJsonCodec {
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .enableComplexMapKeySerialization()
        .create()

    fun encode(project: ProjectSnapshot): String = gson.toJson(project)

    fun decode(json: String): ProjectSnapshot {
        val project = gson.fromJson(json, ProjectSnapshot::class.java)
        require(project.schemaVersion == CURRENT_SCHEMA_VERSION) {
            "Unsupported project schema version: ${project.schemaVersion}"
        }
        // Project schema 1 predates Building.source. Gson bypasses Kotlin
        // constructor defaults, so normalize legacy records explicitly.
        return project.copy(
            loadedBuildings = project.loadedBuildings.map {
                it.copy(source = BuildingSource.AUTOMATIC)
            },
            drawnBuildings = project.drawnBuildings.map { it.copy(source = BuildingSource.MANUAL) },
            shadowAppearance = project.shadowAppearance ?: ShadowAppearance.DEFAULT
        )
    }
}
