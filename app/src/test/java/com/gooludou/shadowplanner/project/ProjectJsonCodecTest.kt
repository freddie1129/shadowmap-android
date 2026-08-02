package com.gooludou.shadowplanner.project

import com.gooludou.shadowplanner.core.model.AutomaticBuildingIdentity
import com.gooludou.shadowplanner.core.model.Building
import com.gooludou.shadowplanner.core.model.BuildingSource
import com.gooludou.shadowplanner.core.model.GeoPoint
import com.gooludou.shadowplanner.core.model.GeoPolygon
import com.gooludou.shadowplanner.core.model.LoadedBuildingOverride
import com.gooludou.shadowplanner.core.model.ShadowAppearance
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
            drawnBuildings = listOf(
                Building("manual", polygon, 8.0, source = BuildingSource.MANUAL)
            ),
            drawnWalls = emptyList(),
            drawnTrees = emptyList(),
            loadedBuildings = emptyList(),
            loadedBuildingOverrides = mapOf(identity to LoadedBuildingOverride(12.0, polygon)),
            suppressedLoadedBuildings = mapOf(identity to polygon),
            shadowAppearance = ShadowAppearance(colorArgb = 0xFF4CAF50L, opacity = 0.4f)
        )

        val decoded = ProjectJsonCodec.decode(ProjectJsonCodec.encode(project))

        assertEquals(project, decoded)
    }
}
