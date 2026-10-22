package com.example.genpox.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.genpox.audio.PoxSynthManager
import com.example.genpox.data.*
import com.example.genpox.data.location.LocationRepository
import com.example.genpox.data.repository.*
import com.example.genpox.map.tile.TileStreamManager
import com.example.genpox.util.DispatchersProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val missionRepository: MissionRepository,
    private val creatureRepository: CreatureRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val geneStockRepository: GeneStockRepository,
    private val synthManager: PoxSynthManager,
    private val locationRepository: LocationRepository,
    val tileStreamManager: TileStreamManager,
    private val dispatchers: DispatchersProvider,
) : ViewModel() {

    val reactorSalt: StateFlow<Float> = MutableStateFlow(0.05f)

    fun startLocationUpdates() {
        locationRepository.startLocationUpdates()
    }

    fun stopLocationUpdates() {
        locationRepository.stopLocationUpdates()
    }

    var onLog: ((String) -> Unit)? = null

    private fun addLog(message: String) {
        onLog?.invoke(message)
    }

    // State flows from Repository
    val activeMissions: StateFlow<List<HarvestMission>> = missionRepository.activeMissions
        .flowOn(dispatchers.io)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val harvestingCreatureIds: StateFlow<Set<String>> = missionRepository.activeMissions
        .map { missions -> missions.filter { !it.isReturned }.map { it.creatureId }.toSet() }
        .flowOn(dispatchers.io)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val activeMissionCoords: StateFlow<Set<String>> = missionRepository.activeMissions
        .map { missions -> missions.filter { !it.isReturned }.map { "${it.lat},${it.lng}" }.toSet() }
        .flowOn(dispatchers.io)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val allMissions: StateFlow<List<HarvestMission>> = missionRepository.allMissions
        .flowOn(dispatchers.io)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val depletedAnomalyCoords: StateFlow<Set<String>> = missionRepository.allMissions
        .map { missions -> missions.filter { it.isReturned }.map { "${it.lat},${it.lng}" }.toSet() }
        .flowOn(dispatchers.io)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val scanRadius: StateFlow<Float> = preferencesRepository.scanRadius
        .stateIn(viewModelScope, SharingStarted.Eagerly, 55f)

    // State Variables
    private val _trackedMissionId = MutableStateFlow<String?>(null)
    val trackedMissionId: StateFlow<String?> = _trackedMissionId.asStateFlow()

    private val _selectedAnomalyId = MutableStateFlow<String?>(null)
    val selectedAnomalyId: StateFlow<String?> = _selectedAnomalyId.asStateFlow()

    private val _scannerSubTab = MutableStateFlow("radar")
    val scannerSubTab: StateFlow<String> = _scannerSubTab.asStateFlow()

    private val _listSubTab = MutableStateFlow("anomalies")
    val listSubTab: StateFlow<String> = _listSubTab.asStateFlow()

    private val _latitude = MutableStateFlow(37.4220)
    val latitude: StateFlow<Double> = _latitude.asStateFlow()

    private val _longitude = MutableStateFlow(-122.0841)
    val longitude: StateFlow<Double> = _longitude.asStateFlow()

    private val _zoomMultiplier = MutableStateFlow(1.0f)
    val zoomMultiplier: StateFlow<Float> = _zoomMultiplier.asStateFlow()

    private val _proceduralHeatwaves = MutableStateFlow<List<String>>(emptyList())
    val proceduralHeatwaves: StateFlow<List<String>> = _proceduralHeatwaves.asStateFlow()

    private val _rawAnomalies = MutableStateFlow<List<PoxAnomaly>>(emptyList())
    val rawAnomalies: StateFlow<List<PoxAnomaly>> = _rawAnomalies.asStateFlow()
    private val _sectorAnomalies = ConcurrentHashMap<String, List<PoxAnomaly>>()
    private val _spawnedSectorsWithGeometry = Collections.synchronizedSet(mutableSetOf<String>())

    private val _celestialWells = MutableStateFlow<List<CelestialWell>>(emptyList())
    val celestialWells: StateFlow<List<CelestialWell>> = _celestialWells.asStateFlow()

    private val _showAnomaliesOnMap = MutableStateFlow(true)
    val showAnomaliesOnMap: StateFlow<Boolean> = _showAnomaliesOnMap.asStateFlow()

    private val _showFontsOnMap = MutableStateFlow(true)
    val showFontsOnMap: StateFlow<Boolean> = _showFontsOnMap.asStateFlow()

    private val _showWellsOnMap = MutableStateFlow(true)
    val showWellsOnMap: StateFlow<Boolean> = _showWellsOnMap.asStateFlow()

    private val _reactorSalt = MutableStateFlow(0.05f)

    private val harvestMissionManager = com.example.genpox.ui.scanner.HarvestMissionManager(
        missionRepository = missionRepository,
        creatureRepository = creatureRepository,
        geneStockRepository = geneStockRepository,
        preferencesRepository = preferencesRepository,
        synthManager = synthManager,
        getUserLocation = { Pair(_latitude.value, _longitude.value) },
        getRawAnomalies = { _rawAnomalies.value },
        getCelestialWells = { _celestialWells.value },
        getReactorSalt = { _reactorSalt.value },
        setTrackedMissionId = { id -> _trackedMissionId.value = id },
        getTrackedMissionId = { _trackedMissionId.value },
        logEmitter = { msg -> addLog(msg) },
    )

    // Map anomalies combining logic (pinned + normal)
    val anomalies: StateFlow<List<PoxAnomaly>> = combine(
        _latitude,
        _longitude,
        _rawAnomalies,
        activeMissions,
    ) { userLat, userLng, rawList, ongoingMissions ->
        val pinnedAnomalies = ongoingMissions.filter { !it.isReturned }.map { m ->
            val animId = "ANM-ACTIVE-${m.creatureId}"
            val animName = "Active Anomaly"
            val dist = calculateDistanceInFeet(userLat, userLng, m.lat, m.lng)
            val baseAnom = PoxAnomaly(
                id = animId,
                name = animName,
                lat = m.lat,
                lng = m.lng,
                gene = m.harvestedPayload.firstOrNull() ?: "AGTCGTAC",
                faction = "Anomaly",
                distance = dist,
                heatZoneDiameter = 500.0 * 3.28084,
                density = 0.0,
            )
            WaveMath.updateAnomalyWithLifecycle(baseAnom, WaveMath.getAdjustedTime())
        }
        val remaining = rawList.filter { raw -> pinnedAnomalies.none { pinned -> Math.abs(pinned.lat - raw.lat) < 0.0001 && Math.abs(pinned.lng - raw.lng) < 0.0001 } }

        val updatedPinned = pinnedAnomalies.map {
            it.copy(distance = calculateDistanceInFeet(userLat, userLng, it.lat, it.lng))
        }
        val updatedRemaining = remaining.map {
            it.copy(distance = calculateDistanceInFeet(userLat, userLng, it.lat, it.lng))
        }

        updatedPinned + updatedRemaining
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val selectedAnomaly: StateFlow<PoxAnomaly?> = combine(selectedAnomalyId, anomalies) { id, list ->
        list.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var missionTickerJob: Job? = null
    private var heatwaveJob: Job? = null

    fun stopAllTickers() {
        missionTickerJob?.cancel()
        heatwaveJob?.cancel()
    }

    public override fun onCleared() {
        super.onCleared()
        stopAllTickers()
    }

    init {
        viewModelScope.launch(dispatchers.main) {
            locationRepository.locationUpdates.collect { loc ->
                loc?.let {
                    updateLocation(it.latitude, it.longitude)
                }
            }
        }

        viewModelScope.launch(dispatchers.default) {
            tileStreamManager.updateCenter(_latitude.value, _longitude.value, radius = TileStreamManager.DEFAULT_RADIUS)
        }

        viewModelScope.launch(dispatchers.default) {
            tileStreamManager.activeTiles.collect { tileMap ->
                if (tileMap.isNotEmpty()) {
                    for ((coord, geom) in tileMap) {
                        val tileKey = "${coord.z}/${coord.x}/${coord.y}"
                        if (!_spawnedSectorsWithGeometry.contains(tileKey)) {
                            val (tileLat, tileLng) = com.example.genpox.map.tile.TileMath.tileToCenterLatLng(coord.z, coord.x, coord.y)
                            val spawned = com.example.genpox.data.AnomalySpawningEngine.generateAnomaliesForSector(
                                sectorLat = tileLat,
                                sectorLng = tileLng,
                                roads = geom.roads.map { way -> way.map { Pair(it.lat, it.lng) } },
                                buildings = geom.buildings,
                                existingAnomalies = _sectorAnomalies.values.flatten(),
                            )
                            if (spawned.isNotEmpty()) {
                                _sectorAnomalies[tileKey] = spawned
                                _rawAnomalies.value = _sectorAnomalies.values.flatten()
                            }
                            if (geom.roads.isNotEmpty() || geom.buildings.isNotEmpty()) {
                                _spawnedSectorsWithGeometry.add(tileKey)
                            }
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            reactorSalt.collect { _reactorSalt.value = it }
        }

        val startLat = _latitude.value
        val startLng = _longitude.value
        val ghDLat = 300.0 / 364000.0
        val ghDLng = -200.0 / (364000.0 * Math.cos(Math.toRadians(startLat)))
        val magDLat = -500.0 / 364000.0
        val magDLng = 400.0 / (364000.0 * Math.cos(Math.toRadians(startLat)))
        val gravDLat = 400.0 / 364000.0
        val gravDLng = 600.0 / (364000.0 * Math.cos(Math.toRadians(startLat)))

        _celestialWells.value = listOf(
            CelestialWell("WELL-GH", "Venusian Greenhouse Anomaly", startLat + ghDLat, startLng + ghDLng, 800.0, "GREENHOUSE", 1.0),
            CelestialWell("WELL-MAG", "Jovian Magnetospheric Anomaly", startLat + magDLat, startLng + magDLng, 1000.0, "MAGNETOSPHERIC", 1.0),
            CelestialWell("WELL-GRAV", "Asteroid Gravitational Anomaly", startLat + gravDLat, startLng + gravDLng, 600.0, "GRAVITATIONAL", 1.0),
        )

        if (!WaveMath.isTestMode) {
            startHeatwaveSimulation()
            startMissionTicker()
        }
        generateInitialFallbackAnomalies(startLat, startLng)
    }

    fun setTrackedMissionId(id: String?) {
        _trackedMissionId.value = id
        if (id != null) {
            addLog("LOCKED_TARGET: Coupled target ID $id")
        } else {
            addLog("LOCKED_TARGET: Released lock channels.")
        }
    }

    fun setSelectedAnomalyId(id: String?) {
        _selectedAnomalyId.value = id
    }

    fun setScannerSubTab(subTab: String) {
        _scannerSubTab.value = subTab
        if (subTab == "radar" || subTab == "list") {
            val currentRaw = _rawAnomalies.value
            if (currentRaw.isNotEmpty()) {
                val updated = currentRaw.map { anom ->
                    WaveMath.updateAnomalyWithLifecycle(anom, WaveMath.getAdjustedTime())
                }
                _rawAnomalies.value = updated
                for ((secKey, secList) in _sectorAnomalies) {
                    _sectorAnomalies[secKey] = secList.map { anom ->
                        WaveMath.updateAnomalyWithLifecycle(anom, WaveMath.getAdjustedTime())
                    }
                }
            } else {
                generateInitialFallbackAnomalies(_latitude.value, _longitude.value)
            }
        }
    }

    fun setListSubTab(subTab: String) {
        _listSubTab.value = subTab
    }

    fun toggleAnomaliesOnMap() {
        _showAnomaliesOnMap.value = !_showAnomaliesOnMap.value
    }

    fun toggleFontsOnMap() {
        _showFontsOnMap.value = !_showFontsOnMap.value
    }

    fun toggleWellsOnMap() {
        _showWellsOnMap.value = !_showWellsOnMap.value
    }

    private var currentPanWorldX: Float = 0f
    private var currentPanWorldY: Float = 0f

    fun updateCameraPan(panWorldX: Float, panWorldY: Float) {
        currentPanWorldX = panWorldX
        currentPanWorldY = panWorldY

        val userLat = _latitude.value
        val userLng = _longitude.value
        val cosLat = kotlin.math.cos(Math.toRadians(userLat)).coerceAtLeast(0.01)
        val effectiveLat = userLat - (panWorldY / 111000.0)
        val effectiveLng = userLng + (panWorldX / (cosLat * 111000.0))
        tileStreamManager.updateCenter(effectiveLat, effectiveLng, radius = TileStreamManager.DEFAULT_RADIUS)
    }

    fun updateLocation(lat: Double, lng: Double) {
        _latitude.value = lat
        _longitude.value = lng
        if (_rawAnomalies.value.isEmpty()) {
            generateInitialFallbackAnomalies(lat, lng)
        }
        val cosLat = kotlin.math.cos(Math.toRadians(lat)).coerceAtLeast(0.01)
        val effectiveLat = lat - (currentPanWorldY / 111000.0)
        val effectiveLng = lng + (currentPanWorldX / (cosLat * 111000.0))
        tileStreamManager.updateCenter(effectiveLat, effectiveLng, radius = TileStreamManager.DEFAULT_RADIUS)
    }

    fun updateZoom(zoom: Float) {
        _zoomMultiplier.value = zoom
    }

    fun refreshMap() {
        val userLat = _latitude.value
        val userLng = _longitude.value
        val cosLat = kotlin.math.cos(Math.toRadians(userLat)).coerceAtLeast(0.01)
        val targetLat = userLat - (currentPanWorldY / 111000.0)
        val targetLng = userLng + (currentPanWorldX / (cosLat * 111000.0))
        tileStreamManager.updateCenter(targetLat, targetLng, radius = TileStreamManager.DEFAULT_RADIUS)
    }

    fun clearMapCache() {
        viewModelScope.launch(Dispatchers.IO) {
            _sectorAnomalies.clear()
            _spawnedSectorsWithGeometry.clear()
            _rawAnomalies.value = emptyList()
            tileStreamManager.updateCenter(_latitude.value, _longitude.value, radius = TileStreamManager.DEFAULT_RADIUS)
            addLog("MAP: Reset tile stream and anomalies.")
        }
    }

    private fun startHeatwaveSimulation() {
        heatwaveJob?.cancel()
        heatwaveJob = viewModelScope.launch(dispatchers.default) {
            val baseCities = listOf(
                Pair(37.4220, -122.0841), // Palo Alto
                Pair(40.7128, -74.0060), // New York
                Pair(51.5074, -0.1278), // London
                Pair(35.6762, 139.6503), // Tokyo
                Pair(-33.8688, 151.2093), // Sydney
            )
            var ticks = 0L
            while (isActive) {
                val list = mutableListOf<String>()
                baseCities.forEach { (baseLat, baseLng) ->
                    val cellX = Math.floor(baseLat / 0.015).toInt()
                    val cellY = Math.floor(baseLng / 0.015).toInt()

                    list.add("$cellX,$cellY")

                    val offset1X = (kotlin.math.sin(ticks * 0.1 + cellX) * 2).toInt()
                    val offset1Y = (kotlin.math.cos(ticks * 0.1 + cellY) * 2).toInt()
                    list.add("${cellX + offset1X},${cellY + offset1Y}")

                    val offset2X = (kotlin.math.cos(ticks * 0.07 - cellX) * 3).toInt()
                    val offset2Y = (kotlin.math.sin(ticks * 0.07 - cellY) * 3).toInt()
                    list.add("${cellX + offset2X},${cellY + offset2Y}")
                }
                _proceduralHeatwaves.value = list
                ticks++
                delay(3000)
            }
        }
    }

    private fun generateInitialFallbackAnomalies(lat: Double, lng: Double) {
        val baseAnomaly = PoxAnomaly(
            id = "ANM-TEST-CLASS-M",
            name = "Class M Conduit-Creeper",
            lat = lat,
            lng = lng,
            gene = "XZYW?!$%",
            faction = "Anomaly",
            distance = 0.0,
            heatZoneDiameter = 100.0 * 3.28084,
            density = 0.0,
            peakAltitudeMeters = 3.5f,
            spectralClass = AnomalySpectralClass.CLASS_M,
        )
        val testMetrics = WaveMath.calculateAnomalyLifecycleForProgress(0.425f, baseSummitMeters = 3.5f)
        val lifecycleAnomaly = WaveMath.updateAnomalyWithLifecycleMetrics(baseAnomaly, testMetrics)
        _rawAnomalies.value = listOf(lifecycleAnomaly)
    }

    fun getSynodicResonanceMod(faction: String, phaseName: String): Int = DeploymentEngine.getSynodicResonanceMod(faction, phaseName)

    fun hasCoherenceShield(creature: Creature): Boolean = harvestMissionManager.hasCoherenceShield(creature)

    fun dispatchMission(creature: Creature, anomaly: PoxAnomaly) {
        harvestMissionManager.dispatchMission(viewModelScope, creature, anomaly)
    }

    fun recallMission(mission: HarvestMission) {
        harvestMissionManager.recallMission(viewModelScope, mission)
    }

    fun recallAllActiveMissions() {
        harvestMissionManager.recallAllActiveMissions(viewModelScope)
    }

    private fun startMissionTicker() {
        missionTickerJob?.cancel()
        missionTickerJob = viewModelScope.launch(dispatchers.default) {
            while (isActive) {
                delay(1000L)
                val currentRaw = _rawAnomalies.value
                if (currentRaw.isNotEmpty()) {
                    val updated = currentRaw.map { anom ->
                        WaveMath.updateAnomalyWithLifecycle(anom, WaveMath.getAdjustedTime())
                    }
                    _rawAnomalies.value = updated
                    for ((secKey, secList) in _sectorAnomalies) {
                        _sectorAnomalies[secKey] = secList.map { anom ->
                            WaveMath.updateAnomalyWithLifecycle(anom, WaveMath.getAdjustedTime())
                        }
                    }
                }

                harvestMissionManager.tickMissions(activeMissions.value)
            }
        }
    }

    private fun calculateDistanceInFeet(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double = com.example.genpox.data.GeoMath.calculateHaversineDistanceInFeet(lat1, lng1, lat2, lng2)
}
