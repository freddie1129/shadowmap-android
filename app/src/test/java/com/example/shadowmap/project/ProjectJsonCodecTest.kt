package com.example.shadowmap.project

import com.example.shadowmap.domain.AutomaticBuildingIdentity
import com.example.shadowmap.domain.DrawnBuilding
import com.example.shadowmap.domain.GeoPoint
import com.example.shadowmap.domain.GeoPolygon
import com.example.shadowmap.domain.LoadedBuildingOverride
import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectJsonCodecTest {
    @Test
    fun roundTrip_preservesProjectDataAndComplexMapKeys() {
        val polygon = GeoPolygon(
            listOf(
                listOf(
                    GeoPoint(153.0, -27.0),
                    GeoPoint(153.001, -27.0),
                    GeoPoint(153.001, -27.001)
                )
            )
        )
        val identity = AutomaticBuildingIdentity("feature", "namespace", "fingerprint")
        val project = ProjectSnapshot(
            id = "project-1",
            name = "Test project",
            createdAt = 1L,
            updatedAt = 2L,
            selectedEpochMillis = 3L,
            displayTimeZoneId = "Australia/Brisbane",
            calculationLocation = GeoPoint(153.0, -27.0),
            selectedLocationLabel = "Brisbane",
            viewport = ProjectViewport(
                center = GeoPoint(153.0, -27.0),
                zoom = 17.0,
                bearing = 2.0,
                pitch = 3.0,
                boundary = polygon
            ),
            drawnBuildings = listOf(DrawnBuilding(polygon = polygon, heightMeters = 8.0)),
            drawnWalls = emptyList(),
            drawnTrees = emptyList(),
            loadedBuildings = emptyList(),
            loadedBuildingOverrides = mapOf(identity to LoadedBuildingOverride(12.0, polygon)),
            suppressedLoadedBuildings = mapOf(identity to polygon)
        )

        val decoded = ProjectJsonCodec.decode(ProjectJsonCodec.encode(project))

        assertEquals(project, decoded)
    }
}
