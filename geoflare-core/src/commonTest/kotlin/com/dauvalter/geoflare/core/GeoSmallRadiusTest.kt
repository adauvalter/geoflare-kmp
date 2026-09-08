package com.dauvalter.geoflare.core

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GeoSmallRadiusTest {
    @Test
    fun zeroAndTinyRadiiQueryLocalCells() {
        for (center in listOf(GeoLocation(37.7749, -122.4194), GeoLocation(0.0, 0.0), GeoLocation(80.0, 45.0))) {
            for (radius in listOf(0.0, Double.MIN_VALUE, 0.001, 0.01, 0.03, 0.1)) {
                val bounds = GeoQueryUtils.getGeohashQueryBoundsInMeters(center, radius)
                val centerHash = GeohashUtils.encode(center)
                assertTrue(bounds.any { centerHash >= it.startAt && centerHash <= it.endAt })
                assertTrue(bounds.all { it.startAt.length == 10 }, "Unexpectedly broad query: $center, $radius, $bounds")
                val farHash = GeohashUtils.encode(GeoLocation(center.latitude - 1.0, center.longitude))
                assertTrue(bounds.none { farHash >= it.startAt && farHash <= it.endAt })
            }
        }
    }

    @Test
    fun centimeterQueriesStillCoverNeighborsAcrossCellBoundaries() {
        val center = GeoLocation(0.0, 0.0)
        val bounds = GeoQueryUtils.getGeohashQueryBoundsInMeters(center, 0.01)
        for (lat in listOf(-1e-8, 0.0, 1e-8)) {
            for (lon in listOf(-1e-8, 0.0, 1e-8)) {
                val point = GeoLocation(lat, lon)
                assertTrue(GeoMath.distanceInMeters(center, point) < 0.01)
                val hash = GeohashUtils.encode(point)
                assertTrue(bounds.any { hash >= it.startAt && hash <= it.endAt })
            }
        }
    }

    @Test
    fun invalidRadiiAreRejectedBeforeQueryConstruction() {
        for (radius in listOf(-1.0, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)) {
            assertFailsWith<IllegalArgumentException> {
                GeoQueryUtils.getGeohashQueryBoundsInMeters(GeoLocation(0.0, 0.0), radius)
            }
        }
    }
}
