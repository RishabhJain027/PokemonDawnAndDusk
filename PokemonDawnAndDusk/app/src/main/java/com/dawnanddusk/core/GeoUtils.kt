package com.dawnanddusk.core

import kotlin.math.*

object GeoUtils {
    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Converts a distance in meters to latitude degrees delta.
     */
    fun metersToLatitudeDegrees(meters: Double): Double {
        return (meters / EARTH_RADIUS_METERS) * (180.0 / Math.PI)
    }

    /**
     * Converts a distance in meters to longitude degrees delta at a given latitude.
     */
    fun metersToLongitudeDegrees(meters: Double, latitude: Double): Double {
        val radLat = Math.toRadians(latitude)
        val cosLat = max(0.0001, cos(radLat))
        return (meters / (EARTH_RADIUS_METERS * cosLat)) * (180.0 / Math.PI)
    }

    /**
     * Computes the bounding box around a center coordinate given a radius in meters.
     */
    fun buildBoundingBox(latitude: Double, longitude: Double, radiusMeters: Double): GeoBounds {
        val deltaLat = metersToLatitudeDegrees(radiusMeters)
        val deltaLon = metersToLongitudeDegrees(radiusMeters, latitude)
        return GeoBounds(
            minLat = latitude - deltaLat,
            maxLat = latitude + deltaLat,
            minLon = longitude - deltaLon,
            maxLon = longitude + deltaLon
        )
    }

    /**
     * Calculates the great-circle distance between two coordinates in meters using the Haversine formula.
     */
    fun calculateDistanceMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2.0).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2.0).pow(2.0)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return EARTH_RADIUS_METERS * c
    }

    /**
     * Checks whether two coordinates are within a specified distance threshold.
     */
    fun isWithinRadius(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double,
        radiusMeters: Double
    ): Boolean {
        return calculateDistanceMeters(lat1, lon1, lat2, lon2) <= radiusMeters
    }

    /**
     * Generates a random coordinate within a GeoBounds rectangle.
     */
    fun randomPointInBounds(bounds: GeoBounds): GeoPoint {
        val lat = bounds.minLat + Math.random() * (bounds.maxLat - bounds.minLat)
        val lon = bounds.minLon + Math.random() * (bounds.maxLon - bounds.minLon)
        return GeoPoint(lat, lon)
    }
}

data class GeoBounds(
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double
)

data class GeoPoint(
    val latitude: Double,
    val longitude: Double
)
