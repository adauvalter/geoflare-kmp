package io.github.adauvalter.geoflare.firestore

import dev.gitlive.firebase.firestore.DocumentSnapshot

/**
 * Represents a document returned from a geospatial query, along with its calculated distance.
 *
 * @property data The deserialized entity.
 * @property distanceInKm The calculated distance from the query center in kilometers.
 * @property snapshot The underlying Firestore [DocumentSnapshot] (if available).
 * @property id The document identifier.
 */
public data class GeoQueryResult<T>(
    public val data: T,
    public val distanceInKm: Double,
    public val snapshot: DocumentSnapshot? = null,
    public val id: String = snapshot?.id ?: ""
) {
    /**
     * The calculated distance from the query center in meters.
     */
    public val distanceInMeters: Double
        get() = distanceInKm * 1000.0
}

/**
 * Represents a raw Firestore [DocumentSnapshot] with its calculated distance.
 *
 * @property snapshot The underlying Firestore [DocumentSnapshot].
 * @property distanceInKm The calculated distance from the query center in kilometers.
 */
public data class GeoDocumentSnapshot(
    public val snapshot: DocumentSnapshot,
    public val distanceInKm: Double
) {
    /**
     * The calculated distance from the query center in meters.
     */
    public val distanceInMeters: Double
        get() = distanceInKm * 1000.0

    /**
     * The ID of the Firestore document.
     */
    public val id: String
        get() = snapshot.id
}
