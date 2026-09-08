package com.dauvalter.geoflare.firestore

import dev.gitlive.firebase.firestore.DocumentReference
import com.dauvalter.geoflare.core.GeoLocation
import com.dauvalter.geoflare.core.GeohashUtils

/**
 * Converts a [GeoLocation] into a map of fields containing the encoded geohash and coordinates.
 */
public fun GeoLocation.toFirestoreMap(
    geohashField: String = "geohash",
    latField: String = "latitude",
    lonField: String = "longitude",
    precision: Int = GeohashUtils.DEFAULT_PRECISION
): Map<String, Any> = mapOf(
    geohashField to GeohashUtils.encode(this, precision),
    latField to latitude,
    lonField to longitude
)

/**
 * Sets or merges geographic coordinates and an automatically computed geohash into this document.
 */
public suspend fun DocumentReference.setGeoLocation(
    location: GeoLocation,
    geohashField: String = "geohash",
    latField: String = "latitude",
    lonField: String = "longitude",
    precision: Int = GeohashUtils.DEFAULT_PRECISION,
    merge: Boolean = true
) {
    val map = location.toFirestoreMap(geohashField, latField, lonField, precision)
    set(map, merge = merge)
}

/**
 * Updates geographic coordinates and an automatically computed geohash in an existing document.
 */
public suspend fun DocumentReference.updateGeoLocation(
    location: GeoLocation,
    geohashField: String = "geohash",
    latField: String = "latitude",
    lonField: String = "longitude",
    precision: Int = GeohashUtils.DEFAULT_PRECISION
) {
    val map = location.toFirestoreMap(geohashField, latField, lonField, precision)
    update(map)
}
