package com.gooludou.shadowplanner.scene

import com.gooludou.shadowplanner.domain.SolarPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapboxSceneSkyGeometryTest {
    private val metrics = MapboxSceneSkyGeometry.calculateMetrics(
        viewportWidthMeters = 100.0,
        viewportHeightMeters = 200.0,
        viewportWidthPixels = 1000,
        viewportHeightPixels = 2000,
        edgePaddingPixels = 100.0,
        pathWidthPixels = 4.0,
        connectorWidthPixels = 2.0
    )

    @Test
    fun metricsFitOuterCompassInsidePaddedShorterEdge() {
        assertEquals(40.0, metrics.outerRadiusMeters, 0.001)
        assertEquals(
            40.0 / MapboxSceneSkyGeometry.DOME_MODEL_OUTER_RADIUS,
            metrics.domeRadiusMeters,
            0.001
        )
        assertEquals(0.4, metrics.pathDiameterMeters, 0.001)
        assertEquals(0.2, metrics.connectorDiameterMeters, 0.001)
    }

    @Test
    fun cardinalAndZenithPositionsUseEastNorthUpAxes() {
        val north = MapboxSceneSkyGeometry.pointOnDome(SolarPosition(0.0, 90.0), 10.0)
        val east = MapboxSceneSkyGeometry.pointOnDome(SolarPosition(90.0, 90.0), 10.0)
        val zenith = MapboxSceneSkyGeometry.pointOnDome(SolarPosition(0.0, 0.0), 10.0)

        assertEquals(0.0, north.eastMeters, 0.001)
        assertEquals(10.0, north.northMeters, 0.001)
        assertEquals(10.0, east.eastMeters, 0.001)
        assertEquals(0.0, east.northMeters, 0.001)
        assertEquals(10.0, zenith.upMeters, 0.001)
        assertEquals(-10.0, north.toMapboxModelTranslation()[1], 0.001)
        assertEquals(10.0, east.toMapboxModelTranslation()[0], 0.001)
    }

    @Test
    fun pathUsesOnlyAboveHorizonPositionsAndSitsOutsideDome() {
        val segments = MapboxSceneSkyGeometry.pathSegments(
            listOf(
                SolarPosition(70.0, 100.0),
                SolarPosition(90.0, 80.0),
                SolarPosition(120.0, 60.0),
                SolarPosition(150.0, 80.0)
            ),
            metrics
        )

        assertEquals(2, segments.size)
        assertTrue(segments.all { it.kind == MapboxSkySegmentKind.PATH })
        assertTrue(segments.all { it.lengthMeters > 0.0 })
    }

    @Test
    fun connectorMidpointAndMarkerShareTheSameEndpoint() {
        val position = SolarPosition(90.0, 60.0)
        val marker = checkNotNull(MapboxSceneSkyGeometry.marker(position, metrics))
        val connector = checkNotNull(MapboxSceneSkyGeometry.connector(position, metrics))

        assertEquals(marker.eastMeters / 2.0, connector.midpoint.eastMeters, 0.001)
        assertEquals(marker.northMeters / 2.0, connector.midpoint.northMeters, 0.001)
        assertEquals(marker.upMeters / 2.0, connector.midpoint.upMeters, 0.001)
        assertEquals(metrics.domeRadiusMeters, connector.lengthMeters, 0.001)
        assertEquals(MapboxSkySegmentKind.CONNECTOR, connector.kind)
        assertEquals(MapboxSkyPoint(0.0, 0.0, 0.0), connector.start)
        assertEquals(marker, connector.end)
    }

    @Test
    fun markerAndConnectorAreHiddenBelowHorizon() {
        val belowHorizon = SolarPosition(90.0, 100.0)

        assertNull(MapboxSceneSkyGeometry.marker(belowHorizon, metrics))
        assertNull(MapboxSceneSkyGeometry.connector(belowHorizon, metrics))
    }

    @Test
    fun connectorRotationsFollowMapboxLongitudeAndVerticalAxes() {
        val north = checkNotNull(
            MapboxSceneSkyGeometry.connector(SolarPosition(0.0, 89.0), metrics)
        )
        val east = checkNotNull(
            MapboxSceneSkyGeometry.connector(SolarPosition(90.0, 89.0), metrics)
        )
        val zenith = checkNotNull(
            MapboxSceneSkyGeometry.connector(SolarPosition(0.0, 0.0), metrics)
        )

        assertEquals(89.0, north.longitudeRotationDegrees, 0.001)
        assertEquals(0.0, north.verticalRotationDegrees, 0.001)
        assertEquals(89.0, east.longitudeRotationDegrees, 0.001)
        assertEquals(90.0, east.verticalRotationDegrees, 0.001)
        assertEquals(0.0, zenith.longitudeRotationDegrees, 0.001)
    }
}
