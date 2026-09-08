package com.dauvalter.geoflare.core

import kotlin.math.PI
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeoDistanceRegressionTest {
    @Test
    fun antipodalRoundoffDoesNotProduceNaN() {
        val a = GeoLocation(-29.528567542495097, -106.60507849687264)
        val b = GeoLocation(29.528567542495097, 73.39492150312736)
        assertEquals(PI * GeoMath.EARTH_RADIUS_KM, GeoMath.distance(a, b), 0.001)
        assertEquals(GeoMath.distance(a, b), GeoMath.distance(b, a))
    }

    @Test
    fun distancesRemainFiniteAtAndNearAntipodes() {
        val random = Random(1234)
        repeat(1000) {
            val a = GeoLocation(random.nextDouble(-89.0, 89.0), random.nextDouble(-180.0, 180.0))
            for (offset in listOf(0.0, 1e-8, -1e-8)) {
                val b = GeoLocation(-a.latitude + offset, GeoMath.wrapLongitude(a.longitude + 180.0))
                val distance = GeoMath.distance(a, b)
                assertTrue(distance.isFinite(), "$a to $b returned $distance")
                assertEquals(PI * GeoMath.EARTH_RADIUS_KM, distance, 0.01)
            }
        }
    }

    @Test
    fun identicalAndNearbyPointsRetainTheirDistance() {
        val a = GeoLocation(0.0, 0.0)
        assertEquals(0.0, GeoMath.distance(a, a))
        assertEquals(PI * GeoMath.EARTH_RADIUS_KM / 180.0, GeoMath.distance(a, GeoLocation(0.0, 1.0)), 1e-9)
    }
}
