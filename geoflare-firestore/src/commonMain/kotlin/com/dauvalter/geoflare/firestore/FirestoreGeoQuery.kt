package com.dauvalter.geoflare.firestore

import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.Query
import dev.gitlive.firebase.firestore.QuerySnapshot
import com.dauvalter.geoflare.core.GeoLocation
import com.dauvalter.geoflare.core.GeoQueryUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

public object FirestoreGeoQuery {

    /**
     * Builds a list of [Query] objects corresponding to the geohash bounding box ranges
     * that cover the circle defined by [center] and [radiusInKm].
     *
     * @param baseQuery The starting query (e.g. collection reference or filtered query).
     * @param center The center coordinates.
     * @param radiusInKm The search radius in kilometers.
     * @param geohashField The document field containing the geohash string (defaults to "geohash").
     */
    public fun buildGeohashQueries(
        baseQuery: Query,
        center: GeoLocation,
        radiusInKm: Double,
        geohashField: String = "geohash"
    ): List<Query> {
        require(radiusInKm >= 0.0) { "Radius must be non-negative, got $radiusInKm" }
        val bounds = GeoQueryUtils.getGeohashQueryBounds(center, radiusInKm)
        return bounds.map { bound ->
            baseQuery.orderBy(geohashField)
                .startAtFieldValues { add(bound.startAt) }
                .endAtFieldValues { add(bound.endAt) }
        }
    }

    /**
     * Merges multiple range queries into a single real-time [Flow], deduplicating documents
     * across ranges and performing client-side distance filtering to discard false positives.
     */
    public fun <T> combineGeoSnapshots(
        queries: List<Query>,
        center: GeoLocation,
        radiusInKm: Double,
        sortByDistance: Boolean = true,
        transform: (DocumentSnapshot) -> T?,
        locationExtractor: (T) -> GeoLocation
    ): Flow<List<GeoQueryResult<T>>> {
        if (queries.isEmpty()) {
            return flowOf(emptyList())
        }

        val flows: List<Flow<QuerySnapshot>> = queries.map { it.snapshots }

        return combine(flows) { snapshotsArray: Array<QuerySnapshot> ->
            filterAndDeduplicate(
                snapshotsArray = snapshotsArray,
                center = center,
                radiusInKm = radiusInKm,
                sortByDistance = sortByDistance,
                transform = transform,
                locationExtractor = locationExtractor
            )
        }
    }

    /**
     * Merges multiple range queries into a single real-time [Flow] of raw [GeoDocumentSnapshot]s.
     */
    public fun combineGeoDocumentSnapshots(
        queries: List<Query>,
        center: GeoLocation,
        radiusInKm: Double,
        locationExtractor: (DocumentSnapshot) -> GeoLocation,
        sortByDistance: Boolean = true
    ): Flow<List<GeoDocumentSnapshot>> {
        if (queries.isEmpty()) {
            return flowOf(emptyList())
        }

        val flows: List<Flow<QuerySnapshot>> = queries.map { it.snapshots }

        return combine(flows) { snapshotsArray: Array<QuerySnapshot> ->
            filterAndDeduplicateRaw(
                snapshotsArray = snapshotsArray,
                center = center,
                radiusInKm = radiusInKm,
                sortByDistance = sortByDistance,
                locationExtractor = locationExtractor
            )
        }
    }

    /**
     * Executes all geohash range queries concurrently, deduplicating and filtering results by distance.
     */
    public suspend fun <T> geoGet(
        queries: List<Query>,
        center: GeoLocation,
        radiusInKm: Double,
        sortByDistance: Boolean = true,
        transform: (DocumentSnapshot) -> T?,
        locationExtractor: (T) -> GeoLocation
    ): List<GeoQueryResult<T>> = coroutineScope {
        val snapshots = queries.map { query ->
            async { query.get() }
        }.awaitAll()

        filterAndDeduplicate(
            snapshotsArray = snapshots.toTypedArray(),
            center = center,
            radiusInKm = radiusInKm,
            sortByDistance = sortByDistance,
            transform = transform,
            locationExtractor = locationExtractor
        )
    }

    /**
     * Executes all geohash range queries concurrently, returning raw [GeoDocumentSnapshot]s.
     */
    public suspend fun geoGetRaw(
        queries: List<Query>,
        center: GeoLocation,
        radiusInKm: Double,
        locationExtractor: (DocumentSnapshot) -> GeoLocation,
        sortByDistance: Boolean = true
    ): List<GeoDocumentSnapshot> = coroutineScope {
        val snapshots = queries.map { query ->
            async { query.get() }
        }.awaitAll()

        filterAndDeduplicateRaw(
            snapshotsArray = snapshots.toTypedArray(),
            center = center,
            radiusInKm = radiusInKm,
            sortByDistance = sortByDistance,
            locationExtractor = locationExtractor
        )
    }

    internal fun <T> filterAndDeduplicate(
        snapshotsArray: Array<QuerySnapshot>,
        center: GeoLocation,
        radiusInKm: Double,
        sortByDistance: Boolean,
        transform: (DocumentSnapshot) -> T?,
        locationExtractor: (T) -> GeoLocation
    ): List<GeoQueryResult<T>> {
        val allItems = snapshotsArray.flatMap { snapshot ->
            snapshot.documents.mapNotNull { doc ->
                val data = transform(doc) ?: return@mapNotNull null
                doc to data
            }
        }
        val filtered = GeoFilterUtils.filterAndDeduplicate(
            items = allItems,
            idExtractor = { (doc, _) -> doc.id },
            locationExtractor = { (_, data) -> locationExtractor(data) },
            center = center,
            radiusInKm = radiusInKm,
            sortByDistance = sortByDistance
        )
        return filtered.map { (item, dist) ->
            val (doc, data) = item
            GeoQueryResult(data = data, distanceInKm = dist, snapshot = doc)
        }
    }

    internal fun filterAndDeduplicateRaw(
        snapshotsArray: Array<QuerySnapshot>,
        center: GeoLocation,
        radiusInKm: Double,
        sortByDistance: Boolean,
        locationExtractor: (DocumentSnapshot) -> GeoLocation
    ): List<GeoDocumentSnapshot> {
        val allDocs = snapshotsArray.flatMap { it.documents }
        val filtered = GeoFilterUtils.filterAndDeduplicate(
            items = allDocs,
            idExtractor = { it.id },
            locationExtractor = locationExtractor,
            center = center,
            radiusInKm = radiusInKm,
            sortByDistance = sortByDistance
        )
        return filtered.map { (doc, dist) ->
            GeoDocumentSnapshot(snapshot = doc, distanceInKm = dist)
        }
    }
}
