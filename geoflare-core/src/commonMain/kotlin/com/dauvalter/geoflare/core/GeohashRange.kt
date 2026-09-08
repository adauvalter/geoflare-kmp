package com.dauvalter.geoflare.core

/**
 * A range of geohashes for database querying.
 *
 * All geohashes within this range satisfy: startAt <= geohash <= endAt (lexicographically).
 *
 * @property startAt The inclusive lower bound of the geohash range.
 * @property endAt The upper bound of the geohash range.
 */
public data class GeohashRange(
    public val startAt: String,
    public val endAt: String
)
