package com.dauvalter.geoflare.firestore

import com.dauvalter.geoflare.core.GeoLocation
import com.dauvalter.geoflare.core.GeohashUtils

/**
 * Criteria for a dynamic geospatial query.
 *
 * @property center Center coordinate of the query.
 * @property radiusInKm Radius in kilometers.
 * @property geohashField Document field containing the geohash string.
 * @property sortByDistance Whether to sort results by ascending distance.
 * @property geohashPrecision Minimum length of geohashes stored in the queried documents.
 */
public data class GeoQueryCriteria(
    public val center: GeoLocation,
    public val radiusInKm: Double,
    public val geohashField: String = "geohash",
    public val sortByDistance: Boolean = true,
    public val geohashPrecision: Int = GeohashUtils.DEFAULT_PRECISION
) {
    init {
        require(radiusInKm.isFinite() && radiusInKm >= 0.0) { "Radius must be finite and non-negative, got $radiusInKm" }
        require(geohashPrecision in 1..GeohashUtils.MAX_PRECISION) { "Invalid geohash precision: $geohashPrecision" }
    }

    public val radiusInMeters: Double
        get() = radiusInKm * 1000.0

    public companion object {
        public fun inMeters(
            center: GeoLocation,
            radiusInMeters: Double,
            geohashField: String = "geohash",
            sortByDistance: Boolean = true,
            geohashPrecision: Int = GeohashUtils.DEFAULT_PRECISION
        ): GeoQueryCriteria = GeoQueryCriteria(
            center = center,
            radiusInKm = radiusInMeters / 1000.0,
            geohashField = geohashField,
            sortByDistance = sortByDistance,
            geohashPrecision = geohashPrecision
        )
    }
}
