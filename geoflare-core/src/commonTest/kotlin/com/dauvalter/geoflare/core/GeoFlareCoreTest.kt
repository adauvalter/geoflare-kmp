package com.dauvalter.geoflare.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.*

class GeoFlareCoreTest {

    // --- GeoLocation Tests ---

    @Test
    fun testValidLocations() {
        val loc1 = GeoLocation(0.0, 0.0)
        assertEquals(0.0, loc1.latitude)
        assertEquals(0.0, loc1.longitude)

        val loc2 = GeoLocation(-90.0, 180.0)
        assertEquals(-90.0, loc2.latitude)
        assertEquals(180.0, loc2.longitude)

        val loc3 = GeoLocation(90.0, -180.0)
        assertEquals(90.0, loc3.latitude)
        assertEquals(-180.0, loc3.longitude)

        val loc4 = GeoLocation(47.235124363, 127.2379654226)
        assertEquals(47.235124363, loc4.latitude)
        assertEquals(127.2379654226, loc4.longitude)
    }

    @Test
    fun testInvalidLocations() {
        assertFailsWith<IllegalArgumentException> { GeoLocation(-90.1, 0.0) }
        assertFailsWith<IllegalArgumentException> { GeoLocation(90.1, 0.0) }
        assertFailsWith<IllegalArgumentException> { GeoLocation(0.0, -180.1) }
        assertFailsWith<IllegalArgumentException> { GeoLocation(0.0, 180.1) }
        assertFailsWith<IllegalArgumentException> { GeoLocation(Double.NaN, 0.0) }
        assertFailsWith<IllegalArgumentException> { GeoLocation(0.0, Double.NaN) }
    }

    // --- GeoMath Tests ---

    @Test
    fun testDegreesAndRadiansConversion() {
        assertEquals(0.0, GeoMath.degreesToRadians(0.0))
        assertTrue(abs(GeoMath.degreesToRadians(45.0) - (PI / 4)) < 1e-10)
        assertTrue(abs(GeoMath.degreesToRadians(90.0) - (PI / 2)) < 1e-10)
        assertTrue(abs(GeoMath.degreesToRadians(180.0) - PI) < 1e-10)

        assertEquals(0.0, GeoMath.radiansToDegrees(0.0))
        assertTrue(abs(GeoMath.radiansToDegrees(PI) - 180.0) < 1e-10)
    }

    @Test
    fun testHaversineDistance() {
        val p1 = GeoLocation(37.7749, -122.4194)
        val p2 = GeoLocation(37.7752, -122.4178)

        val distKm = GeoMath.distance(p1, p2)
        // Expected ~0.1445 km from geofire-common reference
        assertTrue(abs(distKm - 0.1445278) < 1e-4, "Distance in km: $distKm")

        val distM = GeoMath.distanceInMeters(p1, p2)
        assertTrue(abs(distM - 144.5278) < 1e-1, "Distance in meters: $distM")

        // Same point distance should be 0
        assertEquals(0.0, GeoMath.distance(p1, p1))

        // London to Paris ~ 343-344 km
        val london = GeoLocation(51.5074, -0.1278)
        val paris = GeoLocation(48.8566, 2.3522)
        val londonToParis = GeoMath.distance(london, paris)
        assertTrue(londonToParis in 340.0..346.0, "London-Paris distance: $londonToParis")
    }

    @Test
    fun testWrapLongitude() {
        assertEquals(0.0, GeoMath.wrapLongitude(0.0))
        assertEquals(180.0, GeoMath.wrapLongitude(180.0))
        assertEquals(-180.0, GeoMath.wrapLongitude(-180.0))
        assertEquals(-179.0, GeoMath.wrapLongitude(181.0))
        assertEquals(179.0, GeoMath.wrapLongitude(-181.0))
        assertEquals(-180.0, GeoMath.wrapLongitude(540.0))
    }

    // --- GeohashUtils Tests ---

    @Test
    fun testGeohashValidation() {
        assertFailsWith<IllegalArgumentException> { GeohashUtils.validateGeohash("") }
        assertFailsWith<IllegalArgumentException> { GeohashUtils.validateGeohash("aaa") } // 'a' not in Base32
        assertFailsWith<IllegalArgumentException> { GeohashUtils.validateGeohash("9q8yi") } // 'i' not in Base32
        assertFailsWith<IllegalArgumentException> { GeohashUtils.validateGeohash("9q8yl") } // 'l' not in Base32
        assertFailsWith<IllegalArgumentException> { GeohashUtils.validateGeohash("9q8yo") } // 'o' not in Base32

        // Valid hashes should not throw
        GeohashUtils.validateGeohash("4")
        GeohashUtils.validateGeohash("d62dtu")
        GeohashUtils.validateGeohash("000000000000")
        GeohashUtils.validateGeohash("9q8yyk8ytp")
    }

