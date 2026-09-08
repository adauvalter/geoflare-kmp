package com.dauvalter.geoflare.core

import kotlin.math.ceil
import kotlin.math.max

public object GeoQueryUtils {

    /**
     * Calculates the bounding box query for a geohash with [bits] precision.
     *
     * @param geohash The geohash whose bounding box query to generate.
     * @param bits The number of bits of precision.
     * @return A [GeohashRange] containing startAt and endAt geohashes.
     */
    public fun geohashQuery(geohash: String, bits: Int): GeohashRange {
        GeohashUtils.validateGeohash(geohash)
        val precision = ceil(bits.toDouble() / GeoMath.BITS_PER_CHAR).toInt()
        if (geohash.length < precision) {
            return GeohashRange(geohash, "$geohash~")
        }
        val truncatedGeohash = geohash.substring(0, precision)
        val base = truncatedGeohash.substring(0, truncatedGeohash.length - 1)
        val lastValue = GeoMath.BASE32_ALPHABET.indexOf(truncatedGeohash.last())
        val significantBits = bits - (base.length * GeoMath.BITS_PER_CHAR)
        val unusedBits = GeoMath.BITS_PER_CHAR - significantBits
        // delete unused bits
        val startValue = (lastValue shr unusedBits) shl unusedBits
        val endValue = startValue + (1 shl unusedBits)
        return if (endValue > 31) {
            GeohashRange(base + GeoMath.BASE32_ALPHABET[startValue], "$base~")
        } else {
            GeohashRange(base + GeoMath.BASE32_ALPHABET[startValue], base + GeoMath.BASE32_ALPHABET[endValue])
        }
    }

    /**
     * Calculates a set of geohash ranges that fully cover a given circle (center + radius in meters).
     *
     * Each returned range [startAt, endAt] guarantees that any geohash inside the circle
     * will fall between startAt and endAt lexicographically.
     *
     * @param center The center coordinates.
     * @param radiusInMeters The radius in meters.
     * @return List of unique [GeohashRange] bounds.
     */
    public fun getGeohashQueryBoundsInMeters(
        center: GeoLocation,
        radiusInMeters: Double
    ): List<GeohashRange> {
        require(radiusInMeters >= 0.0) { "Radius must be non-negative, got $radiusInMeters" }
        val queryBits = max(1, GeoMath.boundingBoxBits(center, radiusInMeters))
        val geohashPrecision = ceil(queryBits.toDouble() / GeoMath.BITS_PER_CHAR).toInt()
        val coordinates = GeoMath.boundingBoxCoordinates(center, radiusInMeters)
        val queries = coordinates.map { coord ->
            geohashQuery(GeohashUtils.encode(coord, geohashPrecision), queryBits)
        }
        return queries.distinct()
    }

    /**
     * Calculates a set of geohash ranges that fully cover a given circle (center + radius in kilometers).
     *
     * @param center The center coordinates.
     * @param radiusInKm The radius in kilometers.
     * @return List of unique [GeohashRange] bounds.
     */
    public fun getGeohashQueryBounds(
        center: GeoLocation,
        radiusInKm: Double
    ): List<GeohashRange> {
        return getGeohashQueryBoundsInMeters(center, radiusInKm * 1000.0)
    }
}
