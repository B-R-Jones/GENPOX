package com.example.genpox.util

import com.example.genpox.data.location.LocationRepository
import com.example.genpox.domain.model.LocationData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeLocationRepository : LocationRepository {
    private val _locationUpdates = MutableStateFlow<LocationData?>(null)
    override val locationUpdates: StateFlow<LocationData?> = _locationUpdates.asStateFlow()
    override fun startLocationUpdates() {}
    override fun stopLocationUpdates() {}
    override suspend fun getLastKnownLocation(): LocationData? = null

    fun emitLocation(lat: Double, lng: Double) {
        _locationUpdates.value = LocationData(latitude = lat, longitude = lng)
    }
}