    @Test
    fun testGeohashEncoding() {
        assertEquals("7zzzzzzzzz", GeohashUtils.encode(GeoLocation(0.0, 0.0)))
        assertEquals("9q8yyk8ytp", GeohashUtils.encode(GeoLocation(37.7749, -122.4194)))
        assertEquals("dr5regw3pp", GeohashUtils.encode(GeoLocation(40.7128, -74.0060)))

        // Precision truncation
        assertEquals("9q8yy", GeohashUtils.encode(GeoLocation(37.7749, -122.4194), precision = 5))
        assertEquals("9", GeohashUtils.encode(GeoLocation(37.7749, -122.4194), precision = 1))

        // Precision bounds validation
        assertFailsWith<IllegalArgumentException> {
            GeohashUtils.encode(GeoLocation(0.0, 0.0), precision = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            GeohashUtils.encode(GeoLocation(0.0, 0.0), precision = 23)
        }
    }

    @Test
    fun testGeohashDecoding() {
        val originalSF = GeoLocation(37.7749, -122.4194)
        val sfHash = GeohashUtils.encode(originalSF, precision = 10)
        val decodedSF = GeohashUtils.decode(sfHash)

        assertTrue(abs(originalSF.latitude - decodedSF.latitude) < 0.0001)
        assertTrue(abs(originalSF.longitude - decodedSF.longitude) < 0.0001)
    }

    // --- GeoQueryUtils Tests ---

    @Test
    fun testGeohashQueryBoundsSanFrancisco() {
        val sf = GeoLocation(37.7749, -122.4194)
        val bounds = GeoQueryUtils.getGeohashQueryBoundsInMeters(sf, 5000.0)

        // Expected from geofire-common reference:
        // [["9q8ys","9q8y~"],["9q8zh","9q8zs"]]
        assertEquals(2, bounds.size)
        assertEquals(GeohashRange("9q8ys", "9q8y~"), bounds[0])
        assertEquals(GeohashRange("9q8zh", "9q8zs"), bounds[1])

        // Verify that in km overload returns identical result
        val boundsKm = GeoQueryUtils.getGeohashQueryBounds(sf, 5.0)
        assertEquals(bounds, boundsKm)
    }

    @Test
    fun testGeohashQueryBoundsEquator() {
        val center = GeoLocation(0.0, 0.0)
        val bounds = GeoQueryUtils.getGeohashQueryBoundsInMeters(center, 1000.0)

        // Expected from geofire-common reference:
        // [["7zzzzw","7zzzz~"],["kpbpbn","kpbpbs"],["ebpbp8","ebpbpd"],["s00000","s00004"]]
        assertEquals(4, bounds.size)
        assertEquals(GeohashRange("7zzzzw", "7zzzz~"), bounds[0])
        assertEquals(GeohashRange("kpbpbn", "kpbpbs"), bounds[1])
        assertEquals(GeohashRange("ebpbp8", "ebpbpd"), bounds[2])
        assertEquals(GeohashRange("s00000", "s00004"), bounds[3])
    }

    @Test
    fun testQueryBoundsCoverAllPointsWithinRadius() {
        val center = GeoLocation(37.7749, -122.4194)
        val radiusMeters = 5000.0
        val bounds = GeoQueryUtils.getGeohashQueryBoundsInMeters(center, radiusMeters)

        // Generate points in various bearings at 50%, 80%, and 99% of the radius
        val testDistances = listOf(radiusMeters * 0.1, radiusMeters * 0.5, radiusMeters * 0.8, radiusMeters * 0.99)
        val bearings = listOf(0.0, 45.0, 90.0, 135.0, 180.0, 225.0, 270.0, 315.0)

        for (dist in testDistances) {
            for (bearing in bearings) {
                // Compute destination point
                val dRad = dist / (GeoMath.EARTH_RADIUS_KM * 1000.0)
                val bRad = GeoMath.degreesToRadians(bearing)
                val lat1 = GeoMath.degreesToRadians(center.latitude)
                val lon1 = GeoMath.degreesToRadians(center.longitude)

                val lat2 = kotlin.math.asin(sin(lat1) * cos(dRad) + cos(lat1) * sin(dRad) * cos(bRad))
                val lon2 = lon1 + kotlin.math.atan2(
                    sin(bRad) * sin(dRad) * cos(lat1),
                    cos(dRad) - sin(lat1) * sin(lat2)
                )

                val pointLoc = GeoLocation(
                    GeoMath.radiansToDegrees(lat2),
                    GeoMath.wrapLongitude(GeoMath.radiansToDegrees(lon2))
                )

                // Verify the point is within radius
                val actualDistance = GeoMath.distanceInMeters(center, pointLoc)
                assertTrue(actualDistance <= radiusMeters)

                // The point's geohash with length matching the query bounds precision
                val precision = bounds.first().startAt.length
                val pointHash = GeohashUtils.encode(pointLoc, precision)

                // Verify that AT LEAST ONE bound range covers pointHash
                val isCovered = bounds.any { bound ->
                    pointHash >= bound.startAt && pointHash <= bound.endAt
                }
                assertTrue(isCovered, "Point at distance $dist and bearing $bearing (hash $pointHash) not covered by any bounds: $bounds")
            }
        }
    }
}
