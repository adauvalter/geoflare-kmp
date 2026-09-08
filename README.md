# GeoFlare (KMP)

[![CI](https://github.com/adauvalter/geoflare-kmp/actions/workflows/gradle.yml/badge.svg)](https://github.com/adauvalter/geoflare-kmp/actions/workflows/gradle.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4-blue.svg?logo=kotlin)](https://kotlinlang.org)

**GeoFlare** is a modern, lightweight **Kotlin Multiplatform (KMP)** library for geospatial queries and geohashing, inspired by Firebase's GeoFire and `geofire-common`.

Unlike traditional GeoFire libraries tightly coupled to a single SDK or legacy callback patterns, GeoFlare is modular, cross-platform, and designed for modern Kotlin Coroutines and `Flow`.

---

## Architecture

GeoFlare is designed as a multi-module KMP library:

* **`geoflare-core`**: Pure Kotlin Multiplatform module with **zero external dependencies**. Handles Base32 Geohash encoding/decoding (Z-order space-filling curve), Haversine distance calculations, WGS84 geodesy, and bounding box query bounds math.
* **`geoflare-firestore`** *(in development)*: Seamless integration with Cloud Firestore using Kotlin Coroutines and `Flow`, providing real-time geo-queries and client-side false-positive distance filtering.

---

## Supported Platforms

* **JVM** (Java 11+)
* **Android** (minSdk 24)
* **iOS** (`iosArm64`, `iosSimulatorArm64`, `iosX64`)
* **Linux** (`linuxX64`)

---

## Getting Started

### Gradle Dependency

Add `geoflare-core` to your `commonMain` dependencies in `build.gradle.kts`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.adauvalter.geoflare:geoflare-core:0.1.0")
        }
    }
}
```

---

## Quick Example (`geoflare-core`)

### 1. Coordinates and Distance

```kotlin
import io.github.adauvalter.geoflare.core.GeoLocation
import io.github.adauvalter.geoflare.core.GeoMath

val sf = GeoLocation(latitude = 37.7749, longitude = -122.4194)
val sj = GeoLocation(latitude = 37.3382, longitude = -121.8863)

// Calculate Haversine distance in kilometers or meters
val distanceKm = GeoMath.distance(sf, sj)       // ~67.8 km
val distanceMeters = GeoMath.distanceInMeters(sf, sj)
```

### 2. Geohash Encoding & Decoding

```kotlin
import io.github.adauvalter.geoflare.core.GeohashUtils

// Encode coordinates to a geohash (default precision: 10 chars)
val hash = GeohashUtils.encode(sf) // "9q8yyk8ytp"

// Encode with custom precision (1 to 22)
val shortHash = GeohashUtils.encode(sf, precision = 5) // "9q8yy"

// Decode back to approximate coordinates
val location = GeohashUtils.decode(hash)
```

### 3. Calculating Query Bounds

To query points within a radius (e.g. 5 km around a center), compute the bounding geohash ranges:

```kotlin
import io.github.adauvalter.geoflare.core.GeoQueryUtils

val center = GeoLocation(latitude = 37.7749, longitude = -122.4194)
val bounds = GeoQueryUtils.getGeohashQueryBounds(center, radiusInKm = 5.0)

// Each bound provides [startAt, endAt] strings to query your database
for (bound in bounds) {
    println("Range: ${bound.startAt} .. ${bound.endAt}")
}
```

### Target Usage with Database Queries (e.g. Firestore)

```kotlin
// 1. Generate query bounds
val bounds = GeoQueryUtils.getGeohashQueryBounds(center, radiusInKm = 5.0)

// 2. Query each range in your database
val queries = bounds.map { bound ->
    firestore.collection("places")
        .orderBy("geohash")
        .startAt(bound.startAt)
        .endAt(bound.endAt)
        .snapshots() // Flow<QuerySnapshot>
}

// 3. Merge flows and filter points outside the exact circle
val nearbyPlacesFlow = merge(*queries.toTypedArray()).map { snapshot ->
    snapshot.map { it.data<Place>() }
        .filter { place ->
            GeoMath.distance(center, place.location) <= 5.0
        }
}
```

---

## Building and Testing

To build the library and run tests across all supported targets:

```bash
./gradlew check
```

---

## License

```
Copyright 2026 Anton Dauwalter

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```
