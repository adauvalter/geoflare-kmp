package com.dauvalter.geoflare.firestore

import com.dauvalter.geoflare.core.GeoLocation
import com.dauvalter.geoflare.core.GeoQueryUtils
import dev.gitlive.firebase.firestore.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.mockito.Mockito.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import com.google.firebase.firestore.CollectionReference as NativeCollection
import com.google.firebase.firestore.FirebaseFirestore as NativeFirestore
import com.google.firebase.firestore.Query as NativeQuery

class QueryConstructionTest {
    private val center = GeoLocation(37.7749, -122.4194)

    @Test
    fun preconfiguredSdkQueriesAreRejectedBeforeAddingGeoCursors() = runTest {
        val native = mock(NativeQuery::class.java, RETURNS_SELF)
        val base = Query(native)
        val queries = listOf(
            base.orderBy("name"),
            base.orderBy("geohash", Direction.DESCENDING),
            base.limit(1),
            base.startAtFieldValues { add("name") },
            base.where { "active" equalTo true }
        )
        clearInvocations(native)
        for (query in queries) {
            val error = assertFailsWith<IllegalArgumentException> {
                FirestoreGeoQuery.buildGeohashQueries(query, center, 5.0)
            }
            assertTrue(error.message.orEmpty().contains("geoQuery()"))
            assertFailsWith<IllegalArgumentException> { query.geoGet<String>(center, 5.0) { center } }
            assertFailsWith<IllegalArgumentException> { query.geoGetRaw(center, 5.0) { center } }
            assertFailsWith<IllegalArgumentException> { query.geoSnapshots<String>(center, 5.0) { center } }
            assertFailsWith<IllegalArgumentException> { query.geoSnapshotsRaw(center, 5.0) { center } }
            assertFailsWith<IllegalArgumentException> {
                query.geoSnapshots<String>(flowOf(GeoQueryCriteria(center, 5.0))) { center }.first()
            }
        }
        verifyNoInteractions(native)
    }

    @Test
    fun unmodifiedCollectionRemainsSupported() {
        val native = mock(NativeCollection::class.java, RETURNS_SELF)
        val queries = FirestoreGeoQuery.buildGeohashQueries(CollectionReference(native), center, 5.0)
        val bounds = GeoQueryUtils.getGeohashQueryBounds(center, 5.0)
        assertEquals(bounds.size, queries.size)
        verify(native, times(bounds.size)).orderBy("geohash", NativeQuery.Direction.ASCENDING)
        for (bound in bounds) {
            verify(native).startAt(bound.startAt)
            verify(native).endAt(bound.endAt)
        }
    }

    @Test
    fun filteredSourcesPutGeohashFirstAndPreservePrecision() {
        val native = mock(NativeCollection::class.java, RETURNS_SELF)
        val source = CollectionReference(native).geoQuery().where { "active" equalTo true }
        val queries = FirestoreGeoQuery.buildGeohashQueries(source, center, 0.0001, "geo.hash", 4)
        val bounds = GeoQueryUtils.getGeohashQueryBounds(center, 0.0001, 4)
        assertEquals(bounds.size, queries.size)
        val order = inOrder(native)
        order.verify(native).where(any(com.google.firebase.firestore.Filter::class.java))
        for (bound in bounds) {
            order.verify(native).orderBy("geo.hash", NativeQuery.Direction.ASCENDING)
            order.verify(native).startAt(bound.startAt)
            order.verify(native).endAt(bound.endAt)
        }
        order.verifyNoMoreInteractions()
    }

    @Test
    fun collectionGroupsUseTheSafeSourceAndRetainFilters() {
        val nativeQuery = mock(NativeQuery::class.java, RETURNS_SELF)
        val nativeFirestore = mock(NativeFirestore::class.java)
        `when`(nativeFirestore.collectionGroup("places")).thenReturn(nativeQuery)
        val source = FirebaseFirestore(nativeFirestore).geoCollectionGroup("places")
            .where { "active" equalTo true }
        val queries = FirestoreGeoQuery.buildGeohashQueries(source, center, 5.0)
        assertEquals(GeoQueryUtils.getGeohashQueryBounds(center, 5.0).size, queries.size)
        verify(nativeFirestore).collectionGroup("places")
        verify(nativeQuery).where(any(com.google.firebase.firestore.Filter::class.java))
        verify(nativeQuery, times(queries.size)).orderBy("geohash", NativeQuery.Direction.ASCENDING)
    }
}
