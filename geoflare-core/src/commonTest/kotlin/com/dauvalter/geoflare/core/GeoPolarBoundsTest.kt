package com.dauvalter.geoflare.core

import kotlin.test.Test
import kotlin.test.assertTrue

class GeoPolarBoundsTest {
    private fun assertCovered(center: GeoLocation, point: GeoLocation, radiusKm: Double) {
        assertTrue(GeoMath.distance(center, point) <= radiusKm)
        val bounds = GeoQueryUtils.getGeohashQueryBounds(center, radiusKm)
        val hash = GeohashUtils.encode(point)
        assertTrue(bounds.any { hash >= it.startAt && hash <= it.endAt }, "$center -> $point missing from $bounds")
    }

    @Test
    fun circlesCrossingEitherPoleCoverEveryLongitude() {
        for (latitude in listOf(-89.0, 89.0)) {
            for (longitude in listOf(-180.0, -170.0, 0.0, 10.0, 180.0)) {
                val center = GeoLocation(latitude, longitude)
                for (pointLongitude in -180..180 step 10) {
                    assertCovered(center, GeoLocation(latitude, pointLongitude.toDouble()), 300.0)
                }
            }
        }
    }

    @Test
    fun circlesCenteredOnPolesCoverBothHemispheres() {
        for (latitude in listOf(-90.0, 90.0)) {
            for (longitude in -180..180 step 30) {
                assertCovered(GeoLocation(latitude, 10.0), GeoLocation(latitude * 0.999, longitude.toDouble()), 20.0)
            }
        }
    }

    @Test
    fun radiusLargerThanHalfCircumferenceCoversTheWorld() {
        for (center in listOf(GeoLocation(0.0, 10.0), GeoLocation(-45.0, -170.0))) {
            for (latitude in -90..90 step 15) {
                for (longitude in -180..180 step 15) {
                    assertCovered(center, GeoLocation(latitude.toDouble(), longitude.toDouble()), 21000.0)
                }
            }
        }
    }

    @Test
    fun ordinaryAntimeridianQueriesCoverBothSides() {
        for (longitude in listOf(-179.99, 179.99)) {
            val center = GeoLocation(45.0, longitude)
            assertCovered(center, GeoLocation(45.0, -179.999), 5.0)
            assertCovered(center, GeoLocation(45.0, 179.999), 5.0)
        }
    }
}
