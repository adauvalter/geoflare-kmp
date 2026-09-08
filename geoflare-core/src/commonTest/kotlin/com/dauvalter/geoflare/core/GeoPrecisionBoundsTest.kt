package com.dauvalter.geoflare.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GeoPrecisionBoundsTest {
    private val center = GeoLocation(37.7749, -122.4194)

    @Test
    fun tenCentimeterQueryIncludesDefaultPrecisionHashAtCenter() {
        val hash = GeohashUtils.encode(center)
        val bounds = GeoQueryUtils.getGeohashQueryBoundsInMeters(center, 0.1)
        assertTrue(bounds.any { hash >= it.startAt && hash <= it.endAt })
        assertTrue(bounds.all { it.startAt.length <= GeohashUtils.DEFAULT_PRECISION })
    }

    @Test
    fun shortStoredHashesAreCoveredWhenPrecisionIsSpecified() {
        val hash = GeohashUtils.encode(center, 4)
        val bounds = GeoQueryUtils.getGeohashQueryBounds(center, 5.0, geohashPrecision = 4)
        assertTrue(bounds.any { hash >= it.startAt && hash <= it.endAt })
        assertEquals(bounds, GeoQueryUtils.getGeohashQueryBoundsInMeters(center, 5000.0, 4))
    }

    @Test
    fun boundsCoverAllSupportedStoredPrecisionsAndLongerHashes() {
        for (minimumPrecision in 1..GeohashUtils.MAX_PRECISION) {
            for (radiusMeters in listOf(0.1, 1.0, 100.0, 5000.0)) {
                val bounds = GeoQueryUtils.getGeohashQueryBoundsInMeters(center, radiusMeters, minimumPrecision)
                for (actualPrecision in minimumPrecision..GeohashUtils.MAX_PRECISION) {
                    val hash = GeohashUtils.encode(center, actualPrecision)
                    assertTrue(bounds.any { hash >= it.startAt && hash <= it.endAt }, "$hash missing from $bounds")
                }
            }
        }
    }

    @Test
    fun invalidPrecisionAndBitCountsAreRejected() {
        for (precision in listOf(-1, 0, 23)) {
            assertFailsWith<IllegalArgumentException> {
                GeoQueryUtils.getGeohashQueryBounds(center, 5.0, precision)
            }
        }
        for (bits in listOf(-1, 0, 111, Int.MAX_VALUE)) {
            assertFailsWith<IllegalArgumentException> { GeoQueryUtils.geohashQuery("9q8y", bits) }
        }
    }
}
