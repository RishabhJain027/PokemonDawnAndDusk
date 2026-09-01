package com.dawnanddusk.services

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import com.dawnanddusk.core.GeoPoint
import com.dawnanddusk.core.GeoUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocationService(private val context: Context) {

    private val locationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    }

    // Default starting point: Pallet Town / Central hub coordinates (or dynamic)
    private val _currentLocation = MutableStateFlow(GeoPoint(35.6895, 139.6917))
    val currentLocation: StateFlow<GeoPoint> = _currentLocation.asStateFlow()

    private val _isSimulatedMode = MutableStateFlow(false)
    val isSimulatedMode: StateFlow<Boolean> = _isSimulatedMode.asStateFlow()

    private var isListening = false

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (!_isSimulatedMode.value) {
                _currentLocation.value = GeoPoint(location.latitude, location.longitude)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    @SuppressLint("MissingPermission")
    fun startListening() {
        if (isListening || _isSimulatedMode.value) return
        try {
            val hasGps = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
            val hasNetwork = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true

            if (hasGps) {
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000L,
                    3.0f,
                    locationListener
                )
                locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let {
                    _currentLocation.value = GeoPoint(it.latitude, it.longitude)
                }
                isListening = true
            } else if (hasNetwork) {
                locationManager?.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    2000L,
                    3.0f,
                    locationListener
                )
                locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let {
                    _currentLocation.value = GeoPoint(it.latitude, it.longitude)
                }
                isListening = true
            }
        } catch (_: SecurityException) {
            // Permission not granted, will rely on simulated location mode
            _isSimulatedMode.value = true
        }
    }

    fun stopListening() {
        if (!isListening) return
        try {
            locationManager?.removeUpdates(locationListener)
        } catch (_: Exception) {}
        isListening = false
    }

    fun setSimulatedMode(enabled: Boolean) {
        _isSimulatedMode.value = enabled
        if (enabled) {
            stopListening()
        } else {
            startListening()
        }
    }

    /**
     * Moves the trainer by a delta distance in meters (Simulated walk joystick)
     */
    fun moveSimulated(deltaXNorthMeters: Double, deltaYEastMeters: Double) {
        val current = _currentLocation.value
        val dLat = GeoUtils.metersToLatitudeDegrees(deltaXNorthMeters)
        val dLon = GeoUtils.metersToLongitudeDegrees(deltaYEastMeters, current.latitude)
        _currentLocation.value = GeoPoint(current.latitude + dLat, current.longitude + dLon)
    }

    fun setLocation(lat: Double, lon: Double) {
        _currentLocation.value = GeoPoint(lat, lon)
    }
}
