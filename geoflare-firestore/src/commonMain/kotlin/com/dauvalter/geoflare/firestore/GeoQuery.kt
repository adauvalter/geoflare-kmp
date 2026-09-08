package com.dauvalter.geoflare.firestore

import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.Filter
import dev.gitlive.firebase.firestore.FilterBuilder
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Query

/**
 * A collection or collection-group query with filters only.
 * GeoFlare owns ordering and cursors so geohash is always the first ordering field.
 * Limits are intentionally unsupported: limiting candidates before distance filtering
 * cannot guarantee the nearest N results. Apply take(N) to the returned sorted list.
 */
public class GeoQuery private constructor(internal val baseQuery: Query) {
    public fun where(filter: FilterBuilder.() -> Filter): GeoQuery = GeoQuery(baseQuery.where(filter))

    public companion object {
        internal fun collection(reference: CollectionReference): GeoQuery = GeoQuery(reference)

        internal fun collectionGroup(firestore: FirebaseFirestore, collectionId: String): GeoQuery =
            GeoQuery(firestore.collectionGroup(collectionId))
    }
}

/** Starts a geo query that can safely be filtered with [GeoQuery.where]. */
public fun CollectionReference.geoQuery(): GeoQuery = GeoQuery.collection(this)

/** Starts a geo query across all collections with the given ID in this database. */
public fun FirebaseFirestore.geoCollectionGroup(collectionId: String): GeoQuery =
    GeoQuery.collectionGroup(this, collectionId)
