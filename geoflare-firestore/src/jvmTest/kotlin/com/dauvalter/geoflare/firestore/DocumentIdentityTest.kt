package com.dauvalter.geoflare.firestore

import com.dauvalter.geoflare.core.GeoLocation
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DocumentIdentityTest {
    private val center = GeoLocation(0.0, 0.0)
    private val firstPath = "users/a/places/cafe"
    private val secondPath = "users/b/places/cafe"

    @Test
    fun typedAndRawQueriesRetainDifferentPathsButDeduplicateTheSameDocument() {
        val first = nativeDocument(firstPath)
        val second = nativeDocument(secondPath)
        val snapshots = arrayOf(querySnapshot(first, second), querySnapshot(first))
        val typed = FirestoreGeoQuery.filterAndDeduplicate(
            snapshots, center, 5.0, true, transform = { it.id }, locationExtractor = { center }
        )
        val raw = FirestoreGeoQuery.filterAndDeduplicateRaw(snapshots, center, 5.0, true) { center }
        assertEquals(listOf(firstPath, secondPath), typed.map { it.key })
        assertEquals(listOf(firstPath, secondPath), raw.map { it.key })
        assertEquals(listOf("cafe", "cafe"), typed.map { it.id })
        assertEquals(listOf("cafe", "cafe"), raw.map { it.id })
    }

    @Test
    fun eventsTrackEachPathIndependently() = runTest {
        val first = GeoQueryResult("first", 0.0, documentSnapshot(firstPath))
        val second = GeoQueryResult("second", 0.0, documentSnapshot(secondPath))
        val snapshots = flowOf(listOf(first, second), listOf(second))
        val events = snapshots.asGeoEvents().toList()
        assertEquals(2, events.filterIsInstance<GeoEvent.Entered<String>>().size)
        val exit = events.filterIsInstance<GeoEvent.Exited<String>>().single()
        assertEquals(firstPath, exit.key)
        assertEquals("cafe", exit.id)
        val batches = snapshots.asGeoEventBatches().toList()
        assertEquals(listOf(firstPath, secondPath), batches[0].entered.map { it.key })
        assertEquals(listOf(firstPath), batches[1].exited.map { it.key })
        assertEquals(emptyList(), batches[1].moved)
    }
}
