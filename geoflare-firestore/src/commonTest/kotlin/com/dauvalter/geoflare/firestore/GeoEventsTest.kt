package com.dauvalter.geoflare.firestore

import com.dauvalter.geoflare.core.GeoLocation
import com.dauvalter.geoflare.core.GeohashUtils
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class GeoEventsTest {

    data class Place(val name: String)

    @Test
    fun testGeoEventsLifecycle() = runTest {
        val itemA = GeoQueryResult(Place("Cafe A"), distanceInKm = 1.0, id = "a")
        val itemB = GeoQueryResult(Place("Store B"), distanceInKm = 2.0, id = "b")

        // Emission 1: [A, B]
        // Emission 2: [A_moved, C] -> B exited, C entered, A moved
        val itemAMoved = GeoQueryResult(Place("Cafe A"), distanceInKm = 1.5, id = "a")
        val itemC = GeoQueryResult(Place("Park C"), distanceInKm = 0.5, id = "c")

        val snapshotFlow = flowOf(
            listOf(itemA, itemB),
            listOf(itemAMoved, itemC)
        )

        val events = snapshotFlow.asGeoEvents().toList()

        // From emission 1:
        // Entered(A), Entered(B)
        // From emission 2:
        // Moved(A), Entered(C), Exited(B)
        assertEquals(5, events.size)

        val enteredEvents = events.filterIsInstance<GeoEvent.Entered<Place>>()
        assertEquals(3, enteredEvents.size)
        assertEquals(listOf("a", "b", "c"), enteredEvents.map { it.item.id })

        val movedEvents = events.filterIsInstance<GeoEvent.Moved<Place>>()
        assertEquals(1, movedEvents.size)
        assertEquals("a", movedEvents[0].item.id)
        assertEquals(1.5, movedEvents[0].item.distanceInKm)
        assertEquals(1.0, movedEvents[0].previous.distanceInKm)

        val exitedEvents = events.filterIsInstance<GeoEvent.Exited<Place>>()
        assertEquals(1, exitedEvents.size)
        assertEquals("b", exitedEvents[0].id)
    }

    @Test
    fun testGeoEventBatches() = runTest {
        val itemA = GeoQueryResult(Place("A"), distanceInKm = 1.0, id = "a")
        val itemB = GeoQueryResult(Place("B"), distanceInKm = 2.0, id = "b")
        val itemAMoved = GeoQueryResult(Place("A"), distanceInKm = 1.2, id = "a")
        val itemC = GeoQueryResult(Place("C"), distanceInKm = 3.0, id = "c")

        val snapshotFlow = flowOf(
            listOf(itemA, itemB),
            listOf(itemAMoved, itemC)
        )

        val batches = snapshotFlow.asGeoEventBatches().toList()
        assertEquals(2, batches.size)

        // Batch 1: Initial state
        val batch1 = batches[0]
        assertEquals(listOf("a", "b"), batch1.entered.map { it.id })
        assertTrue(batch1.moved.isEmpty())
        assertTrue(batch1.exited.isEmpty())

        // Batch 2: Changes
        val batch2 = batches[1]
        assertEquals(listOf("c"), batch2.entered.map { it.id })
        assertEquals(1, batch2.moved.size)
        assertEquals("a", batch2.moved[0].first.id)
        assertEquals(listOf("b"), batch2.exited.map { it.id })
    }

    @Test
    fun testToFirestoreMap() {
        val sf = GeoLocation(37.7749, -122.4194)
        val map = sf.toFirestoreMap(
            geohashField = "geo_hash",
            latField = "lat",
            lonField = "lng",
            precision = 7
        )

        assertEquals(37.7749, map["lat"])
        assertEquals(-122.4194, map["lng"])
        assertEquals(GeohashUtils.encode(sf, precision = 7), map["geo_hash"])
    }

    @Test
    fun testGeoQueryCriteria() {
        val loc = GeoLocation(10.0, 20.0)
        val criteriaKm = GeoQueryCriteria(loc, radiusInKm = 5.0)
        assertEquals(5.0, criteriaKm.radiusInKm)
        assertEquals(5000.0, criteriaKm.radiusInMeters)

        val criteriaM = GeoQueryCriteria.inMeters(loc, radiusInMeters = 2500.0)
        assertEquals(2.5, criteriaM.radiusInKm)
        assertEquals(2500.0, criteriaM.radiusInMeters)

        assertFailsWith<IllegalArgumentException> {
            GeoQueryCriteria(loc, radiusInKm = -0.1)
        }
    }
}
