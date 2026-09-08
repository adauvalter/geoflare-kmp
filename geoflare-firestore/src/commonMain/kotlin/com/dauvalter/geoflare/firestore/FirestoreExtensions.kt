package com.dauvalter.geoflare.firestore

import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.Query
import com.dauvalter.geoflare.core.GeoLocation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest

/**
 * Returns a real-time [Flow] of typed query results within [radiusInKm] of [center].
 *
 * Automatically:
 * 1. Computes minimal geohash range bounds.
 * 2. Merges real-time snapshots from each range query.
 * 3. Deduplicates documents that may span overlapping ranges.
 * 4. Calculates exact distance to [center] and filters out false positives outside [radiusInKm].
 * 5. Optionally sorts results by distance (closest first).
 */
public inline fun <reified T : Any> Query.geoSnapshots(
    center: GeoLocation,
    radiusInKm: Double,
    geohashField: String = "geohash",
    sortByDistance: Boolean = true,
    noinline locationExtractor: (T) -> GeoLocation
): Flow<List<GeoQueryResult<T>>> {
    val queries = FirestoreGeoQuery.buildGeohashQueries(this, center, radiusInKm, geohashField)
    return FirestoreGeoQuery.combineGeoSnapshots(
        queries = queries,
        center = center,
        radiusInKm = radiusInKm,
        sortByDistance = sortByDistance,
        transform = { doc -> doc.data<T>() },
        locationExtractor = locationExtractor
    )
}

/**
 * Returns a real-time [Flow] of typed query results within [radiusInMeters] of [center].
 */
public inline fun <reified T : Any> Query.geoSnapshotsInMeters(
    center: GeoLocation,
    radiusInMeters: Double,
    geohashField: String = "geohash",
    sortByDistance: Boolean = true,
    noinline locationExtractor: (T) -> GeoLocation
): Flow<List<GeoQueryResult<T>>> = geoSnapshots(
    center = center,
    radiusInKm = radiusInMeters / 1000.0,
    geohashField = geohashField,
    sortByDistance = sortByDistance,
    locationExtractor = locationExtractor
)

/**
 * Returns a real-time [Flow] of raw [GeoDocumentSnapshot]s within [radiusInKm] of [center].
 */
public fun Query.geoSnapshotsRaw(
    center: GeoLocation,
    radiusInKm: Double,
    geohashField: String = "geohash",
    sortByDistance: Boolean = true,
    locationExtractor: (DocumentSnapshot) -> GeoLocation
): Flow<List<GeoDocumentSnapshot>> {
    val queries = FirestoreGeoQuery.buildGeohashQueries(this, center, radiusInKm, geohashField)
    return FirestoreGeoQuery.combineGeoDocumentSnapshots(
        queries = queries,
        center = center,
        radiusInKm = radiusInKm,
        locationExtractor = locationExtractor,
        sortByDistance = sortByDistance
    )
}

/**
 * One-shot query to fetch all typed documents within [radiusInKm] of [center].
 */
public suspend inline fun <reified T : Any> Query.geoGet(
    center: GeoLocation,
    radiusInKm: Double,
    geohashField: String = "geohash",
    sortByDistance: Boolean = true,
    noinline locationExtractor: (T) -> GeoLocation
): List<GeoQueryResult<T>> {
    val queries = FirestoreGeoQuery.buildGeohashQueries(this, center, radiusInKm, geohashField)
    return FirestoreGeoQuery.geoGet(
        queries = queries,
        center = center,
        radiusInKm = radiusInKm,
        sortByDistance = sortByDistance,
        transform = { doc -> doc.data<T>() },
        locationExtractor = locationExtractor
    )
}

/**
 * One-shot query to fetch all typed documents within [radiusInMeters] of [center].
 */
public suspend inline fun <reified T : Any> Query.geoGetInMeters(
    center: GeoLocation,
    radiusInMeters: Double,
    geohashField: String = "geohash",
    sortByDistance: Boolean = true,
    noinline locationExtractor: (T) -> GeoLocation
): List<GeoQueryResult<T>> = geoGet(
    center = center,
    radiusInKm = radiusInMeters / 1000.0,
    geohashField = geohashField,
    sortByDistance = sortByDistance,
    locationExtractor = locationExtractor
)

/**
 * One-shot query to fetch raw [GeoDocumentSnapshot]s within [radiusInKm] of [center].
 */
public suspend fun Query.geoGetRaw(
    center: GeoLocation,
    radiusInKm: Double,
    geohashField: String = "geohash",
    sortByDistance: Boolean = true,
    locationExtractor: (DocumentSnapshot) -> GeoLocation
): List<GeoDocumentSnapshot> {
    val queries = FirestoreGeoQuery.buildGeohashQueries(this, center, radiusInKm, geohashField)
    return FirestoreGeoQuery.geoGetRaw(
        queries = queries,
        center = center,
        radiusInKm = radiusInKm,
        locationExtractor = locationExtractor,
        sortByDistance = sortByDistance
    )
}

/**
 * Returns a real-time [Flow] that dynamically updates query subscriptions whenever [criteriaFlow] emits.
 */
@OptIn(ExperimentalCoroutinesApi::class)
public inline fun <reified T : Any> Query.geoSnapshots(
    criteriaFlow: Flow<GeoQueryCriteria>,
    noinline locationExtractor: (T) -> GeoLocation
): Flow<List<GeoQueryResult<T>>> = criteriaFlow
    .distinctUntilChanged()
    .flatMapLatest { criteria ->
        geoSnapshots(
            center = criteria.center,
            radiusInKm = criteria.radiusInKm,
            geohashField = criteria.geohashField,
            sortByDistance = criteria.sortByDistance,
            locationExtractor = locationExtractor
        )
    }

/**
 * Returns a real-time [Flow] of raw [GeoDocumentSnapshot]s that dynamically updates query subscriptions
 * whenever [criteriaFlow] emits.
 */
@OptIn(ExperimentalCoroutinesApi::class)
public fun Query.geoSnapshotsRaw(
    criteriaFlow: Flow<GeoQueryCriteria>,
    locationExtractor: (DocumentSnapshot) -> GeoLocation
): Flow<List<GeoDocumentSnapshot>> = criteriaFlow
    .distinctUntilChanged()
    .flatMapLatest { criteria ->
        geoSnapshotsRaw(
            center = criteria.center,
            radiusInKm = criteria.radiusInKm,
            geohashField = criteria.geohashField,
            sortByDistance = criteria.sortByDistance,
            locationExtractor = locationExtractor
        )
    }
