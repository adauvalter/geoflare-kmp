package com.dauvalter.geoflare.firestore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Represents granular changes of tracked objects within a geographic query area.
 */
public sealed interface GeoEvent<T> {
    /**
     * An object has entered the query radius or appeared for the first time.
     */
    public data class Entered<T>(
        public val item: GeoQueryResult<T>
    ) : GeoEvent<T>

    /**
     * An object currently within the radius has updated its position or data.
     */
    public data class Moved<T>(
        public val item: GeoQueryResult<T>,
        public val previous: GeoQueryResult<T>
    ) : GeoEvent<T>

    /**
     * An object has left the query radius or was deleted.
     */
    public data class Exited<T>(
        public val id: String,
        public val lastItem: GeoQueryResult<T>
    ) : GeoEvent<T>
}

/**
 * An aggregation of events occurring in a single snapshot emission.
 */
public data class GeoEventBatch<T>(
    public val entered: List<GeoQueryResult<T>> = emptyList(),
    public val moved: List<Pair<GeoQueryResult<T>, GeoQueryResult<T>>> = emptyList(),
    public val exited: List<GeoQueryResult<T>> = emptyList()
) {
    public val isEmpty: Boolean
        get() = entered.isEmpty() && moved.isEmpty() && exited.isEmpty()

    public val isNotEmpty: Boolean
        get() = !isEmpty
}

/**
 * Transforms a stream of query snapshot lists into a stream of batched [GeoEventBatch] events.
 */
public fun <T> Flow<List<GeoQueryResult<T>>>.asGeoEventBatches(): Flow<GeoEventBatch<T>> = flow {
    var previousMap = emptyMap<String, GeoQueryResult<T>>()

    collect { currentList ->
        val currentMap = currentList.associateBy { it.id }

        val entered = mutableListOf<GeoQueryResult<T>>()
        val moved = mutableListOf<Pair<GeoQueryResult<T>, GeoQueryResult<T>>>()
        val exited = mutableListOf<GeoQueryResult<T>>()

        for ((id, currentItem) in currentMap) {
            val previousItem = previousMap[id]
            if (previousItem == null) {
                entered.add(currentItem)
            } else if (previousItem != currentItem) {
                moved.add(currentItem to previousItem)
            }
        }

        for ((id, previousItem) in previousMap) {
            if (id !in currentMap) {
                exited.add(previousItem)
            }
        }

        val batch = GeoEventBatch(entered = entered, moved = moved, exited = exited)
        if (batch.isNotEmpty || previousMap.isEmpty()) {
            emit(batch)
        }

        previousMap = currentMap
    }
}

/**
 * Transforms a stream of query snapshot lists into a stream of individual [GeoEvent] events.
 */
public fun <T> Flow<List<GeoQueryResult<T>>>.asGeoEvents(): Flow<GeoEvent<T>> = flow {
    var previousMap = emptyMap<String, GeoQueryResult<T>>()

    collect { currentList ->
        val currentMap = currentList.associateBy { it.id }

        for ((id, currentItem) in currentMap) {
            val previousItem = previousMap[id]
            if (previousItem == null) {
                emit(GeoEvent.Entered(currentItem))
            } else if (previousItem != currentItem) {
                emit(GeoEvent.Moved(currentItem, previousItem))
            }
        }

        for ((id, previousItem) in previousMap) {
            if (id !in currentMap) {
                emit(GeoEvent.Exited(id, previousItem))
            }
        }

        previousMap = currentMap
    }
}
