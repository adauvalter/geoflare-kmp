# GeoFlare (KMP)

[![CI](https://github.com/adauvalter/geoflare-kmp/actions/workflows/gradle.yml/badge.svg)](https://github.com/adauvalter/geoflare-kmp/actions/workflows/gradle.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4-blue.svg?logo=kotlin)](https://kotlinlang.org)

**GeoFlare** is a modern, lightweight **Kotlin Multiplatform (KMP)** library for geospatial queries and geohashing, inspired by Firebase's GeoFire and `geofire-common`.

Unlike traditional GeoFire libraries tightly coupled to a single SDK or legacy callback patterns, GeoFlare is modular, cross-platform, and designed for modern Kotlin Coroutines and `Flow`.

---

## Modules

* **`geoflare-core`**: Pure Kotlin Multiplatform module with **zero external dependencies**. Handles Base32 Geohash encoding/decoding (Z-order space-filling curve), Haversine distance calculations, WGS84 geodesy, and bounding box query bounds math.
* **`geoflare-firestore`**: Seamless integration with Cloud Firestore using Kotlin Coroutines and `Flow`, powered by the GitLive Firebase SDK (`dev.gitlive:firebase-firestore`). Provides real-time geo-queries, parallel fetching, deduplication, and automatic client-side distance filtering.

---

## Supported Platforms

| Module | JVM | Android | iOS | Linux |
| :--- | :---: | :---: | :---: | :---: |
| **`geoflare-core`** | ✅ | ✅ | ✅ (`arm64`, `simulatorArm64`, `x64`) | ✅ (`x64`) |
| **`geoflare-firestore`** | ✅ | ✅ | ✅ (`arm64`, `simulatorArm64`, `x64`) | — |

---

## Installation

Add the dependencies to your `commonMain` source set in `build.gradle.kts`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            // Pure geohashing and math (zero dependencies)
            implementation("io.github.adauvalter.geoflare:geoflare-core:0.1.0")

            // Firestore Coroutines & Flow integration
            implementation("io.github.adauvalter.geoflare:geoflare-firestore:0.1.0")
        }
    }
}
```

---

## Quick Start

### 1. Coordinates and Distance (`geoflare-core`)

```kotlin
import io.github.adauvalter.geoflare.core.GeoLocation
import io.github.adauvalter.geoflare.core.GeoMath

val sf = GeoLocation(latitude = 37.7749, longitude = -122.4194)
val sj = GeoLocation(latitude = 37.3382, longitude = -121.8863)

// Calculate Haversine distance in kilometers or meters
val distanceKm = GeoMath.distance(sf, sj)       // ~67.8 km
val distanceMeters = GeoMath.distanceInMeters(sf, sj)
```

### 2. Geohash Encoding & Decoding (`geoflare-core`)

```kotlin
import io.github.adauvalter.geoflare.core.GeohashUtils

// Encode coordinates to a geohash (default precision: 10 chars)
val hash = GeohashUtils.encode(sf) // "9q8yyk8ytp"

// Encode with custom precision (1 to 22 chars)
val shortHash = GeohashUtils.encode(sf, precision = 5) // "9q8yy"

// Decode back to approximate coordinates
val location = GeohashUtils.decode(hash)
```

### 3. Real-time Firestore Geo-queries (`geoflare-firestore`)

Store a `geohash` field in your Firestore documents using `GeohashUtils.encode(location)`. Then query reactively:

```kotlin
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import io.github.adauvalter.geoflare.core.GeoLocation
import io.github.adauvalter.geoflare.firestore.geoSnapshots
import kotlinx.serialization.Serializable

@Serializable
data class Place(
    val name: String,
    val geohash: String,
    val latitude: Double,
    val longitude: Double
) {
    val location: GeoLocation get() = GeoLocation(latitude, longitude)
}

val center = GeoLocation(latitude = 37.7749, longitude = -122.4194)

// Real-time Flow of nearby places with automatic distance calculation & filtering:
val nearbyPlacesFlow = Firebase.firestore.collection("places")
    .geoSnapshots<Place>(
        center = center,
        radiusInKm = 5.0,
        geohashField = "geohash", // default
        sortByDistance = true,     // default
        locationExtractor = { it.location }
    )

nearbyPlacesFlow.collect { results ->
    for (result in results) {
        println("${result.data.name} is ${result.distanceInKm} km away")
    }
}
```

### 4. One-Shot Firestore Geo-queries (`geoflare-firestore`)

```kotlin
import io.github.adauvalter.geoflare.firestore.geoGet

// Suspending one-shot fetch (queries all ranges concurrently):
val places: List<GeoQueryResult<Place>> = Firebase.firestore.collection("places")
    .geoGet<Place>(
        center = center,
        radiusInKm = 5.0,
        locationExtractor = { it.location }
    )
```

---

## Building and Testing

To build the library and run tests across all modules and targets:

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
