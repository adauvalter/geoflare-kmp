package com.dauvalter.geoflare.core

import kotlin.math.*

public object GeoMath {
    /**
     * Mean radius of the Earth in kilometers, used in Haversine distance calculations.
     */
    public const val EARTH_RADIUS_KM: Double = 6371.0

    /**
     * Characters used in location geohashes (Base32, excluding a, i, l, o).
     */
    public const val BASE32_ALPHABET: String = "0123456789bcdefghjkmnpqrstuvwxyz"

    /**
     * Number of bits represented by each geohash character.
     */
    public const val BITS_PER_CHAR: Int = 5

    /**
     * Maximum bit precision for a geohash (22 characters * 5 bits).
     */
    public const val MAXIMUM_BITS_PRECISION: Int = 22 * BITS_PER_CHAR

    /**
     * The meridional circumference of the earth in meters.
     */
    public const val EARTH_MERI_CIRCUMFERENCE: Double = 40007860.0

    /**
     * Length of a degree of latitude at the equator in meters.
     */
    public const val METERS_PER_DEGREE_LATITUDE: Double = 110574.0

    /**
     * Equatorial radius of the earth in meters (WGS 84).
     */
    public const val EARTH_EQ_RADIUS: Double = 6378137.0

    /**
     * First eccentricity squared of the Earth ellipsoid (WGS 84).
     * E2 = (EARTH_EQ_RADIUS^2 - EARTH_POL_RADIUS^2) / (EARTH_EQ_RADIUS^2)
     */
    public const val E2: Double = 0.00669447819799

    /**
     * Cutoff threshold for rounding errors on double calculations.
     */
    public const val EPSILON: Double = 1e-12

    /**
     * Base-2 logarithm.
     */
    public fun log2(x: Double): Double = ln(x) / ln(2.0)

    /**
     * Converts angle from degrees to radians.
     */
    public fun degreesToRadians(degrees: Double): Double = degrees * (PI / 180.0)

    /**
     * Converts angle from radians to degrees.
     */
    public fun radiansToDegrees(radians: Double): Double = radians * (180.0 / PI)

    /**
     * Calculates the distance between two coordinates in kilometers using the Haversine formula.
     */
    public fun distance(location1: GeoLocation, location2: GeoLocation): Double {
        val lat1 = degreesToRadians(location1.latitude)
        val lat2 = degreesToRadians(location2.latitude)
        val lon1 = degreesToRadians(location1.longitude)
        val lon2 = degreesToRadians(location2.longitude)

        val dLat = lat2 - lat1
        val dLon = lon2 - lon1

        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_KM * c
    }

    /**
     * Calculates the distance between two coordinates in meters using the Haversine formula.
     */
    public fun distanceInMeters(location1: GeoLocation, location2: GeoLocation): Double {
        return distance(location1, location2) * 1000.0
    }

    /**
     * Calculates the number of degrees of longitude that a given distance in meters corresponds to
     * at a given latitude.
     */
    public fun metersToLongitudeDegrees(distanceMeters: Double, latitude: Double): Double {
        val radians = degreesToRadians(latitude)
        val num = cos(radians) * EARTH_EQ_RADIUS * PI / 180.0
        val denom = 1.0 / sqrt(1.0 - E2 * sin(radians) * sin(radians))
        val deltaDeg = num * denom
        return if (deltaDeg < EPSILON) {
            if (distanceMeters > 0) 360.0 else 0.0
        } else {
            min(360.0, distanceMeters / deltaDeg)
        }
    }

    /**
     * Calculates the bits necessary to reach a given resolution in meters for the longitude at a given latitude.
     */
    public fun longitudeBitsForResolution(resolutionMeters: Double, latitude: Double): Double {
        val degs = metersToLongitudeDegrees(resolutionMeters, latitude)
        return if (abs(degs) > 0.000001) max(1.0, log2(360.0 / degs)) else 1.0
    }

    /**
     * Calculates the bits necessary to reach a given resolution in meters for the latitude.
     */
    public fun latitudeBitsForResolution(resolutionMeters: Double): Double {
        return min(log2(EARTH_MERI_CIRCUMFERENCE / 2.0 / resolutionMeters), MAXIMUM_BITS_PRECISION.toDouble())
    }

    /**
     * Wraps the longitude into [-180.0, 180.0].
     */
    public fun wrapLongitude(longitude: Double): Double {
        if (longitude in -180.0..180.0) {
            return longitude
        }
        val adjusted = longitude + 180.0
        return if (adjusted > 0) {
            (adjusted % 360.0) - 180.0
        } else {
            180.0 - (-adjusted % 360.0)
        }
    }

    /**
     * Calculates the maximum number of bits of a geohash to get a bounding box that is larger than
     * a given size in meters at the given coordinate.
     */
    public fun boundingBoxBits(coordinate: GeoLocation, sizeMeters: Double): Int {
        val latDeltaDegrees = sizeMeters / METERS_PER_DEGREE_LATITUDE
        val latitudeNorth = min(90.0, coordinate.latitude + latDeltaDegrees)
        val latitudeSouth = max(-90.0, coordinate.latitude - latDeltaDegrees)
        val bitsLat = floor(latitudeBitsForResolution(sizeMeters)).toInt() * 2
        val bitsLongNorth = floor(longitudeBitsForResolution(sizeMeters, latitudeNorth)).toInt() * 2 - 1
        val bitsLongSouth = floor(longitudeBitsForResolution(sizeMeters, latitudeSouth)).toInt() * 2 - 1
        return minOf(bitsLat, bitsLongNorth, bitsLongSouth, MAXIMUM_BITS_PRECISION)
    }

    /**
     * Calculates eight points on the bounding box and the center of a given circle.
     * At least one geohash of these nine coordinates, truncated to a precision corresponding to radius,
     * is guaranteed to be a prefix of any geohash within the circle.
     */
    public fun boundingBoxCoordinates(center: GeoLocation, radiusMeters: Double): List<GeoLocation> {
        val latDegrees = radiusMeters / METERS_PER_DEGREE_LATITUDE
        val latitudeNorth = min(90.0, center.latitude + latDegrees)
        val latitudeSouth = max(-90.0, center.latitude - latDegrees)
        val longDegsNorth = metersToLongitudeDegrees(radiusMeters, latitudeNorth)
        val longDegsSouth = metersToLongitudeDegrees(radiusMeters, latitudeSouth)
        val longDegs = max(longDegsNorth, longDegsSouth)
        return listOf(
            GeoLocation(center.latitude, center.longitude),
            GeoLocation(center.latitude, wrapLongitude(center.longitude - longDegs)),
            GeoLocation(center.latitude, wrapLongitude(center.longitude + longDegs)),
            GeoLocation(latitudeNorth, center.longitude),
            GeoLocation(latitudeNorth, wrapLongitude(center.longitude - longDegs)),
            GeoLocation(latitudeNorth, wrapLongitude(center.longitude + longDegs)),
            GeoLocation(latitudeSouth, center.longitude),
            GeoLocation(latitudeSouth, wrapLongitude(center.longitude - longDegs)),
            GeoLocation(latitudeSouth, wrapLongitude(center.longitude + longDegs))
        )
    }
}
