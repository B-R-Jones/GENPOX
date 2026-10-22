package com.example.genpox.data.location

import com.example.genpox.domain.model.LocationData
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface LocationRepository {
    val locationUpdates: StateFlow<LocationData?>
    fun startLocationUpdates()
    fun stopLocationUpdates()
    suspend fun getLastKnownLocation(): LocationData?
}

@Singleton
class DefaultLocationRepository @Inject constructor(
    private val locationDataSource: LocationDataSource,
) : LocationRepository {
    override val locationUpdates: StateFlow<LocationData?> get() = locationDataSource.locationUpdates
    override fun startLocationUpdates() = locationDataSource.startLocationUpdates()
    override fun stopLocationUpdates() = locationDataSource.stopLocationUpdates()
    override suspend fun getLastKnownLocation(): LocationData? = locationDataSource.getLastKnownLocation()
}
