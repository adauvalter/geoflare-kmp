package com.dauvalter.geoflare.firestore

import com.dauvalter.geoflare.core.GeoLocation
import com.dauvalter.geoflare.core.GeoMath

public object GeoFilterUtils {

    /**
     * Filters items by circular radius and deduplicates items by ID.
     *
     * @param items The items to filter (may contain duplicates from multiple query bounds).
     * @param idExtractor Function returning a unique identifier for deduplication.
     * @param locationExtractor Function returning the [GeoLocation] of an item.
     * @param center The center of the search circle.
     * @param radiusInKm The maximum distance in kilometers.
     * @param sortByDistance Whether to sort the resulting list by distance in ascending order.
     * @return List of matching items paired with their calculated distance in kilometers.
     */
    public fun <T> filterAndDeduplicate(
        items: Iterable<T>,
        idExtractor: (T) -> String,
        locationExtractor: (T) -> GeoLocation,
        center: GeoLocation,
        radiusInKm: Double,
        sortByDistance: Boolean = true
    ): List<Pair<T, Double>> {
        require(radiusInKm >= 0.0) { "Radius must be non-negative, got $radiusInKm" }
        val seenIds = mutableSetOf<String>()
        val results = mutableListOf<Pair<T, Double>>()

        for (item in items) {
            val id = idExtractor(item)
            if (!seenIds.add(id)) continue

            val location = locationExtractor(item)
            val distanceKm = GeoMath.distance(center, location)

            if (distanceKm <= radiusInKm) {
                results.add(item to distanceKm)
            }
        }

        if (sortByDistance) {
            results.sortBy { it.second }
        }

        return results
    }
}
