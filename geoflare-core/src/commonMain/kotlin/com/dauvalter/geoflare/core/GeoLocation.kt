package com.dauvalter.geoflare.core

/**
 * Represents the coordinates of a point on a map.
 *
 * @property latitude Latitude in degrees, must be in [-90.0, 90.0].
 * @property longitude Longitude in degrees, must be in [-180.0, 180.0].
 */
public data class GeoLocation(
    public val latitude: Double,
    public val longitude: Double
) {
    init {
        require(!latitude.isNaN()) { "Latitude cannot be NaN" }
        require(!longitude.isNaN()) { "Longitude cannot be NaN" }
        require(latitude in -90.0..90.0) { "Latitude must be in the range [-90, 90], got $latitude" }
        require(longitude in -180.0..180.0) { "Longitude must be in the range [-180, 180], got $longitude" }
    }
}
