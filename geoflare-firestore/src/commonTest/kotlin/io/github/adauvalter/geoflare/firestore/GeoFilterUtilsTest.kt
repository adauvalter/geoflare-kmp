package io.github.adauvalter.geoflare.firestore

import io.github.adauvalter.geoflare.core.GeoLocation
import kotlin.math.abs
import kotlin.test.*

class GeoFilterUtilsTest {

    data class Place(
        val id: String,
        val name: String,
        val location: GeoLocation
    )

    private val sfCenter = GeoLocation(37.7749, -122.4194)
    private val sfDowntown = Place("p1", "SF Downtown", GeoLocation(37.7833, -122.4167)) // ~1.0 km
    private val oakland = Place("p2", "Oakland", GeoLocation(37.8044, -122.2712))       // ~13.5 km
    private val berkeley = Place("p3", "Berkeley", GeoLocation(37.8715, -122.2730))     // ~16.8 km
    private val sanJose = Place("p4", "San Jose", GeoLocation(37.3382, -121.8863))       // ~67.8 km

    @Test
    fun testDistanceFilteringEliminatesFalsePositives() {
        val places = listOf(sfDowntown, oakland, berkeley, sanJose)

        // 15 km radius around SF center should include SF Downtown and Oakland, but exclude Berkeley and San Jose
        val results = GeoFilterUtils.filterAndDeduplicate(
            items = places,
            idExtractor = { it.id },
            locationExtractor = { it.location },
            center = sfCenter,
            radiusInKm = 15.0
        )

        assertEquals(2, results.size)
        assertEquals("p1", results[0].first.id)
        assertEquals("p2", results[1].first.id)
    }

    @Test
    fun testDeduplicationAcrossMultipleQueryBounds() {
        // Same document reported by multiple overlapping geohash range queries
        val duplicatePlaces = listOf(
            sfDowntown,
            oakland,
            sfDowntown.copy(), // Duplicate from another range
            berkeley,
            oakland.copy()     // Duplicate from another range
        )

        val results = GeoFilterUtils.filterAndDeduplicate(
            items = duplicatePlaces,
            idExtractor = { it.id },
            locationExtractor = { it.location },
            center = sfCenter,
            radiusInKm = 20.0
        )

        assertEquals(3, results.size)
        val ids = results.map { it.first.id }
        assertEquals(listOf("p1", "p2", "p3"), ids)
    }

    @Test
    fun testSortingByDistance() {
        val unsortedPlaces = listOf(berkeley, sfDowntown, oakland)

        val sortedResults = GeoFilterUtils.filterAndDeduplicate(
            items = unsortedPlaces,
            idExtractor = { it.id },
            locationExtractor = { it.location },
            center = sfCenter,
            radiusInKm = 25.0,
            sortByDistance = true
        )

        assertEquals("p1", sortedResults[0].first.id) // ~1.0 km
        assertEquals("p2", sortedResults[1].first.id) // ~13.5 km
        assertEquals("p3", sortedResults[2].first.id) // ~16.8 km

        assertTrue(sortedResults[0].second < sortedResults[1].second)
        assertTrue(sortedResults[1].second < sortedResults[2].second)
    }

    @Test
    fun testPreserveOrderWhenNotSorted() {
        val places = listOf(oakland, sfDowntown)

        val unsortedResults = GeoFilterUtils.filterAndDeduplicate(
            items = places,
            idExtractor = { it.id },
            locationExtractor = { it.location },
            center = sfCenter,
            radiusInKm = 25.0,
            sortByDistance = false
        )

        assertEquals("p2", unsortedResults[0].first.id)
        assertEquals("p1", unsortedResults[1].first.id)
    }

    @Test
    fun testNegativeRadiusThrows() {
        assertFailsWith<IllegalArgumentException> {
            GeoFilterUtils.filterAndDeduplicate(
                items = listOf(sfDowntown),
                idExtractor = { it.id },
                locationExtractor = { it.location },
                center = sfCenter,
                radiusInKm = -1.0
            )
        }
    }

    @Test
    fun testEmptyInputReturnsEmpty() {
        val results = GeoFilterUtils.filterAndDeduplicate<Place>(
            items = emptyList(),
            idExtractor = { it.id },
            locationExtractor = { it.location },
            center = sfCenter,
            radiusInKm = 10.0
        )
        assertTrue(results.isEmpty())
    }
}
