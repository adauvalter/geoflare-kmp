package com.dauvalter.geoflare.firestore

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

class SnapshotEventsTest {
    @Test
    fun freshSdkWrappersDoNotGenerateMovement() = runTest {
        val native = nativeDocument("places/cafe")
        val query = querySnapshot(native)
        val first = GeoQueryResult("Cafe", 1.0, query.documents.single())
        val refreshed = GeoQueryResult("Cafe", 1.0, query.documents.single())
        assertNotEquals(first.snapshot, refreshed.snapshot) // Reproduces GitLive 2.5 wrapper identity.
        val source = flowOf(listOf(first), listOf(refreshed))
        assertEquals(listOf(GeoEvent.Entered(first)), source.asGeoEvents().toList())
        assertEquals(1, source.asGeoEventBatches().toList().size)
    }

    @Test
    fun oneDocumentChangingDoesNotMoveEveryOtherResult() = runTest {
        val a = nativeDocument("places/a")
        val b = nativeDocument("places/b")
        val query = querySnapshot(a, b)
        val before = query.documents.map { GeoQueryResult(it.id, 1.0, it) }
        val after = query.documents.map { GeoQueryResult(it.id, if (it.id == "a") 2.0 else 1.0, it) }
        val source = flowOf(before, after)
        val moves = source.asGeoEvents().toList().filterIsInstance<GeoEvent.Moved<String>>()
        assertEquals(listOf("a"), moves.map { it.item.id })
        assertEquals(listOf("a"), source.asGeoEventBatches().toList()[1].moved.map { it.first.id })
    }

    @Test
    fun dataChangesAndDistanceChangesStillMoveAndExitKeepsLatestSnapshot() = runTest {
        val first = GeoQueryResult("Cafe", 1.0, documentSnapshot("places/a"))
        val renamed = GeoQueryResult("New name", 1.0, documentSnapshot("places/a"))
        val moved = GeoQueryResult("New name", 2.0, documentSnapshot("places/a"))
        val refreshed = GeoQueryResult("New name", 2.0, documentSnapshot("places/a"))
        val source = flowOf(listOf(first), listOf(renamed), listOf(moved), listOf(refreshed), emptyList())
        val events = source.asGeoEvents().toList()
        assertEquals(2, events.filterIsInstance<GeoEvent.Moved<String>>().size)
        assertSame(refreshed, events.filterIsInstance<GeoEvent.Exited<String>>().single().lastItem)
        val batches = source.asGeoEventBatches().toList()
        assertEquals(4, batches.size)
        assertSame(refreshed, batches.last().exited.single())
    }
}
