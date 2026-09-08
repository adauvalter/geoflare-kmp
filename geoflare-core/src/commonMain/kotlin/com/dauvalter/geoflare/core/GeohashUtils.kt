package com.dauvalter.geoflare.core

public object GeohashUtils {
    public const val DEFAULT_PRECISION: Int = 10
    public const val MAX_PRECISION: Int = 22

    /**
     * Validates that the inputted string is a valid Geohash.
     * Throws [IllegalArgumentException] if invalid.
     */
    public fun validateGeohash(geohash: String) {
        require(geohash.isNotEmpty()) { "Geohash cannot be empty" }
        for (ch in geohash) {
            require(GeoMath.BASE32_ALPHABET.indexOf(ch) != -1) {
                "Invalid character '$ch' in geohash: $geohash"
            }
        }
    }

    /**
     * Encodes coordinates into a Geohash string of a specified precision/length.
     * Uses the standard Z-order space-filling curve with alternating longitude and latitude bits.
     *
     * @param location The coordinates to encode.
     * @param precision The length of the geohash string (default is [DEFAULT_PRECISION]).
     * @return The geohash string.
     */
    public fun encode(location: GeoLocation, precision: Int = DEFAULT_PRECISION): String {
        require(precision in 1..MAX_PRECISION) {
            "Precision must be in the range [1, $MAX_PRECISION], got $precision"
        }

        var latMin = -90.0
        var latMax = 90.0
        var lonMin = -180.0
        var lonMax = 180.0

        val hash = StringBuilder(precision)
        var hashVal = 0
        var bits = 0
        var isEven = true // Bit 0 is longitude

        while (hash.length < precision) {
            if (isEven) {
                val mid = (lonMin + lonMax) / 2.0
                if (location.longitude > mid) {
                    hashVal = (hashVal shl 1) or 1
                    lonMin = mid
                } else {
                    hashVal = (hashVal shl 1)
                    lonMax = mid
                }
            } else {
                val mid = (latMin + latMax) / 2.0
                if (location.latitude > mid) {
                    hashVal = (hashVal shl 1) or 1
                    latMin = mid
                } else {
                    hashVal = (hashVal shl 1)
                    latMax = mid
                }
            }
            isEven = !isEven

            if (bits < 4) {
                bits++
            } else {
                bits = 0
                hash.append(GeoMath.BASE32_ALPHABET[hashVal])
                hashVal = 0
            }
        }

        return hash.toString()
    }

    /**
     * Decodes a geohash string into its approximate center coordinates.
     *
     * @param geohash The geohash string to decode.
     * @return The approximate center [GeoLocation] of the bounding box.
     */
    public fun decode(geohash: String): GeoLocation {
        validateGeohash(geohash)

        var latMin = -90.0
        var latMax = 90.0
        var lonMin = -180.0
        var lonMax = 180.0
        var isEven = true

        for (ch in geohash) {
            val charVal = GeoMath.BASE32_ALPHABET.indexOf(ch)
            for (bit in 4 downTo 0) {
                val mask = 1 shl bit
                val isBitSet = (charVal and mask) != 0
                if (isEven) {
                    val mid = (lonMin + lonMax) / 2.0
                    if (isBitSet) {
                        lonMin = mid
                    } else {
                        lonMax = mid
                    }
                } else {
                    val mid = (latMin + latMax) / 2.0
                    if (isBitSet) {
                        latMin = mid
                    } else {
                        latMax = mid
                    }
                }
                isEven = !isEven
            }
        }

        val latitude = (latMin + latMax) / 2.0
        val longitude = (lonMin + lonMax) / 2.0
        return GeoLocation(latitude, longitude)
    }
}
