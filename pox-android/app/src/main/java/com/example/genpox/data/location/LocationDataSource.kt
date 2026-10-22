package com.example.genpox.data.location

import com.example.genpox.domain.model.LocationData
import kotlinx.coroutines.flow.StateFlow

interface LocationDataSource {
    val locationUpdates: StateFlow<LocationData?>
    fun startLocationUpdates()
    fun stopLocationUpdates()
    suspend fun getLastKnownLocation(): LocationData?
}
