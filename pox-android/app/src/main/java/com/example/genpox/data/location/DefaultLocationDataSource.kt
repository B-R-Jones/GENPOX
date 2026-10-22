package com.example.genpox.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import com.example.genpox.domain.model.LocationData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultLocationDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocationDataSource {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val _locationUpdates = MutableStateFlow<LocationData?>(null)
    override val locationUpdates: StateFlow<LocationData?> = _locationUpdates.asStateFlow()

    private var isListening = false

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            _locationUpdates.value = LocationData(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy,
                timestamp = location.time,
            )
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    @SuppressLint("MissingPermission")
    override fun startLocationUpdates() {
        if (isListening) return
        try {
            val initialLoc = getLastKnownLocationInternal()
            if (initialLoc != null) {
                _locationUpdates.value = initialLoc
            }

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000L,
                    5f,
                    locationListener,
                )
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000L,
                    5f,
                    locationListener,
                )
            }
            isListening = true
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun stopLocationUpdates() {
        if (!isListening) return
        try {
            locationManager.removeUpdates(locationListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isListening = false
    }

    override suspend fun getLastKnownLocation(): LocationData? = getLastKnownLocationInternal()

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocationInternal(): LocationData? = try {
        var bestLoc: Location? = null
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            val loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (loc != null) bestLoc = loc
        }
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            val loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (loc != null && (bestLoc == null || loc.time > bestLoc.time)) {
                bestLoc = loc
            }
        }
        bestLoc?.let {
            LocationData(
                latitude = it.latitude,
                longitude = it.longitude,
                accuracy = it.accuracy,
                timestamp = it.time,
            )
        }
    } catch (e: Exception) {
        null
    }
}
