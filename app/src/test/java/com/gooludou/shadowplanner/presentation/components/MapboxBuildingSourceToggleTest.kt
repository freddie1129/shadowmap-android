package com.gooludou.shadowplanner.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Test

class MapboxBuildingSourceToggleTest {

    @Test
    fun mapboxNative3dConfig_enablesOnlyMapboxBuildingsAndFacades() {
        assertEquals(
            mapOf(
                "show3dObjects" to true,
                "show3dBuildings" to true,
                "show3dTrees" to false,
                "show3dLandmarks" to false,
                "show3dFacades" to true
            ),
            mapboxNative3dConfig(showBuildings = true)
        )
    }

    @Test
    fun mapboxNative3dConfig_disablesAllNative3dFeaturesForDrawingBuildings() {
        assertEquals(
            mapOf(
                "show3dObjects" to false,
                "show3dBuildings" to false,
                "show3dTrees" to false,
                "show3dLandmarks" to false,
                "show3dFacades" to false
            ),
            mapboxNative3dConfig(showBuildings = false)
        )
    }
}
