package com.example.shadowmap.project

import com.google.gson.Gson
import com.google.gson.GsonBuilder

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
        return project
    }
}
