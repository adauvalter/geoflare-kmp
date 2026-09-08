package com.dauvalter.geoflare.firestore

import com.dauvalter.geoflare.core.GeoLocation
import com.dauvalter.geoflare.core.GeoQueryUtils
import com.google.android.gms.tasks.Tasks
import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.invoke
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.mockito.Mockito.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.google.firebase.firestore.CollectionReference as NativeCollection
import com.google.firebase.firestore.EventListener as NativeEventListener
import com.google.firebase.firestore.ListenerRegistration as NativeListener
import com.google.firebase.firestore.QuerySnapshot as NativeQuerySnapshot

@OptIn(ExperimentalCoroutinesApi::class)
class GeoQueryExecutionTest {
    @Serializable
    data class Place(val name: String, val latitude: Double, val longitude: Double) {
        val location: GeoLocation get() = GeoLocation(latitude, longitude)
    }

    private val center = GeoLocation(37.7749, -122.4194)

    private class Transport {
        val snapshot = mock(NativeQuerySnapshot::class.java)
        val listeners = mutableListOf<NativeListener>()
        val native: NativeCollection = mock(NativeCollection::class.java) { invocation ->
            when (invocation.method.name) {
                "get" -> Tasks.forResult(snapshot)
                "addSnapshotListener" -> {
                    @Suppress("UNCHECKED_CAST")
                    val callback = invocation.arguments.filterIsInstance<NativeEventListener<*>>().single()
                        as NativeEventListener<NativeQuerySnapshot>
                    callback.onEvent(snapshot, null)
                    mock(NativeListener::class.java).also { listeners.add(it) }
                }
                else -> RETURNS_SELF.answer(invocation)
            }
        }
        val collection = CollectionReference(native)
    }

    private fun transport(): Transport = Transport().also {
        val near = nativeDocument("places/near", mapOf("name" to "Near", "latitude" to center.latitude, "longitude" to center.longitude))
        val far = nativeDocument("places/far", mapOf("name" to "Far", "latitude" to 0.0, "longitude" to 0.0))
        `when`(it.snapshot.documents).thenReturn(listOf(near, far))
    }

    @Test
    fun safeAndCollectionOverloadsDecodeFilterAndDeduplicateResults() = runTest {
        val transport = transport()
        val source = transport.collection.geoQuery().where { "active" equalTo true }
        val lists = listOf(
            source.geoGet<Place>(center, 5.0) { it.location },
            source.geoGetInMeters<Place>(center, 5000.0) { it.location },
            source.geoSnapshots<Place>(center, 5.0) { it.location }.first(),
            source.geoSnapshotsInMeters<Place>(center, 5000.0) { it.location }.first(),
            transport.collection.geoGet<Place>(center, 5.0) { it.location },
            transport.collection.geoGetInMeters<Place>(center, 5000.0) { it.location },
            transport.collection.geoSnapshots<Place>(center, 5.0) { it.location }.first(),
            transport.collection.geoSnapshotsInMeters<Place>(center, 5000.0) { it.location }.first()
        )
        lists.forEach { results ->
            assertEquals(listOf("places/near"), results.map { it.key })
            assertEquals("Near", results.single().data.name)
            assertEquals(0.0, results.single().distanceInKm)
        }
        val extract: (dev.gitlive.firebase.firestore.DocumentSnapshot) -> GeoLocation = { it.data<Place>().location }
        val rawLists = listOf(
            source.geoGetRaw(center, 5.0, locationExtractor = extract),
            source.geoSnapshotsRaw(center, 5.0, locationExtractor = extract).first(),
            transport.collection.geoGetRaw(center, 5.0, locationExtractor = extract),
            transport.collection.geoSnapshotsRaw(center, 5.0, locationExtractor = extract).first()
        )
        rawLists.forEach { assertEquals(listOf("places/near"), it.map { item -> item.key }) }
        assertTrue(transport.listeners.isNotEmpty())
        transport.listeners.forEach { verify(it).remove() }
    }

    @Test
    fun dynamicPrecisionChangesReplaceAndCancelSubscriptions() = runTest {
        val transport = transport()
        val criteria = MutableStateFlow(GeoQueryCriteria(center, 0.0001, geohashPrecision = 10))
        val emissions = mutableListOf<List<GeoQueryResult<Place>>>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            transport.collection.geoQuery().geoSnapshots<Place>(criteria) { it.location }.collect { emissions.add(it) }
        }
        runCurrent()
        val previousListeners = transport.listeners.toList()
        assertTrue(previousListeners.isNotEmpty())
        clearInvocations(transport.native)
        criteria.value = criteria.value.copy(geohashPrecision = 4)
        runCurrent()
        previousListeners.forEach { verify(it).remove() }
        val bounds = GeoQueryUtils.getGeohashQueryBounds(center, 0.0001, 4)
        bounds.forEach { verify(transport.native).startAt(it.startAt) }
        assertEquals(2, emissions.size)
        assertEquals(listOf("places/near"), emissions.last().map { it.key })
        job.cancel()
        runCurrent()
        transport.listeners.forEach { verify(it).remove() }
    }
}
